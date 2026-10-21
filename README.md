# Smart Sensor — Android motion sensor demo

Ứng dụng Kotlin minh họa cách dùng cảm biến chuyển động có sẵn trên điện thoại Android qua ba mini game.

| Mini game | Cách chơi | Cảm biến |
| --- | --- | --- |
| Lái xe | Nghiêng điện thoại sang trái/phải để né xe, va chạm thì chơi lại | Gravity; dùng accelerometer đã lọc khi thiếu gravity |
| Lắc xúc xắc | Lắc máy để tung hai xúc xắc, điều chỉnh ngưỡng nhận diện | Accelerometer |
| Bắn vịt | Xoay điện thoại để di chuyển tâm ngắm, bấm **BẮN** để ghi điểm | Gyroscope |

Ứng dụng kiểm tra cảm biến có sẵn trước khi dùng. Listener được đăng ký trong `onResume()` và hủy trong `onPause()`.

## Chạy bằng Android Studio

1. Cài Android Studio, JDK 17 và Android SDK Platform 35.
2. Mở thư mục dự án, chờ Gradle đồng bộ.
3. Kết nối điện thoại Android 8.0 trở lên, bật **USB debugging**, chọn thiết bị trong Android Studio rồi bấm **Run**. Có thể dùng emulator để xem giao diện, nhưng demo chuyển động nên thử trên điện thoại thật.

Build bằng PowerShell:

```powershell
.\gradlew.bat :app:assembleDebug
```

APK debug được tạo tại `app/build/outputs/apk/debug/app-debug.apk`.

## Cấu trúc chính

- `app/src/main/java/vn/edu/smartsensor/SensorEngine.kt`: nhận dữ liệu gia tốc, trọng lực, gyroscope; lọc và nhận diện lắc.
- `app/src/main/java/vn/edu/smartsensor/MainActivity.kt`: điều hướng và giao diện ba game.
- `app/src/main/java/vn/edu/smartsensor/TiltDrivingGameView.kt`: game lái xe bằng độ nghiêng.
- `app/src/main/java/vn/edu/smartsensor/DiceGameView.kt`: xúc xắc động.
- `app/src/main/java/vn/edu/smartsensor/GyroShooterView.kt`: game bắn vịt bằng gyroscope.
- `docs/KICH_BAN_DEMO_VA_KIEM_THU.md`: kịch bản demo và các bước kiểm thử thủ công.
- `slides/output/Cam_Bien_Trong_Android1_So_Do_Luong_5_Buoc_20261021.pptx`: bản slide thuyết trình hiện tại.

## Giới hạn

- Độ nhạy và hướng điều khiển có thể khác nhau giữa các thiết bị; nên thử trước khi thuyết trình.
- Khi thiếu gyroscope, game Bắn vịt sẽ báo trạng thái không hỗ trợ.
- Game lái xe ưu tiên gravity; gia tốc kế đã lọc là nguồn dự phòng và có thể dao động khi máy chuyển động mạnh.

Tài liệu kỹ thuật: [Android Sensors Overview](https://developer.android.com/develop/sensors-and-location/sensors/sensors_overview) · [Motion Sensors](https://developer.android.com/develop/sensors-and-location/sensors/sensors_motion).
