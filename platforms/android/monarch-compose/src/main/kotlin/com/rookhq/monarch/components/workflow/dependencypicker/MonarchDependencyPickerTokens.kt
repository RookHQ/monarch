package com.rookhq.monarch.components.workflow.dependencypicker

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.rookhq.monarch.components.forms.combobox.MonarchComboboxOption
import com.rookhq.monarch.theme.MonarchColors
import com.rookhq.monarch.tokens.MonarchTokens

/** Component tokens and the pure logic of MonarchDependencyPicker. */
internal object MonarchDependencyPickerTokens {
    /** The gap between the groups and the add controls. */
    val SectionGap: Dp = MonarchTokens.Space.space4.dp

    /** The gap between a heading and its rows, and between the add controls. */
    val GroupGap: Dp = MonarchTokens.Space.space1.dp

    /** The gap between the glyph, the key, the title and the remove button of a row. */
    val RowGap: Dp = MonarchTokens.Space.space2.dp

    /** The colour of an item's key. */
    fun keyColor(colors: MonarchColors) = colors.textMuted

    /** The colour of the empty note. */
    fun noteColor(colors: MonarchColors) = colors.textMuted

    /** An item as one line of text: the key, then the title. */
    fun itemLabel(item: MonarchDependencyItem): String = listOfNotNull(item.key, item.title).joinToString(" ")

    /** The links grouped by relation, in the order of [relations], leaving out empty groups. */
    fun groups(
        links: List<MonarchDependencyLink>,
        relations: List<MonarchDependencyRelation>,
    ): List<Pair<MonarchDependencyRelation, List<MonarchDependencyLink>>> =
        relations.map { r -> r to links.filter { it.relation == r } }.filter { it.second.isNotEmpty() }

    /** The items that can still be linked: not excluded and not linked already. */
    fun candidates(
        items: List<MonarchDependencyItem>,
        links: List<MonarchDependencyLink>,
        excludeIds: Set<String>,
    ): List<MonarchDependencyItem> {
        val excluded = excludeIds + links.map { it.item.id }
        return items.filter { it.id !in excluded }
    }

    /** A candidate as a combobox option: the title, the key as a hint and a search keyword, the status below. */
    fun option(item: MonarchDependencyItem): MonarchComboboxOption = MonarchComboboxOption(
        id = item.id,
        label = item.title,
        description = item.status?.name,
        hint = item.key,
        keywords = item.key,
    )
}
