package com.rookhq.monarch.components.typography.inlinecodestyle

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.em
import com.rookhq.monarch.foundation.MonarchTypography
import com.rookhq.monarch.theme.MonarchColors

/** One run of text: plain text or code. */
internal data class InlineRun(val text: String, val isCode: Boolean)

/** Component tokens of MonarchInlineCodeStyle, built from roles only. */
internal object MonarchInlineCodeStyleTokens {
    /** Code is 0.9 of the surrounding text, as on the web, because a monospaced face looks larger. */
    private const val RELATIVE_SIZE = 0.9f

    /** Turns off ligatures, so `->` and `!=` are shown as typed. */
    private const val NO_LIGATURES = "liga 0, calt 0"

    private const val BACKTICK = '`'

    /**
     * The span style of [variant]: the code typeface at 0.9em; Subtle adds
     * the muted background and the default text colour, Plain keeps the
     * colour of the surrounding text.
     */
    fun spanStyle(colors: MonarchColors, typography: MonarchTypography, variant: MonarchInlineCodeVariant): SpanStyle =
        SpanStyle(
            color = if (variant == MonarchInlineCodeVariant.Subtle) colors.textDefault else Color.Unspecified,
            fontSize = RELATIVE_SIZE.em,
            fontFamily = typography.code.fontFamily,
            fontFeatureSettings = NO_LIGATURES,
            background = if (variant == MonarchInlineCodeVariant.Subtle) colors.bgMuted else Color.Unspecified,
        )

    /**
     * Splits [text] at pairs of backticks into plain and code runs. An
     * unmatched backtick and the text after it stay plain; an empty pair
     * gives no run.
     */
    fun runs(text: String): List<InlineRun> {
        val runs = mutableListOf<InlineRun>()
        var start = 0
        while (start < text.length) {
            val open = text.indexOf(BACKTICK, start)
            val close = if (open < 0) -1 else text.indexOf(BACKTICK, open + 1)
            if (open < 0 || close < 0) {
                runs += InlineRun(text.substring(start), isCode = false)
                break
            }
            if (open > start) runs += InlineRun(text.substring(start, open), isCode = false)
            if (close > open + 1) runs += InlineRun(text.substring(open + 1, close), isCode = true)
            start = close + 1
        }
        return runs
    }

    /** The annotated string of [runs], with [code] on the code runs. */
    fun build(runs: List<InlineRun>, code: SpanStyle): AnnotatedString = buildAnnotatedString {
        runs.forEach { run -> if (run.isCode) withStyle(code) { append(run.text) } else append(run.text) }
    }
}
