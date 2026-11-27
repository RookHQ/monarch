package com.rookhq.monarch.components.interaction.appearancepanel

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.rookhq.monarch.theme.MonarchColors
import com.rookhq.monarch.theme.MonarchContrast
import com.rookhq.monarch.tokens.MonarchAccent
import com.rookhq.monarch.tokens.MonarchTokens

/** Component tokens and the pure logic of MonarchAppearancePanel. */
internal object MonarchAppearancePanelTokens {
    /** The space above and below each row. */
    val RowPadding: Dp = MonarchTokens.Space.space4.dp

    /** The space between a row's label and its description. */
    val TextGap: Dp = MonarchTokens.Space.space1.dp

    /** The space between a row's text and its control. */
    val ControlGap: Dp = MonarchTokens.Space.space3.dp

    /** The space under the title. */
    val TitleGap: Dp = MonarchTokens.Space.space2.dp

    /** Contrast choices in the web's order. */
    val ContrastOrder: List<MonarchContrast> =
        listOf(MonarchContrast.Standard, MonarchContrast.More, MonarchContrast.System)

    /** The colour of a row's label. */
    fun label(colors: MonarchColors): Color = colors.textDefault

    /** The colour of a row's description. */
    fun description(colors: MonarchColors): Color = colors.textMuted

    /** The rows to draw: the requested ones in order, each once. */
    fun visibleRows(rows: List<MonarchAppearancePanelRow>): List<MonarchAppearancePanelRow> = rows.distinct()

    /** The accents the menu offers: [accents], plus [current] at the end when it is not among them. */
    fun accentChoices(accents: List<MonarchAccent>, current: MonarchAccent): List<MonarchAccent> {
        val unique = accents.distinctBy { it.id }
        return if (unique.any { it.id == current.id }) unique else unique + current
    }
}
