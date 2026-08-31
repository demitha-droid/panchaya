# 🌸 Dhamma Sermons Android App (ජෙතවනාරාම & පහුර)

A sleek, modern, and minimal native Android application to stream, download, and organize Buddhist sermons from **Jethavanarama Monastery** and the **Pahura YouTube Channel**, featuring a 100% free YouTube audio/MP3 extraction and download engine, background audio playback with lock-screen media controls, and offline storage.

---

## ✨ Features

- **🏯 Jethavanarama Sermons**:
  - Direct integration with [Jethavanarama Monastery Sermons](https://www.jethavanarama.org/sermons).
  - Browse categories: *මහා දේශනාව (Maha Deshanawa)*, *සාවකෝ (Savako)*, *අනුත්තරෝ (Anuththaro)*, *යතිවර යාත්‍රාව (Yathiwara Yathrawa)*, *තෙරවරු (Theravaru)*, *න්‍යාය දේශනා (Niyaya Deshana)*, *භාවනා වැඩසටහන (Bawana)*, *ප්‍රායෝගික නිවන් මග (Prayogika Niwan Maga)*, *ථේරෝපදේශය (Theropadeshaya)*.
  - Filter by Thero (*ගුරු ස්වාමීන් වහන්සේ, පූජ්‍ය බත්තරමුල්ලේ අමාදස්සන හිමි, පූජ්‍ය දොඩංගොඩ ධම්ම ජීව හිමි, ආදී*).
  - Multilingual support: Sinhala, English, and Hindi.
  - Direct high-fidelity MP3 streaming and one-click downloading.

- **🎙️ Pahura (පහුර) YouTube Sermons**:
  - Live video and audio feed from [@pahura](https://www.youtube.com/channel/UC2w5Et5n0nfhHHGUrmogs2A).
  - **100% Free YouTube MP3/Audio Downloader**: Resolves high-quality audio streams (M4A/MP3/Opus) on-device using NewPipeExtractor without requiring paid API keys or third-party subscriptions.

- **🎧 Audio Engine (AndroidX Media3 ExoPlayer)**:
  - Background audio playback with lock-screen media controls and Bluetooth headset button support.
  - Variable playback speed: `0.75x`, `1.0x`, `1.25x`, `1.5x`, `1.75x`, `2.0x`.
  - Sleep Timer: `15m`, `30m`, `45m`, `60m` with live countdown.
  - Fast forward (+30s) and rewind (-10s).
  - Remembers last played position to resume exactly where you left off.

- **💾 Offline Library & Storage Manager**:
  - Downloaded sermons are saved in local storage and tracked with Room SQLite Database.
  - Listen 100% offline without any internet connection.
  - Storage space meter and easy delete/management.

- **🎨 Sleek, Modern, Minimal Design**:
  - Serene obsidian dark mode palette with sacred saffron/amber accents.
  - Floating bottom mini-player.
  - Expandable full-screen player modal sheet with album art glow.

---

## 🛠️ Architecture & Tech Stack

- **Platform**: Android Native (Kotlin 2.0 + Jetpack Compose + Material 3)
- **Audio Engine**: AndroidX `Media3` (`ExoPlayer` + `MediaSessionService`)
- **Database**: AndroidX `Room`
- **Network & Scraping**: `OkHttp3` + `Jsoup`
- **YouTube Media Extraction**: `NewPipeExtractor`
- **Image Loading**: `Coil 2`

---

## 🚀 How to Run & Build in Android Studio

1. Open **Android Studio**.
2. Select **Open** and choose this folder: `c:\Users\d3m1t\OneDrive\Documents\Do app`.
3. Let Gradle sync dependencies automatically.
4. Connect an Android device or launch an Android Emulator.
5. Click the green **Run ▶** button to build and install the APK!
