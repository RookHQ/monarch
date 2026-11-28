package com.rookhq.monarch.components.layout.marketingsection

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.rookhq.monarch.components.datadisplay.accordion.MonarchAccordion
import com.rookhq.monarch.components.datadisplay.accordion.MonarchAccordionItem
import com.rookhq.monarch.components.typography.text.MonarchText
import com.rookhq.monarch.theme.MonarchTheme

/**
 * Questions and answers: each question is a row that expands to show its answer.
 *
 * Built on `MonarchAccordion`, so each question is a button with its expanded or collapsed state, and several
 * answers can be open at once. Which answers are open is kept across configuration changes.
 *
 * @param items the questions, in reading order.
 * @param modifier the modifier of the list.
 */
@Composable
public fun MonarchFaq(
    items: List<MonarchFaqItem>,
    modifier: Modifier = Modifier,
) {
    var expanded by rememberSaveable { mutableStateOf(emptyList<String>()) }
    val rows = items.mapIndexed { index, item -> MonarchAccordionItem(id = index.toString(), title = item.question) }
    MonarchAccordion(
        items = rows,
        expanded = expanded.toSet(),
        onExpandedChange = { expanded = it.toList() },
        modifier = modifier,
        multiple = true,
    ) { row ->
        MonarchText(text = items[row.id.toInt()].answer, color = MonarchTheme.colors.textMuted)
    }
}
