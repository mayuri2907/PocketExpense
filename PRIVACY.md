# PocketExpense Privacy Policy

**Last Updated:** October 2026  
**Status:** Production / Release 1.0.0

---

## 1. Core Principle: 100% On-Device & Zero Network Connectivity

PocketExpense was designed from the ground up as a **strictly local-first** application. 

- **No Cloud Servers**: PocketExpense does not operate any backend servers or remote databases.
- **No Analytics / Telemetry**: PocketExpense contains no analytics SDKs (e.g., Firebase Analytics, Mixpanel, Google Analytics), crash trackers (e.g., Crashlytics, Sentry), or advertising networks.
- **No Internet Permission**: The `android.permission.INTERNET` permission is **deliberately absent** from the `AndroidManifest.xml`. PocketExpense cannot communicate with the internet, upload your financial data, or download external resources.

---

## 2. Permissions Requested & Why

PocketExpense requests only the minimum necessary permissions to automate expense tracking:

### A. Notification Access (`BIND_NOTIFICATION_LISTENER_SERVICE`)
- **Purpose**: Allows `ExpenseNotificationListenerService` to receive incoming system notification events.
- **Scope**: The service inspects notification events only from 4 supported UPI payment apps:
  - Google Pay (`com.google.android.apps.nbu.paisa.user`)
  - PhonePe (`com.phonepe.app`)
  - Paytm (`net.one97.paytm`)
  - CRED (`com.dreamplug.androidapp`)
- **Non-Payment Notifications**: All notifications from other applications are discarded immediately without inspection.

### B. Display Over Other Apps (`android.permission.SYSTEM_ALERT_WINDOW`)
- **Purpose**: Displays the 3-second floating category popup when an expense is detected, allowing you to quickly choose a category without opening the app.
- **Optional**: You can revoke overlay permission at any time. PocketExpense will still record transactions in the background and suggest categories automatically based on merchant history.

### C. Post Notifications (`android.permission.POST_NOTIFICATIONS`)
- **Purpose**: Required on Android 13 (API 33)+ to show system notification alerts (such as transaction confirmations or category reminders) if enabled.

---

## 3. How Notification Data Is Handled

### In-Memory Processing
When a notification arrives from a supported payment application:
1. The notification title and text are scanned in volatile device memory using strict regex patterns to extract the **amount (in integer paise)**, **merchant name**, and **reference ID**.
2. **Strict Sensitive Keyword Filtering**: The parser checks for and immediately rejects messages containing words like:
   - `OTP`, `code`, `PIN`, `verification`, `password`, `login`, `refund`, `cashback`, `failed`, `declined`, `reversed`, `cancelled`.
3. **No Full-Text Retention**: The raw notification body is discarded once the parsed fields are extracted. Only the structured transaction record is stored.
4. **No Sensitive Logging**: In release builds, `BuildConfig.DEBUG` guards disable verbose logcat output, ensuring transaction details and merchant names are never written to the Android system log.

---

## 4. Local Database & Backup Policies

- **Storage Location**: All data (expenses, categories, merchant rules) is stored in a local SQLite database managed by Android Room in your device's private sandbox:
  ```
  /data/user/0/com.example.pocketexpense/databases/pocket_expense_db
  ```
- **Cloud Backup Exclusion**:
  PocketExpense configures Android 12+ `data_extraction_rules.xml` and legacy `backup_rules.xml` to explicitly **exclude** the database and shared preferences from automated Google Drive / cloud backups:
  ```xml
  <cloud-backup>
      <exclude domain="database" path="pocket_expense_db" />
  </cloud-backup>
  ```
- **Device-to-Device Migration**:
  Encrypted device-to-device transfers (such as when migrating to a new phone via direct cable or Wi-Fi Direct) are permitted so you do not lose your financial history during phone upgrades.

---

## 5. Data Export & User Control

- **CSV Export**: You can export your entire transaction history to an RFC-4180 compliant CSV file at any time from **Settings -> Export All Expenses to CSV**. The file is generated in the app's cache directory and shared exclusively via Android's native system Sharesheet.
- **Data Deletion**:
  - Individual expenses can be deleted from the Transactions screen.
  - Uninstalling the application or selecting **Clear Storage** in Android Settings permanently wipes all stored expenses and categories from the device.

---

## 6. Contact & Open Source Auditing

PocketExpense is an open and transparent application. Because the app has no internet access, any user or security engineer can inspect network activity via Android Profiler, NetGuard, or Wireshark and verify that zero bytes of data leave the device.
