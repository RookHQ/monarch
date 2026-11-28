package dev.monarchui.sample.sections.intelligence

import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import dev.monarchui.components.intelligence.aicitation.MonarchAiCitation
import dev.monarchui.components.intelligence.aicitation.MonarchAiCitationDefaults
import dev.monarchui.components.intelligence.aiconversation.MonarchAiSourceKind
import dev.monarchui.components.intelligence.aiconversation.MonarchAiSourcePart
import dev.monarchui.components.typography.text.MonarchText
import dev.monarchui.sample.CatalogueCaption
import dev.monarchui.sample.CataloguePage

private val Pricing = MonarchAiSourcePart(
    id = "pricing",
    title = "Pricing",
    url = "https://www.example.com/pricing",
    kind = MonarchAiSourceKind.Web,
)
private val Plan = MonarchAiSourcePart(
    id = "plan",
    title = "Q3 plan",
    kind = MonarchAiSourceKind.Doc,
    product = "Dossier",
)

/** The catalogue page of AiCitation. */
object MonarchAiCitationSection : CataloguePage {
    override val title = "AiCitation"

    @OptIn(ExperimentalLayoutApi::class)
    @Composable
    override fun Content() {
        var opened by rememberSaveable { mutableStateOf<String?>(null) }
        val numbers = MonarchAiCitationDefaults.sourceNumbers(listOf(Pricing, Plan))
        CatalogueCaption("Tap a marker")
        FlowRow(itemVerticalAlignment = Alignment.CenterVertically) {
            MonarchText(text = "Seats are billed monthly.")
            MonarchAiCitation(
                index = numbers.getValue(Pricing.id),
                source = Pricing,
                quote = "Seats are billed monthly, prorated to the day they were added.",
            )
            MonarchText(text = " The plan adds two seats in March.")
            MonarchAiCitation(index = numbers.getValue(Plan.id), source = Plan, onOpen = { opened = it.title })
        }
        opened?.let { MonarchText(text = "Opened in the app: $it") }
    }
}
