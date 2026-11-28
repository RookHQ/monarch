package dev.monarchui.components.typography.inlinecodestyle

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import dev.monarchui.foundation.MonarchTypography
import dev.monarchui.theme.MonarchColors
import dev.monarchui.theme.MonarchTheme

/**
 * The look of a code fragment inside running text, such as a function name,
 * a path or a command, as a [SpanStyle] for an [AnnotatedString].
 *
 * Running text in Compose is one `Text`, so inline code is a styled run of
 * that text, not a separate element: the code typeface of the theme at 0.9
 * of the surrounding size, on the muted background ([MonarchInlineCodeVariant.Subtle])
 * or without one ([MonarchInlineCodeVariant.Plain]). Pass the result to
 * `MonarchText` or `Text`.
 *
 * ```
 * MonarchText(text = MonarchInlineCodeStyle.markdown("Run `./gradlew build` first."))
 * ```
 */
public object MonarchInlineCodeStyle {
    /**
     * The span style of inline code in the theme that [colors] and
     * [typography] belong to.
     *
     * @param colors the colours of the theme.
     * @param typography the type roles of the theme; the code role gives the typeface.
     * @param variant with or without the muted background.
     */
    public fun spanStyle(
        colors: MonarchColors,
        typography: MonarchTypography,
        variant: MonarchInlineCodeVariant = MonarchInlineCodeVariant.Subtle,
    ): SpanStyle = MonarchInlineCodeStyleTokens.spanStyle(colors, typography, variant)

    /**
     * The span style of inline code in the current `MonarchTheme`.
     *
     * @param variant with or without the muted background.
     */
    @Composable
    @ReadOnlyComposable
    public fun spanStyle(variant: MonarchInlineCodeVariant = MonarchInlineCodeVariant.Subtle): SpanStyle =
        spanStyle(MonarchTheme.colors, MonarchTheme.typography, variant)

    /**
     * [code] as a single run of inline code, to append to other text with
     * `buildAnnotatedString { append(…) }` or `AnnotatedString.plus`.
     *
     * @param code the code fragment.
     * @param variant with or without the muted background.
     */
    @Composable
    @ReadOnlyComposable
    public fun annotatedString(
        code: String,
        variant: MonarchInlineCodeVariant = MonarchInlineCodeVariant.Subtle,
    ): AnnotatedString = MonarchInlineCodeStyleTokens.build(listOf(InlineRun(code, isCode = true)), spanStyle(variant))

    /**
     * [text] with every part between a pair of backticks styled as inline
     * code and the backticks removed: "Run \`build\` first." The rest of the
     * text is unchanged. An unmatched backtick is shown as it is.
     *
     * @param text the text with backticks around code.
     * @param variant with or without the muted background.
     */
    @Composable
    @ReadOnlyComposable
    public fun markdown(
        text: String,
        variant: MonarchInlineCodeVariant = MonarchInlineCodeVariant.Subtle,
    ): AnnotatedString = MonarchInlineCodeStyleTokens.build(MonarchInlineCodeStyleTokens.runs(text), spanStyle(variant))
}
