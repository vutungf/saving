# Sổ tiết kiệm (Android)

App Android native (Kotlin + Jetpack Compose). App theo dõi các sổ tiết kiệm ở ngân hàng Việt Nam, tính tiền lãi, ngày đáo hạn và thống kê, kể cả thống kê riêng theo từng người sở hữu. Dữ liệu chỉ nằm trên máy và được sao lưu bằng file JSON. File JSON tương thích với bản web một file HTML, có thêm phần người sở hữu và mục tiêu.

- **Tiền tệ:** chỉ dùng VND.
- **Lưu trữ:** người dùng chọn một trong hai cách (ở màn chào lần đầu, đổi được trong Cài đặt):
  - **Chỉ trên máy này:** dữ liệu mã hóa, không gửi lên máy chủ nào;
  - **Đồng bộ với tài khoản Google:** đăng nhập Google, dữ liệu được mã hóa đầu-cuối rồi lưu trên Firebase Firestore, dùng chung trên nhiều điện thoại. Cần cài Firebase trước, xem [FIREBASE_SETUP.md](FIREBASE_SETUP.md).
- **Mã hóa:** file trên máy, dữ liệu trên Firebase và (mặc định) file sao lưu xuất ra đều được mã hóa AES-256-GCM.
- **Sao lưu:** xuất và nhập lại bằng file JSON (có thể đặt mật khẩu). Ở chế độ chỉ trên máy, gỡ app hoặc xóa dữ liệu app thì các sổ sẽ mất, nên hãy xuất file định kỳ.

## Tính năng

### Tab Tổng quan

- **Phạm vi thống kê:** chọn *Tất cả*, *từng người sở hữu* hoặc *Chưa gán*. Khi chọn một người, mọi con số chỉ tính phần tiền của người đó trong từng sổ.
- **Mục tiêu:** ở chế độ *Tất cả*, liệt kê các mục tiêu của từng người theo thứ tự ưu tiên (bấm tên người để xem thống kê riêng, bấm *Quản lý* để mở trang mục tiêu). Khi đang xem một người, chỉ hiện mục tiêu của người đó.
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

### Màn chào (lần mở đầu tiên)

Hai lựa chọn: *Lưu trên máy này* hoặc *Đăng nhập Google để đồng bộ*. Người đã có dữ liệu từ bản trước không thấy màn này (tự chọn lưu trên máy).

### Tab Cài đặt

- **Lưu trữ và đồng bộ:**
  - chọn *Chỉ trên máy này* hoặc *Đồng bộ với tài khoản Google*;
  - đăng nhập Google lần đầu thì tạo **mật khẩu đồng bộ** (ít nhất 8 ký tự); máy khác đăng nhập cùng tài khoản phải nhập đúng mật khẩu này;
  - hiện email, thời điểm đồng bộ gần nhất; nút *Đồng bộ ngay*, *Đổi mật khẩu đồng bộ*, *Đăng xuất* (dữ liệu giữ nguyên), *Xóa dữ liệu trên mây*;
  - tự đồng bộ: khi mở app, 2 giây sau mỗi thay đổi, và ngay khi máy khác cập nhật;
  - gộp theo từng sổ / người sở hữu: bản sửa sau cùng thắng; xóa ở một máy thì các máy khác cũng xóa (trừ khi sổ được sửa lại sau đó);
  - mất mạng thì vẫn dùng bình thường, lần sau có mạng sẽ đồng bộ;
  - đang đồng bộ thì lần đồng bộ thành công cũng tính là đã sao lưu (tắt dòng nhắc xuất file).

