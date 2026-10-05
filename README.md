# 🌤️ MeteoTrack - Ứng dụng Theo Dõi Thời Tiết Khi Di Chuyển

**MeteoTrack** là ứng dụng Android hiện đại được xây dựng hoàn toàn bằng **Kotlin** và **Jetpack Compose** (Material Design 3). Trọng tâm của ứng dụng là **tự động phát hiện di chuyển qua GPS** để làm mới dự báo thời tiết khi người dùng đi xa (mặc định > 5km hoặc sau 30 phút), đồng thời ghi lại nhật ký hành trình di chuyển và hỗ trợ cảnh báo thời tiết cá nhân hóa.

---

## 📑 Mục lục
1. [Tổng quan & Điểm nổi bật](#-tổng-quan--điểm-nổi-bật)
2. [Chi tiết tính năng đã thực hiện](#-chi-tiết-tính-năng-đã-thực-hiện)
   - [🔴 Nhóm tính năng cốt lõi (MVP)](#1-nhóm-tính-năng-cốt-lõi-mvp)
   - [🟡 Nhóm tính năng cá nhân hóa](#2-nhóm-tính-năng-cá-nhân-hóa)
   - [🟢 Nhóm tính năng nâng cao & Trải nghiệm](#3-nhóm-tính-năng-nâng-cao--trải-nghiệm)
3. [Cấu trúc tổ chức thư mục & Files](#-cấu-trúc-tổ-chức-thư-mục--files)
4. [Kiến trúc kỹ thuật & Thư viện sử dụng](#-kiến-trúc-kỹ-thuật--thư-viện-sử-dụng)
5. [Hướng dẫn sử dụng & Kiểm thử](#-hướng-dẫn-sử-dụng--kiểm-thử)

---

## 🌟 Tổng quan & Điểm nổi bật

- **API Thời tiết thời gian thực**: Sử dụng **Open-Meteo API** (chuẩn WMO quốc tế, hoàn toàn miễn phí, không cần API key, phản hồi nhanh chóng).
- **Định vị & Theo dõi di chuyển**: Tích hợp `FusedLocationProviderClient` cùng thuật toán đo khoảng cách địa lý `Location.distanceBetween` để tự động kích hoạt làm mới dữ liệu khi người dùng di chuyển ra khỏi vùng vị trí cũ.
- **Lưu trữ dữ liệu cục bộ (Room Database)**: Lưu trữ các địa điểm yêu thích, các ngưỡng cảnh báo thời tiết, và toàn bộ nhật ký di chuyển (Travel History).
- **Gợi ý địa điểm thông minh (Maps Grounding & Gemini AI)**: Tích hợp mô hình `gemini-2.5-flash` để phân tích điều kiện thời tiết thực tế và đề xuất các địa điểm trú mưa, quán cà phê, công viên và trang phục phù hợp.
- **Giao diện động (Dynamic Weather Theme)**: Bảng màu và gradient nền tự động chuyển biến theo từng trạng thái thời tiết (Nắng, Mây rải rác, U ám, Mưa phùn, Mưa to, Dông bão, Tuyết, Trời đêm).

---

## 🎯 Chi tiết tính năng đã thực hiện

### 1. Nhóm tính năng cốt lõi (MVP)
| # | Tính năng | Mô tả chi tiết kỹ thuật |
|---|---|---|
| 1 | **Lấy vị trí GPS & Tọa độ thực** | Sử dụng `FusedLocationProviderClient` lấy vị trí hiện tại (lat/lng), kết hợp `android.location.Geocoder` hiển thị tên Quận/Huyện, Tỉnh/Thành phố chuẩn tiếng Việt. |
| 2 | **Thời tiết hiện tại toàn diện** | Hiển thị nhiệt độ lớn, nhiệt độ cảm nhận (*cảm giác như*), biểu tượng emoji, mô tả chi tiết, độ ẩm (%), tốc độ gió, áp suất khí quyển (hPa), lượng mưa. |
| 3 | **Dự báo 24 giờ tới (Hourly)** | Danh sách thẻ cuộn ngang hiển thị nhiệt độ, biểu tượng thời tiết, xác suất có mưa (`💧%`) cho từng giờ trong ngày. |
| 4 | **Dự báo 7 ngày tới (Daily)** | Danh sách 7 ngày hiển thị dải nhiệt độ Min - Max, biểu tượng thời tiết và khả năng mưa. Chạm vào bất kỳ ngày nào để chuyển sang màn hình **Chi tiết ngày**. |
| 5 | **Chi tiết từng ngày (Day Detail)** | Hiển thị chi tiết giờ **Bình minh / Hoàng hôn**, chỉ số **UV cực đại** kèm khuyến cáo sức khỏe, lượng mưa tích lũy (mm) và tốc độ gió giật lớn nhất. |
| 6 | **Tự động làm mới khi di chuyển (Core)** | Giám sát khoảng cách từ tọa độ lấy thời tiết lần trước đến tọa độ hiện tại. Khi khoảng cách $\ge$ ngưỡng cài đặt (mặc định 5km) hoặc thời gian $\ge$ 30 phút, app tự động tải lại thời tiết và ghi vào nhật ký. |
| 7 | **Nút làm mới thủ công (Pull/Tap Refresh)** | Nút làm mới tức thì trên thanh tiêu đề TopAppBar giúp cập nhật bất kỳ lúc nào. |
| 8 | **Xử lý trạng thái & Quyền hạn** | Hộp thoại yêu cầu quyền định vị (`ACCESS_FINE_LOCATION`, `ACCESS_COARSE_LOCATION`) và thông báo (`POST_NOTIFICATIONS`). Khi lỗi mạng hoặc chưa bật GPS, hiển thị giao diện báo lỗi kèm nút **Thử lại**. |

### 2. Nhóm tính năng cá nhân hóa
| # | Tính năng | Mô tả chi tiết kỹ thuật |
|---|---|---|
| 9 | **Tìm kiếm & Địa điểm yêu thích** | Tìm kiếm địa danh toàn cầu qua **Open-Meteo Geocoding API**. Cho phép lưu địa điểm vào Room DB với các nhãn: *Nhà riêng 🏠*, *Công ty 🏢*, *Du lịch ✈️*, *Tùy chỉnh 📍*. Chạm vào để xem thời tiết ngay lập tức. |
| 10 | **Ngưỡng cảnh báo cá nhân** | Người dùng có thể tự định nghĩa các ngưỡng cảnh báo: Nắng nóng (>35°C), Giá rét (<16°C), Nguy cơ mưa to (>70%), Gió giật mạnh (>30 km/h). Bật/tắt hoặc xóa tùy ý. |
| 11 | **Thông báo thông minh (Notifications)** | Tạo `NotificationChannel` trên Android. Khi dữ liệu thời tiết mới tải về thỏa mãn ngưỡng cảnh báo, ứng dụng phát thông báo rung và chuông đến thiết bị. Có sẵn nút **"Thử thông báo"** để kiểm tra ngay. |
| 12 | **Nhật ký di chuyển (Travel History)** | Bảng `travel_history` trong Room lưu lại lịch sử: tên địa điểm, tọa độ, nhiệt độ, thời tiết lúc ghé thăm, khoảng cách di chuyển từ điểm trước (`+5.5 km`), thời gian ghi nhận. Có nút xóa toàn bộ nhật ký. |
| 13 | **Gợi ý AI & Google Maps Grounding** | Nút *"Gợi ý AI"* mở bảng tổng hợp gợi ý địa điểm thực tế theo điều kiện thời tiết (quán cà phê trú mưa, trung tâm thương mại trong nhà, công viên thoáng mát, lời khuyên trang phục). |

### 3. Nhóm tính năng nâng cao & Trải nghiệm
| # | Tính năng | Mô tả chi tiết kỹ thuật |
|---|---|---|
| 14 | **Dynamic Theme theo thời tiết** | Tự động chuyển đổi màu gradient nền mượt mà theo mã WMO Weather Code: Nắng vàng trời xanh, Mây rải rác, U ám xám xanh, Mưa xám đậm, Dông sét tím sẫm, Đêm sao tím than. |
| 15 | **Biểu đồ nhiệt độ Canvas** | Tự vẽ đường cong Bezier (`CubicTo`) trực quan trên `Canvas` thể hiện xu hướng nhiệt độ trong 24 giờ, kèm hiệu ứng gradient vùng dưới đường cong và các điểm dữ liệu mốc giờ. |
| 16 | **Cài đặt đơn vị & Ngưỡng di chuyển** | Lưu cấu hình người dùng bằng `DataStore Preferences`: chọn đơn vị nhiệt độ (°C / °F), đơn vị gió (km/h, m/s, mph), khoảng cách tối thiểu (1km, 3km, 5km, 10km), chu kỳ thời gian (15, 30, 60 phút). |
| 17 | **Bộ mô phỏng di chuyển (`+5.5km`)** | *Chỉ có trong bản debug.* Nút *"Thử đi +5,5 km"* cho phép kiểm thử tính năng tự động làm mới và ghi nhật ký di chuyển trực tiếp trên máy ảo mà không cần di chuyển thực tế. |
| 18 | **Adaptive Icon (Material You)** | Thiết kế custom adaptive icon chuẩn Android với logo thời tiết trên nền `#1565C0`, đầy đủ các thư mục density (`mdpi`, `hdpi`, `xhdpi`, `xxhdpi`, `xxxhdpi`). |

---

## 📁 Cấu trúc tổ chức thư mục & Files

Dự án áp dụng mô hình kiến trúc **MVVM (Model-View-ViewModel)** và **Repository Pattern** theo khuyến nghị chính thức của Google:

```
app/src/main/
├── AndroidManifest.xml                  # Khai báo permissions (INTERNET, GPS, NOTIFICATION) và launcher activity
├── java/com/example/
│   ├── MainActivity.kt                  # Activity chính, Scaffold điều hướng 4 tab BottomNavigation và xử lý quyền
│   │
│   ├── data/
│   │   ├── local/                       # Tầng lưu trữ dữ liệu cục bộ (Room Database)
│   │   │   ├── Entities.kt              # Các thực thể: FavoritePlaceEntity, TravelHistoryEntity, WeatherAlertEntity
│   │   │   ├── Daos.kt                  # Các DAO: FavoritePlaceDao, TravelHistoryDao, WeatherAlertDao
│   │   │   └── MeteoDatabase.kt         # Database Room kế thừa RoomDatabase, tự khởi tạo seed dữ liệu mẫu ban đầu
│   │   │
│   │   ├── model/                       # Data models & DTOs
│   │   │   └── WeatherModels.kt         # Open-Meteo DTOs (WeatherResponse, Current, Hourly, Daily),
│   │   │                                # GeocodingResponse, và WeatherCodeMapper (ánh xạ mã WMO -> tiếng Việt & gradient)
│   │   │
│   │   ├── remote/                      # Tầng mạng & Gọi API từ xa
│   │   │   ├── OpenMeteoApi.kt          # Retrofit Interface cho Forecast và Geocoding API
│   │   │   ├── ApiClient.kt             # Khởi tạo Retrofit, OkHttpClient (timeout 30s), MoshiConverter
│   │   │   └── GeminiService.kt         # Gọi Gemini 2.5 Flash REST API kết hợp Maps data recommendations
│   │   │
│   │   └── repository/                  # Tầng Repository trung gian
│   │       ├── WeatherRepository.kt     # Phối hợp giữa API mạng, Room Database, kiểm tra ngưỡng cảnh báo và gửi thông báo
│   │       ├── LocationTracker.kt       # Quản lý GPS FusedLocation, Geocoder tên đường phố, thuật toán đo khoảng cách km
│   │       ├── NotificationHelper.kt    # Khởi tạo NotificationChannel, hiển thị cảnh báo thời tiết trên thanh trạng thái
│   │       └── UserPreferencesRepository.kt # Quản lý cài đặt ứng dụng bằng Jetpack DataStore Preferences
│   │
│   └── ui/
│       ├── theme/                       # Hệ thống Design & Theming Material 3
│       │   ├── Color.kt                 # Bảng màu thời tiết (Trời nắng, Mây mưa, Dông bão, Trời đêm)
│       │   ├── Theme.kt                 # MeteoTrackTheme (hỗ trợ Light / Dark mode)
│       │   └── Type.kt                  # Cấu hình Typography chữ chuẩn Material 3
│       │
│       ├── viewmodel/                   # Tầng ViewModel (State Management)
│       │   └── WeatherViewModel.kt      # Quản lý toàn bộ StateFlow (thời tiết, GPS, địa điểm, cài đặt, cảnh báo)
│       │
│       ├── components/                  # Các UI Components tái sử dụng
│       │   ├── WeatherCurrentCard.kt    # Thẻ thời tiết hiện tại (nhiệt độ to, cảm giác như, độ ẩm, gió, áp suất)
│       │   ├── MovementTrackerCard.kt   # Thẻ đo khoảng cách di chuyển, thanh tiến trình % đến mốc và nút mô phỏng +5.5km
│       │   ├── HourlyForecastRow.kt     # Hàng cuộn ngang dự báo 24 giờ tới kèm xác suất mưa
│       │   ├── TemperatureChart.kt      # Biểu đồ đường cong nhiệt độ mượt mà vẽ trên Canvas
│       │   ├── DailyForecastList.kt     # Danh sách dự báo 7 ngày tới kèm thanh dải nhiệt độ Min - Max
│       │   └── AiWeatherInsightsDialog.kt # Hộp thoại ModalBottomSheet hiển thị gợi ý địa điểm từ Gemini AI
│       │
│       └── screens/                     # Các màn hình chính của ứng dụng
│           ├── HomeScreen.kt            # Màn hình chính (Dynamic gradient, thời tiết tức thời, biểu đồ, 24h & 7 ngày)
│           ├── FavoritesScreen.kt       # Màn hình tìm kiếm địa điểm toàn cầu và quản lý danh sách yêu thích
│           ├── DayDetailScreen.kt       # Màn hình chi tiết của 1 ngày (Bình minh, hoàng hôn, UV, lượng mưa, lời khuyên)
│           ├── AlertsAndHistoryScreen.kt# Màn hình 2 tab: Cài đặt ngưỡng cảnh báo cá nhân & Nhật ký điểm đến di chuyển
│           └── SettingsScreen.kt        # Màn hình cài đặt (°C/°F, km/h, bật/tắt auto-refresh, ngưỡng km di chuyển)
│
└── res/
    ├── drawable/
    │   ├── ic_weather_logo.jpg          # Logo biểu tượng thời tiết tùy biến
    │   ├── ic_launcher_foreground.xml   # Foreground adaptive icon trong safe-zone 66dp
    │   ├── ic_launcher_background.xml   # Nền adaptive icon (#1565C0)
    │   └── weather_hero_banner.jpg      # Banner tranh phong cảnh thời tiết minh họa
    └── mipmap-*/                        # Icon đầy đủ các độ phân giải: mdpi, hdpi, xhdpi, xxhdpi, xxxhdpi
```

---

## 🛠️ Kiến trúc kỹ thuật & Thư viện sử dụng

- **Ngôn ngữ**: Kotlin (100%)
- **UI Toolkit**: Jetpack Compose kết hợp Material Design 3 (M3)
- **Kiến trúc**: MVVM + Repository Pattern, đơn luồng dữ liệu (UDF)
- **Bất đồng bộ & Luồng**: Kotlin Coroutines (`Dispatchers.IO`, `viewModelScope`) + `StateFlow`
- **Lưu trữ CSDL (Local DB)**: Android Jetpack Room (`room-runtime`, `room-ktx`, KSP code generator)
- **Cấu hình người dùng**: Android Jetpack DataStore Preferences
- **Mạng (Networking)**: Retrofit 2 + Moshi Converter + OkHttp Logging Interceptor
- **Định vị & Bản đồ**: Google Play Services Location (`FusedLocationProviderClient`) + Android Geocoder
- **Tải ảnh**: Coil Compose
- **Thông báo**: Android Notification Manager + NotificationChannel (`PRIORITY_HIGH`)
- **Trí tuệ nhân tạo (AI)**: Gemini 2.5 Flash API phục vụ phân tích dữ liệu thời tiết & đề xuất địa điểm (Maps Grounding)

---

## 🚀 Hướng dẫn sử dụng & Kiểm thử

1. **Khởi động ứng dụng**:
   - Khi mở app lần đầu, cấp quyền Vị trí (`Location`) và Thông báo (`Notification`).
   - App sẽ tự động định vị GPS thực tế và tải dữ liệu thời tiết tức thì từ Open-Meteo.
2. **Kiểm tra tính năng tự động làm mới khi di chuyển**:
   - Trên màn hình chính, xem thẻ **"Tự động làm mới khi di chuyển"**.
   - Bấm nút **"Thử đi +5.5km"**: Tọa độ sẽ thay đổi tương đương việc bạn vừa di chuyển 5.5km. Thanh tiến trình đạt 100%, app tự động gửi request lấy thời tiết mới và ghi nhận một dòng vào tab **Nhật ký di chuyển**!
3. **Thêm địa điểm yêu thích**:
   - Chuyển sang tab **"Yêu thích"**, nhập tên thành phố (ví dụ: *Đà Lạt*, *Đà Nẵng*, *Tokyo*).
   - Chọn nhãn (Nhà, Công ty, Du lịch) và bấm **Thêm**. Chạm vào thẻ địa điểm vừa thêm để chuyển màn hình chính xem thời tiết nơi đó.
4. **Cài đặt ngưỡng cảnh báo & Test thông báo**:
   - Chuyển sang tab **"Cảnh báo"**, bấm **"Thêm cảnh báo"** để tạo điều kiện theo dõi nhiệt độ hoặc mưa.
   - Bấm **"Thử thông báo"** để kích hoạt thông báo mẫu xuất hiện trên thanh thông báo của Android.
5. **Xem chi tiết ngày & Biểu đồ**:
   - Chạm vào bất kỳ ngày nào trong danh sách **7 ngày tới** để mở trang chi tiết chỉ số UV, mặt trời mọc/lặn và lời khuyên sinh hoạt.

---

## 📦 Build & phát hành

**Yêu cầu:** JDK 17+, Android SDK 36. Chạy `./gradlew` (wrapper dùng Gradle 9.1.0).

```bash
./gradlew :app:testDebugUnitTest      # unit test (đơn vị, tương phản màu, ánh xạ mã thời tiết…)
./gradlew :app:lintDebug              # lint
./gradlew :app:assembleDebug          # APK debug (có nút mô phỏng di chuyển)
./gradlew :app:assembleRelease        # APK release: bật R8 + shrinkResources (~2,4 MB)
```

**Ký bản release** – không lưu khoá trong repo; truyền qua biến môi trường:

```bash
export KEYSTORE_PATH=/đường/dẫn/upload-key.jks STORE_PASSWORD=... KEY_PASSWORD=... [KEY_ALIAS=upload]
./gradlew :app:bundleRelease          # AAB để đưa lên Google Play
```
Thiếu các biến này thì `assembleRelease` vẫn chạy nhưng cho APK chưa ký.

CI (GitHub Actions) ở `.github/workflows/android.yml` chạy test, lint và build release cho mỗi push / PR.

### ⚠️ Việc cần xử lý trước khi phát hành công khai
1. **Open-Meteo**: gói miễn phí chỉ dành cho *mục đích phi thương mại*. Nếu bán/thu phí hoặc có quảng cáo, cần gói API thương mại (đổi `baseUrl` trong `ApiClient.kt`). Ứng dụng đã ghi nguồn (CC BY 4.0) ở màn hình chính và Cài đặt.
2. **Gemini API key**: khoá đặt trong `.env` sẽ bị nhúng vào APK và có thể bị trích xuất. Để trống (ứng dụng dùng gợi ý ngoại tuyến, có ghi chú rõ cho người dùng) hoặc chuyển lời gọi qua backend của bạn.
3. **Application ID** hiện là `com.aistudio.meteotrack.vwqy` (sinh tự động) – đổi trong `app/build.gradle.kts` *trước* lần đăng đầu tiên, vì không thể đổi sau đó.
4. **Chính sách quyền riêng tư**: Google Play yêu cầu URL chính sách khi dùng quyền vị trí. Nội dung tóm tắt có trong Cài đặt → Quyền riêng tư.
5. Giao diện hiện chỉ có tiếng Việt (chuỗi nằm trong code); muốn đa ngôn ngữ cần chuyển sang `strings.xml`.

## 🎨 Nguyên tắc UX/UI
- **Dễ đọc**: chữ trắng trên nền gradient đạt WCAG AA (có unit test kiểm tra từng màu); thẻ dùng lớp phủ tối thay vì trắng mờ; cỡ chữ tối thiểu 12sp và theo cỡ chữ hệ thống.
- **Dễ tiếp cận**: vùng chạm ≥ 48dp, TalkBack đọc gọn từng thẻ (nhiệt độ, dự báo giờ/ngày…), emoji chỉ là trang trí, công tắc bấm được cả hàng.
- **Không gây khó chịu**: màn chào giải thích lý do xin quyền trước khi hiện hộp thoại hệ thống; có thể bỏ qua; làm mới giữ nguyên dữ liệu cũ thay vì xoay vòng toàn màn hình; kéo xuống để làm mới; dữ liệu lần trước hiện ngay cả khi offline.
- **An toàn thao tác**: xóa địa điểm/cảnh báo có “Hoàn tác”, xóa nhật ký có xác nhận, nhập ngưỡng cảnh báo có kiểm tra hợp lệ.
- **Tablet/xoay ngang**: nội dung giới hạn 640dp và căn giữa.
