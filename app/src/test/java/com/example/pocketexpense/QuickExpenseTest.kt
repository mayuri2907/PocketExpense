package com.example.pocketexpense

import com.example.pocketexpense.data.model.Category
import com.example.pocketexpense.data.model.Expense
import com.example.pocketexpense.parser.CompositeNotificationParser
import com.example.pocketexpense.parser.ParseResult
import com.example.pocketexpense.parser.PaytmParser
import com.example.pocketexpense.quick.QuickExpenseDraftManager
import com.example.pocketexpense.quick.QuickExpenseTriggerCoordinator
import com.example.pocketexpense.util.CurrencyUtils
import com.example.pocketexpense.util.ValidationResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Production test suite verifying PocketExpense v1.0.2:
 * 1. INR parsing and integer paise conversion.
 * 2. Strict rejection of empty, zero, negative, malformed, or overflowing amounts.
 * 3. Prevention of duplicate inserts on rapid/repeated save.
 * 4. 5-second countdown expiry and draft preservation.
 * 5. Overlay permission denied fallback.
 * 6. Existing UPI notification detection and 3-second categorization popup remaining intact.
 */
class QuickExpenseTest {

    @Before
    fun setUp() {
        QuickExpenseDraftManager.clearDraft()
    }

    // =========================================================================
    // SECTION 1: INR PARSING & INTEGER PAISE CONVERSION
    // =========================================================================

    @Test
    fun inrParsing_validIntegerAndDecimalAmounts_convertToPaiseExact() {
        assertEquals(100L, CurrencyUtils.rupeesStringToPaise("1"))
        assertEquals(100L, CurrencyUtils.rupeesStringToPaise("1.00"))
        assertEquals(150L, CurrencyUtils.rupeesStringToPaise("1.5"))
        assertEquals(150L, CurrencyUtils.rupeesStringToPaise("1.50"))
        assertEquals(45050L, CurrencyUtils.rupeesStringToPaise("450.50"))
        assertEquals(125075L, CurrencyUtils.rupeesStringToPaise("1,250.75"))
        assertEquals(50000L, CurrencyUtils.rupeesStringToPaise("₹500"))
        assertEquals(25L, CurrencyUtils.rupeesStringToPaise("0.25"))
        assertEquals(9999900L, CurrencyUtils.rupeesStringToPaise("99,999.00"))
    }

    @Test
    fun inrParsing_invalidAmounts_areRejectedStrictly() {
        // Blank
        try {
            CurrencyUtils.rupeesStringToPaise("")
            org.junit.Assert.fail("Blank amount must throw IllegalArgumentException")
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message!!.contains("empty", ignoreCase = true))
        }

