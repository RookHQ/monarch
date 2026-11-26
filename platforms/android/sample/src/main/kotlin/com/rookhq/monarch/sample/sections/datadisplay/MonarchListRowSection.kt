package com.rookhq.monarch.sample.sections.datadisplay

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.rookhq.monarch.components.datadisplay.badge.MonarchBadge
import com.rookhq.monarch.components.datadisplay.listrow.MonarchListRow
import com.rookhq.monarch.foundation.MonarchTone
import com.rookhq.monarch.foundation.icon.MonarchIcon
import com.rookhq.monarch.sample.CatalogueCaption
import com.rookhq.monarch.sample.CataloguePage
import com.rookhq.monarch.sampleicons.SampleIcons
import com.rookhq.monarch.theme.MonarchTheme

/** The catalogue page of List row. */
object MonarchListRowSection : CataloguePage {
    override val title = "List row"

    @Composable
    override fun Content() {
        CatalogueCaption("Tappable rows with a divider")
        Column {
            MonarchListRow(
                headlineContent = { Text("Invoice INV-1042") },
                supportingContent = { Text("Acme Corp · Paid Mar 3") },
                leadingContent = { MonarchIcon(imageVector = SampleIcons.Card, contentDescription = null) },
                trailingContent = { Text("$420.00") },
                onClick = {},
                divider = true,
            )
            MonarchListRow(
                headlineContent = { Text("Invoice INV-1043") },
                supportingContent = { Text("Globex · Due Mar 18") },
                leadingContent = { MonarchIcon(imageVector = SampleIcons.Card, contentDescription = null) },
                trailingContent = { MonarchBadge(text = "Due", tone = MonarchTone.Warning) },
                onClick = {},
            )
        }
        CatalogueCaption("Selected, disabled and an overline")
        var current by remember { mutableStateOf("Weekly") }
        Column {
            listOf("Daily", "Weekly").forEach { option ->
                MonarchListRow(
                    headlineContent = { Text(option) },
                    onClick = { current = option },
                    selected = current == option,
                    divider = true,
                )
            }
            MonarchListRow(
                overlineContent = { Text("Enterprise plan") },
                headlineContent = { Text("Hourly") },
                trailingContent = {
                    MonarchIcon(
                        imageVector = SampleIcons.Lock,
                        contentDescription = null,
                        tint = MonarchTheme.colors.textMuted,
                    )
                },
                onClick = {},
                enabled = false,
            )
        }
    }
}
