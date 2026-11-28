package dev.monarchui.components.billing.payslipview

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.monarchui.internal.MonarchShapeDefaults
import dev.monarchui.theme.MonarchColors
import dev.monarchui.tokens.MonarchTokens

/** Component tokens and the pure rules of MonarchPayslipView. */
internal object MonarchPayslipViewTokens {
    /** The gap between the header, the details, the groups, the summary and the year-to-date figures. */
    val SectionGap: Dp = MonarchTokens.Space.space6.dp

    /** The gap between the lines of a group and between summary rows. */
    val BlockGap: Dp = MonarchTokens.Space.space3.dp

    /** The gap between a line's name and its note, and between the header lines. */
    val LineGap: Dp = MonarchTokens.Space.space1.dp

    /** The gap between a name and its amount, and between tiles and actions. */
    val CellGap: Dp = MonarchTokens.Space.space3.dp

    /** The padding inside a year-to-date tile. */
    val TilePadding: Dp = MonarchTokens.Space.space2.dp

    /** The smallest width of a year-to-date tile. */
    val TileMinWidth: Dp = MonarchTokens.Space.space24.dp

    /** From this width on, the details are in three columns. */
    val WideFrom: Dp = MonarchTokens.Breakpoint.sm.dp

    /** The number of detail columns at wide widths. */
    const val WIDE_COLUMNS: Int = 3

    /** The year-to-date tiles in one row, as on the web. */
    const val TILES_PER_ROW: Int = 4

    /** The padding of the raised totals panel. */
    val SectionPadding: Dp = MonarchTokens.Space.space4.dp

    /** The shape of a year-to-date tile. */
    val TileShape = MonarchShapeDefaults.Control

    /** The groups in the order they are shown. */
    val GroupOrder: List<MonarchPayslipLineKind> = MonarchPayslipLineKind.entries

    /** The colour of secondary text: the issuer, the subtitle, notes and tile labels. */
    fun mutedColor(colors: MonarchColors): Color = colors.textMuted

    /** The fill of a year-to-date tile. */
    fun tileColor(colors: MonarchColors): Color = colors.bgSubtle

    /** The raised panel of the totals. */
    fun panelColor(colors: MonarchColors): Color = colors.bgSubtle


    /**
     * The groups that are shown: each kind with its lines, in [GroupOrder],
     * without empty groups, and without employer contributions when [hideEmployer].
     */
    fun groups(
        lines: List<MonarchPayslipLine>,
        hideEmployer: Boolean,
    ): List<Pair<MonarchPayslipLineKind, List<MonarchPayslipLine>>> =
        GroupOrder
            .filterNot { hideEmployer && it == MonarchPayslipLineKind.Employer }
            .map { kind -> kind to lines.filter { it.kind == kind } }
            .filter { it.second.isNotEmpty() }

    /** Whether the reimbursements summary row shows: given and not zero. */
    fun reimbursementsShown(totals: MonarchPayslipTotals): Boolean =
        totals.reimbursements?.let { it.signum() != 0 } == true
}
