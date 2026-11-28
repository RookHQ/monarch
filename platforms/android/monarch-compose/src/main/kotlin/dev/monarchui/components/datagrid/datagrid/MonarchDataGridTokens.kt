package dev.monarchui.components.datagrid.datagrid

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.monarchui.accessibility.MonarchTouchTarget
import dev.monarchui.theme.MonarchColors
import dev.monarchui.tokens.MonarchTokens

/** Component tokens and the colour roles of MonarchDataGrid. */
internal object MonarchDataGridTokens {
    /** The width of a column without its own width: 160dp. */
    val ColumnWidth: Dp = (MonarchTokens.Space.space24 + MonarchTokens.Space.space16).dp

    /** The narrowest a column can be made from its menu. */
    val MinColumnWidth: Dp = MonarchTokens.Space.space24.dp

    /** The widest a column can be made from its menu. */
    val MaxColumnWidth: Dp = (MonarchTokens.Space.space24 * MAX_WIDTH_STEPS).dp

    /** How much Widen and Narrow change a column, as the web's Alt+Arrow keys do. */
    val ResizeStep: Dp = MonarchTokens.Space.space4.dp

    /** The width of the checkbox column and the actions column: one touch target. */
    val UtilityWidth: Dp = MonarchTouchTarget.Minimum

    /** The width of the actions column when the actions button shows text instead of the app's icon. */
    val ActionsTextWidth: Dp = MonarchTokens.Space.space24.dp

    /** The space inside a cell at the start and end. */
    val CellPaddingHorizontal: Dp = MonarchTokens.Space.space3.dp

    /** The space inside a cell above and below, comfortable density. */
    val CellPaddingComfortable: Dp = MonarchTokens.Space.space2.dp

    /** The space inside a cell above and below, compact density. */
    val CellPaddingCompact: Dp = MonarchTokens.Space.space0_5.dp

    /** The space inside a record of the list layout. */
    val RecordPadding: Dp = MonarchTokens.Space.space3.dp

    /** The space between the lines of a record and between the fields of the editor. */
    val LineGap: Dp = MonarchTokens.Space.space1.dp

    /** The space between the parts of the editor sheet. */
    val SheetGap: Dp = MonarchTokens.Space.space4.dp

    /** The divider under each row and the line at the edge of the pinned columns. */
    val DividerWidth: Dp = MonarchTokens.Space.px.dp

    /** At most this share of the grid's width is pinned; wider pins scroll with the rest. */
    const val MAX_PINNED_SHARE: Float = 0.5f

    /** Placeholder rows while the first rows load. */
    const val SKELETON_ROWS: Int = 4

    /** Tabular figures for numeric cells. */
    const val TABULAR_FIGURES: String = "tnum"

    private const val MAX_WIDTH_STEPS = 5

    /** The vertical padding of a cell at [density]. */
    fun cellPadding(density: MonarchDataGridDensity): Dp = when (density) {
        MonarchDataGridDensity.Comfortable -> CellPaddingComfortable
        MonarchDataGridDensity.Compact -> CellPaddingCompact
    }

    /** The fill of a row: the accent's subtle fill while selected. */
    fun rowColor(selected: Boolean, colors: MonarchColors): Color =
        if (selected) colors.accentSubtle else colors.bgSurface

    /** The fill of the header row. */
    fun headerColor(colors: MonarchColors): Color = colors.bgSubtle

    /** The text of headers and field names. */
    fun headerText(colors: MonarchColors): Color = colors.textMuted

    /** The line at the inner edge of the pinned columns. */
    fun pinnedEdgeColor(colors: MonarchColors): Color = colors.borderDefault

    /** The text of a row's name when it opens. */
    fun openableText(colors: MonarchColors): Color = colors.textDefault
}
