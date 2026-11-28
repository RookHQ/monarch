package com.rookhq.monarch.components.technical.installverify

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.rookhq.monarch.theme.MonarchColors
import com.rookhq.monarch.tokens.MonarchTokens

/** Component tokens and the pure logic of MonarchInstallVerify. */
internal object MonarchInstallVerifyTokens {
    /** The gap between the two halves. */
    val SectionGap: Dp = MonarchTokens.Space.space6.dp

    /** The gap between the parts of a half. */
    val Gap: Dp = MonarchTokens.Space.space3.dp

    /** The gap inside a row. */
    val SmallGap: Dp = MonarchTokens.Space.space2.dp

    /** The padding inside the status box. */
    val StatusPadding: Dp = MonarchTokens.Space.space3.dp

    /** The side of the status indicator. */
    val IndicatorSize: Dp = MonarchTokens.Space.space4.dp

    /** The side of a site's dot. */
    val DotSize: Dp = MonarchTokens.Space.space2.dp

    /** The hosts named in the connected title: [connectedOn], else the sites that have been seen. */
    fun hosts(connectedOn: List<String>?, targets: List<MonarchInstallTarget>): List<String> =
        connectedOn ?: targets.filter { it.lastSeenAt != null }.map { it.label }

    /** The id a snippet tab shows: [selection] when a snippet has it, else the first snippet's. */
    fun current(selection: String?, snippets: List<MonarchInstallSnippet>): String? =
        snippets.firstOrNull { it.id == selection }?.id ?: snippets.firstOrNull()?.id

    /** The fill and border of the status box. */
    fun statusColors(status: MonarchInstallVerifyStatus, colors: MonarchColors): Pair<Color, Color> = when (status) {
        MonarchInstallVerifyStatus.Connected -> colors.successSubtle to colors.successBorder
        MonarchInstallVerifyStatus.Unavailable -> colors.infoSubtle to colors.infoBorder
        MonarchInstallVerifyStatus.Waiting -> colors.bgSubtle to colors.borderSubtle
    }

    /** The colour of the status indicator. */
    fun indicatorColor(status: MonarchInstallVerifyStatus, colors: MonarchColors): Color = when (status) {
        MonarchInstallVerifyStatus.Connected -> colors.successSolid
        MonarchInstallVerifyStatus.Unavailable -> colors.infoSolid
        MonarchInstallVerifyStatus.Waiting -> colors.accentSolid
    }

    /** The colour of a site's dot: success once seen, the strong border before. */
    fun dotColor(seen: Boolean, colors: MonarchColors): Color = if (seen) colors.successSolid else colors.borderStrong
}