- **Người sở hữu:** thêm, đổi tên (bút chì), xóa. Mỗi người có một màu riêng. Chỉ xóa được người không còn phần trong sổ nào.
- **Mục tiêu** (biểu tượng lá cờ cạnh mỗi người): mở trang mục tiêu của người đó.
  - Một người có **nhiều mục tiêu**, mỗi mục tiêu gồm tên, số tiền và hạn (hạn không bắt buộc).
  - Thứ tự từ trên xuống là **mức ưu tiên**; đổi thứ tự bằng mũi tên lên/xuống.
  - Tiến độ **chỉ tính tiền gốc** đang gửi của người đó (phần của họ trong các sổ chưa tất toán). Tiền gốc được dồn cho mục tiêu trên cùng trước, đủ rồi mới sang mục tiêu tiếp theo. Ví dụ có 150 triệu, mục tiêu 1 là 100 triệu và mục tiêu 2 là 1 tỷ thì mục tiêu 1 đạt 100%, mục tiêu 2 được 50 triệu (5%).
  - Mỗi mục tiêu hiện thanh tiến độ, %, số còn thiếu, số ngày còn lại và số tiền cần gửi thêm mỗi tháng để kịp hạn. Quá hạn mà chưa đạt thì chữ màu đỏ; đạt rồi thì hiện "Đã đạt mục tiêu".
- **Khóa ứng dụng:**
  - bật khóa và chọn kiểu: **mã PIN** (4–6 chữ số) hoặc **hình vẽ** (nối ít nhất 4 điểm trên lưới 3×3, giống khóa màn hình Android); mã mới phải nhập hoặc vẽ 2 lần;
  - *Đổi kiểu hoặc mã khóa* và tắt khóa đều phải xác nhận mã hiện tại trước;
  - *Mở khóa bằng vân tay / khuôn mặt* nếu máy hỗ trợ;
  - *Tự khóa khi rời app*: ngay lập tức, sau 1 phút hoặc sau 5 phút. App bị tắt hẳn thì lần mở sau luôn hỏi mã;
  - nhập sai 5 lần liên tiếp phải chờ 30 giây;
  - mở trình chọn file hoặc chia sẻ để sao lưu thì quay lại không bị khóa;
  - *Quên mã PIN? / Quên hình mở khóa?* trên màn khóa: chỉ có cách xóa toàn bộ dữ liệu (xác nhận 2 lần) rồi nhập lại từ file JSON;
  - mã PIN và hình vẽ không lưu dạng rõ, chỉ lưu mã băm SHA-256 kèm muối ngẫu nhiên.
- **Xuất file sao lưu:** lưu file `so-tiet-kiem-YYYY-MM-DD.json`, chia sẻ qua app khác, hoặc xem nội dung để sao chép. Trước khi xuất, app hỏi **mật khẩu mã hóa** (mặc định bật, ít nhất 6 ký tự, nhập 2 lần); có thể tắt để xuất file thường. Có dòng chữ đỏ khi có thay đổi chưa sao lưu.
- **Nhập file sao lưu:** chọn file hoặc dán nội dung. File mã hóa thì hỏi mật khẩu. Các mục sai bị bỏ qua. Sau đó chọn *Gộp vào hiện tại* (bỏ qua sổ trùng ID) hoặc *Thay thế toàn bộ* (có hỏi xác nhận).
- **Nhắc đáo hạn:** thông báo trước 3 ngày và vào đúng ngày đáo hạn.
- **Dữ liệu:**
  - *Xem thử với dữ liệu mẫu* (cũng có ở màn Tổng quan khi chưa có sổ nào): tạo 2 người sở hữu, mục tiêu và 5 sổ chia phần, ghi chú bắt đầu bằng `[Mẫu]`. Bấm lại không tạo trùng người hay mục tiêu.

    | Người | Mục tiêu (theo thứ tự ưu tiên) | Phần trong các sổ |
    |---|---|---|
    | Bố | Quỹ khẩn cấp 100 tr · Mua nhà 1,5 tỷ (hạn 2 năm) · Nghỉ hưu 2 tỷ (hạn 10 năm) | Quỹ mua nhà 300 tr, Tiền học cho con 100 tr, Quỹ hưu trí 300 tr |
    | Mẹ | Học phí đại học cho con 500 tr (hạn 3 năm) · Du lịch gia đình 80 tr (hạn 8 tháng) | Quỹ mua nhà 200 tr, Quỹ dự phòng 120 tr, Tiền học cho con 100 tr |
    | Chưa gán | | Gửi thử Cake 50 tr |

  - xóa toàn bộ dữ liệu (phải xác nhận hai lần).
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

## Mã hóa

