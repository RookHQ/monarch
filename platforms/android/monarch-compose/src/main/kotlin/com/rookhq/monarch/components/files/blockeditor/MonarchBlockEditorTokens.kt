package com.rookhq.monarch.components.files.blockeditor

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.rookhq.monarch.accessibility.MonarchTouchTarget
import com.rookhq.monarch.foundation.MonarchTone
import com.rookhq.monarch.internal.MonarchDefaultIcons
import androidx.compose.runtime.Composable
import com.rookhq.monarch.foundation.MonarchTypography
import com.rookhq.monarch.theme.MonarchColors
import com.rookhq.monarch.tokens.MonarchTokens

/** Component tokens of MonarchBlockEditor, built from roles and the spacing scale only. */
@Suppress("TooManyFunctions")
internal object MonarchBlockEditorTokens {
    /** The smallest height of a block's text, so that every block is a 48dp touch target. */
    val MinBlockHeight: Dp = MonarchTouchTarget.Minimum

    /** The space between blocks. */
    val BlockGap: Dp = MonarchTokens.Space.space1.dp

    /** The space around the document inside the list. */
    val ContentPadding: Dp = MonarchTokens.Space.space2.dp

    /** The space between a block's handle, its marker and its text. */
    val PartGap: Dp = MonarchTokens.Space.space2.dp

    /** The width of the bar at the start of a quote. */
    val QuoteBar: Dp = MonarchTokens.Space.space1.dp

    /** The padding inside a code block and around a toggle's content. */
    val InsetPadding: Dp = MonarchTokens.Space.space3.dp

    /**
     * The tones the callout menu offers, in the web's picker order (`CALLOUT_TONES`). Accent and AI are left
     * out on purpose: the web callout block has only these five, so a document edited here keeps tones the
     * web editor can read and offer, and a callout's tone keeps its status meaning. An app can still give a
     * callout block any tone in code.
     */
    val CalloutTones: List<MonarchTone> = listOf(
        MonarchTone.Neutral,
        MonarchTone.Info,
        MonarchTone.Success,
        MonarchTone.Warning,
        MonarchTone.Danger,
    )

    /** The z order of a block while it is dragged, above the others. */
    const val DRAGGED_Z: Float = 1f

    /** The most the list scrolls in one frame while a dragged block is held past its edge. */
    val AutoScrollStep: Dp = MonarchTokens.Space.space4.dp

    /** The text style of a text block. */
    fun textStyle(style: MonarchBlockEditorTextStyle, typography: MonarchTypography): TextStyle = when (style) {
        MonarchBlockEditorTextStyle.Heading1 -> typography.title
        MonarchBlockEditorTextStyle.Heading2 -> typography.heading
        MonarchBlockEditorTextStyle.Heading3 -> typography.subheading
        MonarchBlockEditorTextStyle.Code -> typography.code
        else -> typography.body
    }

    /** The colour of a block's text: the default text, or the disabled text while the editor is disabled. */
    fun text(colors: MonarchColors, enabled: Boolean): Color = if (enabled) colors.textDefault else colors.textDisabled

    /** The colour of a quote, muted against the body text. */
    fun quote(colors: MonarchColors, enabled: Boolean): Color = if (enabled) colors.textMuted else colors.textDisabled

    /** The colour of the placeholder and of list markers. */
    fun placeholder(colors: MonarchColors): Color = colors.textSubtle

    /** The colour of a list's bullet or number. */
    fun marker(colors: MonarchColors, enabled: Boolean): Color = if (enabled) colors.textMuted else colors.textDisabled

    /** The bar at the start of a quote. */
    fun quoteBar(colors: MonarchColors): Color = colors.borderStrong

    /** The fill of a code block. */
    fun codeFill(colors: MonarchColors): Color = colors.bgSubtle

    /** The border of a code block and of a toggle's content. */
    fun inset(colors: MonarchColors): Color = colors.borderSubtle

    /** The caret. */
    fun cursor(colors: MonarchColors): Color = colors.accentStrong

    /** A dragged block's outline, so the state is not the shadow alone. */
    fun dragged(colors: MonarchColors): Color = colors.accentBorder

    /** The fill of a dragged block. */
    fun draggedFill(colors: MonarchColors): Color = colors.bgSurface

    /** The icon of a callout tone in the tone row: a dot, info, check, warning or error mark. */
    fun toneIcon(tone: MonarchTone): @Composable () -> Unit = when (tone) {
        MonarchTone.Info -> MonarchDefaultIcons.Info
        MonarchTone.Success -> MonarchDefaultIcons.CheckCircle
        MonarchTone.Warning -> MonarchDefaultIcons.Warning
        MonarchTone.Danger -> MonarchDefaultIcons.Error
        else -> MonarchDefaultIcons.Dot
    }
}
