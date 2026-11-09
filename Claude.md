# Sổ tiết kiệm: ứng dụng quản lý và thống kê sổ tiết kiệm

Ứng dụng web chạy trong một file HTML duy nhất. Nó giúp theo dõi các sổ tiết kiệm gửi ở ngân hàng Việt Nam, tính tiền lãi và ngày đáo hạn, và thống kê tổng quan.

- **Tiền tệ:** chỉ dùng VND.
- **Lưu trữ:** dữ liệu nằm trong trình duyệt (`localStorage`), không gửi lên máy chủ nào.
- **Sao lưu:** xuất và nhập lại bằng file JSON.

---

## 1. Dữ liệu một sổ tiết kiệm

| Trường | Bắt buộc | Ghi chú |
|---|---|---|
| Tên sổ | Có | Ví dụ: "Quỹ mua nhà", tối đa 80 ký tự |
| Ngân hàng | Có | Chọn từ danh sách có logo, hoặc tự nhập tên |
| Số tiền gửi | Có | VND, tự định dạng khi gõ (`150.000.000`) và hiện thêm số tiền bằng chữ |
| Ngày gửi | Có | Mặc định là hôm nay |
| Kỳ hạn | Có | Chọn nhanh 1, 3, 6, 9, 12, 18, 24 hoặc 36 tháng, hoặc nhập từ 1 đến 600 tháng |
| Lãi suất | Có | Đơn vị %/năm, nhập được dấu phẩy (`5,2`) |
| Hình thức lĩnh lãi | Có | **Cuối kỳ** (mặc định), Hàng tháng hoặc Đầu kỳ |
| Ghi chú | Không | Số sổ, chi nhánh, mục đích...; tối đa 500 ký tự |

### Giá trị app tự tính

- **Ngày đáo hạn** bằng ngày gửi cộng kỳ hạn. Nếu tháng đáo hạn không có ngày tương ứng thì lấy ngày cuối tháng, ví dụ gửi 31/01 kỳ hạn 1 tháng thì đáo hạn 28/02 hoặc 29/02.
- **Số ngày gửi:** số ngày thực tế từ ngày gửi đến ngày đáo hạn.
- **Tiền lãi dự kiến** = gốc × lãi suất × số ngày / 365 (lãi đơn).
- **Tổng nhận khi đáo hạn** = gốc + lãi dự kiến.
- **Lãi mỗi tháng**, chỉ áp dụng khi lĩnh lãi hàng tháng: lãi dự kiến chia cho số tháng.
- **Lãi tích lũy đến hiện tại:** tính theo thời gian thực đến từng giây.
- **Trạng thái của sổ:**
  - *Đang gửi:* còn hơn 30 ngày nữa mới đáo hạn.
  - *Sắp đáo hạn:* còn 30 ngày trở xuống.
  - *Đã đáo hạn:* đã qua ngày đáo hạn nhưng chưa tất toán.
  - *Đã tất toán:* người dùng đã đóng sổ.

> Lưu ý: cách tính trên là lãi đơn. Kết quả có thể lệch vài nghìn đồng so với ngân hàng, vì mỗi ngân hàng làm tròn và tính ngày theo cách riêng. Với hình thức lĩnh lãi đầu kỳ, hãy nhập đúng mức lãi suất đầu kỳ mà ngân hàng áp dụng (thường thấp hơn lãi suất cuối kỳ).

---

## 2. Danh sách ngân hàng

Danh sách gồm 58 ngân hàng. Logo lấy từ bộ dữ liệu VietQR và được nhúng sẵn vào file dưới dạng base64, nên app vẫn hiển thị logo khi không có mạng.

- **Ngân hàng số:** Cake, Ubank, Timo, Vikki, PVcomBank Pay, TNEX, Liobank, Digimi.
- **Ngân hàng thương mại trong nước:** Vietcombank, VietinBank, BIDV, Agribank, Techcombank, MB Bank, ACB, VPBank, TPBank, Sacombank, HDBank, VIB, SHB, Eximbank, MSB, OCB, SeABank, LPBank, BVBank, Nam A Bank, Bac A Bank, ABBANK, PVcomBank, NCB, PGBank, VietBank, VietABank, BaoViet Bank, KienlongBank, Saigonbank, SCB, MBV, CB Bank, GPBank, Co-opBank...
- **Ngân hàng nước ngoài và liên doanh:** HSBC, Standard Chartered, Shinhan Bank, Woori Bank, UOB, CIMB, Public Bank, Hong Leong, KEB Hana, Kookmin, IBK, DBS Bank, KBank, Indovina Bank, VRB.

