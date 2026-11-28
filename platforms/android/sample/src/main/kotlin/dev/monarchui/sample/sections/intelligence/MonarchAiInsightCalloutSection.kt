package dev.monarchui.sample.sections.intelligence

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import dev.monarchui.components.actions.button.MonarchButton
import dev.monarchui.components.intelligence.aiinsightcallout.MonarchAiInsightCallout
import dev.monarchui.components.intelligence.aiinsightcallout.MonarchAiInsightOrigin
import dev.monarchui.components.typography.text.MonarchText
import dev.monarchui.foundation.MonarchTone
import dev.monarchui.foundation.icon.MonarchIcon
import dev.monarchui.sample.CatalogueCaption
import dev.monarchui.sample.CataloguePage
import dev.monarchui.sampleicons.SampleIcons

/** The catalogue page of AiInsightCallout. */
object MonarchAiInsightCalloutSection : CataloguePage {
    override val title = "AiInsightCallout"

    @Composable
    override fun Content() {
        var shown by rememberSaveable { mutableStateOf(true) }
        var planned by rememberSaveable { mutableStateOf(false) }
        CatalogueCaption("AI insight with evidence, action and dismiss")
        if (shown) {
            MonarchAiInsightCallout(
                title = "Renewal at risk",
                why = "Usage fell 40% in the last 30 days and two admins left.",
                origin = MonarchAiInsightOrigin.Ai,
                tone = MonarchTone.Warning,
                actionLabel = "Plan outreach",
                onAction = { planned = true },
                onDismiss = { shown = false },
                icon = { MonarchIcon(SampleIcons.Bot, contentDescription = null) },
            ) { MonarchText(text = "Weekly active users: 120 to 72") }
        } else {
            MonarchButton(text = "Show the insight again", onClick = { shown = true })
        }
        if (planned) MonarchText(text = "Outreach planned")
        CatalogueCaption("Rule-based")
        MonarchAiInsightCallout(
            title = "Invoice overdue",
            why = "Unpaid for 30 days.",
            origin = MonarchAiInsightOrigin.Rule,
            tone = MonarchTone.Danger,
        )
        CatalogueCaption("Good news")
        MonarchAiInsightCallout(
            title = "Upsell signal",
            why = "Seats are 95% used.",
            origin = MonarchAiInsightOrigin.Ai,
            tone = MonarchTone.Success,
        )
    }
}
