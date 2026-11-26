package com.rookhq.monarch.components.typography.prosestyle

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.rookhq.monarch.foundation.MonarchTypography
import com.rookhq.monarch.theme.MonarchColors
import com.rookhq.monarch.tokens.MonarchTokens

/** One block of a `MonarchProse`, as the scope collected it. */
internal sealed interface ProseBlock {
    data class Heading(val text: AnnotatedString, val level: Int) : ProseBlock
    data class Paragraph(val text: AnnotatedString) : ProseBlock
    data class ListItem(val text: AnnotatedString, val marker: String?, val depth: Int) : ProseBlock
    data class Quote(val text: AnnotatedString) : ProseBlock
    data class Code(val text: String) : ProseBlock
    data class Caption(val text: String) : ProseBlock
    data class Table(val columns: List<String>, val rows: List<List<String>>) : ProseBlock
    data object Rule : ProseBlock
}

/** The colours of the parts of prose that are not text. */
internal data class ProseDecorationColors(
    val quoteBar: Color,
    val marker: Color,
    val codeBackground: Color,
    val codeBorder: Color,
    val rule: Color,
)

/** Collects the blocks of a `MonarchProse` in order. */
internal class ProseBlocks : MonarchProseScope {
    val blocks: MutableList<ProseBlock> = mutableListOf()

    override fun heading(text: AnnotatedString, level: Int) {
        blocks += ProseBlock.Heading(text, MonarchProseStyleTokens.clampLevel(level))
    }

    override fun paragraph(text: AnnotatedString) {
        blocks += ProseBlock.Paragraph(text)
    }

    override fun listItem(text: AnnotatedString, marker: String?, depth: Int) {
        blocks += ProseBlock.ListItem(text, marker, maxOf(0, depth))
    }

    override fun quote(text: AnnotatedString) {
        blocks += ProseBlock.Quote(text)
    }

    override fun code(text: String) {
        blocks += ProseBlock.Code(text)
    }

    override fun caption(text: String) {
        blocks += ProseBlock.Caption(text)
    }

    override fun table(columns: List<String>, rows: List<List<String>>) {
        blocks += ProseBlock.Table(columns, rows)
    }

    override fun rule() {
        blocks += ProseBlock.Rule
    }
}

/**
 * Component tokens of MonarchProseStyle: the web Prose rhythm, in multiples
 * of the base size ("em"), built from type and space tokens only.
 */
internal object MonarchProseStyleTokens {
    /** The reading measure, in base sizes: 42 × 16sp = 672dp at the default font size. */
    const val MEASURE_EM: Float = 42f

    const val PARAGRAPH_GAP_EM: Float = 1.25f
    const val HEADING_GAP_EM: Float = 1.75f
    const val AFTER_HEADING_GAP_EM: Float = 0.5f
    const val LIST_ITEM_GAP_EM: Float = 0.375f
    const val LIST_INDENT_EM: Float = 1.5f
    const val QUOTE_INDENT_EM: Float = 1f
    const val CODE_EM: Float = 0.875f
    const val CAPTION_EM: Float = 0.875f

    val QuoteBarWidth: Dp = (MonarchTokens.Space.space0_5 + MonarchTokens.Space.px).dp
    val CodeVerticalPadding: Dp = MonarchTokens.Space.space3.dp
    val CodeHorizontalPadding: Dp = MonarchTokens.Space.space4.dp
    val TableLineWidth: Dp = MonarchTokens.Space.px.dp
    val TableCellVerticalPadding: Dp = MonarchTokens.Space.space1_5.dp
    val TableCellEndPadding: Dp = MonarchTokens.Space.space2.dp

    private const val MAX_LEVEL = 6
    private val headingScale = floatArrayOf(1.875f, 1.5f, 1.25f, 1.125f, 1f, 1f)

    /** The base font size in sp. */
    fun baseSize(size: MonarchProseSize): Float = when (size) {
        MonarchProseSize.Sm -> MonarchTokens.FontSize.sm
        MonarchProseSize.Md -> MonarchTokens.FontSize.base
        MonarchProseSize.Lg -> MonarchTokens.FontSize.lg
    }

