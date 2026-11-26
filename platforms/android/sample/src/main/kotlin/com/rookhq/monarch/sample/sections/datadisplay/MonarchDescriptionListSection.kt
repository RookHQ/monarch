package com.rookhq.monarch.sample.sections.datadisplay

import androidx.compose.runtime.Composable
import com.rookhq.monarch.components.datadisplay.badge.MonarchBadge
import com.rookhq.monarch.components.datadisplay.descriptionlist.MonarchDescriptionItem
import com.rookhq.monarch.components.datadisplay.descriptionlist.MonarchDescriptionList
import com.rookhq.monarch.components.datadisplay.descriptionlist.MonarchDescriptionListLayout
import com.rookhq.monarch.foundation.MonarchTone
import com.rookhq.monarch.foundation.icon.MonarchIcon
import com.rookhq.monarch.sample.CatalogueCaption
import com.rookhq.monarch.sample.CataloguePage
import com.rookhq.monarch.sampleicons.SampleIcons

/** The catalogue page of DescriptionList. */
object MonarchDescriptionListSection : CataloguePage {
    override val title = "DescriptionList"

    private val items = listOf(
        MonarchDescriptionItem("Owner", "Ada Lovelace"),
        MonarchDescriptionItem("Stage", "Won", content = { MonarchBadge(text = "Won", tone = MonarchTone.Success) }),
        MonarchDescriptionItem("Customer ID", "cus_9s6X…i3", copyValue = "cus_9s6XKzkNRiz8i3"),
        MonarchDescriptionItem("Close date", null),
        MonarchDescriptionItem(
            "Notes",
            "Asked for a second call with the finance team before signing the annual contract in March.",
            maxLines = 1,
            wide = true,
        ),
    )

    @Composable
    override fun Content() {
        CatalogueCaption("Stacked, two columns on wide screens")
        MonarchDescriptionList(items = items, columns = 2)
        CatalogueCaption("Inline, with an icon copy button")
        MonarchDescriptionList(
            items = items.dropLast(1),
            layout = MonarchDescriptionListLayout.Inline,
            copyIcon = { MonarchIcon(imageVector = SampleIcons.Copy, contentDescription = null) },
        )
    }
}
