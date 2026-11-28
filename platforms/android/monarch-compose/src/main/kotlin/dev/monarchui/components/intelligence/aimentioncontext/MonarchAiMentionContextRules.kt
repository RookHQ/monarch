package dev.monarchui.components.intelligence.aimentioncontext

import dev.monarchui.components.collaboration.mentionpicker.MonarchMentionPickerStatus
import dev.monarchui.components.collaboration.mentionpicker.MonarchMentionQuery

/** The grouping and open logic of MonarchAiMentionContext, as pure functions. */
internal object MonarchAiMentionContextRules {
    /** The order of the groups. */
    val KindOrder: List<MonarchAiMentionKind> = listOf(
        MonarchAiMentionKind.Doc,
        MonarchAiMentionKind.Person,
        MonarchAiMentionKind.Record,
        MonarchAiMentionKind.App,
    )

    /** The results by kind, in [KindOrder], without empty groups; the order within a kind is kept. */
    fun groups(items: List<MonarchAiMentionItem>): List<Pair<MonarchAiMentionKind, List<MonarchAiMentionItem>>> =
        KindOrder.map { kind -> kind to items.filter { it.kind == kind } }.filter { it.second.isNotEmpty() }

    /** Whether headings are shown: when more than one kind is present. */
    fun grouped(items: List<MonarchAiMentionItem>): Boolean = groups(items).size > 1

    /** Whether the list shows: a query, unless a query with a space found nothing, which is ordinary prose. */
    fun isOpen(query: MonarchMentionQuery?, status: MonarchMentionPickerStatus, count: Int): Boolean =
        query != null && !(status == MonarchMentionPickerStatus.Ready && count == 0 && ' ' in query.text)

    /** What TalkBack reads for a result: the label, the description and the token estimate. */
    fun spoken(item: MonarchAiMentionItem, tokens: String?): String =
        listOfNotNull(item.label, item.description?.takeIf { it.isNotBlank() }, tokens).joinToString(", ")
}
