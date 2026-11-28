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
import dev.monarchui.components.datadisplay.keyvalue.MonarchKeyValueItem
import dev.monarchui.components.intelligence.aitoolresultcard.MonarchAiToolResultCard
import dev.monarchui.components.intelligence.aitoolresultcard.MonarchAiToolResultKind
import dev.monarchui.components.typography.text.MonarchText
import dev.monarchui.foundation.MonarchTextStyle
import dev.monarchui.foundation.icon.MonarchIcon
import dev.monarchui.sample.CatalogueCaption
import dev.monarchui.sample.CataloguePage
import dev.monarchui.sampleicons.SampleIcons
import dev.monarchui.theme.MonarchTheme
import kotlinx.coroutines.delay

private const val SimulatedUndoMillis = 800L

/** The catalogue page of AiToolResultCard. */
object MonarchAiToolResultCardSection : CataloguePage {
    override val title = "AiToolResultCard"

    @Composable
    override fun Content() {
        var round by rememberSaveable { mutableIntStateOf(0) }
        var failNext by rememberSaveable { mutableStateOf(true) }
        var opened by rememberSaveable { mutableIntStateOf(0) }
        CatalogueCaption("A record created, with Open and Undo. The first Undo fails; the second works.")
        key(round) {
            MonarchAiToolResultCard(
                title = "Acme Ltd",
                kind = MonarchAiToolResultKind.RecordCreated,
                description = "Added to Accounts",
                meta = listOf(MonarchKeyValueItem("Owner", "Ana Flores"), MonarchKeyValueItem("Region", "EMEA")),
                icon = { MonarchIcon(SampleIcons.Wrench, contentDescription = null) },
                onOpen = { opened++ },
                onUndo = {
                    delay(SimulatedUndoMillis)
                    if (failNext) {
                        failNext = false
                        error("The CRM did not answer")
                    }
                },
            )
        }
        if (opened > 0) {
            MonarchText(
                text = "Opened $opened times",
                style = MonarchTextStyle.Caption,
                color = MonarchTheme.colors.textMuted,
            )
        }
        MonarchButton(
            text = "Reset the example",
            onClick = {
                round++
                failNext = true
            },
            variant = MonarchButtonVariant.Ghost,
        )
        CatalogueCaption("An email sent, without Undo")
        MonarchAiToolResultCard(
            title = "Renewal reminder",
            kind = MonarchAiToolResultKind.EmailSent,
            description = "To 3 people",
        )
    }
}
