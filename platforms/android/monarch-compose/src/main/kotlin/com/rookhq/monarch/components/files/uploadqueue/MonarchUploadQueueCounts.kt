package com.rookhq.monarch.components.files.uploadqueue

import androidx.compose.runtime.Immutable
import com.rookhq.monarch.components.files.attachmentchip.MonarchUploadStatus

/**
 * How many uploads of a `MonarchUploadQueue` are in each status, for the
 * summary and the announcements.
 *
 * @property total every upload.
 * @property queued uploads waiting for their turn.
 * @property uploading uploads being transferred.
 * @property done uploads transferred.
 * @property failed uploads stopped by an error.
 * @property cancelled uploads stopped by the user.
 */
@Immutable
public data class MonarchUploadQueueCounts(
    public val total: Int,
    public val queued: Int,
    public val uploading: Int,
    public val done: Int,
    public val failed: Int,
    public val cancelled: Int,
) {
    /** Uploads that can still finish: queued and uploading. */
    public val active: Int get() = queued + uploading

    /** Uploads that ended: done, failed and cancelled. */
    public val finished: Int get() = done + failed + cancelled

    /** Counting uploads. */
    public companion object {
        /** Counts [items] by status. */
        public fun of(items: List<MonarchUploadQueueItem>): MonarchUploadQueueCounts {
            val byStatus = items.groupingBy { it.status }.eachCount()
            return MonarchUploadQueueCounts(
                total = items.size,
                queued = byStatus[MonarchUploadStatus.Queued] ?: 0,
                uploading = byStatus[MonarchUploadStatus.Uploading] ?: 0,
                done = byStatus[MonarchUploadStatus.Done] ?: 0,
                failed = byStatus[MonarchUploadStatus.Failed] ?: 0,
                cancelled = byStatus[MonarchUploadStatus.Cancelled] ?: 0,
            )
        }
    }
}
