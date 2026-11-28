package dev.monarchui.sample.sections.datadisplay

import androidx.compose.runtime.Composable
import dev.monarchui.components.datadisplay.keyvalue.MonarchKeyValue
import dev.monarchui.components.datadisplay.keyvalue.MonarchKeyValueItem
import dev.monarchui.components.datadisplay.keyvalue.MonarchKeyValueLayout
import dev.monarchui.components.datadisplay.keyvalue.MonarchKeyValueSeparator
import dev.monarchui.components.datadisplay.keyvalue.MonarchKeyValueSize
import dev.monarchui.sample.CatalogueCaption
import dev.monarchui.sample.CataloguePage

/** The catalogue page of KeyValue. */
object MonarchKeyValueSection : CataloguePage {
    override val title = "KeyValue"

    private val workspace = listOf(
        MonarchKeyValueItem("Region", "eu-west"),
        MonarchKeyValueItem("Plan", "Team"),
        MonarchKeyValueItem("ID", "4f2a…", copyValue = "4f2a9c1e-77b0-4d1e", monospace = true),
        MonarchKeyValueItem("Owner", null),
    )

    @Composable
    override fun Content() {
        CatalogueCaption("Wrap with dots")
        MonarchKeyValue(items = workspace)
        CatalogueCaption("Dividers, body size")
        MonarchKeyValue(items = workspace, separator = MonarchKeyValueSeparator.Divider, size = MonarchKeyValueSize.Md)
        CatalogueCaption("Stack, for a narrow card")
        MonarchKeyValue(items = workspace, layout = MonarchKeyValueLayout.Stack)
    }
}
