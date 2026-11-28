package dev.monarchui.components.files.uploadqueue

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.monarchui.components.files.attachmentchip.MonarchUploadStatus
import dev.monarchui.internal.MonarchDefaultIcons
import dev.monarchui.theme.MonarchColors
import dev.monarchui.tokens.MonarchTokens
import kotlin.math.roundToInt

/** A change of the queue that is announced. */
internal sealed interface MonarchUploadQueueEvent {
    /** An upload finished. */
    data class Done(val name: String) : MonarchUploadQueueEvent

    /** An upload failed, with its error message. */
    data class Failed(val name: String, val error: String?) : MonarchUploadQueueEvent

    /** The last active upload of several ended. */
    data class Finished(val counts: MonarchUploadQueueCounts) : MonarchUploadQueueEvent
}

/** Component tokens and logic of MonarchUploadQueue, built from primitives and roles only. */
internal object MonarchUploadQueueTokens {
    /** The gap between the header and the rows, and between rows. */
    val SectionGap: Dp = MonarchTokens.Space.space3.dp

    /** The gap between the icon and the text of a row, and between header parts. */
    val RowGap: Dp = MonarchTokens.Space.space3.dp

    /** The gap between the lines of a row. */
    val LineGap: Dp = MonarchTokens.Space.space1.dp

    /** The gap between the buttons of a row. */
    val ActionGap: Dp = MonarchTokens.Space.space2.dp

    private const val PERCENT: Float = 100f

    /** The colour of the status line: success when done, danger when failed, muted otherwise. */
    fun statusColor(status: MonarchUploadStatus, colors: MonarchColors): Color = when (status) {
        MonarchUploadStatus.Done -> colors.successStrong
        MonarchUploadStatus.Failed -> colors.dangerStrong
        MonarchUploadStatus.Queued, MonarchUploadStatus.Uploading, MonarchUploadStatus.Cancelled -> colors.textMuted
    }

    /** The mark before the status line: a check when done, the error mark when failed, none otherwise. */
    fun statusIcon(status: MonarchUploadStatus): (@Composable () -> Unit)? = when (status) {
        MonarchUploadStatus.Done -> MonarchDefaultIcons.CheckCircle
        MonarchUploadStatus.Failed -> MonarchDefaultIcons.Error
        MonarchUploadStatus.Queued, MonarchUploadStatus.Uploading, MonarchUploadStatus.Cancelled -> null
    }

    /** The colour of the file name. */
    fun nameColor(colors: MonarchColors): Color = colors.textDefault

    /** The colour of the size and the summary. */
    fun mutedColor(colors: MonarchColors): Color = colors.textMuted

    /** The fraction kept between 0 and 1, or `null` when unknown. */
    fun fraction(progress: Float?): Float? = progress?.takeIf { it.isFinite() }?.coerceIn(0f, 1f)

    /** The whole percentage of a fraction, or `null` when unknown. */
    fun percent(progress: Float?): Int? = fraction(progress)?.let { (it * PERCENT).roundToInt() }

    /** Whether Retry is offered: the upload failed or was cancelled and may be retried. */
    fun canRetry(item: MonarchUploadQueueItem): Boolean =
        item.retryable && (item.status == MonarchUploadStatus.Failed || item.status == MonarchUploadStatus.Cancelled)

    /**
     * The total progress, from 0 to 1, of the uploads that can still finish:
     * failed and cancelled uploads are left out. Weighted by size when every
     * remaining upload has one.
     */
    fun totalProgress(items: List<MonarchUploadQueueItem>): Float {
        val live = items.filter {
            it.status != MonarchUploadStatus.Failed && it.status != MonarchUploadStatus.Cancelled
        }
        if (live.isEmpty()) return 0f
        val bySize = live.all { (it.size ?: 0L) > 0L }
        var sum = 0.0
        var weight = 0.0
        live.forEach { item ->
            val w = if (bySize) (item.size ?: 0L).toDouble() else 1.0
            val done = when (item.status) {
                MonarchUploadStatus.Done -> 1f
                MonarchUploadStatus.Uploading -> fraction(item.progress) ?: 0f
                else -> 0f
            }
            sum += w * done
            weight += w
        }
        return (sum / weight).toFloat()
    }

    /**
     * What to announce after the items changed from [previous] (id to
     * status; `null` on the first composition, when nothing is announced):
     * uploads that became done or failed, then the end of the batch when the
     * last active upload of several ended.
     */
    fun events(
        previous: Map<String, MonarchUploadStatus>?,
        items: List<MonarchUploadQueueItem>,
    ): List<MonarchUploadQueueEvent> {
        if (previous == null) return emptyList()
        val events = mutableListOf<MonarchUploadQueueEvent>()
        var ended = false
        items.forEach { item ->
            val was = previous[item.id]
            if (was == item.status) return@forEach
            if (item.status == MonarchUploadStatus.Done) events += MonarchUploadQueueEvent.Done(item.name)
            if (item.status == MonarchUploadStatus.Failed) {
                events += MonarchUploadQueueEvent.Failed(item.name, item.errorMessage)
            }
            if (was != null && was.active && !item.status.active) ended = true
        }
        val counts = MonarchUploadQueueCounts.of(items)
        if (ended && counts.active == 0 && items.size > 1) events += MonarchUploadQueueEvent.Finished(counts)
        return events
    }
}
