package com.example.pocketexpense.floating

import java.util.concurrent.ConcurrentLinkedQueue

/**
 * Thread-safe bounded queue for managing floating expense popups.
 * If multiple payments arrive in close succession, each transaction is presented
 * sequentially with its own 3-second countdown.
 *
 * If the queue exceeds [maxCapacity], older pending items are automatically finalized
 * with their default category to prevent memory accumulation.
 */
class FloatingExpenseQueue(val maxCapacity: Int = 5) {

    private val queue = ConcurrentLinkedQueue<PendingExpenseItem>()

    @Synchronized
    fun enqueue(item: PendingExpenseItem): PendingExpenseItem {
        while (queue.size >= maxCapacity) {
            val oldest = queue.poll()
            oldest?.commitDefault()
        }
        queue.offer(item)
        return item
    }

    @Synchronized
    fun poll(): PendingExpenseItem? {
        return queue.poll()
    }

    @Synchronized
    fun peek(): PendingExpenseItem? {
        return queue.peek()
    }

    val size: Int get() = queue.size

    val isEmpty: Boolean get() = queue.isEmpty()

    @Synchronized
    fun clear() {
        while (!queue.isEmpty()) {
            val item = queue.poll()
            item?.commitDefault()
        }
    }
}
