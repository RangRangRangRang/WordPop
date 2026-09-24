# WordPop

## Giới thiệu

WordPop là ứng dụng Android hỗ trợ học từ vựng và thành ngữ tiếng Anh trong những khoảng thời gian ngắn hằng ngày. Ứng dụng hiển thị thẻ từ vựng trên màn hình, hỗ trợ phát âm, lưu nội dung cần ôn tập và theo dõi lịch sử học tập.

## Tính năng chính

- Hiển thị thẻ từ vựng nổi trên màn hình điện thoại.
- Lựa chọn từ vựng theo các mức độ từ A1 đến C2.
- Hiển thị cách phát âm và nghĩa của từ.
- Phát âm từ vựng và thành ngữ bằng chức năng đọc văn bản.
- Hỗ trợ học thành ngữ kèm nghĩa, gợi ý nghĩa đen, câu ví dụ và bản dịch.
- Lưu từ vựng hoặc thành ngữ vào sổ tay.
- Xem lịch sử học trong ngày.
- Tìm kiếm nội dung đã có trong ứng dụng.
- Đặt lịch nhắc học thành ngữ.
- Hỗ trợ hoạt động nền và khởi động lại lịch nhắc sau khi thiết bị khởi động lại.

## Công nghệ sử dụng

- Kotlin
- Android SDK
- Jetpack Compose
- Material 3
- Room Database
- DataStore Preferences
- TextToSpeech
- WorkManager
- AlarmManager

## Yêu cầu môi trường

- Android Studio phiên bản hỗ trợ Kotlin và Jetpack Compose.
- JDK 11 trở lên.
- Android SDK tối thiểu API 26.
- Android SDK biên dịch API 37.

## Cách chạy dự án

1. Mở thư mục dự án bằng Android Studio.
2. Chờ Android Studio hoàn tất đồng bộ Gradle.
3. Kết nối thiết bị Android hoặc mở một máy ảo Android.
4. Nhấn nút Run để biên dịch và cài đặt ứng dụng.
5. Khi mở lần đầu, cấp các quyền cần thiết và hoàn thành phần thiết lập ban đầu.

Có thể biên dịch bằng dòng lệnh trên Windows:

```powershell
.\gradlew.bat assembleDebug
```

Tệp cài đặt thử nghiệm được tạo trong thư mục `app/build/outputs/apk/debug/`.

## Quyền ứng dụng

Để hoạt động đầy đủ, WordPop có thể yêu cầu các quyền sau:

- Quyền hiển thị trên ứng dụng khác để hiển thị thẻ từ nổi.
- Quyền thông báo để duy trì dịch vụ tiền cảnh.
- Quyền bỏ qua tối ưu hóa pin để hạn chế việc hệ thống dừng tác vụ nền.
- Quyền báo thức chính xác để thực hiện lịch nhắc thành ngữ.
- Quyền nhận sự kiện khi thiết bị khởi động lại.

## Cấu trúc chính

- `app/src/main/java/com/rang/wordpop/ui`: giao diện các màn hình và thẻ từ.
- `app/src/main/java/com/rang/wordpop/data`: cơ sở dữ liệu, kho từ vựng và lớp truy cập dữ liệu.
- `app/src/main/java/com/rang/wordpop/service`: dịch vụ nền, lịch nhắc và xử lý sự kiện hệ thống.
- `app/src/main/java/com/rang/wordpop/util`: các tiện ích như đọc phát âm và kiểm tra quyền.
- `app/src/main/assets/vocab`: dữ liệu từ vựng theo từng mức độ.

## Dữ liệu và quyền riêng tư

Dữ liệu học tập được lưu cục bộ trên thiết bị bằng cơ sở dữ liệu Room. Dự án hiện chưa triển khai tài khoản người dùng hoặc đồng bộ dữ liệu trực tuyến.

## Hướng phát triển

- Bổ sung thêm từ vựng theo chủ đề.
- Xây dựng cơ chế nhắc lại dựa trên mức độ ghi nhớ của người học.
- Thêm các bài luyện tập ngắn như chọn nghĩa, điền từ và nghe đoán từ.
- Cho phép người dùng tự tạo bộ từ vựng.
- Bổ sung đồng bộ dữ liệu giữa nhiều thiết bị.

---

# WordPop

## Introduction

WordPop is an Android application that helps users learn English vocabulary and idioms through short daily learning moments. The application displays vocabulary cards on the screen, supports pronunciation, saves items for later review, and tracks learning history.

## Main Features

- Display floating vocabulary cards on the phone screen.
- Choose vocabulary levels from A1 to C2.
- Show pronunciation and word meanings.
- Pronounce words and idioms using text-to-speech.
- Learn idioms with meanings, literal hints, example sentences, and translations.
- Save words and idioms to a personal notebook.
- View the learning history for the current day.
- Search available learning content.
- Schedule idiom learning reminders.
- Support background operation and restore reminders after device reboot.

## Technologies

- Kotlin
- Android SDK
- Jetpack Compose
- Material 3
- Room Database
- DataStore Preferences
- TextToSpeech
- WorkManager
- AlarmManager

## Requirements

- Android Studio with Kotlin and Jetpack Compose support.
- JDK 11 or newer.
- Minimum Android SDK: API 26.
- Compile Android SDK: API 37.

## Getting Started

1. Open the project folder in Android Studio.
2. Wait for Gradle synchronization to finish.
3. Connect an Android device or start an Android emulator.
4. Press Run to build and install the application.
5. On the first launch, grant the required permissions and complete the initial setup.

The project can also be built from the Windows command line:

```powershell
.\gradlew.bat assembleDebug
```

The debug application package is generated in `app/build/outputs/apk/debug/`.

## Application Permissions

For full functionality, WordPop may request the following permissions:

- Display over other apps permission for floating vocabulary cards.
- Notification permission for the foreground service.
- Battery optimization exemption to reduce background task interruptions.
- Exact alarm permission for idiom reminders.
- Permission to receive the device boot event.

## Main Structure

- `app/src/main/java/com/rang/wordpop/ui`: application screens and vocabulary cards.
- `app/src/main/java/com/rang/wordpop/data`: database, vocabulary data, and data access classes.
- `app/src/main/java/com/rang/wordpop/service`: background services, reminders, and system event handling.
- `app/src/main/java/com/rang/wordpop/util`: utilities such as text-to-speech and permission checks.
- `app/src/main/assets/vocab`: vocabulary data grouped by level.

## Data and Privacy

Learning data is stored locally on the device using Room Database. The project does not currently provide user accounts or online data synchronization.

## Future Improvements

- Add more topic-based vocabulary.
- Build a review system based on the learner's memory level.
- Add short exercises such as meaning selection, fill-in-the-blank, and listening quizzes.
- Allow users to create custom vocabulary sets.
- Add synchronization across multiple devices.

## License

This project is currently intended for learning and academic purposes.
