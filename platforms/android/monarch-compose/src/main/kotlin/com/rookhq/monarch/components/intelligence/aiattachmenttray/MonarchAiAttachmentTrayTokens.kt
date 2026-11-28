package com.rookhq.monarch.components.intelligence.aiattachmenttray

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.rookhq.monarch.components.files.attachmentchip.MonarchUploadStatus
import com.rookhq.monarch.tokens.MonarchTokens

/** Component tokens and the pure logic of MonarchAiAttachmentTray. */
internal object MonarchAiAttachmentTrayTokens {
    /** The gap between tiles. */
    val Gap: Dp = MonarchTokens.Space.space2.dp

    /** The widest a tile grows before the font scale: 320dp. */
    val MaxTileWidth: Dp = (MonarchTokens.Space.space24 * 3 + MonarchTokens.Space.space8).dp

    /** The side of the square that holds the file type's letters. */
    val BadgeSize: Dp = MonarchTokens.Space.space8.dp

    /** The most letters of a file type the badge shows. */
    const val BADGE_LETTERS: Int = 4

    /** The letters of the file type badge: the extension in capitals, or empty when the name has none. */
    fun badgeText(name: String): String =
        name.substringAfterLast('.', "").takeIf { it.isNotEmpty() && it.length <= BADGE_LETTERS }?.uppercase().orEmpty()

    /** The status of a file: failed when flagged or with a message, uploading below 1, else uploaded. */
    fun status(file: MonarchAiAttachmentTrayFile): MonarchUploadStatus {
        val progress = file.progress?.takeIf { !it.isNaN() }?.coerceIn(0f, 1f)
        return when {
            file.failed || file.errorMessage != null -> MonarchUploadStatus.Failed
            progress != null && progress < 1f -> MonarchUploadStatus.Uploading
            else -> MonarchUploadStatus.Done
        }
    }

    /**
     * What to announce after [current] replaces [previous]: files that finished uploading, then files removed
     * with their button ([removedIds]), in one message. Failures are announced by the chip itself.
     */
    fun announcement(
        previous: List<MonarchAiAttachmentTrayFile>,
        current: List<MonarchAiAttachmentTrayFile>,
        removedIds: Set<String>,
        ready: (String) -> String,
        removed: (String) -> String,
    ): String {
        val before = previous.associateBy { it.id }
        val now = current.map { it.id }.toSet()
        val uploaded = current.filter { file ->
            val old = before[file.id]
            old != null && status(old) == MonarchUploadStatus.Uploading && status(file) == MonarchUploadStatus.Done
        }.map { ready(it.name) }
        val gone = previous.filter { it.id in removedIds && it.id !in now }.map { removed(it.name) }
        return (uploaded + gone).joinToString(". ")
    }
}
