package com.example.pocketexpense

import com.example.pocketexpense.data.model.Category
import com.example.pocketexpense.data.model.Expense
import com.example.pocketexpense.floating.FloatingExpenseQueue
import com.example.pocketexpense.floating.PendingExpenseItem
import com.example.pocketexpense.parser.AutoCategoryMapper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger

class FloatingExpensePopupTest {

    private val sampleCategories = Category.defaultCategories()

    @Test
    fun categorySelection_commitsChosenCategoryAndFinalizes() {
        var committedCategoryId: Long? = null
        var dismissedCalled = false

        val item = PendingExpenseItem(
            expenseId = 101L,
            amountInPaise = 45000L,
            merchant = "Starbucks",
            suggestedCategoryId = 1L, // Food
            isConfidentCategory = true,
            appName = "Google Pay",
            categories = sampleCategories,
            onCategoryCommitted = { committedCategoryId = it },
            onDismissed = { dismissedCalled = true }
        )

        assertFalse(item.isFinalized)

        // User taps Category 4 (Shopping)
        val success = item.commitCategory(4L)
        assertTrue(success)
        assertTrue(item.isFinalized)
        assertEquals(4L, committedCategoryId)
        assertFalse(dismissedCalled)

        // Subsequent timeout or tap attempt must return false and not overwrite
        val secondAttempt = item.commitDefault()
        assertFalse(secondAttempt)
        assertEquals(4L, committedCategoryId)
    }

    @Test
    fun timeout_autoSavesSuggestedCategoryWhenConfident() {
        var committedCategoryId: Long? = null

        val item = PendingExpenseItem(
            expenseId = 102L,
            amountInPaise = 18900L,
            merchant = "Uber India",
            suggestedCategoryId = 2L, // Travel
            isConfidentCategory = true,
            appName = "PhonePe",
            categories = sampleCategories,
            onCategoryCommitted = { committedCategoryId = it },
            onDismissed = {}
        )

        // Simulate 3s timer expiration
        val success = item.commitDefault()
        assertTrue(success)
        assertTrue(item.isFinalized)
        assertEquals(2L, committedCategoryId)
    }

    @Test
    fun timeout_autoSavesUncategorizedWhenNotConfident() {
        var committedCategoryId: Long? = null

        val item = PendingExpenseItem(
            expenseId = 103L,
            amountInPaise = 50000L,
            merchant = "Unknown Hardware Vendor",
            suggestedCategoryId = 8L, // Uncategorized
            isConfidentCategory = false,
            appName = "Paytm",
            categories = sampleCategories,
            onCategoryCommitted = { committedCategoryId = it },
            onDismissed = {}
        )

        val success = item.commitDefault()
        assertTrue(success)
        assertEquals(8L, committedCategoryId)
    }

    @Test
    fun popupDismissal_finalizesItem() {
        var dismissedCalled = false
        var committedCategoryId: Long? = null

        val item = PendingExpenseItem(
            expenseId = 104L,
            amountInPaise = 25000L,
            merchant = "Subway",
            suggestedCategoryId = 1L,
            isConfidentCategory = true,
            appName = "Google Pay",
            categories = sampleCategories,
            onCategoryCommitted = { committedCategoryId = it },
            onDismissed = { dismissedCalled = true }
        )

        val success = item.dismiss()
        assertTrue(success)
        assertTrue(item.isFinalized)
        assertTrue(dismissedCalled)
        assertNull(committedCategoryId)

        // Cannot commit category after dismissal
        assertFalse(item.commitCategory(3L))
    }

