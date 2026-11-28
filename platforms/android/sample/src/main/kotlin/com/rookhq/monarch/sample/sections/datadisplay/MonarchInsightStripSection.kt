package com.rookhq.monarch.sample.sections.datadisplay

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.rookhq.monarch.components.datadisplay.insightstrip.MonarchInsight
import com.rookhq.monarch.components.datadisplay.insightstrip.MonarchInsightOrigin
import com.rookhq.monarch.components.datadisplay.insightstrip.MonarchInsightStrip
import com.rookhq.monarch.components.datadisplay.insightstrip.MonarchInsightStripLayout
import com.rookhq.monarch.foundation.MonarchTone
import com.rookhq.monarch.sample.CatalogueCaption
import com.rookhq.monarch.sample.CataloguePage

/** The catalogue page of InsightStrip. */
object MonarchInsightStripSection : CataloguePage {
    override val title = "InsightStrip"

    private val insights = listOf(
        MonarchInsight(
            id = "renewals",
            title = "3 domains expire in 30 days",
            why = "Auto-renew is off for all three.",
            origin = MonarchInsightOrigin.Rule,
            tone = MonarchTone.Warning,
            evidence = "example.com, example.org, example.net",
        ),
        MonarchInsight(
            id = "churn",
            title = "Renewal at risk",
            why = "Usage fell by half in the last month.",
            origin = MonarchInsightOrigin.Ai,
            tone = MonarchTone.Danger,
        ),
        MonarchInsight(
            id = "seats",
            title = "Usage is up",
            why = "Twelve more seats were active this week.",
            origin = MonarchInsightOrigin.Rule,
            tone = MonarchTone.Success,
        ),
    )

    @Composable
    override fun Content() {
        var dismissed by rememberSaveable { mutableStateOf(listOf<String>()) }
        CatalogueCaption("Scrolling row, dismissable")
        MonarchInsightStrip(
            insights = insights.filterNot { it.id in dismissed },
            title = "Needs attention",
            onDismiss = { dismissed = dismissed + it },
        )
        CatalogueCaption("Stack with a limit")
        MonarchInsightStrip(insights = insights, layout = MonarchInsightStripLayout.Stack, limit = 1)
        CatalogueCaption("Grid")
        MonarchInsightStrip(insights = insights, layout = MonarchInsightStripLayout.Grid)
        CatalogueCaption("Loading, error and empty")
        MonarchInsightStrip(insights = emptyList(), loading = true)
        MonarchInsightStrip(insights = emptyList(), errorText = "Insights could not be loaded.", onRetry = {})
        MonarchInsightStrip(insights = emptyList())
    }
}
