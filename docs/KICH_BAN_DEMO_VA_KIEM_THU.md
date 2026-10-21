# Kịch bản demo và kiểm thử Smart Sensor

## Chuẩn bị

- Điện thoại Android 8.0 trở lên có gia tốc kế và gyroscope, cài app và mở sẵn.
- Có thể chiếu màn hình điện thoại để cả lớp theo dõi.
- Thử trước ngưỡng lắc trên thiết bị cụ thể, bắt đầu tại 5,5 m/s².
- Nếu điện thoại không có gravity sensor, game lái xe dùng gia tốc kế đã lọc làm nguồn dự phòng.

## Demo 3–4 phút

| Thời gian | Thao tác | Điểm giải thích |
| --- | --- | --- |
| 0:00–0:45 | Mở **Lái xe**, nghiêng máy trái/phải để né xe; bấm **Chơi lại** sau va chạm | Góc nghiêng ngang điều khiển xe, điểm tăng theo thời gian sống sót |
| 0:45–1:20 | Tiếp tục **Lái xe**, né vài xe và để xảy ra va chạm | Góc nghiêng ngang điều khiển xe; va chạm dừng trò chơi |
| 1:20–2:15 | Mở **Lắc**, di chuyển nhẹ rồi lắc rõ ràng | Ngưỡng phân biệt; hysteresis và 650 ms tránh đếm trùng |
| 2:15–2:50 | Kéo ngưỡng sang trái/phải và thử lại | Ngưỡng thấp nhạy hơn; ngưỡng cao cần lực lớn hơn |
| 2:50–4:00 | Mở **Bắn vịt**, xoay máy để ngắm vịt bay rồi bắn; chỉnh độ nhạy ngang/dọc | Gyroscope đo tốc độ quay; độ nhạy quyết định quãng di chuyển của tâm ngắm. |

## Ma trận kiểm thử thủ công

| Mã | Thao tác | Kết quả mong đợi |
| --- | --- | --- |
| T01 | Di chuyển nhẹ dưới ngưỡng | Xúc xắc không tung |
| T02 | Lắc mạnh một lần | Xúc xắc tung một lần, không tung liên tục cho cùng nhịp |
| T03 | Kéo thanh ngưỡng lên cao | Cùng lực lắc trở nên khó được nhận diện hơn |
| T04 | Nhấn Chơi lại ở màn Lắc | Điểm và lượt xúc xắc về 0 |
| T05 | Chuyển app ra nền rồi quay lại | App tiếp tục cập nhật; không giữ listener khi ở nền |
| T06 | Máy không có gravity sensor | Game lái xe dùng gia tốc kế đã lọc để lấy góc nghiêng |
| T07 | Máy không có accelerometer | Màn Lắc báo thiếu cảm biến, thanh ngưỡng bị vô hiệu hóa; app không crash |
| T08 | Mở Lái xe, nghiêng máy trái/phải | Xe di chuyển theo hướng nghiêng; có thể tránh xe phía trước |
| T09 | Để xe va chạm rồi bấm Chơi lại | Game dừng khi va chạm; điểm, chướng ngại vật và vị trí xe được đặt lại |
| T10 | Mở Bắn vịt, xoay máy theo hai hướng | Tâm ngắm dịch theo chuyển động quay, vịt tiếp tục bay |
| T11 | Đổi độ nhạy ngang/dọc rồi xoay lại | Quãng dịch chuyển thay đổi theo từng trục |
| T12 | Máy thiếu gyroscope | Màn Bắn vịt báo không hỗ trợ, không crash |

**Ghi kết quả khi nộp:** điền thiết bị, phiên bản Android, ngày thử và Pass/Fail cho từng dòng.

**Kết quả kiểm tra hiện có (21/10/2026):** `:app:assembleDebug --offline` chạy thành công. Chưa thử bản lái xe mới trên điện thoại thật; các ca kiểm thử cảm biến và va chạm vẫn cần thao tác vật lý.

## Câu hỏi thuyết trình thường gặp

**Vì sao có số liệu dù điện thoại đứng yên?** Gia tốc kế đo cả ảnh hưởng trọng lực. Nhiễu cảm biến làm số dao động nhẹ.

**Vì sao dùng gravity cho đo nghiêng?** Dữ liệu này ước lượng hướng trọng lực, ít bị chuyển động tức thời tác động hơn gia tốc kế thô.

**Vì sao không đếm mỗi mẫu vượt ngưỡng?** Một lần lắc tạo nhiều sự kiện liên tiếp. Điều kiện hạ ngưỡng và 650 ms giới hạn mỗi nhịp thành một lần.

**Tại sao tắt listener khi ra nền?** Android vẫn có thể tiếp tục thu dữ liệu và dùng pin nếu listener không được hủy.

**Nếu thiết bị thiếu cảm biến thì sao?** App kiểm tra `getDefaultSensor`; phần nào không có nguồn phù hợp sẽ báo rõ và không đăng ký listener rỗng.
