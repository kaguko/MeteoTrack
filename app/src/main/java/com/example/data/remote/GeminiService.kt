package com.example.data.remote

import android.util.Log
import com.example.BuildConfig
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object GeminiService {
    private const val TAG = "GeminiService"
    private const val MODEL = "gemini-2.5-flash"
    private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    suspend fun getPlaceRecommendationsForWeather(
        locationName: String,
        latitude: Double,
        longitude: Double,
        temperature: Double,
        weatherDescription: String,
        isRaining: Boolean
    ): String = withContext(Dispatchers.IO) {
        val apiKey = try {
            BuildConfig.GEMINI_API_KEY
        } catch (e: Throwable) {
            ""
        }

        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext getOfflineSmartSuggestions(
                locationName = locationName,
                temperature = temperature,
                weatherDesc = weatherDescription,
                isRaining = isRaining
            )
        }

        val prompt = """
            Bạn là trợ lý thời tiết & du lịch địa phương thông minh.
            Vị trí hiện tại: $locationName (Tọa độ: $latitude, $longitude).
            Thời tiết hiện tại: $temperature°C, $weatherDescription ${if (isRaining) "(Đang có mưa)" else "(Không mưa)"}.
            
            Hãy sử dụng dữ liệu Google Maps để đưa ra:
            1. Tóm tắt nhanh về tác động của thời tiết này đến việc di chuyển & sức khỏe.
            2. Gợi ý 3 địa điểm thực tế phù hợp nhất quanh khu vực này trong điều kiện thời tiết này (ví dụ: quán cà phê ấm cúng/trú mưa, bảo tàng/trung tâm thương mại trong nhà nếu mưa, hoặc công viên/điểm ngắm cảnh ngoài trời nếu nắng đẹp).
            3. Lời khuyên trang phục và vật dụng cần mang theo.
            
            Trả lời ngắn gọn, lịch sự, có emoji trực quan bằng tiếng Việt.
        """.trimIndent()

        val jsonBody = JSONObject().apply {
            val contentsArr = JSONArray().apply {
                val contentObj = JSONObject().apply {
                    val partsArr = JSONArray().apply {
                        put(JSONObject().apply { put("text", prompt) })
                    }
                    put("parts", partsArr)
                }
                put(contentObj)
            }
            put("contents", contentsArr)

            // Add tools with googleSearch or googleMaps grounding
            val toolsArr = JSONArray().apply {
                put(JSONObject().apply {
                    put("googleMaps", JSONObject())
                })
            }
            put("tools", toolsArr)
        }

        val url = "https://generativelanguage.googleapis.com/v1beta/models/$MODEL:generateContent?key=$apiKey"
        val request = Request.Builder()
            .url(url)
            .post(jsonBody.toString().toRequestBody(JSON_MEDIA_TYPE))
            .build()

        try {
            val response = client.newCall(request).execute()
            val responseStr = response.body?.string() ?: ""
            if (!response.isSuccessful) {
                Log.w(TAG, "Gemini call returned ${response.code}: $responseStr")
                // Fallback attempt without tools if tools wasn't supported
                return@withContext retryWithoutTools(apiKey, prompt, locationName, temperature, weatherDescription, isRaining)
            }

            val jsonRes = JSONObject(responseStr)
            val candidates = jsonRes.optJSONArray("candidates")
            val firstCandidate = candidates?.optJSONObject(0)
            val content = firstCandidate?.optJSONObject("content")
            val parts = content?.optJSONArray("parts")
            val textBuilder = StringBuilder()
            if (parts != null) {
                for (i in 0 until parts.length()) {
                    val part = parts.getJSONObject(i)
                    val text = part.optString("text")
                    if (text.isNotBlank()) {
                        textBuilder.append(text).append("\n")
                    }
                }
            }

            if (textBuilder.isNotBlank()) {
                textBuilder.toString().trim()
            } else {
                getOfflineSmartSuggestions(locationName, temperature, weatherDescription, isRaining)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Gemini call failed", e)
            getOfflineSmartSuggestions(locationName, temperature, weatherDescription, isRaining)
        }
    }

    private fun retryWithoutTools(
        apiKey: String,
        prompt: String,
        locationName: String,
        temperature: Double,
        weatherDescription: String,
        isRaining: Boolean
    ): String {
        return try {
            val jsonBody = JSONObject().apply {
                val contentsArr = JSONArray().apply {
                    val contentObj = JSONObject().apply {
                        val partsArr = JSONArray().apply {
                            put(JSONObject().apply { put("text", prompt) })
                        }
                        put("parts", partsArr)
                    }
                    put(contentObj)
                }
                put("contents", contentsArr)
            }

            val url = "https://generativelanguage.googleapis.com/v1beta/models/$MODEL:generateContent?key=$apiKey"
            val request = Request.Builder()
                .url(url)
                .post(jsonBody.toString().toRequestBody(JSON_MEDIA_TYPE))
                .build()

            val response = client.newCall(request).execute()
            val responseStr = response.body?.string() ?: ""
            val jsonRes = JSONObject(responseStr)
            val candidates = jsonRes.optJSONArray("candidates")
            val first = candidates?.optJSONObject(0)?.optJSONObject("content")?.optJSONArray("parts")?.optJSONObject(0)
            val result = first?.optString("text")
            if (!result.isNullOrBlank()) result else getOfflineSmartSuggestions(locationName, temperature, weatherDescription, isRaining)
        } catch (e: Exception) {
            getOfflineSmartSuggestions(locationName, temperature, weatherDescription, isRaining)
        }
    }

    private fun getOfflineSmartSuggestions(
        locationName: String,
        temperature: Double,
        weatherDesc: String,
        isRaining: Boolean
    ): String {
        return if (isRaining) {
            """
                ☔ **Tóm tắt tại $locationName:**
                Thời tiết $temperature°C, $weatherDesc. Đường trơn ướt và tầm nhìn hạn chế.
                
                🏛️ **Gợi ý địa điểm thích hợp (Trong nhà & Trú mưa):**
                1. ☕ **Quán cà phê không gian yên tĩnh**: Thích hợp ngồi làm việc, thư giãn ngắm mưa ấm cúng.
                2. 🛍️ **Trung tâm thương mại / Siêu thị lớn**: Không gian trong nhà rộng rãi, tích hợp ăn uống & giải trí không lo ướt.
                3. 📚 **Nhà sách hoặc Bảo tàng nghệ thuật**: Trải nghiệm văn hóa thanh bình khi trời mưa.
                
                🧥 **Lưu ý trang phục:**
                - Luôn mang theo áo mưa bộ hoặc ô dù chống gió giật.
                - Mang bọc giày đi mưa hoặc giày chống thấm.
                - Giảm tốc độ xe khi qua các vạch kẻ đường trơn trượt.
            """.trimIndent()
        } else if (temperature >= 33.0) {
            """
                ☀️ **Tóm tắt tại $locationName:**
                Thời tiết $temperature°C, $weatherDesc. Nắng gắt và chỉ số UV cao.
                
                🌳 **Gợi ý địa điểm mát mẻ:**
                1. 🥤 **Quán trà sữa / Cà phê máy lạnh**: Tránh nắng đỉnh điểm từ 11h - 15h.
                2. 🌳 **Công viên nhiều cây xanh hoặc Hồ nước**: Đi dạo mát vào cuối giờ chiều khi nhiệt độ hạ.
                3. 🎬 **Rạp chiếu phim hoặc khu tổ hợp trong nhà**: Tránh nóng hiệu quả.
                
                🧢 **Lưu ý trang phục:**
                - Bôi kem chống nắng SPF 50+, đeo kính râm và áo khoác chống tia cực tím.
                - Uống đủ từ 2 - 2.5 lít nước trong ngày để tránh mất nước.
            """.trimIndent()
        } else {
            """
                🌤️ **Tóm tắt tại $locationName:**
                Thời tiết $temperature°C, $weatherDesc. Khí hậu rất dễ chịu và lý tưởng cho các hoạt động di chuyển.
                
                🚶 **Gợi ý điểm đến:**
                1. 🏞️ **Khu phố đi bộ hoặc Quảng trường trung tâm**: Thích hợp dạo phố, chụp ảnh ngoài trời.
                2. 🍰 **Quán cà phê sân thượng (Rooftop / Outdoor)**: Tận hưởng không khí thoáng đãng và ngắm cảnh phố.
                3. 🚴 **Tuyến đường ven hồ hoặc công viên sinh thái**: Rất tốt cho việc tập thể dục hoặc đạp xe dã ngoại.
                
                👕 **Lưu ý trang phục:**
                - Trang phục thoáng mát, năng động.
                - Mang theo áo khoác nhẹ khi di chuyển bằng xe máy lúc sáng sớm hoặc tối muộn.
            """.trimIndent()
        }
    }
}
