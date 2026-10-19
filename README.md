# PocketExpense

PocketExpense is a privacy-first, local-only Android expense tracker built with modern Jetpack Compose and Room. It automatically captures UPI payment transactions from incoming system notifications, offers an instant 3-second floating category selection popup, and organizes your financial life entirely on your device with **zero cloud dependencies and zero internet permissions**.

---

## Key Features

- **Automatic Payment Detection**: Detects and parses UPI payment notifications from supported apps (Google Pay, PhonePe, Paytm, CRED) in real time.
- **3-Second Floating Category Popup**: A lightweight, non-intrusive floating overlay (powered by Android's `WindowManager`) prompts you to confirm or change the expense category right after a transaction. If dismissed or ignored, it auto-finalizes with a smart category suggestion.
- **Paise Precision**: All monetary values are calculated and stored as integer paise (`Long`), eliminating floating-point rounding errors.
- **Zero Internet / Local-Only Privacy**: Does not request `android.permission.INTERNET`. Your financial data never leaves your device. No analytics, no telemetry, no tracking.
- **Clean Dark Theme UI**: A refined Jetpack Compose dashboard with monthly spending totals, categorized breakdowns, quick actions, and transaction search.
- **Data Portability**: User-initiated RFC-4180 compliant CSV export via Android's native system Sharesheet.
- **OEM Reliability Guide**: Built-in troubleshooting recommendations for aggressive background-process battery managers (MIUI/HyperOS, OneUI, ColorOS/FuntouchOS).

---

## Tech Stack & Architecture

- **Language**: Kotlin 2.2.10
- **UI Toolkit**: Jetpack Compose with Material 3
- **Local Persistence**: Room SQLite 2.8.2 (integer paise storage, robust migrations)
- **Async & Reactive Flow**: Kotlin Coroutines & StateFlow
- **Background Ingestion**: `ExpenseNotificationListenerService` (`NotificationListenerService`)
- **Quick-Tag Overlay**: Custom `WindowManager` overlay with lifecycle-aware ComposeView
- **Architecture**: MVVM with Repository pattern and clean separation of concerns

---

## Supported Payment Applications

PocketExpense parses locally visible notification text from:
1. **Google Pay** (`com.google.android.apps.nbu.paisa.user`)
2. **PhonePe** (`com.phonepe.app`)
3. **Paytm** (`net.one97.paytm`)
4. **CRED** (`com.dreamplug.androidapp`)

> **Note**: PocketExpense only interprets locally visible notifications on your device. It never connects to banking APIs, never requests bank credentials, and strictly filters out OTPs, PIN prompts, refunds, and failed transactions.

---

## Getting Started

### Prerequisites
- **JDK**: Version 17 or 21+
- **Android SDK**: `minSdk 26` (Android 8.0 Oreo), `targetSdk 35` (Android 15), `compileSdk 36`
- **Gradle**: 9.6.0 (wrapper included)

### Building from Source

1. Clone or open the repository in Android Studio:
   ```bash
   git clone <repo-url>
   cd PocketExpense
   ```

2. Run the automated unit test suite (50 tests):
   ```bash
   ./gradlew test
   ```

3. Build the debug APK:
   ```bash
   ./gradlew assembleDebug
   ```
   Output: `app/build/outputs/apk/debug/app-debug.apk`

4. Build the release APK:
   ```bash
   ./gradlew assembleRelease
   ```
   Output: `app/build/outputs/apk/release/app-release.apk`

5. Build the release Android App Bundle (AAB):
   ```bash
   ./gradlew bundleRelease
   ```
   Output: `app/build/outputs/bundle/release/app-release.aab`

---

## Required Android Permissions

PocketExpense requests only the minimum permissions necessary for its core features:

| Permission | Purpose |
|------------|---------|
| `BIND_NOTIFICATION_LISTENER_SERVICE` | Required to read payment alerts from Google Pay, PhonePe, Paytm, and CRED. |
| `android.permission.SYSTEM_ALERT_WINDOW` | Required to display the 3-second floating category popup over other apps. |
| `android.permission.POST_NOTIFICATIONS` | Allows PocketExpense to post reminders and system notifications (Android 13+). |

*Note: `android.permission.INTERNET` is deliberately omitted.*

---

## Manufacturer-Specific Background Settings

Certain Android manufacturers aggressively kill background services or prevent floating overlays:

- **Xiaomi / Redmi / POCO (MIUI / HyperOS)**:
  - Settings -> Apps -> Manage Apps -> PocketExpense: Enable **Autostart**.
  - Battery Saver: Set to **No restrictions**.
  - Other Permissions: Allow **Display pop-up windows while running in the background**.
- **Samsung (OneUI)**:
  - Settings -> Battery -> Background usage limits: Ensure PocketExpense is in **Never sleeping apps**.
- **Oppo / Realme / OnePlus (ColorOS / OxygenOS)**:
  - App Info -> Battery usage: Allow **Background activity** and **Auto-launch**.
- **Vivo / iQOO (FuntouchOS / OriginOS)**:
  - Settings -> Battery -> High background power consumption: Enable for PocketExpense.

---

## Documentation

- [Privacy Policy](file:///c:/Users/chaud/AndroidStudioProjects/PocketExpense/PRIVACY.md)
- [Release Checklist](file:///c:/Users/chaud/AndroidStudioProjects/PocketExpense/RELEASE_CHECKLIST.md)
- [Testing Report](file:///c:/Users/chaud/AndroidStudioProjects/PocketExpense/TESTING_REPORT.md)
- [Deployment Guide](file:///c:/Users/chaud/AndroidStudioProjects/PocketExpense/DEPLOYMENT.md)

---

## License

Personal and Open Source use. Distributed without warranty.
