# Sổ tiết kiệm (Android)

App Android native (Kotlin + Jetpack Compose). App theo dõi các sổ tiết kiệm ở ngân hàng Việt Nam, tính tiền lãi, ngày đáo hạn và thống kê, kể cả thống kê riêng theo từng người sở hữu. Dữ liệu chỉ nằm trên máy và được sao lưu bằng file JSON. File JSON tương thích với bản web một file HTML, có thêm phần người sở hữu và mục tiêu.

- **Tiền tệ:** chỉ dùng VND.
- **Lưu trữ:** chỉ trên điện thoại, không gửi lên máy chủ nào.
- **Sao lưu:** xuất và nhập lại bằng file JSON. Gỡ app hoặc xóa dữ liệu app thì các sổ sẽ mất, nên hãy xuất file định kỳ.

## Tính năng

### Tab Tổng quan

- **Phạm vi thống kê:** chọn *Tất cả*, *từng người sở hữu* hoặc *Chưa gán*. Khi chọn một người, mọi con số chỉ tính phần tiền của người đó trong từng sổ.
- **Mục tiêu:** ở chế độ *Tất cả*, liệt kê tiến độ mục tiêu của từng người đã đặt mục tiêu (bấm vào một người để xem thống kê riêng người đó). Khi đang xem một người, chỉ hiện mục tiêu của người đó.
- **Khối chính:** tổng tiền gốc đang gửi (kèm số tiền bằng chữ và số sổ), lãi tích lũy tăng theo từng giây, lãi sinh thêm mỗi ngày.
- **Dải số liệu:** lãi dự kiến cả kỳ, tổng nhận khi đáo hạn, lãi suất bình quân có trọng số theo số tiền, số sổ đang gửi.
- **Đến hạn trong 30 ngày tới:** các sổ sắp hoặc đã đáo hạn. Bấm một dòng để mở trang chi tiết. Sổ đã đáo hạn có sẵn nút *Tái tục* và *Tất toán*.
- **Tính tiền về đến ngày:** chọn một ngày (hoặc bấm nhanh *Cuối tháng*, *3 tháng*, *Cuối năm*) để xem tổng tiền lãi, tiền gốc và tổng gốc cộng lãi của các sổ đáo hạn đến hết ngày đó. Mặc định không tính sổ đã tất toán; bật *Tính cả sổ đã tất toán* nếu muốn. Bấm "Xem N sổ" để xem từng sổ được tính.
- **Phân bổ theo người sở hữu:** biểu đồ tròn, chỉ hiện ở chế độ *Tất cả* khi đã có người sở hữu.
- **Phân bổ theo ngân hàng:** biểu đồ tròn. Có hơn 7 ngân hàng thì các ngân hàng nhỏ được gộp thành một nhóm.
- **Tiền về theo tháng:** biểu đồ cột cho 12 tháng tới. Bấm vào cột để xem gốc và lãi của tháng đó.
- **Nhắc sao lưu:** dòng nhắc màu vàng hiện khi chưa xuất file lần nào, khi có thay đổi chưa lưu ra file, hoặc khi lần xuất gần nhất đã quá 30 ngày. Nút *Xuất file* lưu file JSON ngay.

### Tab Sổ của tôi

- Mỗi sổ là một thẻ gồm: logo ngân hàng, tên sổ, trạng thái, tiền gốc, lãi suất, kỳ hạn, lãi dự kiến, hình thức lĩnh lãi, thanh tiến độ, số ngày còn lại, người sở hữu và ghi chú.
- **Bộ lọc:**
  - tìm theo tên sổ, ngân hàng hoặc ghi chú, không cần gõ dấu;
  - lọc theo **người sở hữu**: hàng nút *Mọi người*, từng người kèm số sổ, và *Chưa gán*;
  - lọc theo trạng thái (Đang gửi, Sắp đáo hạn, Đã đáo hạn, Đã tất toán, Tất cả);
  - lọc theo ngân hàng.
