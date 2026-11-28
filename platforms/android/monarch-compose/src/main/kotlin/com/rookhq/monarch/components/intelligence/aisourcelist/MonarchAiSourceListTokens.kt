package com.rookhq.monarch.components.intelligence.aisourcelist

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.rookhq.monarch.components.collaboration.linkcard.MonarchLinkCardTokens
import com.rookhq.monarch.theme.MonarchColors
import com.rookhq.monarch.tokens.MonarchTokens
import java.net.URI
import java.net.URISyntaxException

/** Component tokens and the pure logic of MonarchAiSourceList. */
internal object MonarchAiSourceListTokens {
    val SectionGap: Dp = MonarchTokens.Space.space2.dp
    val RowGap: Dp = MonarchTokens.Space.space3.dp
    val RowVerticalPadding: Dp = MonarchTokens.Space.space2.dp
    val RowHorizontalPadding: Dp = MonarchTokens.Space.space1.dp
    val LineGap: Dp = MonarchTokens.Space.space0_5.dp
    val GroupTopPadding: Dp = MonarchTokens.Space.space2.dp
    const val SNIPPET_LINES: Int = 2
    const val DEFAULT_COLLAPSE_AFTER: Int = 3

    /** A source with its citation number, from 1. */
    data class Numbered(val number: Int, val item: MonarchAiSourceListItem)

    /** Whether the list is split into Used and Considered: some source was considered but not used. */
    fun grouped(items: List<MonarchAiSourceListItem>): Boolean = items.any { !it.used }

    /** The sources in display order, with their citation numbers: used first, then considered. */
    fun ordered(items: List<MonarchAiSourceListItem>): List<Numbered> {
        val numbered = items.mapIndexed { index, item -> Numbered(index + 1, item) }
        return numbered.filter { it.item.used } + numbered.filterNot { it.item.used }
    }

    /** Whether the list has more sources than it shows while collapsed. */
    fun collapsible(count: Int, collapseAfter: Int?): Boolean = collapseAfter != null && count > collapseAfter

    /** The sources shown: every one when expanded or not collapsible, otherwise the first [collapseAfter]. */
    fun shown(items: List<MonarchAiSourceListItem>, collapseAfter: Int?, expanded: Boolean): List<Numbered> {
        val ordered = ordered(items)
        if (expanded || collapseAfter == null || !collapsible(items.size, collapseAfter)) return ordered
        return ordered.take(collapseAfter.coerceAtLeast(0))
    }

    /** The address to open: only http, https and mailto links; anything else is not opened. */
    fun safeUrl(url: String?): String? = url?.trim()?.takeIf { it.isNotEmpty() && MonarchLinkCardTokens.isSafe(it) }

    /** The host of an address without "www.", or `null`. */
    fun host(url: String?): String? = try {
        url?.trim()?.let { URI(it).host }?.removePrefix("www.")?.takeIf { it.isNotEmpty() }
    } catch (_: URISyntaxException) {
        null
    }

    /** The meta line: host, product and kind, those that are present, separated by a middle dot. */
    fun meta(host: String?, product: String?, kind: String): String =
        listOfNotNull(host, product?.takeIf { it.isNotBlank() }, kind).joinToString(separator = " · ")

    /** The title is in the link colour when the source opens, otherwise in the default text colour. */
    fun titleColor(opens: Boolean, colors: MonarchColors): Color = if (opens) colors.textLink else colors.textDefault
}
