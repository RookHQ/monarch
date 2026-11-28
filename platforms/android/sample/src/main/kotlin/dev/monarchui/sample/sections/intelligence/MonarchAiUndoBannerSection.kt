package dev.monarchui.sample.sections.intelligence

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import dev.monarchui.components.actions.button.MonarchButton
import dev.monarchui.components.actions.button.MonarchButtonVariant
import dev.monarchui.components.intelligence.aiundobanner.MonarchAiChange
import dev.monarchui.components.intelligence.aiundobanner.MonarchAiUndoBanner
import dev.monarchui.components.typography.text.MonarchText
import dev.monarchui.foundation.MonarchTextStyle
import dev.monarchui.sample.CatalogueCaption
import dev.monarchui.sample.CataloguePage
import dev.monarchui.theme.MonarchTheme
import kotlinx.coroutines.delay

private const val SimulatedUndoMillis = 800L
private const val UndoWindowMillis = 15_000L

/** The catalogue page of AiUndoBanner. */
object MonarchAiUndoBannerSection : CataloguePage {
    override val title = "AiUndoBanner"

    private val dealChanges = listOf(
        MonarchAiChange("Status", "Open", "Closed won"),
        MonarchAiChange("Owner", "Ana Flores", "Sam Lee"),
    )

    private val manyChanges = listOf(
        MonarchAiChange("Stage", "Proposal", "Negotiation"),
        MonarchAiChange("Amount", "€12,000", "€14,500"),
        MonarchAiChange("Close date", "30 Nov", "15 Dec"),
        MonarchAiChange("Next step", "Send proposal", "Book legal review"),
    )

    @Composable
    override fun Content() {
        var round by rememberSaveable { mutableIntStateOf(0) }
        var dismissed by rememberSaveable { mutableStateOf(false) }
        var audits by rememberSaveable { mutableIntStateOf(0) }
        CatalogueCaption("Two changes, with the audit log and Dismiss")
        key(round) {
            if (!dismissed) {
                MonarchAiUndoBanner(
                    title = "AI changed 2 fields",
                    onUndo = { delay(SimulatedUndoMillis) },
                    description = "On the Acme Ltd deal",
                    changes = dealChanges,
                    onAudit = { audits++ },
                    onDismiss = { dismissed = true },
                )
            }
        }
        if (audits > 0) {
            MonarchText(
                text = "Audit log opened $audits times",
                style = MonarchTextStyle.Caption,
                color = MonarchTheme.colors.textMuted,
            )
        }
        CatalogueCaption("Four changes in a disclosure, Undo for 15 seconds")
        key(round) {
            MonarchAiUndoBanner(
                title = "AI updated the forecast",
                onUndo = { delay(SimulatedUndoMillis) },
                changes = manyChanges,
                durationMillis = UndoWindowMillis,
            )
        }
        MonarchButton(
            text = "Reset the examples",
            onClick = {
                round++
                dismissed = false
            },
            variant = MonarchButtonVariant.Ghost,
        )
    }
}
