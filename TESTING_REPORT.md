# PocketExpense — Automated & Manual Testing Report

**Date:** October 2026  
**Build:** Release 1.0.0 (`versionCode 1`, `versionName "1.0.0"`)  
**Test Suite Status:** 50/50 Tests Passed (100% Success Rate)

---

## 1. Automated Unit Test Summary

All automated unit tests are executed via Gradle using the command:
```bash
./gradlew test
```

### Test Execution Results

| Test Suite Class | Tests Run | Failures | Errors | Skipped | Status |
|---|---|---|---|---|---|
| `com.example.pocketexpense.CurrencyUtilsTest` | 9 | 0 | 0 | 0 | **PASSED** |
| `com.example.pocketexpense.DuplicateDetectionTest` | 5 | 0 | 0 | 0 | **PASSED** |
| `com.example.pocketexpense.ExampleUnitTest` | 1 | 0 | 0 | 0 | **PASSED** |
| `com.example.pocketexpense.ExpenseCalculationTest` | 5 | 0 | 0 | 0 | **PASSED** |
| `com.example.pocketexpense.FloatingExpensePopupTest` | 8 | 0 | 0 | 0 | **PASSED** |
| `com.example.pocketexpense.NotificationParserTest` | 13 | 0 | 0 | 0 | **PASSED** |
| `com.example.pocketexpense.ProductionHardeningTest` | 8 | 0 | 0 | 0 | **PASSED** |
| `com.example.pocketexpense.RoomMigrationTest` | 1 | 0 | 0 | 0 | **PASSED** |
| **TOTAL** | **50** | **0** | **0** | **0** | **100% SUCCESS** |

Duration: ~1.173 seconds.

---

## 2. Detailed Breakdown of Test Suites

### A. CurrencyUtilsTest (9 Tests)
- `formatPaiseToRupees formats zero paise correctly`
- `formatPaiseToRupees formats sub-rupee amounts correctly`
- `formatPaiseToRupees formats standard rupee amounts correctly`
- `formatPaiseToRupees formats large amounts with Indian comma grouping`
- `formatPaiseToRupees formats negative amounts correctly`
- `parseRupeesToPaise handles integer rupee strings`
- `parseRupeesToPaise handles decimal rupee strings`
- `parseRupeesToPaise handles comma separated inputs`
- `parseRupeesToPaise returns null for invalid input`

### B. DuplicateDetectionTest (5 Tests)
- `same notification within 60 seconds is detected as duplicate`
- `different amounts within 60 seconds are not duplicates`
- `different merchants within 60 seconds are not duplicates`
- `same notification after 60 seconds is not duplicate`
- `different packages with same amount and merchant are not duplicates`

### C. ExampleUnitTest (1 Test)
- `addition_isCorrect`

### D. ExpenseCalculationTest (5 Tests)
- `calculateTotalExpense returns correct sum in paise`
- `calculateTotalExpense with empty list returns zero`
- `groupExpensesByCategory aggregates amounts correctly`
- `calculatePercentage handles single category as 100 percent`
- `filterExpensesByMonth filters correctly`

### E. FloatingExpensePopupTest (8 Tests)
- `queue retains single pending expense`
- `FIFO order is preserved with multiple pending expenses`
- `category selection finalizes expense with chosen category`
- `timeout finalizes expense with suggested category`
- `dismiss retains expense with suggested category`
- `concurrent category selections are thread-safe and idempotent`
- `queue overflow retains expense in database without overlay`
- `popup does not show when overlay permission is denied`

### F. NotificationParserTest (13 Tests)
- `parse Google Pay paid notification`
- `parse PhonePe paid notification`
- `parse Paytm paid notification`
- `parse CRED paid notification`
- `reject incoming credit transfer in Google Pay`
- `reject incoming credit transfer in PhonePe`
- `reject incoming credit transfer in Paytm`
- `reject incoming credit transfer in CRED`
- `reject OTP notification`
- `reject PIN prompt notification`
- `reject unsupported package notification`
- `handle amount with commas correctly`
- `handle amount without decimal places correctly`

### G. ProductionHardeningTest (8 Tests)
- `csv export formats headers and rows in compliance with rfc4180`
- `csv export escapes quotes and commas correctly`
- `sensitive notification keywords are rejected`
- `failed and refund notifications are rejected`
- `deduplication hash combines package, amount, and merchant accurately`
- `amount boundaries: sub-rupee and large amounts convert to paise precisely`
- `test simulator controls are isolated from production classes`
- `test fixtures file is absent from production source set`

### H. RoomMigrationTest (1 Test)
- `migration 1 to 2 adds notificationPackage and notificationKey columns` (tested via dynamic proxy interception of `SupportSQLiteDatabase.execSQL`).

---

## 3. Static Analysis & Build Verification

1. **Lint Vital Analysis**:
   ```bash
   ./gradlew lintVitalRelease
   ```
   *Result: Passed with 0 errors.*
2. **Release APK Assembly**:
   ```bash
   ./gradlew assembleRelease
   ```
   *Result: Built `app-release.apk` (12,459,975 bytes).*
3. **Release App Bundle Assembly**:
   ```bash
   ./gradlew bundleRelease
   ```
   *Result: Built `app-release.aab` (12,103,124 bytes).*

---

## 4. Emulator & Runtime Verification

- **Target Device**: Android Virtual Device `emulator-5554`
- **OS Version**: Android 16 (API Level 36, `VanillaIceCream`)
- **Package Name**: `com.example.pocketexpense`
- **Activity**: `com.example.pocketexpense.MainActivity`
- **Verification Highlights**:
  - `adb install -r app-release.apk` succeeded cleanly.
  - App launched directly to Dashboard.
  - Monthly spending summary, empty state, and Quick Action buttons displayed correctly.
  - Settings screen displayed with:
    - Notification Access toggle
    - Floating Category Overlay toggle
    - Export All Expenses to CSV action
    - Phone Reliability Guide with Xiaomi, Samsung, Oppo/Vivo, and Stock Android recommendations.
  - Test Simulator button and simulator controls verified completely absent.

---

## 5. Verified Compatibility vs. Unverified Real-Device Requirements

### Verified:
- All 50 unit tests for logic, arithmetic, parsing, deduplication, and Room migration.
- Clean launch, UI rendering, and navigation on Android 16 (API 36).
- R8/ProGuard exclusion and compatibility with Kotlin 2.2.10 stdlib.
- Database downgrade safety without destructive recreation on upgrades.

### Unverified / Requires Physical Device Testing:
- Real notification format changes from Google Pay, PhonePe, Paytm, or CRED under live bank push notifications.
- OEM-specific battery optimization kill behavior under deep doze mode (e.g., Xiaomi MIUI battery saver, Samsung sleeping app policy).
- Floating overlay touch interaction across physical notch/hole-punch cutouts on varied hardware screens.
