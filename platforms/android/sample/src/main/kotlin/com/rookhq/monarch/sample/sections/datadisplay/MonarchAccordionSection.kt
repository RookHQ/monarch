package com.rookhq.monarch.sample.sections.datadisplay

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.rookhq.monarch.components.datadisplay.accordion.MonarchAccordion
import com.rookhq.monarch.components.datadisplay.accordion.MonarchAccordionItem
import com.rookhq.monarch.foundation.icon.MonarchIcon
import com.rookhq.monarch.sample.CatalogueCaption
import com.rookhq.monarch.sample.CataloguePage
import com.rookhq.monarch.sampleicons.SampleIcons

/** The catalogue page of Accordion. */
object MonarchAccordionSection : CataloguePage {
    override val title = "Accordion"

    private val items = listOf(
        MonarchAccordionItem("billing", "How does billing work?"),
        MonarchAccordionItem("cancel", "Can I cancel at any time?"),
        MonarchAccordionItem("export", "Can I export my data?"),
        MonarchAccordionItem("legacy", "Legacy plans", enabled = false),
    )
    private val answers = mapOf(
        "billing" to "Invoices are sent on the first day of each month.",
        "cancel" to "Yes. The plan stays active until the end of the period.",
        "export" to "Yes, as CSV from the settings screen.",
    )

    @Composable
    override fun Content() {
        var single by rememberSaveable { mutableStateOf(listOf("billing")) }
        var multiple by rememberSaveable { mutableStateOf(listOf<String>()) }
        CatalogueCaption("One open at a time")
        MonarchAccordion(
            items = items,
            expanded = single.toSet(),
            onExpandedChange = { single = it.toList() },
            trailingIcon = { MonarchIcon(imageVector = SampleIcons.ChevronDown, contentDescription = null) },
        ) { item -> Text(answers[item.id].orEmpty()) }
        CatalogueCaption("Any number open")
        MonarchAccordion(
            items = items.filter { it.enabled },
            expanded = multiple.toSet(),
            onExpandedChange = { multiple = it.toList() },
            multiple = true,
        ) { item -> Text(answers[item.id].orEmpty()) }
    }
}
