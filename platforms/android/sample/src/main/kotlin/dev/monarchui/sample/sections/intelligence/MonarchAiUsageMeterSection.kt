package dev.monarchui.sample.sections.intelligence

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import dev.monarchui.components.actions.button.MonarchButton
import dev.monarchui.components.actions.button.MonarchButtonSize
import dev.monarchui.components.intelligence.aiusagemeter.MonarchAiUsageMeter
import dev.monarchui.components.intelligence.aiusagemeter.MonarchAiUsageScope
import dev.monarchui.components.intelligence.aiusagemeter.MonarchAiUsageUnit
import dev.monarchui.sample.CatalogueCaption
import dev.monarchui.sample.CataloguePage

/** The catalogue page of AiUsageMeter. */
object MonarchAiUsageMeterSection : CataloguePage {
    override val title = "AiUsageMeter"

    @Composable
    override fun Content() {
        var conversation by rememberSaveable { mutableDoubleStateOf(StartingTokens) }
        CatalogueCaption("Turn, conversation and month")
        MonarchAiUsageMeter(
            scopes = listOf(
                MonarchAiUsageScope("This turn", used = 1_850.0),
                MonarchAiUsageScope("Conversation", used = conversation, limit = 50_000.0),
                MonarchAiUsageScope("This month", used = 920.0, limit = 1_000.0, unit = MonarchAiUsageUnit.Credits),
            ),
        )
        MonarchButton(
            text = "Use 6,000 tokens",
            onClick = { conversation += TokensPerReply },
            size = MonarchButtonSize.Sm,
        )
        CatalogueCaption("Titled, loading")
        MonarchAiUsageMeter(scopes = emptyList(), title = "Image generation", period = "November", loading = true)
    }
}

private const val StartingTokens = 36_000.0
private const val TokensPerReply = 6_000.0