- **Sắp xếp:** đáo hạn sớm nhất, số tiền lớn nhất, lãi suất cao nhất hoặc mới thêm gần đây.
- Bấm vào thẻ để mở **trang chi tiết**. Menu ⋮ trên thẻ có Sửa, Tái tục, Tất toán hoặc Mở lại, và Xóa.
- Bộ lọc, ô tìm kiếm và vị trí cuộn được **giữ nguyên** khi mở trang chi tiết hoặc form rồi quay lại.

### Trang chi tiết sổ

- **Phần đầu:** ngân hàng, trạng thái, tiền gốc (kèm số tiền bằng chữ), lãi tích lũy theo từng giây, thanh tiến độ "Đã gửi x/y ngày (z%)" và số ngày còn lại.
- **Người sở hữu:** phần gốc, tỷ lệ % và lãi dự kiến của từng người.
- **Thời gian:** ngày gửi, kỳ hạn, ngày đáo hạn, số ngày gửi, ngày tất toán.
- **Tiền lãi:** lãi suất, hình thức lĩnh lãi, lãi mỗi ngày, lãi mỗi tháng, lãi dự kiến cả kỳ, tổng nhận khi đáo hạn.
- **Lịch nhận lãi:** chỉ có ở sổ lĩnh lãi hàng tháng. Mỗi kỳ có ngày nhận và số tiền, đánh dấu ✓ cho kỳ đã nhận.
- **Phần cuối:** ghi chú, thời điểm tạo và cập nhật.
- **Thao tác:** nút sửa (bút chì) và menu ⋮ ở góc trên. Nút lớn ở cuối trang đổi theo trạng thái: *Tái tục* và *Tất toán* khi sổ sắp hoặc đã đáo hạn, *Mở lại* khi sổ đã tất toán, *Sửa sổ* trong các trường hợp còn lại.
- Tái tục từ trang chi tiết thì sau khi lưu sẽ chuyển sang chi tiết của sổ mới.

### Form thêm, sửa, tái tục sổ

- **Tên tự động:** sổ mới có sẵn tên "Tiết kiệm N", với N lớn hơn số lớn nhất đang dùng. Tên sửa được.
- **Ngân hàng:** chọn từ 58 ngân hàng có logo (tìm không dấu, nhận cả mã như `VCB`, `cake`), hoặc tự nhập tên.
- **Số tiền:** tự thêm dấu chấm khi gõ (`150.000.000`) và hiện số tiền bằng chữ.
- **Kỳ hạn:** chọn nhanh 1, 3, 6, 9, 12, 18, 24, 36 tháng, hoặc nhập từ 1 đến 600 tháng.
- **Lãi suất:** nhập được dấu phẩy (`5,2`).
- **Hình thức lĩnh lãi:** Cuối kỳ, Hàng tháng hoặc Đầu kỳ.
- **Người sở hữu** (không bắt buộc):
  - chọn một hay nhiều người, hoặc bấm *Thêm người* để tạo nhanh;
  - 1 người: toàn bộ số tiền thuộc về người đó;
  - từ 2 người trở lên: mỗi người có ô nhập số tiền, app báo *Còn thiếu* hoặc *Vượt*, có nút *Chia đều*. Tổng các phần phải bằng số tiền gửi thì mới lưu được.
- **Kiểm tra dữ liệu:** báo lỗi ngay dưới trường bị sai.
- **Xem trước:** ngày đáo hạn, số ngày gửi, lãi dự kiến và tổng nhận, cập nhật trong lúc nhập.
- **Tái tục:** ngày gửi bằng ngày đáo hạn của sổ cũ. Sổ cũ lĩnh lãi cuối kỳ thì số tiền mới bằng gốc cộng lãi. Phần của từng người được chia lại theo tỷ lệ cũ. Sau khi lưu, sổ cũ tự chuyển sang Đã tất toán.

### Tab Cài đặt