Cách dùng:

- Ô tìm kiếm không cần gõ dấu và nhận cả mã ngân hàng, ví dụ `vietin`, `VCB` hoặc `cake`.
- TNEX, Liobank và Digimi không có logo trong bộ dữ liệu, nên app hiện huy hiệu chữ cái có màu thay thế.
- Ngân hàng không có trong danh sách thì nhập tên tay. App cũng sẽ hiện huy hiệu chữ cái cho ngân hàng đó.

---

## 3. Các màn hình

### 3.1 Tổng quan

- **Khối chính:**
  - Tổng tiền gốc đang gửi, kèm số tiền bằng chữ và số sổ.
  - Tiền lãi đã tích lũy đến lúc này, con số tăng mỗi giây.
  - Số tiền lãi sinh thêm mỗi ngày.
- **Dải số liệu:**
  - Lãi dự kiến cả kỳ.
  - Tổng nhận khi đáo hạn.
  - Lãi suất bình quân, có trọng số theo số tiền.
  - Số sổ đang gửi.
- **Đến hạn trong 30 ngày tới:** liệt kê các sổ sắp và đã đáo hạn. Sổ đã đáo hạn có sẵn nút *Tái tục* và *Tất toán*.
- **Phân bổ theo ngân hàng:** biểu đồ tròn kèm chú thích số tiền và tỷ lệ phần trăm. Nếu có hơn 7 ngân hàng, các ngân hàng nhỏ được gộp thành một nhóm.
- **Tiền về theo tháng:** biểu đồ cột cho 12 tháng tới, mỗi cột là tổng gốc và lãi đáo hạn trong tháng đó. Sổ đã đáo hạn mà chưa tất toán được tính vào tháng hiện tại.
- **Nhắc sao lưu:** hiện khi chưa xuất file lần nào, hoặc lần xuất gần nhất đã quá 30 ngày.

### 3.2 Sổ của tôi

- Mỗi sổ hiển thị dạng thẻ, gồm:
  - logo ngân hàng, tên sổ và trạng thái;
  - số tiền gốc;
  - lãi suất, kỳ hạn, lãi dự kiến và hình thức lĩnh lãi;
  - thanh tiến độ, ngày gửi, ngày đáo hạn và số ngày còn lại;
  - ghi chú, nếu có.
- **Bộ lọc:** tìm theo tên sổ, ngân hàng hoặc ghi chú; lọc theo trạng thái (Đang gửi, Sắp đáo hạn, Đã đáo hạn, Đã tất toán, Tất cả); lọc theo ngân hàng.
- **Sắp xếp:** đáo hạn sớm nhất, số tiền lớn nhất, lãi suất cao nhất hoặc mới thêm gần đây.
- **Thao tác trên từng sổ:**
  - *Sửa*.
  - *Xóa*, có hỏi xác nhận trước.
  - *Tái tục*: mở form sổ mới với ngày gửi bằng ngày đáo hạn của sổ cũ. Nếu sổ cũ lĩnh lãi cuối kỳ thì số tiền gửi mới bằng gốc cộng lãi. Sau khi lưu, sổ cũ tự chuyển sang trạng thái Đã tất toán.
  - *Tất toán* và *Mở lại*.

### 3.3 Form thêm hoặc sửa sổ

- Kiểm tra từng trường và báo lỗi ngay dưới trường bị sai.
- Có khung xem trước ngày đáo hạn, số ngày gửi, tiền lãi dự kiến và tổng nhận, cập nhật trong lúc nhập.

### 3.4 Sao lưu dữ liệu