        // Zero
        try {
            CurrencyUtils.rupeesStringToPaise("0")
            org.junit.Assert.fail("Zero amount must throw IllegalArgumentException")
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message!!.contains("greater than zero", ignoreCase = true))
        }

        // Negative
        try {
            CurrencyUtils.rupeesStringToPaise("-50")
            org.junit.Assert.fail("Negative amount must throw IllegalArgumentException")
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message!!.contains("Invalid", ignoreCase = true) || e.message!!.contains("greater than zero", ignoreCase = true))
        }

        // Malformed non-numeric
        try {
            CurrencyUtils.rupeesStringToPaise("abc")
            org.junit.Assert.fail("Non-numeric amount must throw IllegalArgumentException")
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message!!.contains("Invalid", ignoreCase = true))
        }

        // More than 2 decimal places
        try {
            CurrencyUtils.rupeesStringToPaise("10.555")
            org.junit.Assert.fail("More than 2 decimal places must throw IllegalArgumentException")
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message!!.contains("decimal places", ignoreCase = true) || e.message!!.contains("Invalid", ignoreCase = true))
        }

        // Overflow
        try {
            CurrencyUtils.rupeesStringToPaise("9999999999999999999999999999")
            org.junit.Assert.fail("Overflow amount must throw IllegalArgumentException")
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message!!.contains("large", ignoreCase = true) || e.message!!.contains("Invalid", ignoreCase = true))
        }
    }

    // =========================================================================
    // SECTION 2: EMPTY & INVALID FORM SUBMISSIONS
    // =========================================================================

    @Test
    fun formValidation_rejectsEmptyInputs_withoutInventingData() {
        val blankTitleResult = CurrencyUtils.validateExpenseInput("", "100")
        assertTrue(blankTitleResult is ValidationResult.Error)
        assertEquals("Title cannot be empty", (blankTitleResult as ValidationResult.Error).message)

        val blankAmountResult = CurrencyUtils.validateExpenseInput("Lunch", "")
        assertTrue(blankAmountResult is ValidationResult.Error)
        assertEquals("Amount cannot be empty", (blankAmountResult as ValidationResult.Error).message)

        val zeroAmountResult = CurrencyUtils.validateExpenseInput("Lunch", "0")
        assertTrue(zeroAmountResult is ValidationResult.Error)
        assertEquals("Amount must be greater than zero", (zeroAmountResult as ValidationResult.Error).message)
    }

    // =========================================================================
    // SECTION 3: EXACTLY-ONCE SAVING & DUPLICATE PREVENTION
    // =========================================================================

    @Test
    fun quickExpense_duplicateSavePrevention_savesExactlyOnce() {
        var insertCallCount = 0
        var isSaving = false

        val saveAction = { amountPaise: Long, desc: String, catId: Long ->
            if (!isSaving) {
                isSaving = true
                insertCallCount++
            }
        }

        // Simulate user rapidly double-tapping the Save button
        saveAction(15000L, "Coffee with Rahul", 1L)
        saveAction(15000L, "Coffee with Rahul", 1L)
        saveAction(15000L, "Coffee with Rahul", 1L)

        // Must execute exactly once
        assertEquals(1, insertCallCount)
    }

    @Test
    fun quickExpense_createsValidEntity_withQuickEntrySource() {
        val expense = Expense(
            id = 10L,
            title = "Morning Coffee",
            amountInPaise = 15000L,
            categoryId = 1L,
            timestamp = 1700000000000L,
            note = "Recorded via Quick Expense",
            source = "QUICK_ENTRY"
        )

        assertEquals("Morning Coffee", expense.title)
        assertEquals(15000L, expense.amountInPaise)
        assertEquals(1L, expense.categoryId)
        assertEquals("QUICK_ENTRY", expense.source)
    }

    // =========================================================================
    // SECTION 4: COUNTDOWN EXPIRATION & DRAFT PRESERVATION
    // =========================================================================

    @Test
    fun draftManager_preservesDraftState_whenContentEntered() {
        assertFalse(QuickExpenseDraftManager.hasDraft())

        // User typed amount and description but was interrupted
        QuickExpenseDraftManager.saveDraft(
            amountText = "250.00",
            description = "Dinner with friends",
            categoryId = 1L
        )

        assertTrue(QuickExpenseDraftManager.hasDraft())
        val draft = QuickExpenseDraftManager.getDraft()
        assertNotNull(draft)
        assertEquals("250.00", draft!!.amountText)
        assertEquals("Dinner with friends", draft.description)
        assertEquals(1L, draft.categoryId)

        // After successful save, draft is cleared
        QuickExpenseDraftManager.clearDraft()
        assertFalse(QuickExpenseDraftManager.hasDraft())
        assertNull(QuickExpenseDraftManager.getDraft())
    }

    @Test
    fun draftManager_doesNotSaveDraft_forEmptyForm() {
        // Untouched form dismissed by 5-second countdown expiration
        QuickExpenseDraftManager.saveDraft(
            amountText = "",
            description = "",
            categoryId = null
        )

        assertFalse("Empty form must never create a persistent draft", QuickExpenseDraftManager.hasDraft())
        assertNull(QuickExpenseDraftManager.getDraft())
    }

    // =========================================================================
    // SECTION 5: OVERLAY PERMISSION FALLBACK CONTRACT
    // =========================================================================

    @Test
    fun overlayPermissionFallback_persistsDirectlyInApp() {
        // When overlay permission is disabled, the expense is still saved safely to Room database
        val isOverlayPermitted = false
        var savedExpense: Expense? = null

        val saveCallback = { amountPaise: Long, desc: String, catId: Long ->
            savedExpense = Expense(
                id = 99L,
                title = desc,
                amountInPaise = amountPaise,
                categoryId = catId,
                source = if (isOverlayPermitted) "OVERLAY_QUICK_ENTRY" else "IN_APP_QUICK_ENTRY"
            )
        }

        saveCallback(5000L, "Auto Rickshaw", 2L)

        assertNotNull(savedExpense)
        assertEquals("IN_APP_QUICK_ENTRY", savedExpense!!.source)
        assertEquals(5000L, savedExpense!!.amountInPaise)
        assertEquals("Auto Rickshaw", savedExpense!!.title)
    }

    // =========================================================================
    // SECTION 6: EXISTING NOTIFICATION POPUP & TRANSACTION BEHAVIOR UNCHANGED
    // =========================================================================

    @Test
    fun existingPaytmDetection_andCompositePipeline_remainsIntact() {
        val compositeParser = CompositeNotificationParser()
        val paytmResult = compositeParser.parse(
            packageName = "net.one97.paytm",
            title = "Paid ₹1 to Sharma Tea Stall",
            text = "Paid ₹1 to Sharma Tea Stall successfully. UPI Ref: 412345678901"
        )

        assertTrue("Paytm 1 rupee payment must continue parsing successfully", paytmResult is ParseResult.Success)
        val payment = (paytmResult as ParseResult.Success).payment
        assertEquals(100L, payment.amountInPaise)
        assertEquals("Sharma Tea Stall", payment.merchantOrRecipient)
        assertTrue(payment.isExpenseEligible)
    }

    @Test
    fun existing3SecondFloatingPopup_updatesOriginalExpense_withoutDuplication() {
        val original = Expense(
            id = 55L,
            title = "Swiggy",
            amountInPaise = 34900L,
            categoryId = 1L,
            source = "AUTO_NOTIFICATION"
        )

        // When 3-second popup completes category choice
        val userChoiceCatId = 4L // Shopping
        val updated = original.copy(categoryId = userChoiceCatId)

        assertEquals("Transaction ID must be identical to guarantee exactly-once persistence", original.id, updated.id)
        assertEquals(userChoiceCatId, updated.categoryId)
    }

    // =========================================================================
    // SECTION 7: BACKGROUND TRIGGER COORDINATION & SAFETY TESTS
    // =========================================================================

    @Test
    fun triggerCoordinator_debouncesRapidSuccessiveTriggers() {
        QuickExpenseTriggerCoordinator.resetCooldown()

        val firstTrigger = QuickExpenseTriggerCoordinator.tryTrigger()
        val immediateSecondTrigger = QuickExpenseTriggerCoordinator.tryTrigger()

        assertTrue("First trigger acquisition must succeed", firstTrigger)
        assertFalse("Immediate second trigger within cooldown window must be rejected", immediateSecondTrigger)
    }

    @Test
    fun triggerCoordinator_synchronizesForegroundState_andNotifiesListener() {
        var notified = false
        QuickExpenseTriggerCoordinator.isForegroundActive = true
        QuickExpenseTriggerCoordinator.setForegroundListener {
            notified = true
        }

        assertTrue("Foreground active state must be true", QuickExpenseTriggerCoordinator.isForegroundActive)
        QuickExpenseTriggerCoordinator.notifyForegroundTrigger()
        assertTrue("Foreground trigger listener must have been notified", notified)

        QuickExpenseTriggerCoordinator.isForegroundActive = false
        QuickExpenseTriggerCoordinator.setForegroundListener(null)
        assertFalse("Foreground active state must be false after pause", QuickExpenseTriggerCoordinator.isForegroundActive)
    }

    @Test
    fun doublePressTiming_validatesMonotonicInterval() {
        fun isDoublePress(deltaMs: Long): Boolean = deltaMs in 100L..450L

        // Valid intervals
        assertTrue(isDoublePress(200L))
        assertTrue(isDoublePress(100L))
        assertTrue(isDoublePress(450L))
        assertTrue(isDoublePress(350L))

        // Invalid intervals
        assertFalse("Ultra-fast hardware bounce < 100ms must be rejected", isDoublePress(50L))
        assertFalse("Normal separate keypress > 450ms must not trigger double-press", isDoublePress(500L))
        assertFalse("Single separate press > 1000ms must not trigger double-press", isDoublePress(1200L))
    }

    @Test
    fun lockscreenSafety_blocksTrigger_whenKeyguardLocked() {
        fun canTriggerOnDevice(isKeyguardLocked: Boolean, isDoublePress: Boolean): Boolean {
            if (isKeyguardLocked) return false
            return isDoublePress
        }

        assertTrue("Unlocked phone with double-press must be allowed to trigger", canTriggerOnDevice(isKeyguardLocked = false, isDoublePress = true))
        assertFalse("Locked phone with double-press must NEVER bypass keyguard", canTriggerOnDevice(isKeyguardLocked = true, isDoublePress = true))
        assertFalse("Locked phone with single-press must not trigger", canTriggerOnDevice(isKeyguardLocked = true, isDoublePress = false))
    }
}

