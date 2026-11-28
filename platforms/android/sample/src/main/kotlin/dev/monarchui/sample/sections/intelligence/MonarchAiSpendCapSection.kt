@file:OptIn(ExperimentalMonarchApi::class)

package dev.monarchui.sample.sections.intelligence

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import dev.monarchui.ExperimentalMonarchApi
import dev.monarchui.components.actions.button.MonarchButton
import dev.monarchui.components.intelligence.aispendcap.MonarchAiSpendCap
import dev.monarchui.components.intelligence.aispendcap.MonarchAiSpendCapBehavior
import dev.monarchui.components.intelligence.aispendcap.MonarchAiSpendCapBreakdownItem
import dev.monarchui.components.intelligence.aispendcap.MonarchAiSpendCapPeriod
import dev.monarchui.sample.CatalogueCaption
import dev.monarchui.sample.CataloguePage

private const val StartSpent = 1_240L
private const val SpendStep = 900L
private const val StartLimit = 5_000L
private const val StartWarnAt = 0.8
private const val ResearchSpent = 820L
private const val SummariesSpent = 420L
private const val ReadOnlySpent = 5_000L

/** The catalogue page of AiSpendCap. */
object MonarchAiSpendCapSection : CataloguePage {
    override val title = "AiSpendCap"

    @Composable
    override fun Content() {
        var spent by rememberSaveable { mutableLongStateOf(StartSpent) }
        var limit by rememberSaveable { mutableStateOf<Long?>(StartLimit) }
        var warnAt by rememberSaveable { mutableStateOf(StartWarnAt) }
        var behavior by rememberSaveable { mutableStateOf(MonarchAiSpendCapBehavior.Pause) }
        CatalogueCaption("Spend more to move through the states; edit the limit")
        MonarchAiSpendCap(
            spent = spent,
            limit = limit,
            currencyCode = "GBP",
            period = MonarchAiSpendCapPeriod.Month,
            warnAt = warnAt,
            behavior = behavior,
            breakdown = listOf(
                MonarchAiSpendCapBreakdownItem("research", "Research agent", ResearchSpent),
                MonarchAiSpendCapBreakdownItem("summaries", "Meeting summaries", SummariesSpent),
            ),
            onSave = { settings ->
                limit = settings.limit
                warnAt = settings.warnAt
                behavior = settings.behavior
            },
        )
        MonarchButton(text = "Spend more", onClick = { spent += SpendStep })
        CatalogueCaption("Read only, at the limit")
        MonarchAiSpendCap(
            spent = ReadOnlySpent,
            limit = StartLimit,
            currencyCode = "USD",
            period = MonarchAiSpendCapPeriod.Day,
            canEdit = false,
            onRequestIncrease = {},
        )
    }
}