- **Người sở hữu:** thêm, sửa, xóa. Mỗi người có một màu riêng. Chỉ xóa được người không còn phần trong sổ nào.
- **Mục tiêu cho từng người** (bấm biểu tượng bút chì):
  - nhập số tiền mục tiêu và hạn (hạn không bắt buộc);
  - tiến độ **chỉ tính tiền gốc** đang gửi của người đó (phần của họ trong các sổ chưa tất toán);
  - app hiện thanh tiến độ, %, số còn thiếu, số ngày còn lại và số tiền cần gửi thêm mỗi tháng để kịp hạn;
  - quá hạn mà chưa đạt thì dòng chữ chuyển màu đỏ; đạt rồi thì hiện "Đã đạt mục tiêu".
- **Xuất file JSON:** lưu file `so-tiet-kiem-YYYY-MM-DD.json`, chia sẻ qua app khác, hoặc xem nội dung để sao chép. Có dòng chữ đỏ khi có thay đổi chưa lưu ra file.
- **Nhập file JSON:** chọn file hoặc dán nội dung. Các mục sai bị bỏ qua. Sau đó chọn *Gộp vào hiện tại* (bỏ qua sổ trùng ID) hoặc *Thay thế toàn bộ* (có hỏi xác nhận).
- **Nhắc đáo hạn:** thông báo trước 3 ngày và vào đúng ngày đáo hạn.
- **Dữ liệu:** tạo 5 sổ mẫu để xem thử; xóa toàn bộ dữ liệu (phải xác nhận hai lần).
- Biểu tượng tab Cài đặt có **chấm đỏ** khi có thay đổi chưa xuất file.

### Cách app tính

- **Ngày đáo hạn** = ngày gửi + kỳ hạn. Nếu tháng đáo hạn không có ngày tương ứng thì lấy ngày cuối tháng (gửi 31/01 kỳ hạn 1 tháng thì đáo hạn 28/02 hoặc 29/02).
- **Tiền lãi dự kiến** = gốc × lãi suất × số ngày / 365 (lãi đơn).
- **Lãi mỗi tháng** (chỉ khi lĩnh lãi hàng tháng) = lãi dự kiến / số tháng.
- **Trạng thái:**
  - *Đang gửi:* còn hơn 30 ngày;
  - *Sắp đáo hạn:* còn từ 1 đến 30 ngày;
  - *Đã đáo hạn:* từ chính ngày đáo hạn trở đi, khi chưa tất toán;
  - *Đã tất toán:* người dùng đã đóng sổ.
- **Theo người sở hữu:** mỗi sổ chỉ tính phần gốc của người đó. Lãi đơn tỷ lệ thuận với gốc, nên lãi của từng người cũng chia theo tỷ lệ đó.

## Danh sách ngân hàng

58 ngân hàng, logo lấy từ bộ dữ liệu VietQR và nhúng sẵn trong app nên vẫn hiện khi không có mạng.

- **Ngân hàng số:** Cake, Ubank, Timo, Vikki, PVcomBank Pay, TNEX, Liobank, Digimi.
- **Ngân hàng thương mại trong nước:** Vietcombank, VietinBank, BIDV, Agribank, Techcombank, MB Bank, ACB, VPBank, TPBank, Sacombank, HDBank, VIB, SHB, Eximbank, MSB, OCB, SeABank, LPBank, BVBank, Nam A Bank, Bac A Bank, ABBANK, PVcomBank, NCB, PGBank, VietBank, VietABank, BaoViet Bank, KienlongBank, Saigonbank, SCB, MBV, CB Bank, GPBank, Co-opBank.
- **Ngân hàng nước ngoài và liên doanh:** HSBC, Standard Chartered, Shinhan Bank, Woori Bank, UOB, CIMB, Public Bank, Hong Leong, KEB Hana, Kookmin, IBK, DBS Bank, KBank, Indovina Bank, VRB.

TNEX, Liobank, Digimi và ngân hàng tự nhập tên không có logo, nên app hiện huy hiệu chữ cái có màu thay thế.

## Định dạng file JSON