- **Xuất file JSON:** tải về file tên `so-tiet-kiem-YYYY-MM-DD.json`. Nếu trình duyệt không tự tải file, app hiện nội dung JSON để bạn sao chép.
- **Nhập file JSON:** chọn file, kéo thả, hoặc dán nội dung. App kiểm tra định dạng, bỏ qua các mục không hợp lệ, rồi cho chọn:
  - *Gộp vào dữ liệu hiện tại:* sổ trùng ID sẽ được bỏ qua.
  - *Thay thế toàn bộ:* có hỏi xác nhận trước.
- **Xóa toàn bộ dữ liệu:** phải xác nhận hai lần.

---

## 4. Định dạng file JSON

```json
{
  "app": "so-tiet-kiem",
  "version": 1,
  "currency": "VND",
  "exportedAt": "2026-11-09T10:00:00.000Z",
  "accounts": [
    {
      "id": "3f2b6c1e-...",
      "name": "Quỹ mua nhà",
      "amount": 500000000,
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
      "updatedAt": "2026-11-09T08:00:00.000Z"
    }
  ]
}
```

| Trường | Kiểu | Ý nghĩa |
|---|---|---|
| `amount` | số nguyên | Tiền gốc (VND) |
| `startDate`, `maturityDate`, `closedAt` | `YYYY-MM-DD` | Ngày gửi, ngày đáo hạn, ngày tất toán. `maturityDate` chỉ để tham khảo; khi nhập file, app tự tính lại |
| `termMonths` | số nguyên | Kỳ hạn tính theo tháng |
| `interestRate` | số | Lãi suất %/năm |
| `interestPayout` | chuỗi | `maturity` (cuối kỳ), `monthly` (hàng tháng) hoặc `upfront` (đầu kỳ) |
| `bankCode` | chuỗi | Mã ngân hàng (VCB, BIDV, TCB, CAKE...), hoặc `OTHER` nếu tự nhập tên |
| `bankName` | chuỗi | Tên ngân hàng để hiển thị |
| `closed` | boolean | Sổ đã tất toán hay chưa |

Khi nhập file, app chấp nhận một trong ba dạng: object có mảng `accounts`, object có mảng `books`, hoặc một mảng sổ trực tiếp. Mỗi sổ phải có tên, số tiền lớn hơn 0, ngày gửi hợp lệ, kỳ hạn từ 1 đến 600 tháng và lãi suất từ 0 đến 100%. Sổ nào thiếu hoặc sai thì bị bỏ qua.

---

## 5. Kỹ thuật

- **Tệp:** một file HTML tự chứa (khoảng 690 KB, phần lớn là logo).
- **Mã nguồn:** HTML, CSS và JavaScript thuần, không dùng framework. Biểu đồ vẽ bằng SVG.
- **Font:** Be Vietnam Pro, lấy từ Google Fonts. Nếu không tải được thì dùng font hệ thống.
- **Lưu trữ:** `localStorage`, với khóa `stk:v1`.
- **Giao diện:** tự theo chế độ sáng hoặc tối của thiết bị, và hiển thị tốt trên điện thoại.
- **Chạy offline:** mọi tính năng đều hoạt động khi không có mạng, trừ việc tải font.

---

## 6. Lưu ý khi sử dụng

1. Dữ liệu chỉ nằm trong trình duyệt trên thiết bị đang dùng. Nếu xóa dữ liệu trình duyệt, dùng chế độ ẩn danh hoặc đổi máy, các sổ sẽ không còn.
2. Nên xuất file JSON định kỳ, nhất là sau khi thêm hoặc sửa sổ.
3. Muốn chuyển sang máy khác: xuất file ở máy cũ, mở app ở máy mới, rồi nhập file đó.
4. Nút "Xem thử với dữ liệu mẫu" tạo 5 sổ mẫu để xem trước. Hãy xóa các sổ này trước khi nhập dữ liệu thật.

---

## 7. Hướng phát triển thêm (tùy chọn)

- Hỗ trợ kỳ hạn tính theo tuần hoặc ngày.
- Tính lãi nhập gốc cho sổ tự động tái tục nhiều kỳ.
- Xuất báo cáo dạng CSV hoặc Excel.
- Thêm biểu đồ lịch sử tổng tài sản tiết kiệm theo thời gian.
- Nhắc nhở trước ngày đáo hạn.