    /** Normal leading at Sm, relaxed otherwise, as on the web. */
    private fun bodyLeading(size: MonarchProseSize): Float =
        if (size == MonarchProseSize.Sm) MonarchTokens.LineHeight.normal else MonarchTokens.LineHeight.relaxed

    /** Tight tracking for a level 1 heading, snug for the others. */
    private fun tracking(level: Int): Float =
        if (level == 1) MonarchTokens.LetterSpacing.tight else MonarchTokens.LetterSpacing.snug

    /** A heading level from 1 to 6. */
    fun clampLevel(level: Int): Int = level.coerceIn(1, MAX_LEVEL)

    /** The running text: the body face at the base size, relaxed leading (normal at Sm), default text colour. */
    fun body(size: MonarchProseSize, typography: MonarchTypography, colors: MonarchColors): TextStyle =
        typography.body.copy(
            fontSize = baseSize(size).sp,
            lineHeight = bodyLeading(size).em,
            color = colors.textDefault,
        )

    /**
     * A heading: semibold, 1.875, 1.5, 1.25, 1.125 or 1 times the base size
     * for levels 1 to 6, tight leading for 1 and 2, the muted colour for 6.
     */
    fun heading(
        level: Int,
        size: MonarchProseSize,
        typography: MonarchTypography,
        colors: MonarchColors,
    ): TextStyle {
        val clamped = clampLevel(level)
        val large = clamped <= 2
        return typography.heading.copy(
            fontSize = (baseSize(size) * headingScale[clamped - 1]).sp,
            fontWeight = FontWeight(MonarchTokens.FontWeight.semibold),
            lineHeight = (if (large) MonarchTokens.LineHeight.tight else MonarchTokens.LineHeight.snug).em,
            letterSpacing = tracking(clamped).em,
            color = if (clamped == MAX_LEVEL) colors.textMuted else colors.textDefault,
        )
    }

    /** A code block: the code face at 0.875 of the base size, normal leading, default text colour. */
    fun code(size: MonarchProseSize, typography: MonarchTypography, colors: MonarchColors): TextStyle =
        typography.code.copy(
            fontSize = (baseSize(size) * CODE_EM).sp,
            lineHeight = MonarchTokens.LineHeight.normal.em,
            color = colors.textDefault,
        )

    /** A caption: 0.875 of the base size in the muted colour. */
    fun caption(size: MonarchProseSize, typography: MonarchTypography, colors: MonarchColors): TextStyle =
        body(size, typography, colors).copy(fontSize = (baseSize(size) * CAPTION_EM).sp, color = colors.textMuted)

    /**
     * The colours of the decorations: the strong border colour for the quote
     * bar, the muted text colour for list markers, the subtle background and
     * border for the code panel and the subtle border for the rule.
     */
    fun decorations(colors: MonarchColors): ProseDecorationColors = ProseDecorationColors(
        quoteBar = colors.borderStrong,
        marker = colors.textMuted,
        codeBackground = colors.bgSubtle,
        codeBorder = colors.borderSubtle,
        rule = colors.borderSubtle,
    )

    /**
     * The space above [block], in base sizes: none for the first block, 1.75
     * above a heading, 0.5 after a heading, 0.375 between list items and 1.25
     * between other blocks, as in the web's Prose.
     */
    fun spaceBefore(block: ProseBlock, previous: ProseBlock?): Float = when {
        previous == null -> 0f
        block is ProseBlock.Heading -> HEADING_GAP_EM
        previous is ProseBlock.Heading -> AFTER_HEADING_GAP_EM
        block is ProseBlock.ListItem && previous is ProseBlock.ListItem -> LIST_ITEM_GAP_EM
        else -> PARAGRAPH_GAP_EM
    }

    /** The bullet of an unnumbered item: a disc, and a ring for nested lists, as on the web. */
    fun bullet(depth: Int): String = if (depth % 2 == 0) "•" else "◦"
}
