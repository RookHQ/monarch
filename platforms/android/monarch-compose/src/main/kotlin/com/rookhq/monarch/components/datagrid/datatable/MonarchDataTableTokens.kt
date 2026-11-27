package com.rookhq.monarch.components.datagrid.datatable

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.rookhq.monarch.theme.MonarchColors
import com.rookhq.monarch.tokens.MonarchTokens

/** Component tokens and the colour roles of MonarchDataTable. */
internal object MonarchDataTableTokens {
    /** The space between cells and between the parts of a record. */
    val CellGap: Dp = MonarchTokens.Space.space2.dp

    /** The space inside a row at the start and end. */
    val RowPaddingHorizontal: Dp = MonarchTokens.Space.space2.dp

    /** The space inside a row above and below. */
    val RowPaddingVertical: Dp = MonarchTokens.Space.space2.dp

    /** The space between the lines of a record in the list layout. */
    val LineGap: Dp = MonarchTokens.Space.space0_5.dp

    /** The width of a skeleton line while rows load. */
    val SkeletonHeight: Dp = MonarchTokens.Space.space6.dp

    /** The narrowest a column of the table layout gets before the table scrolls sideways. */
    val MinColumnWidth: Dp = (MonarchTokens.Space.space24 + MonarchTokens.Space.space4).dp

    /** The width of the selection and the actions columns. */
    val FixedColumnWidth: Dp = MonarchTokens.Space.space12.dp

    /** From this font scale the automatic layout is a list, so large text is not squeezed into cells. */
    const val LARGE_TEXT_SCALE: Float = 1.5f

    /** How many placeholder rows show while rows load. */
    const val SKELETON_ROWS: Int = 3

    /** Tabular figures for numeric cells. */
    const val TABULAR_FIGURES: String = "tnum"

    /** The fill of a row: the accent's subtle fill while selected. */
    fun rowColor(selected: Boolean, colors: MonarchColors): Color =
        if (selected) colors.accentSubtle else colors.bgSurface

    /** The text of headers and field names. */
    fun headerColor(colors: MonarchColors): Color = colors.textMuted

    /** The text of a row's name when it opens. */
    fun openableNameColor(colors: MonarchColors): Color = colors.textDefault
}
