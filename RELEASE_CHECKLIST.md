# PocketExpense — Production Release Checklist

Use this checklist before distributing a new release build (e.g., GitHub Releases, F-Droid, or Google Play).

---

## 1. Versioning & Build Configuration
- [ ] **Application ID**: Verify `applicationId = "com.example.pocketexpense"` in `app/build.gradle.kts`.
- [ ] **SDK Targets**:
  - `minSdk = 26` (Android 8.0 Oreo)
  - `targetSdk = 35` (Android 15)
  - `compileSdk = 36` (Android 16 preview/stable)
- [ ] **Version Code & Name**: Bump `versionCode` (monotonically increasing integer) and `versionName` (semantic versioning, e.g., `1.0.1`).
- [ ] **BuildConfig**: Ensure `buildConfig = true` is enabled.

---

## 2. Test Suite & Static Analysis
- [ ] **Unit Tests**: Run all unit tests via command line:
  ```bash
  ./gradlew test
  ```
  *Requirement: 50/50 tests passing (0 failures, 0 skipped, 100% success rate).*
- [ ] **Lint Vital Check**: Run release lint validation:
  ```bash
  ./gradlew lintVitalRelease
  ```
  *Requirement: No fatal release lint errors.*

---

## 3. Simulator & Debug Code Isolation
- [ ] **UI Audit**: Verify that all test simulator buttons, fake payment triggers, and queue test controls are absent from:
  - Settings screen
  - Dashboard screen
  - Navigation bar and menus
- [ ] **Test Fixtures Isolation**: Ensure `NotificationTestFixtures.kt` is strictly in `src/test/` and not packaged in release APK.
- [ ] **Logcat Privacy**: Verify that all sensitive logging in `ExpenseNotificationListenerService.kt` is wrapped in `if (BuildConfig.DEBUG)`.

---

## 4. Signing Key Setup & Verification
- [ ] **Generate Production Keystore** (one-time setup):
  ```bash
  keytool -genkey -v -keystore release.jks -keyalg RSA -keysize 2048 -validity 10000 -alias pocketexpense
  ```
- [ ] **Configure Environment Variables**:
  ```bash
  export POCKETEXPENSE_RELEASE_STORE_FILE="/path/to/release.jks"
  export POCKETEXPENSE_RELEASE_STORE_PASSWORD="your-keystore-password"
  export POCKETEXPENSE_RELEASE_KEY_ALIAS="pocketexpense"
  export POCKETEXPENSE_RELEASE_KEY_PASSWORD="your-key-password"
  ```
- [ ] **Build Release Artifacts**:
  ```bash
  ./gradlew assembleRelease
  ./gradlew bundleRelease
  ```
- [ ] **Verify Signature with `apksigner`**:
  ```bash
  apksigner verify --verbose app/build/outputs/apk/release/app-release.apk
  ```
  *Requirement: Verified using v2/v3 scheme, no signing warnings.*

---

## 5. Device Verification (Emulator / Hardware)
- [ ] **Clean Install**:
  ```bash
  adb install -r app/build/outputs/apk/release/app-release.apk
  ```
- [ ] **Launch Test**: Verify app launches immediately without crashing to the Dashboard screen.
- [ ] **Permission Granting**:
  - Open Settings screen.
  - Enable "Notification Access" and confirm toggle responds.
  - Enable "Floating Overlay Permission" and confirm toggle opens system settings.
- [ ] **CSV Export**: Tap "Export All Expenses to CSV" and verify Android Sharesheet opens with a valid CSV attachment.
- [ ] **Database Downgrade Protection**: Verify `fallbackToDestructiveMigrationOnDowngrade()` is active and preserves existing SQLite records across app updates.

---

## 6. Rollback & Emergency Plan
- In the event that a critical issue is discovered after distribution:
  1. **Do not force a database schema rollback with destructive recreation.**
  2. Increment `versionCode` and deploy a hotfix APK.
  3. If downgrading to a previous APK is required, note that user data will be wiped due to `fallbackToDestructiveMigrationOnDowngrade()`. Instruct users to use **Export to CSV** prior to downgrading.
