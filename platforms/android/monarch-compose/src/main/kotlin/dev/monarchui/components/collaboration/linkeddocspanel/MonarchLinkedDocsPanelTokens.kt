package dev.monarchui.components.collaboration.linkeddocspanel

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.monarchui.components.files.filetypeicon.MonarchFileTypeFamily
import dev.monarchui.components.layout.sectioncard.MonarchSectionCardState
import dev.monarchui.theme.MonarchColors
import dev.monarchui.tokens.MonarchTokens
import java.net.URI
import java.text.Collator

/** Component tokens and the pure rules of MonarchLinkedDocsPanel. */
internal object MonarchLinkedDocsPanelTokens {
    /** The space between the chips, the rows, the form and the notes. */
    val SectionGap: Dp = MonarchTokens.Space.space3.dp

    /** The space between rows. */
    val RowGap: Dp = MonarchTokens.Space.space1.dp

    /** The space between the parts of a row and between chips. */
    val PartGap: Dp = MonarchTokens.Space.space2.dp

    /** The space inside a row above and below. */
    val RowPaddingVertical: Dp = MonarchTokens.Space.space1.dp

    /** The filter key of rows without a Space. */
    const val NO_SPACE: String = "\u0000none"

    /** The filter key that shows every row. */
    const val ALL: String = ""

    /** One Space filter chip: its key, its name and how many rows it has. */
    data class Chip(val key: String, val name: String?, val count: Int)

    /** The Space chips, by name, with the rows without a Space last. */
    fun chips(items: List<MonarchLinkedDocsPanelItem>): List<Chip> {
        val collator = Collator.getInstance()
        return items.groupBy { it.spaceId ?: NO_SPACE }
            .map { (key, rows) -> Chip(key, rows.first().spaceName.takeIf { key != NO_SPACE }, rows.size) }
            .sortedWith { a, b ->
                when {
                    a.key == NO_SPACE -> 1
                    b.key == NO_SPACE -> -1
                    else -> collator.compare(a.name ?: a.key, b.name ?: b.key)
                }
            }
    }

    /** The filter in effect: [ALL] without chips or for a Space that no longer has rows. */
    fun activeKey(filter: String, chips: List<Chip>, showChips: Boolean): String =
        if (!showChips || chips.none { it.key == filter }) ALL else filter

    /** The rows the filter [key] shows. */
    fun shown(items: List<MonarchLinkedDocsPanelItem>, key: String): List<MonarchLinkedDocsPanelItem> =
        if (key == ALL) items else items.filter { (it.spaceId ?: NO_SPACE) == key }

    /** The docs the menu offers: not linked yet, and matching [query] in the label or the description. */
    fun options(
        options: List<MonarchLinkedDocsPanelOption>,
        linked: Set<String>,
        query: String,
    ): List<MonarchLinkedDocsPanelOption> {
        val needle = query.trim()
        return options.filter { option ->
            option.docId !in linked && (
                needle.isEmpty() ||
                    option.label.contains(needle, ignoreCase = true) ||
                    option.description?.contains(needle, ignoreCase = true) == true
                )
        }
    }

    /** A full http or https address with a host, trimmed, or `null`. */
    fun parseLinkUrl(raw: String): String? {
        val text = raw.trim()
        if (text.isEmpty()) return null
        val uri = runCatching { URI(text) }.getOrNull() ?: return null
        val scheme = uri.scheme?.lowercase()
        return text.takeIf { (scheme == "http" || scheme == "https") && !uri.host.isNullOrEmpty() }
    }

    /** The host of [url] without "www.", or an empty string. */
    fun hostOf(url: String?): String {
        val host = url?.let { runCatching { URI(it.trim()).host }.getOrNull() }
        return host?.removePrefix("www.").orEmpty()
    }

    /** The secondary line of a row: its meta, or a link's host name. */
    fun metaOf(item: MonarchLinkedDocsPanelItem): String? =
        item.meta ?: hostOf(item.url).takeIf { item.kind == MonarchLinkedDocsPanelKind.Link && it.isNotEmpty() }

    /** The colour of the secondary text of a row and of the notes. */
    fun mutedText(colors: MonarchColors): Color = colors.textMuted

    /** The file family drawn for a row: from its file name, else a document for a doc and other for a link. */
    fun familyOf(item: MonarchLinkedDocsPanelItem): MonarchFileTypeFamily = when {
        item.fileName != null -> MonarchFileTypeFamily.of(item.fileName)
        item.kind == MonarchLinkedDocsPanelKind.Doc -> MonarchFileTypeFamily.Document
        else -> MonarchFileTypeFamily.Other
    }

    /** The state of the section card: the rows once loaded, the error while nothing loaded, else loading. */
    fun cardState(loaded: Boolean, failed: Boolean): MonarchSectionCardState = when {
        loaded -> MonarchSectionCardState.Ready
        failed -> MonarchSectionCardState.Error
        else -> MonarchSectionCardState.Loading
    }
}
