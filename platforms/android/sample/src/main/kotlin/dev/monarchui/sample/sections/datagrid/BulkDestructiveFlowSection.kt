package dev.monarchui.sample.sections.datagrid

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import dev.monarchui.components.actions.button.MonarchButton
import dev.monarchui.components.actions.button.MonarchButtonVariant
import dev.monarchui.components.datadisplay.progress.MonarchProgress
import dev.monarchui.components.feedback.banner.MonarchBanner
import dev.monarchui.components.feedback.banner.MonarchBannerAnnouncement
import dev.monarchui.components.feedback.consequencelist.MonarchConsequence
import dev.monarchui.components.feedback.consequencelist.MonarchConsequenceKind
import dev.monarchui.components.feedback.consequencelist.MonarchConsequenceList
import dev.monarchui.components.forms.typetoconfirminput.MonarchTypeToConfirmInput
import dev.monarchui.components.forms.typetoconfirminput.MonarchTypeToConfirmInputDefaults
import dev.monarchui.components.overlays.bottomsheet.MonarchBottomSheet
import dev.monarchui.components.typography.text.MonarchText
import dev.monarchui.foundation.MonarchTextStyle
import dev.monarchui.foundation.MonarchTone
import dev.monarchui.sample.CatalogueCaption
import dev.monarchui.sample.CataloguePage
import dev.monarchui.theme.MonarchTheme
import kotlinx.coroutines.delay

/** The catalogue page of BulkDestructiveFlow: delete 37 leads in a locked sheet; one of them fails. */
object BulkDestructiveFlowSection : CataloguePage {
    override val title = "BulkDestructiveFlow"

    private const val LEAD_COUNT = 37
    private const val SAMPLE = 3
    private const val FAILING = "Lead 7"
    private const val STEP_MILLIS = 60L
    private val leads = (1..LEAD_COUNT).map { "Lead $it" }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        var open by rememberSaveable { mutableStateOf(false) }
        var typed by rememberSaveable { mutableStateOf("") }
        var running by rememberSaveable { mutableStateOf(false) }
        var finished by rememberSaveable { mutableStateOf(false) }
        var done by rememberSaveable { mutableIntStateOf(0) }
        var failed by rememberSaveable { mutableStateOf(listOf<String>()) }
        LaunchedEffect(running) {
            if (running) {
                leads.drop(done).forEach { lead ->
                    delay(STEP_MILLIS)
                    if (lead == FAILING) failed = failed + lead
                    done++
                }
                running = false
                finished = true
            }
        }
        val phrase = "${leads.size}"
        CatalogueCaption("Delete the selected leads")
        MonarchButton(
            text = "Delete ${leads.size} leads",
            onClick = {
                typed = ""
                done = 0
                failed = emptyList()
                finished = false
                open = true
            },
            variant = MonarchButtonVariant.Danger,
        )
        if (open) {
            MonarchBottomSheet(
                onDismissRequest = { if (!running) open = false },
                dismissible = !running,
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(MonarchTheme.spacing.space4),
                ) {
                    when {
                        finished -> {
                            MonarchBanner(
                                title = "${leads.size - failed.size} of ${leads.size} leads deleted",
                                message = if (failed.isEmpty()) {
                                    "Every lead was deleted."
                                } else {
                                    "Not deleted: ${failed.joinToString()}"
                                },
                                tone = if (failed.isEmpty()) MonarchTone.Success else MonarchTone.Warning,
                                announcement = MonarchBannerAnnouncement.Polite,
                            )
                            MonarchButton(
                                text = "Done",
                                onClick = { open = false },
                                modifier = Modifier.fillMaxWidth(),
                                variant = MonarchButtonVariant.Primary,
                            )
                        }
                        running -> MonarchProgress(
                            progress = done / leads.size.toFloat(),
                            label = "$done of ${leads.size} deleted",
                        )
                        else -> {
                            MonarchText(text = "Delete ${leads.size} leads", style = MonarchTextStyle.Heading)
                            MonarchText(
                                text = "${leads.take(SAMPLE).joinToString()}, +${leads.size - SAMPLE} more. " +
                                    "This cannot be undone.",
                            )
                            MonarchConsequenceList(
                                items = listOf(
                                    MonarchConsequence(
                                        MonarchConsequenceKind.Removed,
                                        "leads are deleted",
                                        count = leads.size,
                                    ),
                                    MonarchConsequence(
                                        MonarchConsequenceKind.Changed,
                                        "Their open tasks are unassigned",
                                    ),
                                ),
                            )
                            MonarchTypeToConfirmInput(
                                phrase = phrase,
                                value = typed,
                                onValueChange = { typed = it },
                                label = "Type $phrase to confirm",
                            )
                            MonarchButton(
                                text = "Delete ${leads.size} leads",
                                onClick = { running = true },
                                modifier = Modifier.fillMaxWidth(),
                                variant = MonarchButtonVariant.Danger,
                                enabled = MonarchTypeToConfirmInputDefaults.matches(typed, phrase),
                            )
                        }
                    }
                }
            }
        }
    }
}
