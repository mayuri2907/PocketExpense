# PocketExpense — Deployment & Distribution Guide

This guide covers how to build, sign, and distribute PocketExpense for **free direct APK distribution** (e.g., via GitHub Releases) as well as **Google Play Store distribution** (via Android App Bundle).

---

## 1. Direct APK Sideloading vs. Google Play Store

| Feature | Direct APK (GitHub Releases) | Google Play Store (AAB) |
|---|---|---|
| **Cost** | 100% Free | $25 one-time developer registration fee |
| **Artifact Format** | Universal `.apk` | Dynamic feature / App Bundle `.aab` |
| **Review Process** | None; instant deployment | 2–7 days Google Play policy review |
| **Special Permissions** | Allowed (Notification Listener, System Alert Window) | Requires prominent disclosures & privacy policy URL |
| **Updates** | User downloads new APK manually or via in-app checker | Automated background updates via Play Store |

---

## 2. Generating a Production Signing Key

To distribute release APKs that can be installed and updated on users' devices without signature mismatch errors, generate a permanent release keystore:

### Step 1: Run `keytool`
```bash
keytool -genkey -v -keystore pocketexpense-release.jks \
  -keyalg RSA -keysize 2048 -validity 10000 \
  -alias pocketexpense \
  -storetype JKS
```

### Step 2: Store Credentials Securely
> **CRITICAL SECURITY RULE**: Never commit `pocketexpense-release.jks` or passwords to git or public repositories. Add `*.jks` to `.gitignore`.

### Step 3: Configure Environment Variables
Set the following environment variables on your build machine or CI/CD environment:

**On Linux / macOS:**
```bash
export POCKETEXPENSE_RELEASE_STORE_FILE="/absolute/path/to/pocketexpense-release.jks"
export POCKETEXPENSE_RELEASE_STORE_PASSWORD="your_keystore_password"
export POCKETEXPENSE_RELEASE_KEY_ALIAS="pocketexpense"
export POCKETEXPENSE_RELEASE_KEY_PASSWORD="your_key_password"
```

**On Windows (PowerShell):**
```powershell
$env:POCKETEXPENSE_RELEASE_STORE_FILE="C:\path\to\pocketexpense-release.jks"
$env:POCKETEXPENSE_RELEASE_STORE_PASSWORD="your_keystore_password"
$env:POCKETEXPENSE_RELEASE_KEY_ALIAS="pocketexpense"
$env:POCKETEXPENSE_RELEASE_KEY_PASSWORD="your_key_password"
```

*Note: If these environment variables are not set, `app/build.gradle.kts` falls back to the local Android debug keystore for development and local testing.*

---

## 3. Building Release Artifacts

### A. Build Universal Release APK (for Direct Sideloading)
```bash
./gradlew assembleRelease
```
- **Output Artifact**: `app/build/outputs/apk/release/app-release.apk`
- **Use Case**: Direct distribution via GitHub Releases, Telegram, website download, or local adb installation.

### B. Build Android App Bundle (for Google Play Store)
```bash
./gradlew bundleRelease
```
- **Output Artifact**: `app/build/outputs/bundle/release/app-release.aab`
- **Use Case**: Uploading to Google Play Console.

---

## 4. Verifying APK Signature

Before distributing an APK, verify that it is properly signed and conforms to Android's APK Signature Scheme v2/v3 using the Android SDK `apksigner` tool:

```bash
apksigner verify --verbose app/build/outputs/apk/release/app-release.apk
```

Expected output:
```
Verifies
Verified using v1 scheme (JAR signing): true
Verified using v2 scheme (APK Signature Scheme v2): true
Verified using v3 scheme (APK Signature Scheme v3): true
```

---

## 5. Free Direct Distribution via GitHub Releases

1. **Tag the Release**:
   ```bash
   git tag -a v1.0.0 -m "PocketExpense v1.0.0 Production Release"
   git push origin v1.0.0
   ```
2. **Draft a GitHub Release**:
   - Go to your GitHub repository -> **Releases** -> **Draft a new release**.
   - Select tag `v1.0.0`.
   - Title: `PocketExpense v1.0.0`.
   - Attach `app/build/outputs/apk/release/app-release.apk`.
   - Rename the attachment to `PocketExpense-v1.0.0.apk` for user clarity.
   - Publish release.

---

## 6. End-User Installation & Sideloading Instructions

Provide these instructions to users downloading the APK directly:

1. **Download the APK**: Download `PocketExpense-v1.0.0.apk` onto your Android phone.
2. **Allow Installation from Browser**:
   - Tap the downloaded file.
   - If prompted: **"For your security, your phone is not allowed to install unknown apps from this source"**, tap **Settings** and turn on **"Allow from this source"**.
3. **Google Play Protect Notice**:
   - Because PocketExpense is distributed directly and not signed by Google Play, Google Play Protect may display a prompt saying *"Unrecognized app"*.
   - Tap **"More details"** -> **"Install anyway"**.
4. **Grant Permissions**:
   - Open PocketExpense.
   - Navigate to **Settings** and enable **Notification Access** (required to read UPI payment notifications).
   - Enable **Floating Category Overlay** (recommended for the 3-second quick category popup).
5. **Upgrading to Newer Versions**:
   - Simply download and install the new APK over the existing installation.
   - As long as both APKs are signed with the same release key, all expenses, categories, and settings will be preserved without data loss.
