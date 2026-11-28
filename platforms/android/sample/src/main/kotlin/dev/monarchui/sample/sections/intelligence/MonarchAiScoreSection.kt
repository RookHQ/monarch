package dev.monarchui.sample.sections.intelligence

import androidx.compose.material3.Slider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import dev.monarchui.components.intelligence.aiscore.MonarchAiScore
import dev.monarchui.components.intelligence.aiscore.MonarchAiScoreFactor
import dev.monarchui.components.intelligence.aiscore.MonarchAiScoreSize
import dev.monarchui.components.intelligence.aiscore.MonarchAiScoreVariant
import dev.monarchui.sample.CatalogueCaption
import dev.monarchui.sample.CataloguePage

/** The catalogue page of AiScore. */
object MonarchAiScoreSection : CataloguePage {
    override val title = "AiScore"

    private const val START_SCORE = 72f
    private const val MAX_SCORE = 100f
    private const val PRICING_IMPACT = 18.0
    private const val REPLY_IMPACT = -9.0
    private const val CHURN = 34.0

    private val factors = listOf(
        MonarchAiScoreFactor("Visited the pricing page", impact = PRICING_IMPACT, detail = "Twice in the last 7 days"),
        MonarchAiScoreFactor("No reply to the last email", impact = REPLY_IMPACT),
    )

    @Composable
    override fun Content() {
        var score by rememberSaveable { mutableFloatStateOf(START_SCORE) }
        CatalogueCaption("Ring with rationale, factors and the band legend")
        MonarchAiScore(
            value = score.toDouble(),
            label = "Lead score",
            rationale = "Engaged with pricing this week and matches the ideal customer profile.",
            factors = factors,
        )
        Slider(value = score, onValueChange = { score = it }, valueRange = 0f..MAX_SCORE)
        CatalogueCaption("Bar")
        MonarchAiScore(value = CHURN, variant = MonarchAiScoreVariant.Bar, label = "Churn risk", showBands = false)
        CatalogueCaption("Small, not scored yet")
        MonarchAiScore(value = null, size = MonarchAiScoreSize.Sm, label = "Fit", showBands = false)
    }
}