    @Test
    fun rapidConsecutivePayments_queueHandlesSequentiallyWithoutOverwriting() {
        val queue = FloatingExpenseQueue(maxCapacity = 5)

        val commitments = mutableListOf<Pair<Long, Long>>() // expenseId to categoryId

        val item1 = PendingExpenseItem(
            expenseId = 1L,
            amountInPaise = 32000L,
            merchant = "Starbucks",
            suggestedCategoryId = 1L,
            isConfidentCategory = true,
            appName = "Google Pay",
            categories = sampleCategories,
            onCategoryCommitted = { commitments.add(1L to it) },
            onDismissed = {}
        )

        val item2 = PendingExpenseItem(
            expenseId = 2L,
            amountInPaise = 18900L,
            merchant = "Uber",
            suggestedCategoryId = 2L,
            isConfidentCategory = true,
            appName = "PhonePe",
            categories = sampleCategories,
            onCategoryCommitted = { commitments.add(2L to it) },
            onDismissed = {}
        )

        // Enqueue both rapid payments
        queue.enqueue(item1)
        queue.enqueue(item2)
        assertEquals(2, queue.size)

        // Display first item
        val activeItem1 = queue.poll()
        assertNotNull(activeItem1)
        assertEquals(1L, activeItem1!!.expenseId)
        assertEquals("Starbucks", activeItem1.merchant)

        // User categorizes first item
        activeItem1.commitCategory(1L)

        // Display second item
        val activeItem2 = queue.poll()
        assertNotNull(activeItem2)
        assertEquals(2L, activeItem2!!.expenseId)
        assertEquals("Uber", activeItem2.merchant)

        // User categorizes second item
        activeItem2.commitCategory(2L)

        // Both committed independently without cross-over
        assertEquals(2, commitments.size)
        assertEquals(1L to 1L, commitments[0])
        assertEquals(2L to 2L, commitments[1])
        assertTrue(queue.isEmpty)
    }

    @Test
    fun boundedQueue_autoCommitsOldestWhenCapacityExceeded() {
        val queue = FloatingExpenseQueue(maxCapacity = 3)
        val committedCategories = mutableListOf<Long>()

        for (i in 1..5) {
            val item = PendingExpenseItem(
                expenseId = i.toLong(),
                amountInPaise = (i * 1000).toLong(),
                merchant = "Merchant $i",
                suggestedCategoryId = i.toLong(),
                isConfidentCategory = true,
                appName = "App",
                categories = sampleCategories,
                onCategoryCommitted = { committedCategories.add(it) },
                onDismissed = {}
            )
            queue.enqueue(item)
        }

        // When 5 items were queued with maxCapacity=3:
        // Item 1 and Item 2 were auto-committed to avoid unbounded queue growth
        assertEquals(2, committedCategories.size)
        assertEquals(1L, committedCategories[0])
        assertEquals(2L, committedCategories[1])
        assertEquals(3, queue.size)
    }

    @Test
    fun raceConditionSafety_atomicSingleCommitOnly() {
        val successfulCommits = AtomicInteger(0)
        val threadCount = 20
        val latch = CountDownLatch(threadCount)
        val executor = Executors.newFixedThreadPool(threadCount)

        val item = PendingExpenseItem(
            expenseId = 999L,
            amountInPaise = 10000L,
            merchant = "Cafe Coffee Day",
            suggestedCategoryId = 1L,
            isConfidentCategory = true,
            appName = "GPay",
            categories = sampleCategories,
            onCategoryCommitted = { successfulCommits.incrementAndGet() },
            onDismissed = { successfulCommits.incrementAndGet() }
        )

        for (i in 0 until threadCount) {
            executor.submit {
                try {
                    when (i % 3) {
                        0 -> item.commitCategory(4L)
                        1 -> item.commitDefault()
                        2 -> item.dismiss()
                    }
                } finally {
                    latch.countDown()
                }
            }
        }

        latch.await()
        executor.shutdown()

        // Despite 20 concurrent threads trying to commit/timeout/dismiss simultaneously,
        // exactly ONE decision must succeed!
        assertEquals(1, successfulCommits.get())
        assertTrue(item.isFinalized)
    }

    @Test
    fun confidentCategoryDetection_matchesExpectedCategories() {
        assertTrue(AutoCategoryMapper.hasConfidentMatch("Starbucks Coffee"))
        assertTrue(AutoCategoryMapper.hasConfidentMatch("Uber India"))
        assertTrue(AutoCategoryMapper.hasConfidentMatch("Blinkit Commerce"))
        assertFalse(AutoCategoryMapper.hasConfidentMatch("Random Person Transfer XYZ 123"))

        assertEquals(1L, AutoCategoryMapper.suggestCategoryId("Starbucks Coffee"))
        assertEquals(8L, AutoCategoryMapper.suggestCategoryId("Random Person Transfer XYZ 123"))
    }
}
