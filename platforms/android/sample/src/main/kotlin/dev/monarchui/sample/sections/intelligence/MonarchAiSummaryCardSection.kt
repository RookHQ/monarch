@file:OptIn(ExperimentalMonarchApi::class)

package dev.monarchui.sample.sections.intelligence

import android.text.format.DateUtils
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import dev.monarchui.ExperimentalMonarchApi
import dev.monarchui.components.actions.button.MonarchButton
import dev.monarchui.components.actions.button.MonarchButtonSize
import dev.monarchui.components.actions.button.MonarchButtonVariant
import dev.monarchui.components.intelligence.aiconversation.MonarchAiSourceKind
import dev.monarchui.components.intelligence.aiconversation.MonarchAiSourcePart
import dev.monarchui.components.intelligence.aisummarycard.MonarchAiSummaryCard
import dev.monarchui.components.intelligence.aisummarycard.MonarchAiSummaryCardStatus
import dev.monarchui.sample.CatalogueCaption
import dev.monarchui.sample.CataloguePage

private val Sources = listOf(
    MonarchAiSourcePart("t1", "Ticket 4821: exports time out", null, MonarchAiSourceKind.Record),
    MonarchAiSourcePart("d1", "Renewal contract 2026", "https://example.com/contract", MonarchAiSourceKind.Doc),
)

private const val Summary = "**Acme** renewed for 2026 in March. Two open tickets mention *slow exports*; " +
    "both are with the data team.\n\n- Renewal: 120 seats\n- Next review: June"

private const val AgeHours = 3

/** The catalogue page of AiSummaryCard. */
object MonarchAiSummaryCardSection : CataloguePage {
    override val title = "AiSummaryCard"

    @Composable
    override fun Content() {
        var refreshing by rememberSaveable { mutableStateOf(false) }
        var generatedAt by rememberSaveable {
            mutableLongStateOf(System.currentTimeMillis() - AgeHours * DateUtils.HOUR_IN_MILLIS)
        }
        CatalogueCaption("A summary with its age, sources and Refresh; tap Finish to end the refresh")
        MonarchAiSummaryCard(
            title = "Account summary",
            summary = Summary,
            refreshing = refreshing,
            generatedAt = generatedAt,
            sources = Sources,
            onRefresh = { refreshing = true },
            actions = {
                if (refreshing) {
                    MonarchButton(
                        text = "Finish",
                        onClick = {
                            refreshing = false
                            generatedAt = System.currentTimeMillis()
                        },
                        variant = MonarchButtonVariant.Ghost,
                        size = MonarchButtonSize.Sm,
                    )
                }
            },
        )
        CatalogueCaption("Stale, generating and failed")
        MonarchAiSummaryCard(
            title = "Deal summary",
            summary = "Closing in May.",
            status = MonarchAiSummaryCardStatus.Stale,
        )
        MonarchAiSummaryCard(title = "Deal summary", summary = "", status = MonarchAiSummaryCardStatus.Generating)
        MonarchAiSummaryCard(
            title = "Deal summary",
            summary = "",
            status = MonarchAiSummaryCardStatus.Error,
            error = "The model did not answer in time.",
            onRefresh = {},
        )
    }
}
