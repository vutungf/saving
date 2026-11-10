# Cài đặt Firebase cho tính năng đồng bộ Google

Chưa làm các bước này thì app vẫn build và chạy bình thường, chỉ có tùy chọn *Đồng bộ với tài khoản Google* bị ẩn (báo "chưa được cấu hình Firebase"). Gói miễn phí Spark là đủ, **không cần thẻ thanh toán**.

## 1. Tạo project

1. Vào <https://console.firebase.google.com> và đăng nhập tài khoản Google.
2. Bấm **Create a project** (Tạo dự án), đặt tên ví dụ `so-tiet-kiem`. Google Analytics: tắt cũng được.
3. Không cần nâng cấp gói. Đừng liên kết tài khoản thanh toán, để project luôn ở gói miễn phí.

## 2. Thêm app Android

1. Trong trang project, bấm biểu tượng **Android** để thêm app.
2. **Android package name:** `vn.sotietkiem.app` (phải đúng như `applicationId` trong `app/build.gradle.kts`).
3. **Debug signing certificate SHA-1:** bắt buộc cho đăng nhập Google. Lấy bằng một trong hai cách:
   - Android Studio: mở tab **Gradle** ở cạnh phải → `app` → `Tasks` → `android` → chạy **signingReport**, chép dòng `SHA1` của biến thể `debug`.
   - Dòng lệnh trong thư mục project: `./gradlew signingReport`.
4. Bấm **Register app**, rồi **Download google-services.json**.
5. Chép file đó vào thư mục **`app/`** của project (cùng chỗ với `app/build.gradle.kts`).

> Bản release hiện ký bằng khóa debug nên dùng chung SHA-1 trên. Mỗi máy tính có khóa debug riêng: build trên máy khác (hoặc trên GitHub Actions) thì phải thêm SHA-1 của máy đó trong **Project settings → Your apps → Add fingerprint**, nếu không đăng nhập Google sẽ báo lỗi. Khi tạo khóa phát hành riêng, nhớ thêm cả SHA-1 của khóa đó.

## 3. Bật đăng nhập Google

1. Menu trái: **Build → Authentication** → **Get started**.
2. Tab **Sign-in method** → chọn **Google** → bật **Enable**, chọn email hỗ trợ → **Save**.
3. Tải lại `google-services.json` (Project settings → Your apps → tải lại) và chép đè vào `app/`. Bước này cần để file có mã client của đăng nhập Google (`default_web_client_id`).

## 4. Tạo Cloud Firestore

1. Menu trái: **Build → Firestore Database** → **Create database**.
2. Chọn vị trí gần Việt Nam, ví dụ `asia-southeast1 (Singapore)`. Vị trí không đổi được sau này.
3. Chọn **Start in production mode**.
4. Vào tab **Rules**, xóa nội dung cũ, dán toàn bộ nội dung file [`firestore.rules`](firestore.rules) trong project này, bấm **Publish**.

## 5. Build và thử

1. Android Studio: **File → Sync Project with Gradle Files**, rồi **Run**.
2. Trong app: **Cài đặt → Lưu trữ và đồng bộ → Đồng bộ với tài khoản Google** (hoặc chọn ở màn chào lần đầu).
3. Chọn tài khoản Google, tạo **mật khẩu đồng bộ** (ít nhất 8 ký tự).
4. Trên Firebase Console → Firestore → Data sẽ thấy `users/{mã người dùng}` với các trường `salt`, `check`, `data`… Nội dung `data` là chuỗi đã mã hóa, không đọc được.

## 6. APK build trên GitHub Actions

Để APK tải từ tab Actions đăng nhập Google được, GitHub phải ký app bằng **cùng khóa debug** với máy của bạn (cùng SHA-1 đã thêm ở bước 2):

1. Trên máy Windows, mở PowerShell và chạy (chép khóa dưới dạng chữ vào clipboard):
   ```powershell
   [Convert]::ToBase64String([IO.File]::ReadAllBytes("$env:USERPROFILE\.android\debug.keystore")) | Set-Clipboard
   ```
2. GitHub → repo → **Settings → Secrets and variables → Actions → New repository secret**. Tên: `DEBUG_KEYSTORE_BASE64`, dán nội dung vừa chép, **Add secret**.
3. Push hoặc bấm **Run workflow**. Trong log bước *Restore debug keystore* sẽ in SHA-1; phải trùng SHA-1 trên Firebase.

Chưa có secret thì workflow vẫn build, kèm cảnh báo, nhưng APK đó không đăng nhập Google được.

## Dữ liệu được lưu thế nào

| Trường | Ý nghĩa |
|---|---|
| `salt`, `iter` | Muối và số vòng PBKDF2 để suy khóa từ mật khẩu đồng bộ (không bí mật) |
| `check` | Chuỗi mẫu đã mã hóa, để kiểm tra mật khẩu đúng hay sai |
| `data` | Toàn bộ sổ, người sở hữu, mục tiêu, danh sách đã xóa, mã hóa AES-256-GCM |
| `device`, `updatedAt` | Máy ghi gần nhất và thời điểm ghi |

Mật khẩu đồng bộ không bao giờ được gửi lên. **Quên mật khẩu đồng bộ thì không ai mở được dữ liệu trên mây**, kể cả bạn; dữ liệu trên các máy đang dùng vẫn còn.

## Có tốn tiền không

Với gói Spark (miễn phí): 1 GiB lưu trữ, 50.000 lượt đọc và 20.000 lượt ghi mỗi ngày. Mỗi người dùng app chỉ có 1 tài liệu vài chục KB, mỗi lần đồng bộ khoảng 1 lượt đọc và 1 lượt ghi, nên dùng thoải mái. Vượt hạn mức thì Firebase tạm dừng chứ không trừ tiền, vì project không liên kết thẻ.

## Lỗi thường gặp

| Lỗi | Cách sửa |
|---|---|
| Cài đặt báo "chưa được cấu hình Firebase" | Thiếu `app/google-services.json`, hoặc chưa Sync Gradle sau khi chép file |
| "Thiếu cấu hình đăng nhập Google trong Firebase" | Chưa bật Google ở Authentication, hoặc chưa tải lại `google-services.json` sau khi bật |
| Đăng nhập báo lỗi `10` / `DEVELOPER_ERROR` / không có tài khoản nào | SHA-1 của máy build chưa được thêm vào Firebase |
| "Không kết nối được Firebase: PERMISSION_DENIED" | Chưa dán `firestore.rules` hoặc chưa bấm Publish |
| Điện thoại không có Google Play Services | Không đăng nhập Google được; dùng chế độ chỉ lưu trên máy |