```json
{
  "app": "so-tiet-kiem",
  "version": 1,
  "currency": "VND",
  "exportedAt": "2026-11-09T10:00:00.000Z",
  "owners": [
    { "id": "a1b2...", "name": "Cha", "goalAmount": 1000000000, "goalDate": "2027-12-31" },
    { "id": "c3d4...", "name": "Mẹ", "goalAmount": null, "goalDate": null }
  ],
  "accounts": [
    {
      "id": "3f2b6c1e-...",
      "name": "Tiết kiệm 1",
      "amount": 150000000,
      "startDate": "2026-04-09",
      "termMonths": 12,
      "maturityDate": "2027-04-09",
      "interestRate": 4.8,
      "interestPayout": "maturity",
      "bankCode": "VCB",
      "bankName": "Vietcombank",
      "note": "Sổ giấy, chi nhánh Bến Thành",
      "closed": false,
      "closedAt": null,
      "createdAt": "2026-11-09T08:00:00.000Z",
      "updatedAt": "2026-11-09T08:00:00.000Z",
      "owners": [
        { "ownerId": "a1b2...", "amount": 100000000 },
        { "ownerId": "c3d4...", "amount": 50000000 }
      ]
    }
  ]
}
```

**Người sở hữu** (`owners` ở gốc file):

| Trường | Kiểu | Ý nghĩa |
|---|---|---|
| `id` | chuỗi | Mã người sở hữu |
| `name` | chuỗi | Tên hiển thị, tối đa 40 ký tự |
| `goalAmount` | số nguyên hoặc `null` | Mục tiêu tiền gốc (VND) |
| `goalDate` | `YYYY-MM-DD` hoặc `null` | Hạn đạt mục tiêu, không bắt buộc |

**Sổ tiết kiệm** (`accounts`):

| Trường | Kiểu | Ý nghĩa |
|---|---|---|
| `id` | chuỗi | Mã sổ |
| `name` | chuỗi | Tên sổ, tối đa 80 ký tự |
| `amount` | số nguyên | Tiền gốc (VND) |
| `startDate`, `maturityDate`, `closedAt` | `YYYY-MM-DD` | Ngày gửi, ngày đáo hạn, ngày tất toán. `maturityDate` chỉ để tham khảo; khi nhập file, app tự tính lại |
| `termMonths` | số nguyên | Kỳ hạn tính theo tháng, từ 1 đến 600 |
| `interestRate` | số | Lãi suất %/năm, từ 0 đến 100 |
| `interestPayout` | chuỗi | `maturity` (cuối kỳ), `monthly` (hàng tháng) hoặc `upfront` (đầu kỳ) |
| `bankCode` | chuỗi | Mã ngân hàng (VCB, BIDV, TCB, CAKE...), hoặc `OTHER` nếu tự nhập tên |
| `bankName` | chuỗi | Tên ngân hàng để hiển thị |
| `note` | chuỗi | Ghi chú, tối đa 500 ký tự |
| `closed` | boolean | Sổ đã tất toán hay chưa |
| `owners` | mảng | Phần tiền của từng người: `ownerId` và `amount`. Mảng rỗng nghĩa là *Chưa gán* |

Quy tắc khi nhập file:

- App chấp nhận một trong ba dạng: object có mảng `accounts`, object có mảng `books`, hoặc một mảng sổ trực tiếp.
- Mỗi sổ phải có tên, số tiền lớn hơn 0, ngày gửi hợp lệ, kỳ hạn từ 1 đến 600 tháng và lãi suất từ 0 đến 100%. Sổ nào thiếu hoặc sai thì bị bỏ qua.
- File cũ không có `owners` vẫn nhập được; các sổ sẽ là *Chưa gán*.
- Phần người sở hữu của một sổ chỉ được giữ khi mỗi phần lớn hơn 0, không trùng người và tổng bằng `amount`. Sai thì sổ vẫn được nhập nhưng thành *Chưa gán*.
- Phần nào trỏ tới người không có trong danh sách thì sổ đó thành *Chưa gán*.
- *Gộp* thêm những người sở hữu chưa có (theo `id`) và bỏ qua sổ trùng `id`. *Thay thế* dùng danh sách người và sổ trong file.

