package com.rookhq.monarch.sample.sections.datadisplay

import androidx.compose.runtime.Composable
import com.rookhq.monarch.components.datadisplay.keyvalue.MonarchKeyValue
import com.rookhq.monarch.components.datadisplay.keyvalue.MonarchKeyValueItem
import com.rookhq.monarch.components.datadisplay.keyvalue.MonarchKeyValueLayout
import com.rookhq.monarch.components.datadisplay.keyvalue.MonarchKeyValueSeparator
import com.rookhq.monarch.components.datadisplay.keyvalue.MonarchKeyValueSize
import com.rookhq.monarch.sample.CatalogueCaption
import com.rookhq.monarch.sample.CataloguePage

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
