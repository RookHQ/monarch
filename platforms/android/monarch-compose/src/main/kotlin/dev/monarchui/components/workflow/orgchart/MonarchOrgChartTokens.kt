package dev.monarchui.components.workflow.orgchart

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.monarchui.accessibility.MonarchTouchTarget
import dev.monarchui.theme.MonarchColors
import dev.monarchui.tokens.MonarchTokens
import kotlin.math.roundToInt

/**
 * The colours of one card.
 *
 * @property container the fill.
 * @property border the border colour.
 * @property borderWidth the border width; thicker for the focused card and a drop target.
 * @property name the name's colour.
 * @property secondary the colour of the title, meta and notes.
 */
@Immutable
internal data class OrgChartCardColors(
    val container: Color,
    val border: Color,
    val borderWidth: Dp,
    val name: Color,
    val secondary: Color,
)

/** Component tokens and pure logic of MonarchOrgChart, built from primitives and roles only. */
internal object MonarchOrgChartTokens {
    /** The card width in the chart at font scale 1; it grows with the font scale. */
    val CardWidth: Dp = (MonarchTokens.Space.space24 * 2).dp
    val CardPadding: Dp = MonarchTokens.Space.space3.dp
    val CardGap: Dp = MonarchTokens.Space.space2.dp
    val SiblingGap: Dp = MonarchTokens.Space.space4.dp
    val LevelGap: Dp = MonarchTokens.Space.space6.dp
    val ChartPadding: Dp = MonarchTokens.Space.space4.dp
    val ListIndent: Dp = MonarchTokens.Space.space6.dp
    val ListGap: Dp = MonarchTokens.Space.space2.dp

    /** The width of the tree of the list layout, which is centred. */
    val ListWidth: Dp = (MonarchTokens.Space.space24 * 3 + MonarchTokens.Space.space4).dp

    /** The distance of the rail line from the start of the card of the manager. */
    val RailOffset: Dp = MonarchTokens.Space.space3.dp

    /** The distance of the chip under a card from the start of the card. */
    val ChipInset: Dp = MonarchTokens.Space.space2.dp

    /** How far the chip's touch target rises over the card above it. */
    val ChipOverlap: Dp = MonarchTokens.Space.space5.dp
    val ChipGap: Dp = MonarchTokens.Space.space1.dp
    val BorderWidth: Dp = MonarchTokens.Space.px.dp
    val StrongBorderWidth: Dp = MonarchTokens.Space.space0_5.dp
    val LineWidth: Dp = MonarchTokens.Space.px.dp
    val SpinnerSize: Dp = MonarchTokens.Space.space4.dp
    val SpinnerStroke: Dp = MonarchTokens.Space.space0_5.dp

    /** The width at which Auto shows the chart: the web's 40rem. */
    val ChartMinWidth: Dp = MonarchTokens.Breakpoint.sm.dp
    const val CHART_MAX_FONT_SCALE: Float = 1.3f
    const val LIST_MAX_INDENT_LEVELS: Int = 4
    const val EXPANDED_ROTATION: Float = 90f
    const val DASH: Float = 4f
    const val DASH_GAP: Float = 3f
    const val ZOOM_MIN: Float = 0.4f
    const val ZOOM_MAX: Float = 1.6f
    const val ZOOM_STEP: Float = 0.1f
    private const val PERCENT: Int = 100

    /** Whether [MonarchOrgChartLayout.Auto] shows the chart at this width and font scale. */
    fun showsChart(layout: MonarchOrgChartLayout, width: Dp, fontScale: Float): Boolean = when (layout) {
        MonarchOrgChartLayout.Chart -> true
        MonarchOrgChartLayout.List -> false
        MonarchOrgChartLayout.Auto -> width >= ChartMinWidth && fontScale <= CHART_MAX_FONT_SCALE
    }

    /**
     * The touch area of a chip in the scaled chart: 48dp divided by [zoom] below 100%, so it is 48dp on the
     * screen at every zoom level; the plain 48dp from 100%.
     */
    fun chipTouchSize(zoom: Float): Dp = MonarchTouchTarget.Minimum / zoom.coerceIn(ZOOM_MIN, 1f)

    /** [zoom] rounded to a tenth and kept between 40% and 160%. */
    fun clampZoom(zoom: Float): Float =
        ((zoom / ZOOM_STEP).roundToInt() * ZOOM_STEP).coerceIn(ZOOM_MIN, ZOOM_MAX)

    /** The zoom level as a whole percentage. */
    fun zoomPercent(zoom: Float): Int = (zoom * PERCENT).roundToInt()

    /** The list indent of [depth]; it stops growing after four levels so deep teams keep room on a phone. */
    fun listIndent(depth: Int): Dp = ListIndent * depth.coerceAtMost(LIST_MAX_INDENT_LEVELS)

    /**
     * The card colours: the surface with a subtle border, the muted fill for
     * an open position, the strong border for a dashed card, a thicker accent
     * border for the focused card, the subtle accent fill with the strong
     * accent border for a drop target, and the disabled roles when the chart
     * is disabled.
     */
    @Suppress("LongParameterList", "CyclomaticComplexMethod") // One flat decision per role, tested in every theme.
    fun cardColors(
        colors: MonarchColors,
        vacancy: Boolean,
        dashed: Boolean,
        focused: Boolean,
        dropTarget: Boolean,
        enabled: Boolean,
    ): OrgChartCardColors {
        val container = when {
            dropTarget -> colors.accentSubtle
            vacancy -> colors.bgMuted
            else -> colors.bgSurface
        }
        val border = when {
            !enabled -> colors.borderSubtle
            dropTarget -> colors.accentStrong
            focused -> colors.accentSolid
            dashed || vacancy -> colors.borderStrong
            else -> colors.borderSubtle
        }
        val width = if (enabled && (dropTarget || focused)) StrongBorderWidth else BorderWidth
        return OrgChartCardColors(
            container = container,
            border = border,
            borderWidth = width,
            name = if (enabled) colors.textDefault else colors.textDisabled,
            secondary = if (enabled) colors.textMuted else colors.textDisabled,
        )
    }

    /** The tile that stands in for the avatar of an open position. */
    fun vacancyTile(colors: MonarchColors): Color = colors.bgEmphasis

    /** The count, spinner and chevron of a team chip; the disabled text colour when it cannot be pressed. */
    fun chipContent(
        colors: MonarchColors,
        enabled: Boolean,
    ): Color = if (enabled) colors.textMuted else colors.textDisabled

    /** The connector lines. */
    fun lineColor(colors: MonarchColors): Color = colors.borderDefault

    /** The dotted reporting lines. */
    fun dottedColor(colors: MonarchColors): Color = colors.textSubtle
}
