package dev.monarchui.screenshots.showcase

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.monarchui.components.datadisplay.tag.MonarchChip
import dev.monarchui.components.datadisplay.tag.MonarchTag
import dev.monarchui.components.forms.checkbox.MonarchCheckbox
import dev.monarchui.components.forms.checkboxgroup.MonarchCheckboxGroup
import dev.monarchui.components.forms.checkboxgroup.MonarchCheckboxGroupOption
import dev.monarchui.components.forms.radio.MonarchRadio
import dev.monarchui.components.forms.radiocardgroup.MonarchRadioCardGroup
import dev.monarchui.components.forms.radiocardgroup.MonarchRadioCardOption
import dev.monarchui.components.forms.radiogroup.MonarchRadioGroup
import dev.monarchui.components.forms.rangeslider.MonarchRangeSlider
import dev.monarchui.components.forms.segmentedcontrol.MonarchSegmentedControl
import dev.monarchui.components.forms.slider.MonarchSlider
import dev.monarchui.components.forms.switch.MonarchSwitch
import dev.monarchui.components.forms.switchsettingslist.MonarchSwitchSettingsList
import dev.monarchui.components.forms.switchsettingslist.MonarchSwitchSettingsRow
import dev.monarchui.foundation.MonarchTone
import dev.monarchui.foundation.icon.MonarchIcon
import dev.monarchui.foundation.icon.MonarchIconSize
import dev.monarchui.sampleicons.SampleIcons

private val chipGap = Arrangement.spacedBy(8.dp)

/**
 * The selection controls (W2), one realistic scenario each (PREMIUM rule 14): real copy, real icons, no stacked
 * test states. States (invalid, disabled, sizes) live in the regression fixtures, not here. Registered after the
 * field showcases, so these replace the older entries.
 */