| Dữ liệu | Cách mã hóa | Khóa |
|---|---|---|
| File trên máy (`stk_v1.json`, `stk_owners.json`, `stk_deleted.json`) | AES-256-GCM | Sinh trong Android Keystore, không rời khỏi máy. File cũ chưa mã hóa được đọc và mã hóa lại ở lần lưu sau |
| Dữ liệu trên Firebase | AES-256-GCM, mã hóa trên điện thoại trước khi gửi | Suy từ mật khẩu đồng bộ (PBKDF2-HMAC-SHA256, 120.000 vòng, muối ngẫu nhiên). Khóa đã suy được lưu trên máy, bọc bằng khóa Keystore |
| File sao lưu xuất ra | AES-256-GCM | Suy từ mật khẩu nhập khi xuất (PBKDF2 như trên) |
| Mã khóa ứng dụng (PIN / hình vẽ) | Băm SHA-256 kèm muối | Không lưu dạng rõ |

Vì khóa trên máy nằm trong Keystore của riêng điện thoại đó, các file dữ liệu **không** đi theo Android Auto Backup sang máy mới. Chuyển máy bằng file sao lưu hoặc đồng bộ Google.

File sao lưu mã hóa có dạng:

```json
{
  "app": "so-tiet-kiem",
  "encrypted": true,
  "version": 1,
  "cipher": "AES-256-GCM",
  "kdf": "PBKDF2WithHmacSHA256",
  "iter": 120000,
  "salt": "base64...",
  "data": "base64(iv 12 byte + dữ liệu mã hóa + thẻ xác thực)"
}
```

Giải mã `data` ra đúng nội dung file JSON không mã hóa mô tả ở mục dưới. Bản web một file HTML chỉ đọc được file không mã hóa.

## Định dạng file JSON

