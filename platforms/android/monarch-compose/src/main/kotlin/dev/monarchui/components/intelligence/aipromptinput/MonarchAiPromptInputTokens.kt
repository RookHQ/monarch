package dev.monarchui.components.intelligence.aipromptinput

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.monarchui.internal.MonarchSurfaceColors
import dev.monarchui.theme.MonarchColors
import dev.monarchui.tokens.MonarchTokens
import kotlin.math.floor

/** Where the count of a `MonarchAiPromptInput` stands against its limit. */
internal enum class AiPromptInputLimit { Ok, Warning, Over }

/** Component tokens and pure logic of MonarchAiPromptInput, built from primitives and roles only. */
internal object MonarchAiPromptInputTokens {
    /** The gap between the label, the menu, the box and the footer. */
    val Gap: Dp = MonarchTokens.Space.space2.dp

    /** The gap between the parts inside the box. */
    val InnerGap: Dp = MonarchTokens.Space.space1.dp

    /** The padding inside the box. */
    val BoxPadding: PaddingValues = PaddingValues(
        start = MonarchTokens.Space.space3.dp,
        end = MonarchTokens.Space.space1.dp,
        top = MonarchTokens.Space.space1.dp,
        bottom = MonarchTokens.Space.space1.dp,
    )

    /** The padding at the end of the text, away from the box's edge. */
    val FieldPadding: PaddingValues = PaddingValues(end = MonarchTokens.Space.space2.dp)

    /** The text grows to this many lines by default, then scrolls, as on the web. */
    const val MAX_LINES: Int = 8

    /** The share of the limit at which the counter turns to warning by default. */
    const val WARN_AT: Float = 0.9f

    /** The files a prompt input takes by default when content is pasted, dropped or sent by the keyboard. */
    val DefaultAccept: List<String> = listOf("image/*")

    /** Where [count] stands against [maxLength]: over it, at or past [warnAt] of it, or fine. */
    fun limit(count: Int, maxLength: Int?, warnAt: Float): AiPromptInputLimit = when {
        maxLength == null -> AiPromptInputLimit.Ok
        count > maxLength -> AiPromptInputLimit.Over
        count >= floor(maxLength * warnAt).toInt() -> AiPromptInputLimit.Warning
        else -> AiPromptInputLimit.Ok
    }

    /**
     * Whether the prompt can be sent: enabled, no response running, not
     * already submitting, not over the limit, and with text or attachments.
     */
    @Suppress("LongParameterList")
    fun canSubmit(
        text: CharSequence,
        enabled: Boolean,
        busy: Boolean,
        submitting: Boolean,
        limit: AiPromptInputLimit,
        hasAttachments: Boolean,
    ): Boolean = enabled && !busy && !submitting && limit != AiPromptInputLimit.Over &&
        (text.isNotBlank() || hasAttachments)

    /**
     * What to announce when the limit state moves from [previous] to [next]:
     * the characters left on the way to warning, the excess on the way over,
     * nothing when it falls back.
     */
    @Suppress("LongParameterList")
    fun limitAnnouncement(
        previous: AiPromptInputLimit,
        next: AiPromptInputLimit,
        count: Int,
        maxLength: Int?,
        warning: (Int) -> String,
        over: (Int) -> String,
    ): String? {
        if (maxLength == null || previous == next) return null
        return when {
            next == AiPromptInputLimit.Warning && previous == AiPromptInputLimit.Ok -> warning(maxLength - count)
            next == AiPromptInputLimit.Over -> over(count - maxLength)
            else -> null
        }
    }

    /** The box: muted when disabled, the accent border while files are dragged over it, danger over the limit. */
    fun boxColors(enabled: Boolean, over: Boolean, dragging: Boolean, colors: MonarchColors): MonarchSurfaceColors =
        when {
            !enabled -> MonarchSurfaceColors(colors.bgSubtle, colors.textDisabled, colors.borderSubtle)
            dragging -> MonarchSurfaceColors(colors.accentSubtle, colors.textDefault, colors.accentBorder)
            over -> MonarchSurfaceColors(colors.bgSurface, colors.textDefault, colors.dangerBorder)
            else -> MonarchSurfaceColors(colors.bgSurface, colors.textDefault, colors.borderInput)
        }

    /** The counter colour: muted, the strong warning colour near the limit, the strong danger colour over it. */
    fun counterColor(limit: AiPromptInputLimit, colors: MonarchColors): Color = when (limit) {
        AiPromptInputLimit.Ok -> colors.textMuted
        AiPromptInputLimit.Warning -> colors.warningStrong
        AiPromptInputLimit.Over -> colors.dangerStrong
    }

    /** The text colour. */
    fun textColor(enabled: Boolean, colors: MonarchColors): Color =
        if (enabled) colors.textDefault else colors.textDisabled

    /** The placeholder colour. */
    fun placeholderColor(colors: MonarchColors): Color = colors.textSubtle

    /** The colour of the token estimate and the disabled reason. */
    fun mutedColor(colors: MonarchColors): Color = colors.textMuted

    /** The colour of the drop hint over the box. */
    fun dropHintColor(colors: MonarchColors): Color = colors.accentStrong

    /** The caret colour. */
    fun cursorColor(colors: MonarchColors): Color = colors.accentSolid
}
