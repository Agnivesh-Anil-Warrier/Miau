# 🐾 Miau — Smart Fake Call & Emergency Escape Tool for Android

<p align="center">
  <img width="390" height="389" alt="image" src="https://github.com/user-attachments/assets/6781a068-c38e-40b2-ae8f-0e9be3c1e2fb" />
</p>

<p align="center">
  <strong>Your instant exit pass from awkward dates, endless meetings, and uncomfortable social situations.</strong>
</p>

<p align="center">
  <a href="#features">
    <img src="https://img.shields.io/badge/Platform-Android-green?style=flat-square&logo=android" alt="Platform">
  </a>
  <a href="#tech-stack--architecture">
    <img src="https://img.shields.io/badge/Language-Kotlin-purple?style=flat-square&logo=kotlin" alt="Language">
  </a>
  <a href="#tech-stack--architecture">
    <img src="https://img.shields.io/badge/UI-Jetpack%20Compose-blue?style=flat-square&logo=jetpackcompose" alt="Jetpack Compose">
  </a>
  <a href="LICENSE">
    <img src="https://img.shields.io/badge/License-MIT-orange?style=flat-square" alt="License">
  </a>
</p>

---

## 📌 Overview

**Miau** (formerly **SafeCall**) is an open-source Android application designed to generate realistic fake incoming phone calls.

Whether you need an excuse to leave a boring date, escape an overrunning meeting, or gracefully exit an uncomfortable situation, Miau provides a believable incoming-call experience that can work even when the device screen is locked.

---

## ✨ Features

* 👤 **Custom Caller Information**
  Enter a custom caller name and phone number, such as *Boss*, *Mom*, or *Emergency Call*.

* 📇 **Native Contact Picker**
  Select a real contact directly from your device's phonebook.

* ⚡ **Quick Preset Timers**
  Schedule a fake call using quick presets:
  `5s` · `15s` · `30s` · `1m` · `5m`

* 🎚️ **Precision Timer**
  Use the timer control when a custom delay is required.

* 📱 **Realistic Call Interface**
  Incoming and active-call screens are built with Jetpack Compose.

* 🔒 **Lock Screen Support**
  Uses Android's full-screen notification and alarm APIs to display the incoming call experience when the device is locked.

* 👂 **Proximity Sensor Support**
  During an active fake call, bringing the device close to your ear can turn the screen off using the proximity sensor.

* 🔊 **Ringtone & Vibration**
  Uses the device's system ringtone and vibration capabilities to make the simulated call more convincing.

* 📋 **System Call Log Integration**
  Can insert simulated call records into the Android system call history.

---

## 🛠️ Tech Stack & Architecture

### Core Technologies

| Component               | Technology           |
| ----------------------- | -------------------- |
| Language                | Kotlin               |
| UI                      | Jetpack Compose      |
| Design                  | Material 3           |
| Minimum Android Version | Android 7.0 (API 24) |
| Target SDK              | Android 36           |
| Build System            | Gradle               |

### Android APIs

* `AlarmManager` — schedules the fake call and supports precise triggering.
* `BroadcastReceiver` — receives scheduled call events.
* `NotificationManager` — handles incoming-call notifications.
* `PendingIntent` — launches the full-screen call experience.
* `SensorManager` — handles proximity sensor events.
* `PowerManager` — manages screen/wake behavior.
* `RingtoneManager` — accesses the system ringtone.
* `VibratorManager` — controls vibration feedback.
* `CallLog` — optionally writes simulated call records.
* `ContactsContract` — provides native contact selection.

### Architecture

Miau follows a modern Android architecture using:

* Jetpack Compose
* Unidirectional Data Flow (UDF)
* Event-driven Android components
* Separation between UI and system-level functionality

---

## 📸 Screenshots

|             Setup Screen             |        Incoming Call        |            Active Call           |
| :----------------------------------: | :-------------------------: | :------------------------------: |
| *Configure caller details and delay* | *Full-screen incoming call* | *Active call timer and controls* |
|                  📱                  |              📱             |                📱                |

> Add screenshots to the repository and replace the placeholders above with image paths such as:
>
> `![Setup Screen](screenshots/setup.png)`

---

## 🚀 Getting Started

### Prerequisites

* Android Studio Ladybug (`2024.2.1`) or newer
* JDK 17
* Kotlin 2.0+
* Android SDK 36
* Android device or emulator running Android 7.0+ (API 24+)

### Installation

#### 1. Clone the repository

```bash
git clone https://github.com/Agnivesh-Anil-Warrier/Miau.git
cd Miau
```

#### 2. Open the project

Open the cloned project directory in **Android Studio**.

Allow Android Studio to sync the Gradle project and download any required dependencies.

#### 3. Build and run

Connect an Android device with USB debugging enabled, or start an Android emulator.

Then run the application from Android Studio.

Alternatively, build the debug APK with:

```bash
./gradlew assembleDebug
```

The generated APK will be located under:

```text
app/build/outputs/apk/debug/
```

---

## 🔐 Permissions

Miau requests several Android permissions to provide its simulated call experience.

| Permission               | Purpose                                                                     |
| ------------------------ | --------------------------------------------------------------------------- |
| `SCHEDULE_EXACT_ALARM`   | Allows fake calls to be triggered at precise scheduled times.               |
| `USE_FULL_SCREEN_INTENT` | Allows the incoming-call interface to appear as a full-screen notification. |
| `POST_NOTIFICATIONS`     | Allows Android notifications to be displayed.                               |
| `VIBRATE`                | Provides vibration feedback during simulated calls.                         |
| `WAKE_LOCK`              | Helps keep the device awake when a scheduled call is triggered.             |
| `WRITE_CALL_LOG`         | Allows simulated call records to be written to the system call history.     |
| `READ_CALL_LOG`          | Allows access to call-log information where required by the application.    |
| `READ_CONTACTS`          | Allows the native contact picker to access contact information.             |

Some permissions may be optional depending on which Miau features are being used.

---

## ⚠️ Android Version & Device Behavior

Some features depend on Android's system-level restrictions and may behave differently across manufacturers and Android versions.

In particular:

* Exact alarms may require additional user authorization.
* Full-screen notifications are subject to Android notification policies.
* Lock-screen behavior can vary between device manufacturers.
* Battery optimization and background execution restrictions may affect scheduled calls.
* Call-log modification is restricted by Android permissions and system policies.

For the most reliable experience, test Miau on a physical Android device.

---

## 📂 Project Structure

```text
Miau/
├── app/
│   └── src/
│       └── main/
│           ├── java/
│           │   └── com/example/safecall/
│           │       ├── ...
│           │       └── ...
│           │
│           └── res/
│               ├── drawable/
│               ├── mipmap/
│               └── values/
│
├── gradle/
├── build.gradle.kts
├── settings.gradle.kts
├── gradlew
├── gradlew.bat
├── LICENSE
└── README.md
```

> Update the package-specific files in this section if the project structure changes.

---

## 🧪 Testing

Miau is intended to be tested primarily on physical Android devices because several features interact directly with Android system services.

Recommended test cases include:

* Scheduled call while the application is open
* Scheduled call while the application is in the background
* Scheduled call while the screen is locked
* Device in Doze mode
* Incoming call ringtone and vibration
* Accepting and declining the simulated call
* Proximity sensor behavior
* Call-log insertion
* Contact picker
* Different Android versions and device manufacturers

---

## 📄 License

This project is licensed under the **MIT License**.

See the [`LICENSE`](LICENSE) file for the complete license text.

---

<p align="center">
  Made with ❤️ & Kotlin for quick escapes.
</p>
