# Contributing to LAMIA 🤝

Thank you for your interest in contributing to **LAMIA (AI Assistive Hands-Free Controller & AAC Platform)**! 

Our mission is to make dignity, communication, and independence accessible to everyone with severe motor impairments at zero hardware cost — turning everyday smartphones and AI smart glasses into assistive lifelines.

---

## 📋 Code of Conduct

We are committed to providing a welcoming, inclusive, and harassment-free environment. Please treat all contributors, testers, caregivers, and community members with respect and kindness.

---

## 🛠️ Development Setup

### Prerequisites
- **Android Studio** (Hedgehog 2023.1.1+, Iguana, Jellyfish, or newer)
- **JDK 17** (standard for modern Android Gradle Plugin)
- **Android Device** running Android 9.0 (API 28, Pie) or higher with front-facing camera and Bluetooth capability
  - *Note: Android Emulator does not support Bluetooth HID Device peripheral mode. Physical hardware is required for end-to-end Bluetooth HID testing.*

### Clone & Build
```bash
git clone https://github.com/YOUR_USERNAME/lamia.git
cd lamia
```
Build debug APK:
```bash
./gradlew assembleDebug
```
The output APK is located at:
`app/build/outputs/apk/debug/app-debug.apk`

---

## 🏗️ Project Architecture

```
lamia/
├── app/src/main/java/com/lamia/assistive/
│   ├── MainActivity.kt               # Central coordinator & Activity lifecycle
│   ├── config/
│   │   ├── GestureConfig.kt          # Dynamic thresholds & cooldown timings
│   │   └── UserSettings.kt           # Caregiver personalization & voice settings
│   ├── gestures/
│   │   ├── ModeSwitchDetector.kt     # 5-gaze sequence detector with deadzones
│   │   ├── RemoteNavigationHandler.kt# D-Pad auto-repeat & head roll actions
│   │   └── LockGestureDetector.kt    # Smile controller lock detector
│   ├── vision/
│   │   ├── FaceGestureAnalyzer.kt    # ML Kit Face Detection & EMA tremor smoothing
│   │   ├── FaceGestureState.kt       # Sensor data model
│   │   └── HeadPoseDirection.kt      # Directional enum (UP, DOWN, LEFT, RIGHT, CENTER)
│   ├── bluetooth/
│   │   ├── BluetoothController.kt    # Bluetooth HID peripheral profile service
│   │   ├── KeyboardSender.kt         # Atomic HID keystroke report dispatcher
│   │   └── reports/                  # USB HID byte report structures
│   ├── speech/
│   │   └── AacSpeechManager.kt       # Voice Banking & personalized TTS engine
│   └── ui/
│       └── UiStateManager.kt         # D-Pad highlights, pills, animations, guidance
```

---

## 🎯 High-Priority Contribution Areas

1. **Accessibility & Clinical Testing:**
   - Feedback from caregivers, speech therapists, and individuals with Cerebral Palsy or ALS.
2. **Additional Language Packs:**
   - Add new language localizations in `app/src/main/res/values-[lang]/strings.xml`.
3. **Landscape / Wheelchair Mount Mode:**
   - Support dynamic sensor rotation for horizontal wheelchair clamp brackets.
4. **Mouse Pointer HID Emulation:**
   - Wire up the existing HID Mouse collection descriptor for smooth head-guided cursor tracking.
5. **Caregiver Settings UI:**
   - A dedicated Settings Activity/Dialog to tune sensitivity sliders without manual code edits.

---

## 🚀 Pull Request Guidelines

1. Fork the repository and create a feature branch:
   ```bash
   git checkout -b feature/my-new-feature
   ```
2. Commit your changes with clear, descriptive commit messages:
   ```bash
   git commit -m "feat(gestures): add configurable blink-to-click detection"
   ```
3. Ensure the project compiles cleanly:
   ```bash
   ./gradlew compileDebugSources
   ```
4. Push to your branch and open a Pull Request against `main`.

---

## 📄 License
By contributing to LAMIA, you agree that your contributions will be licensed under the **Apache License 2.0**.