```json
{
  "app": "so-tiet-kiem",
  "version": 1,
  "currency": "VND",
  "exportedAt": "2026-11-09T10:00:00.000Z",
  "owners": [
    {
      "id": "a1b2...",
      "name": "Cha",
      "goals": [
        { "id": "g1...", "name": "Quỹ khẩn cấp", "amount": 100000000, "date": null },
        { "id": "g2...", "name": "Mua nhà", "amount": 1000000000, "date": "2027-12-31" }
      ]
    },
    { "id": "c3d4...", "name": "Mẹ", "goals": [] }
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
| `goals` | mảng | Các mục tiêu theo thứ tự ưu tiên: `id`, `name`, `amount` (VND), `date` (`YYYY-MM-DD` hoặc `null`) |
| `goalAmount`, `goalDate` | cũ | Định dạng một mục tiêu của bản trước; khi nhập file app tự chuyển thành một mục trong `goals` |

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

**Đồng bộ Google:** cần thêm `app/google-services.json`, xem [FIREBASE_SETUP.md](FIREBASE_SETUP.md). Thiếu file này app vẫn build được, chỉ ẩn tùy chọn đồng bộ.

**GitHub Actions:** mỗi lần push, workflow `.github/workflows/android.yml` build APK. Tải ở tab Actions → lần chạy mới nhất → Artifacts → `so-tiet-kiem-apk`. Để APK này đăng nhập Google được, thêm secret `DEBUG_KEYSTORE_BASE64` (xem mục 6 trong [FIREBASE_SETUP.md](FIREBASE_SETUP.md)).

Bản release đang ký bằng khóa debug để cài thử được ngay. Trước khi đưa lên Google Play, hãy tạo keystore riêng và sửa `signingConfig` trong `app/build.gradle.kts`.

## Cấu trúc mã nguồn

```
app/src/main/java/vn/sotietkiem/
  MainActivity.kt                 điều hướng 3 tab, trang chi tiết, form; giữ trạng thái từng tab;
                                  đồng hồ 1 giây cho lãi tích lũy; xuất JSON từ dòng nhắc
  data/Account.kt                 model sổ, người sở hữu (Owner, OwnerShare), file sao lưu
  data/Banks.kt                   58 ngân hàng, logo ở assets/banks/<mã>.webp
  data/Repository.kt              lưu file trên máy (mã hóa); nhập/xuất, kiểm tra và mã hóa file sao lưu
  domain/Merge.kt                 gộp dữ liệu máy và mây theo updatedAt, có danh sách đã xóa
  sync/CloudSync.kt               đăng nhập Google, mật khẩu đồng bộ, đồng bộ Firestore mã hóa đầu-cuối
  util/Crypto.kt                  AES-256-GCM, khóa Keystore, PBKDF2
  domain/Calc.kt                  ngày đáo hạn, lãi đơn, trạng thái, tổng quan, biểu đồ, tính đến ngày
  domain/Owners.kt                phạm vi theo người, chia lại phần khi tái tục, chia đều, phân bổ
  util/Format.kt                  định dạng VND, đọc số bằng chữ, tìm kiếm không dấu
  util/AppLock.kt                 cài đặt khóa: kiểu PIN/hình vẽ, mã băm, vân tay, thời gian tự khóa
  util/Biometric.kt               kiểm tra và hiện hộp thoại vân tay / khuôn mặt
  notify/MaturityWorker.kt        nhắc đáo hạn (WorkManager, chạy trên máy)
  ui/AppViewModel.kt              trạng thái app, ghi nhận thay đổi để nhắc sao lưu
  ui/screens/OverviewScreen.kt    Tổng quan
  ui/screens/AccountsScreen.kt    Sổ của tôi
  ui/screens/DetailScreen.kt      Trang chi tiết sổ
  ui/screens/FormScreen.kt        Thêm, sửa, tái tục; chọn ngân hàng; chia phần người sở hữu
  ui/screens/BackupScreen.kt      Cài đặt: người sở hữu, khóa ứng dụng, sao lưu, nhắc đáo hạn, dữ liệu
  ui/screens/GoalsScreen.kt       Mục tiêu của một người: thêm, sửa, xóa, đổi thứ tự
  ui/screens/WelcomeScreen.kt     Màn chào: chọn lưu trên máy hoặc đăng nhập Google
  ui/components/CloudSettings.kt  Thẻ Lưu trữ và đồng bộ, hộp thoại mật khẩu đồng bộ
  ui/components/Exporter.kt       Xuất file sao lưu có mật khẩu, nhập file mã hóa
  ui/screens/LockScreen.kt        Màn khóa: bàn phím PIN hoặc lưới hình vẽ, vân tay, quên mã
  ui/components/PatternPad.kt     Lưới 3×3 để vẽ hình mở khóa
  ui/components/LockSettings.kt   Thẻ Khóa ứng dụng và các bước đặt / đổi / tắt khóa
  ui/components/                  thẻ sổ, logo, biểu đồ, tính tiền về đến ngày, người sở hữu, hộp thoại
```

## Khác biệt so với bản web (file HTML)

| Bản web | Bản Android |
|---|---|
| `localStorage` khóa `stk:v1` | File mã hóa trong bộ nhớ riêng của app, hoặc đồng bộ tài khoản Google qua Firebase (mã hóa đầu-cuối) |
| File JSON không mã hóa | File sao lưu mặc định mã hóa bằng mật khẩu |
| Logo base64 nhúng trong HTML | Logo WebP trong `assets/banks` (dữ liệu VietQR, qua gói npm `vietqr-zero`, MIT) |
| Font tải từ Google Fonts | Be Vietnam Pro nhúng sẵn trong `res/font` (SIL OFL), chạy offline hoàn toàn |
| Kéo thả file để nhập | Chọn file, dán JSON, hoặc chia sẻ |
| Bấm thẻ sổ để sửa | Bấm thẻ sổ để mở trang chi tiết |
| — | Người sở hữu, chia tiền nhiều người, thống kê theo người |
| — | Nhiều mục tiêu tiền gốc cho từng người, sắp xếp theo ưu tiên |
| — | Khóa ứng dụng bằng mã PIN, hình vẽ, vân tay hoặc khuôn mặt |
| — | Tính tiền về đến một ngày chọn |
| — | Tên sổ tự động "Tiết kiệm N" |
| — | Nhắc sao lưu khi có thay đổi chưa xuất file |
| — | Thông báo nhắc đáo hạn |
