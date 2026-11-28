package dev.monarchui.sample.sections.workflow

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import dev.monarchui.components.actions.button.MonarchButton
import dev.monarchui.components.actions.button.MonarchButtonVariant
import dev.monarchui.components.datadisplay.card.MonarchCard
import dev.monarchui.components.forms.durationinput.MonarchDurationInput
import dev.monarchui.components.forms.durationinput.MonarchDurationInputUnit
import dev.monarchui.components.forms.select.MonarchSelect
import dev.monarchui.components.forms.select.MonarchSelectOption
import dev.monarchui.components.forms.textfield.MonarchTextField
import dev.monarchui.components.interaction.reorderablelist.MonarchReorderableList
import dev.monarchui.components.overlays.bottomsheet.MonarchBottomSheet
import dev.monarchui.components.typography.text.MonarchText
import dev.monarchui.foundation.MonarchTextStyle
import dev.monarchui.sample.CatalogueCaption
import dev.monarchui.sample.CataloguePage
import dev.monarchui.theme.MonarchTheme

private const val MinutesPerDay = 1440
private const val MinutesPerHour = 60
private const val ListHeightSteps = 8
private const val FirstNewStep = 3

/** The catalogue page of SequenceBuilder: steps in a reorderable list, each edited in a sheet with a preview. */
object SequenceBuilderSection : CataloguePage {
    override val title = "SequenceBuilder"

    private data class Step(
        val id: String,
        val channel: String,
        val waitMinutes: Int?,
        val subject: String,
        val body: String,
    )

    private val channels = listOf(MonarchSelectOption("email", "Email"), MonarchSelectOption("chat", "Chat message"))
    private val sample = mapOf("firstName" to "Ada", "company" to "Northwind")

    private fun render(text: String) =
        sample.entries.fold(text) { acc, (token, value) -> acc.replace("{{$token}}", value) }

    private fun channelName(value: String) = channels.firstOrNull { it.value == value }?.label ?: value

    private fun whenSent(minutes: Int?) = when {
        minutes == null || minutes == 0 -> "Sent straight away"
        minutes % MinutesPerDay == 0 -> "Sent ${minutes / MinutesPerDay} d after the previous step"
        else -> "Sent ${minutes / MinutesPerHour} h ${minutes % MinutesPerHour} min after the previous step"
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        var steps by remember {
            mutableStateOf(
                listOf(
                    Step("s1", "email", 0, "Welcome to {{company}}", "Hi {{firstName}}, thanks for signing up."),
                    Step("s2", "email", 2 * MinutesPerDay, "Getting started", "Hi {{firstName}}, here are three tips."),
                ),
            )
        }
        var editing by remember { mutableStateOf<String?>(null) }
        var next by remember { mutableIntStateOf(FirstNewStep) }
        CatalogueCaption("A welcome sequence")
        MonarchReorderableList(
            items = steps,
            key = { it.id },
            itemLabel = { "Step ${steps.indexOf(it) + 1}, ${channelName(it.channel)}" },
            onReorder = { from, to -> steps = steps.toMutableList().apply { add(to, removeAt(from)) } },
            modifier = Modifier.heightIn(max = MonarchTheme.spacing.space24 * ListHeightSteps),
            moveButtons = true,
        ) { step, state ->
            MonarchCard(onClick = { editing = step.id }, modifier = Modifier.weight(1f)) {
                MonarchText(
                    text = "Step ${state.index + 1} · ${channelName(step.channel)}",
                    style = MonarchTextStyle.BodyStrong,
                )
                MonarchText(text = whenSent(step.waitMinutes), style = MonarchTextStyle.Caption)
                MonarchText(text = render(step.subject.ifEmpty { step.body }), maxLines = 2)
            }
        }
        MonarchButton(
            text = "Add step",
            onClick = {
                val id = "s$next"
                next += 1
                steps = steps + Step(id, "email", MinutesPerDay, "", "")
                editing = id
            },
        )
        val step = steps.firstOrNull { it.id == editing }
        if (step != null) {
            val update = { changed: Step -> steps = steps.map { if (it.id == changed.id) changed else it } }
            MonarchBottomSheet(onDismissRequest = { editing = null }) {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(MonarchTheme.spacing.space3),
                ) {
                    MonarchDurationInput(
                        minutes = step.waitMinutes,
                        onMinutesChange = { update(step.copy(waitMinutes = it)) },
                        label = "Wait before sending",
                        largestUnit = MonarchDurationInputUnit.Days,
                        step = MinutesPerHour,
                    )
                    MonarchSelect(
                        options = channels,
                        selected = step.channel,
                        onSelect = { update(step.copy(channel = it)) },
                        modifier = Modifier.fillMaxWidth(),
                        label = "Channel",
                    )
                    if (step.channel == "email") {
                        MonarchTextField(
                            value = step.subject,
                            onValueChange = { update(step.copy(subject = it)) },
                            modifier = Modifier.fillMaxWidth(),
                            label = "Subject",
                        )
                    }
                    MonarchTextField(
                        value = step.body,
                        onValueChange = { update(step.copy(body = it)) },
                        modifier = Modifier.fillMaxWidth(),
                        label = "Message",
                        helperText = "Insert {{firstName}} or {{company}}.",
                        singleLine = false,
                    )
                    MonarchText(text = "Preview with sample data", style = MonarchTextStyle.Subheading)
                    MonarchText(text = render(step.body))
                    MonarchButton(
                        text = "Remove step",
                        onClick = {
                            steps = steps.filterNot { it.id == step.id }
                            editing = null
                        },
                        variant = MonarchButtonVariant.Danger,
                    )
                    MonarchButton(
                        text = "Done",
                        onClick = { this@MonarchBottomSheet.dismiss() },
                        variant = MonarchButtonVariant.Primary,
                    )
                }
            }
        }
    }
}
