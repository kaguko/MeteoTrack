package com.example.data.remote

import android.util.Log
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale
import java.util.concurrent.TimeUnit

/** Result of a suggestion request. [isOffline] is true when the built-in tips were used instead of Gemini. */
data class AiInsights(val text: String, val isOffline: Boolean)

/**
 * Optional "what should I do in this weather" suggestions.
 *
 * With a `GEMINI_API_KEY` the request goes to Gemini (with Google Maps grounding when available);
 * without one, or when the call fails, curated offline tips are returned and flagged as such so
 * the UI never presents them as live AI output.
 *
 * NOTE: a key compiled into BuildConfig can be extracted from the APK. For a public release,
 * leave it empty or route the call through your own backend.
 */
object GeminiService {
    private const val TAG = "GeminiService"
    private const val MODEL = "gemini-2.5-flash"
    private const val ENDPOINT = "https://generativelanguage.googleapis.com/v1beta/models/$MODEL:generateContent"
    private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    val isConfigured: Boolean
        get() = apiKey().let { it.isNotBlank() && it != "MY_GEMINI_API_KEY" }

    private fun apiKey(): String = try {
        BuildConfig.GEMINI_API_KEY
    } catch (e: Throwable) {
        ""
    }

    suspend fun getPlaceRecommendationsForWeather(
        locationName: String,
        latitude: Double,
        longitude: Double,
        temperature: Double,
        weatherDescription: String,
        isRaining: Boolean
    ): AiInsights = withContext(Dispatchers.IO) {
        val offline = {
            AiInsights(offlineSuggestions(locationName, temperature, weatherDescription, isRaining), isOffline = true)
        }
        if (!isConfigured) return@withContext offline()

        val prompt = """
            Bạn là trợ lý thời tiết & du lịch địa phương thông minh.
            Vị trí hiện tại: $locationName (Tọa độ: $latitude, $longitude).
            Thời tiết hiện tại: ${"%.1f".format(Locale.US, temperature)}°C, $weatherDescription ${if (isRaining) "(Đang có mưa)" else "(Không mưa)"}.

            Hãy sử dụng dữ liệu Google Maps để đưa ra:
            1. Tóm tắt nhanh về tác động của thời tiết này đến việc di chuyển & sức khỏe.
            2. Gợi ý 3 địa điểm thực tế phù hợp nhất quanh khu vực này trong điều kiện thời tiết này.
            3. Lời khuyên trang phục và vật dụng cần mang theo.

            Trả lời ngắn gọn, lịch sự, bằng tiếng Việt. Dùng gạch đầu dòng, không dùng bảng.
        """.trimIndent()

        // Try with Maps grounding first, then plain generation, then offline tips.
        val text = request(prompt, withMaps = true) ?: request(prompt, withMaps = false)
        if (text.isNullOrBlank()) offline() else AiInsights(text, isOffline = false)
    }

    private fun request(prompt: String, withMaps: Boolean): String? {
        val body = JSONObject().apply {
            put("contents", JSONArray().put(JSONObject().put("parts", JSONArray().put(JSONObject().put("text", prompt)))))
            if (withMaps) put("tools", JSONArray().put(JSONObject().put("googleMaps", JSONObject())))
        }
        val request = Request.Builder()
            .url(ENDPOINT)
            // Header instead of ?key= so the key never ends up in URLs or logs.
            .header("x-goog-api-key", apiKey())
            .post(body.toString().toRequestBody(JSON_MEDIA_TYPE))
            .build()

        return try {
            client.newCall(request).execute().use { response ->
                val raw = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    Log.w(TAG, "Gemini call (maps=$withMaps) returned ${response.code}")
                    return null
                }
                val parts = JSONObject(raw)
                    .optJSONArray("candidates")?.optJSONObject(0)
                    ?.optJSONObject("content")?.optJSONArray("parts")
                buildString {
                    if (parts != null) for (i in 0 until parts.length()) {
                        val t = parts.optJSONObject(i)?.optString("text").orEmpty()
                        if (t.isNotBlank()) append(t).append('\n')
                    }
                }.trim().ifBlank { null }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Gemini call failed", e)
            null
        }
    }

    private fun offlineSuggestions(
        locationName: String,
        temperature: Double,
        weatherDesc: String,
        isRaining: Boolean
    ): String {
        val temp = "%.0f".format(Locale.US, temperature)
        return when {
            isRaining -> """
                **Tóm tắt tại $locationName**
                $temp°C, $weatherDesc. Đường trơn và tầm nhìn hạn chế.

                **Nên đến**
                - Quán cà phê yên tĩnh để trú mưa, làm việc hoặc thư giãn.
                - Trung tâm thương mại, siêu thị: không gian trong nhà, đủ ăn uống và giải trí.
                - Nhà sách hoặc bảo tàng.

                **Trang phục & vật dụng**
                - Áo mưa bộ hoặc ô chống gió.
                - Giày chống thấm, mang thêm tất khô.
                - Giảm tốc độ khi đi qua vạch kẻ đường và nắp cống.
            """.trimIndent()

            temperature >= 33.0 -> """
                **Tóm tắt tại $locationName**
                $temp°C, $weatherDesc. Nắng gắt, chỉ số UV có thể cao.

                **Nên đến**
                - Quán cà phê hoặc trà sữa có máy lạnh, tránh nắng từ 11h đến 15h.
                - Công viên nhiều cây xanh hoặc ven hồ vào cuối chiều.
                - Rạp chiếu phim hoặc khu vui chơi trong nhà.

                **Trang phục & vật dụng**
                - Kem chống nắng SPF 50+, kính râm, áo khoác chống tia UV.
                - Uống đủ 2 – 2,5 lít nước trong ngày.
            """.trimIndent()

            else -> """
                **Tóm tắt tại $locationName**
                $temp°C, $weatherDesc. Thời tiết dễ chịu cho các hoạt động ngoài trời.

                **Nên đến**
                - Phố đi bộ hoặc quảng trường trung tâm để dạo phố, chụp ảnh.
                - Quán cà phê sân thượng hoặc ngoài trời.
                - Đường ven hồ, công viên sinh thái để đi bộ hoặc đạp xe.

                **Trang phục & vật dụng**
                - Trang phục thoáng, năng động.
                - Mang áo khoác mỏng cho buổi sáng sớm hoặc tối muộn.
            """.trimIndent()
        }
    }
}