internal val selectionShowcases: Map<String, @Composable () -> Unit> = mapOf(
    "checkbox" to {
        var mentions by remember { mutableStateOf(true) }
        var summary by remember { mutableStateOf(false) }
        var product by remember { mutableStateOf(true) }
        ShowcaseColumn {
            Column {
                MonarchCheckbox(
                    checked = mentions,
                    onCheckedChange = { mentions = it },
                    label = "Email me when I am mentioned",
                    description = "Comments and replies in your projects.",
                )
                MonarchCheckbox(checked = summary, onCheckedChange = { summary = it }, label = "Weekly summary")
                MonarchCheckbox(checked = product, onCheckedChange = { product = it }, label = "Product updates")
            }
        }
    },
    "checkbox-group" to {
        var value by remember { mutableStateOf(listOf("mentions", "status")) }
        ShowcaseColumn {
            MonarchCheckboxGroup(
                options = listOf(
                    MonarchCheckboxGroupOption("mentions", "Mentions"),
                    MonarchCheckboxGroupOption("comments", "Comments on my issues"),
                    MonarchCheckboxGroupOption("status", "Status changes"),
                ),
                value = value,
                onValueChange = { value = it },
                label = "Notify me about",
                helperText = "We send at most one email an hour.",
                selectAll = true,
            )
        }
    },
    "radio-group" to {
        var plan by remember { mutableStateOf("team") }
        ShowcaseColumn {
            MonarchRadioGroup(label = "Plan") {
                MonarchRadio(selected = plan == "solo", onClick = { plan = "solo" }, label = "Solo")
                MonarchRadio(
                    selected = plan == "team",
                    onClick = { plan = "team" },
                    label = "Team",
                    description = "Up to 20 people and shared projects.",
                )
                MonarchRadio(
                    selected = plan == "enterprise",
                    onClick = { plan = "enterprise" },
                    label = "Enterprise",
                    description = "Single sign-on and audit logs.",
                )
            }
        }
    },
    "radio-card-group" to {
        var speed by remember { mutableStateOf("express") }
        ShowcaseColumn {
            MonarchRadioCardGroup(
                options = listOf(
                    MonarchRadioCardOption("standard", "Standard", "3 to 5 business days", meta = "$5"),
                    MonarchRadioCardOption("express", "Express", "1 to 2 business days", meta = "$15"),
                    MonarchRadioCardOption("overnight", "Overnight", "Next business day", meta = "$30"),
                ),
                selected = speed,
                onSelect = { speed = it },
                label = "Shipping",
            )
        }
    },
    "switch" to {
        var alerts by remember { mutableStateOf(true) }
        var digest by remember { mutableStateOf(false) }
        var twoStep by remember { mutableStateOf(true) }
        ShowcaseColumn {
            Column {
                MonarchSwitch(
                    checked = alerts,
                    onCheckedChange = { alerts = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = "Deploy alerts",
                    description = "Tell me when a deploy fails.",
                )
                MonarchSwitch(
                    checked = digest,
                    onCheckedChange = { digest = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = "Weekly digest",
                )
                MonarchSwitch(
                    checked = twoStep,
                    onCheckedChange = { twoStep = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = "Two-step sign-in",
                )
            }
        }
    },
    "switch-settings-list" to {
        var mentions by remember { mutableStateOf(true) }
        var digest by remember { mutableStateOf(false) }
        ShowcaseColumn {
            MonarchSwitchSettingsList(
                modifier = Modifier.fillMaxWidth(),
                title = "Notifications",
                description = "Changes save as you go.",
            ) {
                MonarchSwitchSettingsRow(
                    checked = mentions,
                    onCheckedChange = { mentions = it },
                    onSave = {},
                    label = "Mentions",
                    description = "When someone mentions you in a comment.",
                )
                MonarchSwitchSettingsRow(
                    checked = digest,
                    onCheckedChange = { digest = it },
                    onSave = {},
                    label = "Weekly digest",
                )
            }
        }
    },
    "segmented-control" to {
        var view by remember { mutableIntStateOf(1) }
        ShowcaseColumn {
            MonarchSegmentedControl {
                Segment(
                    selected = view == 0,
                    onClick = { view = 0 },
                    label = "List",
                    icon = { MonarchIcon(SampleIcons.ListSimple, null, size = MonarchIconSize.Sm) },
                )
                Segment(
                    selected = view == 1,
                    onClick = { view = 1 },
                    label = "Board",
                    icon = { MonarchIcon(SampleIcons.Board, null, size = MonarchIconSize.Sm) },
                )
                Segment(
                    selected = view == 2,
                    onClick = { view = 2 },
                    label = "Grid",
                    icon = { MonarchIcon(SampleIcons.Grid, null, size = MonarchIconSize.Sm) },
                )
            }
        }
    },
    "slider" to {
        var volume by remember { mutableFloatStateOf(40f) }
        ShowcaseColumn {
            MonarchSlider(
                value = volume,
                onValueChange = { volume = it },
                valueRange = 0f..100f,
                label = "Volume",
                valueLabel = { "${it.toInt()}%" },
            )
        }
    },
    "range-slider" to {
        var price by remember { mutableStateOf(20f..150f) }
        ShowcaseColumn {
            MonarchRangeSlider(
                value = price,
                onValueChange = { price = it },
                valueRange = 0f..200f,
                label = "Price",
                valueLabel = { "$${it.toInt()}" },
            )
        }
    },
    "tag" to {
        var open by remember { mutableStateOf(true) }
        var mine by remember { mutableStateOf(true) }
        var closed by remember { mutableStateOf(false) }
        ShowcaseColumn {
            // An issue list header: filter chips, then the labels of one issue.
            Wrap(chipGap) {
                MonarchChip(text = "Open", selected = open, onSelectedChange = { open = it })
                MonarchChip(text = "Assigned to me", selected = mine, onSelectedChange = { mine = it })
                MonarchChip(text = "Closed", selected = closed, onSelectedChange = { closed = it })
            }
            Wrap(chipGap) {
                MonarchTag(text = "In review", tone = MonarchTone.Info)
                MonarchTag(text = "Billing")
                MonarchTag(text = "Blocked", tone = MonarchTone.Danger)
            }
        }
    },
)
