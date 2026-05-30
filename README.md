# 🌐 FamilyBrowser — Android Browser App

Fast, Safe & Family-Friendly browser built with **Kotlin + Jetpack Compose**.  
Play Store ready. No external extensions needed — everything is built in.

---

## 📁 Project Structure

```
FamilyBrowser/
├── app/
│   ├── src/main/
│   │   ├── java/com/familybrowser/
│   │   │   ├── MainActivity.kt          ← UI (Jetpack Compose)
│   │   │   ├── BrowserViewModel.kt      ← Central state management
│   │   │   ├── AdBlocker.kt             ← Ad/tracker/adult content blocking
│   │   │   ├── TabManager.kt            ← Multi-tab system
│   │   │   ├── UserProfileManager.kt    ← Multi-profile + Kids/Guest modes
│   │   │   ├── DownloadManager.kt       ← Built-in download manager
│   │   │   ├── BrowserApplication.kt   ← App class
│   │   │   └── service/
│   │   │       └── BackgroundAudioService.kt  ← YouTube background play
│   │   ├── res/
│   │   │   ├── values/
│   │   │   │   ├── strings.xml          ← All strings (localizable)
│   │   │   │   ├── colors.xml
│   │   │   │   └── themes.xml
│   │   │   ├── xml/
│   │   │   │   ├── network_security_config.xml
│   │   │   │   ├── file_paths.xml
│   │   │   │   ├── backup_rules.xml
│   │   │   │   └── data_extraction_rules.xml
│   │   │   └── drawable/
│   │   │       └── ic_splash.xml
│   │   └── AndroidManifest.xml
│   ├── build.gradle.kts                 ← App-level Gradle
│   └── proguard-rules.pro
├── build.gradle.kts                     ← Project-level Gradle
├── settings.gradle.kts
├── gradle.properties
└── gradle/wrapper/
    └── gradle-wrapper.properties
```

---

## 🚀 Build & Run

### Requirements
- Android Studio Hedgehog (2023.1.1) or newer
- JDK 17
- Android SDK 34

### Steps
1. Open **Android Studio** → `File > Open` → select `FamilyBrowser/`
2. Let Gradle sync complete
3. Add a launcher icon:
   - Right-click `res/` → `New > Image Asset` → set up `ic_launcher`
4. Run on device or emulator (API 26+)

---

## 🔑 Key Features

| Feature | Status |
|---|---|
| Ad Blocker (17+ networks) | ✅ Built-in |
| Tracker Blocker (30+ domains) | ✅ Built-in |
| Adult Content Filter + PIN | ✅ Built-in |
| Multi-Tab (Chrome style) | ✅ Built-in |
| Multi-Profile (up to 5) | ✅ Built-in |
| Kids Mode (whitelist only) | ✅ Built-in |
| Guest Mode (no history) | ✅ Built-in |
| YouTube Background Play | ✅ Built-in |
| Download Manager | ✅ Built-in |
| Dark Mode injection | ✅ Built-in |
| Reader Mode | ✅ Built-in |
| Find in Page | ✅ Built-in |
| Desktop Mode | ✅ Built-in |
| Incognito Tabs | ✅ Built-in |
| DNS over HTTPS ready | ✅ Cloudflare 1.1.1.1 |
| Hardware Acceleration | ✅ Enabled |
| ProGuard / R8 | ✅ Configured |

---

## 📦 Play Store Checklist

- [ ] Add `ic_launcher` and `ic_launcher_round` icons (512×512 for store listing)
- [ ] Create a release keystore and configure `signingConfigs` in `build.gradle.kts`
- [ ] Set `versionCode` / `versionName` for each release
- [ ] Add your Privacy Policy URL to Play Console
- [ ] Fill out content rating questionnaire (browser = generic rating)
- [ ] Test on API 26 (min) and API 34 (target)

---

## 🔒 Security Notes

- All ad/adult blocking is done **locally** — no data sent to any server
- Passwords stored in `EncryptedSharedPreferences` (AES-256-GCM)
- `android:usesCleartextTraffic="false"` — HTTPS enforced
- Profile PINs are stored encrypted
- Guest mode auto-clears all data on exit

---

## 📜 License

MIT License — free for personal and commercial use.
