package dev.monarchui.components.intelligence.aierrormessage

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.monarchui.foundation.MonarchTone
import dev.monarchui.internal.MonarchSurfaceColors
import dev.monarchui.internal.MonarchTonePalette
import dev.monarchui.theme.MonarchColors
import dev.monarchui.tokens.MonarchTokens

/** The built-in actions of the card. */
internal enum class MonarchAiErrorAction { Retry, EditPrompt, SwitchModel, NewConversation, Summarize }

/** Component tokens and the pure logic of MonarchAiErrorMessage. */
internal object MonarchAiErrorMessageTokens {
    val HorizontalPadding: Dp = MonarchTokens.Space.space4.dp
    val VerticalPadding: Dp = MonarchTokens.Space.space3.dp
    val ContentGap: Dp = MonarchTokens.Space.space3.dp
    val TitleGap: Dp = MonarchTokens.Space.space0_5.dp
    val ActionGap: Dp = MonarchTokens.Space.space2.dp
    val DetailsPadding: Dp = MonarchTokens.Space.space2.dp

    /** How often the countdown is updated, as on the web. */
    const val TICK_MILLIS: Long = 500L
    private const val MILLIS_PER_SECOND = 1000L

    /** Rate limits, long conversations and safety blocks are warnings; the rest are errors. */
    fun tone(kind: MonarchAiErrorKind): MonarchTone = when (kind) {
        MonarchAiErrorKind.RateLimit, MonarchAiErrorKind.ContextOverflow, MonarchAiErrorKind.SafetyBlock ->
            MonarchTone.Warning
        MonarchAiErrorKind.Network, MonarchAiErrorKind.ToolFailure, MonarchAiErrorKind.Unknown -> MonarchTone.Danger
    }

    /** The surface of the card: the same colours as `MonarchBanner` in the tone. */
    fun colors(kind: MonarchAiErrorKind, colors: MonarchColors): MonarchSurfaceColors {
        val palette = MonarchTonePalette.of(tone(kind), colors)
        return MonarchSurfaceColors(container = palette.subtle, content = palette.strong, border = palette.border)
    }

    /** The box of the technical details: the surface colour with the default border. */
    fun detailsColors(colors: MonarchColors): MonarchSurfaceColors =
        MonarchSurfaceColors(container = colors.bgSurface, content = colors.textDefault, border = colors.borderDefault)

    /** The action order of each kind; the first available one is the primary button. */
    fun order(kind: MonarchAiErrorKind): List<MonarchAiErrorAction> = when (kind) {
        MonarchAiErrorKind.RateLimit -> listOf(
            MonarchAiErrorAction.Retry,
            MonarchAiErrorAction.SwitchModel,
            MonarchAiErrorAction.EditPrompt,
            MonarchAiErrorAction.NewConversation,
        )
        MonarchAiErrorKind.ContextOverflow -> listOf(
            MonarchAiErrorAction.NewConversation,
            MonarchAiErrorAction.Summarize,
            MonarchAiErrorAction.EditPrompt,
            MonarchAiErrorAction.SwitchModel,
            MonarchAiErrorAction.Retry,
        )
        MonarchAiErrorKind.SafetyBlock -> listOf(
            MonarchAiErrorAction.EditPrompt,
            MonarchAiErrorAction.Retry,
            MonarchAiErrorAction.NewConversation,
            MonarchAiErrorAction.SwitchModel,
        )
        MonarchAiErrorKind.Network, MonarchAiErrorKind.ToolFailure, MonarchAiErrorKind.Unknown -> listOf(
            MonarchAiErrorAction.Retry,
            MonarchAiErrorAction.EditPrompt,
            MonarchAiErrorAction.SwitchModel,
            MonarchAiErrorAction.NewConversation,
        )
    }

    /** The whole seconds left of a countdown, rounded up. */
    fun secondsLeft(millis: Long): Int =
        ((millis + MILLIS_PER_SECOND - 1) / MILLIS_PER_SECOND).toInt().coerceAtLeast(0)

    /** Whether a retry delay is a countdown: positive and finite. */
    fun countsDown(retryAfterMillis: Long?): Boolean = retryAfterMillis != null && retryAfterMillis > 0
}
