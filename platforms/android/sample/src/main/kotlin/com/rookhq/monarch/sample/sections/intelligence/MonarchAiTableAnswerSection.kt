package com.rookhq.monarch.sample.sections.intelligence

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.rookhq.monarch.components.actions.button.MonarchButton
import com.rookhq.monarch.components.actions.button.MonarchButtonSize
import com.rookhq.monarch.components.intelligence.aiconversation.MonarchAiJSON
import com.rookhq.monarch.components.intelligence.aitableanswer.MonarchAiTableAnswer
import com.rookhq.monarch.components.intelligence.aitableanswer.MonarchAiTableColumn
import com.rookhq.monarch.components.typography.text.MonarchText
import com.rookhq.monarch.sample.CatalogueCaption
import com.rookhq.monarch.sample.CataloguePage

private val Regions = listOf(
    "North" to 1200.0,
    "South" to 950.5,
    "East" to 1875.0,
    "West" to 640.0,
)

private fun regionRow(region: String, revenue: Double, closed: String) = mapOf(
    "region" to MonarchAiJSON.Text(region),
    "revenue" to MonarchAiJSON.Number(revenue),
    "closed" to MonarchAiJSON.Text(closed),
)

private val Rows = Regions.mapIndexed { index, (region, revenue) ->
    regionRow(region, revenue, "2024-10-1${index + 1}")
}

/** The catalogue page of AiTableAnswer. */
object MonarchAiTableAnswerSection : CataloguePage {
    override val title = "AiTableAnswer"

    @Composable
    override fun Content() {
        var arrived by rememberSaveable { mutableIntStateOf(1) }
        var opened by rememberSaveable { mutableStateOf(false) }
        CatalogueCaption("A finished table with named columns")
        MonarchAiTableAnswer(
            rows = Rows,
            title = "Revenue by region",
            columns = listOf(
                MonarchAiTableColumn("region", label = "Region"),
                MonarchAiTableColumn("revenue", label = "Revenue"),
                MonarchAiTableColumn("closed", label = "Closed"),
            ),
            onOpenAsArtifact = { opened = true },
        )
        if (opened) MonarchText(text = "Opened as an artifact")
        CatalogueCaption("Rows streaming in")
        MonarchAiTableAnswer(rows = Rows.take(arrived), streaming = arrived < Rows.size)
        MonarchButton(
            text = if (arrived < Rows.size) "Add a row" else "Start again",
            onClick = { arrived = if (arrived < Rows.size) arrived + 1 else 1 },
            size = MonarchButtonSize.Sm,
        )
    }
}
