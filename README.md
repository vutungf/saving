# Sổ tiết kiệm (Android)

App Android native (Kotlin + Jetpack Compose) theo spec trong `Claude.md`: theo dõi sổ tiết kiệm ở ngân hàng Việt Nam, tính lãi, ngày đáo hạn và thống kê. Dữ liệu chỉ nằm trên máy, sao lưu bằng file JSON cùng định dạng với bản web.

## Build

**Android Studio:** File → Open → chọn thư mục này → đợi Gradle sync → Run.

**Dòng lệnh** (cần JDK 17 và Android SDK, `ANDROID_HOME` đã đặt):

```bash
./gradlew testDebugUnitTest    # chạy unit test phần tính lãi và nhập JSON
./gradlew assembleDebug        # app/build/outputs/apk/debug/app-debug.apk
./gradlew assembleRelease      # app/build/outputs/apk/release/app-release.apk (ký bằng khóa debug)
```

**GitHub Actions:** mỗi lần push, workflow `.github/workflows/android.yml` build APK. Tải ở tab Actions → lần chạy mới nhất → Artifacts → `so-tiet-kiem-apk`.

Bản release đang ký bằng khóa debug để cài thử được ngay. Trước khi đưa lên Google Play, tạo keystore riêng và sửa `signingConfig` trong `app/build.gradle.kts`.

## Cấu trúc

```
app/src/main/java/vn/sotietkiem/
  MainActivity.kt            điều hướng 3 tab + form, đồng hồ 1 giây cho lãi tích lũy
  data/Account.kt            model sổ + file sao lưu (khớp JSON mục 4 của spec)
  data/Banks.kt              58 ngân hàng, logo ở assets/banks/<mã>.webp
  data/Repository.kt         lưu file stk_v1.json, nhập/xuất JSON, kiểm tra dữ liệu nhập
  domain/Calc.kt             ngày đáo hạn, lãi đơn, trạng thái, tổng quan, biểu đồ
  util/Format.kt             định dạng VND, đọc số bằng chữ, tìm kiếm không dấu
  notify/MaturityWorker.kt   nhắc trước 3 ngày và vào ngày đáo hạn (WorkManager, chạy trên máy)
  ui/screens/                Tổng quan, Sổ của tôi, Form thêm/sửa/tái tục, Sao lưu
  ui/components/             thẻ sổ, logo, biểu đồ tròn và cột, hộp thoại
```

## Khác biệt so với bản web

| Bản web | Bản Android |
|---|---|
| `localStorage` khóa `stk:v1` | File `stk_v1.json` trong bộ nhớ riêng của app (được Android Auto Backup sao lưu cùng tài khoản Google) |
| Logo base64 nhúng trong HTML | Logo WebP trong `assets/banks` (dữ liệu VietQR, qua gói npm `vietqr-zero`, MIT) |
| Font tải từ Google Fonts | Be Vietnam Pro nhúng sẵn trong `res/font` (SIL OFL), chạy offline hoàn toàn |
| Kéo thả file để nhập | Chọn file, dán JSON, hoặc chia sẻ |
| — | Thông báo nhắc đáo hạn (mục 7 của spec) |

Ngày đáo hạn được tính là *Đã đáo hạn* từ chính ngày đáo hạn trở đi (đã đến hạn rút).
