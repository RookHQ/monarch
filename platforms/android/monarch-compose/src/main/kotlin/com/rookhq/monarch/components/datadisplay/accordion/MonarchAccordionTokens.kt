package com.rookhq.monarch.components.datadisplay.accordion

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rookhq.monarch.components.layout.divider.MonarchDividerVariant
import com.rookhq.monarch.foundation.MonarchTypography
import com.rookhq.monarch.theme.MonarchColors
import com.rookhq.monarch.tokens.MonarchTokens

/** Component tokens and the pure rules of MonarchAccordion. */
internal object MonarchAccordionTokens {
    /** The header height, as on the web (3.5rem). */
    val HeaderMinHeight: Dp = (MonarchTokens.Space.space12 + MonarchTokens.Space.space2).dp

    /** The vertical padding of the header text. */
    val HeaderPadding: Dp = MonarchTokens.Space.space4.dp

    /** The space between the title and the trailing icon. */
    val HeaderGap: Dp = MonarchTokens.Space.space4.dp

    /** The space under the content of an open section. */
    val ContentBottomPadding: Dp = MonarchTokens.Space.space5.dp

    /** The trailing icon turns half a turn while the section is open, so a chevron points up. */
    const val EXPANDED_ROTATION: Float = 180f

    /**
     * The expanded set after the user activates the header of [id]. With [multiple] any number stay open;
     * otherwise opening one closes the others, and without [collapsible] the open one cannot be closed.
     */
    fun toggle(expanded: Set<String>, id: String, multiple: Boolean, collapsible: Boolean): Set<String> = when {
        id in expanded -> if (canCollapse(multiple, collapsible)) expanded - id else expanded
        multiple -> expanded + id
        else -> setOf(id)
    }

    /** Whether an open section can be closed by its header. */
    fun canCollapse(multiple: Boolean, collapsible: Boolean): Boolean = multiple || collapsible

    /** The title of a header: 16sp medium, like the web (the web does not use semibold here). */
    fun titleStyle(typography: MonarchTypography): TextStyle = typography.body.copy(fontWeight = FontWeight.Medium)

    /** The text of an open section: 14sp muted, like the web, not the 16sp body of the screen. */
    fun contentStyle(typography: MonarchTypography): TextStyle = typography.body.copy(
        fontSize = MonarchTokens.FontSize.sm.sp,
        lineHeight = (MonarchTokens.FontSize.sm * MonarchTokens.LineHeight.normal).sp,
    )

    /** The colour of a header's title. */
    fun titleColor(enabled: Boolean, colors: MonarchColors): Color =
        if (enabled) colors.textDefault else colors.textDisabled

    /** The colour of a header's trailing icon. */
    fun iconColor(enabled: Boolean, colors: MonarchColors): Color =
        if (enabled) colors.textMuted else colors.textDisabled

    /** The colour of the content text: muted, as on the web. */
    fun contentColor(colors: MonarchColors): Color = colors.textMuted

    /** The colour of the second line of a header. */
    fun subtitleColor(enabled: Boolean, colors: MonarchColors): Color =
        if (enabled) colors.textMuted else colors.textDisabled

    /** The colour of the lines between sections. */
    val DividerVariant: MonarchDividerVariant = MonarchDividerVariant.Default
}