## Build

**Android Studio:** File → Open → chọn thư mục này → đợi Gradle sync → Run.

**Dòng lệnh** (cần JDK 17 và Android SDK, đã đặt `ANDROID_HOME`):

```bash
./gradlew testDebugUnitTest    # unit test phần tính lãi và nhập JSON
./gradlew assembleDebug        # app/build/outputs/apk/debug/app-debug.apk
./gradlew assembleRelease      # app/build/outputs/apk/release/app-release.apk (ký bằng khóa debug)
```

**GitHub Actions:** mỗi lần push, workflow `.github/workflows/android.yml` build APK. Tải ở tab Actions → lần chạy mới nhất → Artifacts → `so-tiet-kiem-apk`.

Bản release đang ký bằng khóa debug để cài thử được ngay. Trước khi đưa lên Google Play, hãy tạo keystore riêng và sửa `signingConfig` trong `app/build.gradle.kts`.

## Cấu trúc mã nguồn

```
app/src/main/java/vn/sotietkiem/
  MainActivity.kt                 điều hướng 3 tab, trang chi tiết, form; giữ trạng thái từng tab;
                                  đồng hồ 1 giây cho lãi tích lũy; xuất JSON từ dòng nhắc
  data/Account.kt                 model sổ, người sở hữu (Owner, OwnerShare), file sao lưu
  data/Banks.kt                   58 ngân hàng, logo ở assets/banks/<mã>.webp
  data/Repository.kt              lưu stk_v1.json và stk_owners.json; nhập/xuất và kiểm tra JSON
  domain/Calc.kt                  ngày đáo hạn, lãi đơn, trạng thái, tổng quan, biểu đồ, tính đến ngày
  domain/Owners.kt                phạm vi theo người, chia lại phần khi tái tục, chia đều, phân bổ
  util/Format.kt                  định dạng VND, đọc số bằng chữ, tìm kiếm không dấu
  notify/MaturityWorker.kt        nhắc đáo hạn (WorkManager, chạy trên máy)
  ui/AppViewModel.kt              trạng thái app, ghi nhận thay đổi để nhắc sao lưu
  ui/screens/OverviewScreen.kt    Tổng quan
  ui/screens/AccountsScreen.kt    Sổ của tôi
  ui/screens/DetailScreen.kt      Trang chi tiết sổ
  ui/screens/FormScreen.kt        Thêm, sửa, tái tục; chọn ngân hàng; chia phần người sở hữu
  ui/screens/BackupScreen.kt      Cài đặt: người sở hữu, sao lưu, nhắc đáo hạn, dữ liệu
  ui/components/                  thẻ sổ, logo, biểu đồ, tính tiền về đến ngày, người sở hữu, hộp thoại
```

## Khác biệt so với bản web (file HTML)

| Bản web | Bản Android |
|---|---|
| `localStorage` khóa `stk:v1` | File `stk_v1.json` (sổ) và `stk_owners.json` (người sở hữu) trong bộ nhớ riêng của app, được Android Auto Backup sao lưu theo tài khoản Google |
| Logo base64 nhúng trong HTML | Logo WebP trong `assets/banks` (dữ liệu VietQR, qua gói npm `vietqr-zero`, MIT) |
| Font tải từ Google Fonts | Be Vietnam Pro nhúng sẵn trong `res/font` (SIL OFL), chạy offline hoàn toàn |
| Kéo thả file để nhập | Chọn file, dán JSON, hoặc chia sẻ |
| Bấm thẻ sổ để sửa | Bấm thẻ sổ để mở trang chi tiết |
| — | Người sở hữu, chia tiền nhiều người, thống kê theo người |
| — | Mục tiêu tiền gốc cho từng người, có hạn tùy chọn |
| — | Tính tiền về đến một ngày chọn |
| — | Tên sổ tự động "Tiết kiệm N" |
| — | Nhắc sao lưu khi có thay đổi chưa xuất file |
| — | Thông báo nhắc đáo hạn |
