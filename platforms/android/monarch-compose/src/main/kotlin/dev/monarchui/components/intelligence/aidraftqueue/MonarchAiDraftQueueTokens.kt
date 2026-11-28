package dev.monarchui.components.intelligence.aidraftqueue

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.monarchui.components.actions.button.MonarchButtonVariant
import dev.monarchui.foundation.MonarchTone
import dev.monarchui.internal.MonarchDefaultIcons
import dev.monarchui.theme.MonarchColors
import dev.monarchui.tokens.MonarchTokens

/** Component tokens and pure logic of MonarchAiDraftQueue, built from primitives and roles only. */
@Suppress("TooManyFunctions")
internal object MonarchAiDraftQueueTokens {
    /** The gap between the header and the drafts, and between drafts. */
    val StackGap: Dp = MonarchTokens.Space.space4.dp

    /** The gap between the parts of a draft. */
    val CardGap: Dp = MonarchTokens.Space.space3.dp

    /** The gap between buttons and between small items. */
    val Gap: Dp = MonarchTokens.Space.space2.dp

    /** The gap between a heading and its text. */
    val TextGap: Dp = MonarchTokens.Space.space1.dp

    /** The padding inside the context block. */
    val ContextPadding: Dp = MonarchTokens.Space.space3.dp

    /**
     * A decision offered on a draft, with its button variant (approve and
     * send are primary, skip is ghost) and whether it moves focus to the
     * next draft that needs review.
     */
    enum class Action(
        val variant: MonarchButtonVariant,
        val advances: Boolean,
        val icon: (@Composable () -> Unit)? = null,
    ) {
        Approve(MonarchButtonVariant.Primary, advances = true, icon = MonarchDefaultIcons.Check),
        Send(MonarchButtonVariant.Primary, advances = true),
        Edit(MonarchButtonVariant.Secondary, advances = false, icon = MonarchDefaultIcons.Pencil),
        Regenerate(MonarchButtonVariant.Secondary, advances = false, icon = MonarchDefaultIcons.Retry),
        Skip(MonarchButtonVariant.Ghost, advances = true),
    }

    /** The glyph of a status badge, as the web: pencil to review, retry while writing, check once approved. */
    fun statusIcon(status: MonarchAiDraftStatus): @Composable () -> Unit = when (status) {
        MonarchAiDraftStatus.Draft -> MonarchDefaultIcons.Pencil
        MonarchAiDraftStatus.Regenerating -> MonarchDefaultIcons.Retry
        MonarchAiDraftStatus.Approved -> MonarchDefaultIcons.Check
        MonarchAiDraftStatus.Sent -> MonarchDefaultIcons.CheckCircle
        MonarchAiDraftStatus.Skipped -> MonarchDefaultIcons.Dash
    }

    /** The tone of a status badge. The badge also names the status, so it does not rest on colour. */
    fun tone(status: MonarchAiDraftStatus): MonarchTone = when (status) {
        MonarchAiDraftStatus.Skipped -> MonarchTone.Neutral
        // As the web: a draft the assistant wrote, and one it is rewriting, carry the AI colour.
        MonarchAiDraftStatus.Draft, MonarchAiDraftStatus.Regenerating -> MonarchTone.Ai
        MonarchAiDraftStatus.Approved, MonarchAiDraftStatus.Sent -> MonarchTone.Success
    }

    /** The statuses of drafts with a decision. */
    private val Reviewed = setOf(MonarchAiDraftStatus.Approved, MonarchAiDraftStatus.Sent, MonarchAiDraftStatus.Skipped)

    /** The number of drafts with a decision: approved, sent or skipped. */
    fun reviewedCount(drafts: List<MonarchAiDraft>): Int = drafts.count { it.status in Reviewed }

    /** The share of drafts reviewed, 0 to 1. */
    fun fraction(drafts: List<MonarchAiDraft>): Float =
        if (drafts.isEmpty()) 0f else reviewedCount(drafts).toFloat() / drafts.size

    /** The decisions offered for a status. Nothing while a draft is written, or once it is sent or skipped. */
    fun actions(status: MonarchAiDraftStatus, canSend: Boolean): List<Action> = when (status) {
        MonarchAiDraftStatus.Draft -> listOf(Action.Approve, Action.Edit, Action.Regenerate, Action.Skip)
        MonarchAiDraftStatus.Approved -> if (canSend) listOf(Action.Send, Action.Edit) else listOf(Action.Edit)
        MonarchAiDraftStatus.Regenerating, MonarchAiDraftStatus.Sent, MonarchAiDraftStatus.Skipped -> emptyList()
    }

    /** The next draft that needs review after [id], wrapping to the top, or `null` when no other one does. */
    fun next(after: String, drafts: List<MonarchAiDraft>): String? {
        val index = drafts.indexOfFirst { it.id == after }
        if (index < 0) return drafts.firstOrNull { it.status == MonarchAiDraftStatus.Draft }?.id
        val ordered = drafts.drop(index + 1) + drafts.take(index)
        return ordered.firstOrNull { it.status == MonarchAiDraftStatus.Draft }?.id
    }

    /** The edited text to save: trimmed, or `null` when it is empty or unchanged. */
    fun edited(text: String, original: String): String? {
        val trimmed = text.trim()
        return trimmed.takeIf { it.isNotEmpty() && it != original.trim() }
    }

    /** The statuses by id; the first draft with an id wins. */
    fun statuses(drafts: List<MonarchAiDraft>): Map<String, MonarchAiDraftStatus> {
        val result = LinkedHashMap<String, MonarchAiDraftStatus>()
        drafts.forEach { result.putIfAbsent(it.id, it.status) }
        return result
    }

    /** The drafts whose status changed since [previous]. Drafts that were not there before are left out. */
    fun changed(previous: Map<String, MonarchAiDraftStatus>, drafts: List<MonarchAiDraft>): List<MonarchAiDraft> =
        drafts.filter { draft -> previous[draft.id]?.let { it != draft.status } == true }

    /** The colour of a body: muted once skipped. */
    fun bodyColor(status: MonarchAiDraftStatus, colors: MonarchColors): Color =
        if (status == MonarchAiDraftStatus.Skipped) colors.textMuted else colors.textDefault

    /** The fill of the context block. */
    fun contextFill(colors: MonarchColors): Color = colors.bgSubtle
}
