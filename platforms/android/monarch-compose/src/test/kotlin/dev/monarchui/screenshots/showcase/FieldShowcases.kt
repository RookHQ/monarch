package dev.monarchui.screenshots.showcase

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import dev.monarchui.components.forms.combobox.MonarchCombobox
import dev.monarchui.components.forms.combobox.MonarchComboboxOption
import dev.monarchui.components.forms.formfield.MonarchFormField
import dev.monarchui.components.forms.formfield.MonarchFormFieldLayout
import dev.monarchui.components.forms.numberinput.MonarchNumberInput
import dev.monarchui.components.forms.passwordinput.MonarchPasswordInput
import dev.monarchui.components.forms.passwordinput.MonarchPasswordInputPurpose
import dev.monarchui.components.forms.select.MonarchSelect
import dev.monarchui.components.forms.select.MonarchSelectOption
import dev.monarchui.components.forms.switch.MonarchSwitch
import dev.monarchui.components.forms.textfield.MonarchTextField
import dev.monarchui.components.forms.textfield.MonarchTextFieldLabelVisibility
import dev.monarchui.components.search.searchbar.MonarchSearchBar
import dev.monarchui.foundation.icon.MonarchIcon
import dev.monarchui.sampleicons.SampleIcons

internal val fieldShowcases: Map<String, @Composable () -> Unit> = mapOf(
    "text-field" to {
        var name by remember { mutableStateOf("Sales team signature") }
        var email by remember { mutableStateOf("priya@example") }
        var notes by remember { mutableStateOf("") }
        ShowcaseColumn {
            MonarchTextField(
                value = name,
                onValueChange = { name = it },
                modifier = Modifier.fillMaxWidth(),
                label = "Template name",
                required = true,
                helperText = "Visible to your organization.",
            )
            MonarchTextField(
                value = email,
                onValueChange = { email = it },
                modifier = Modifier.fillMaxWidth(),
                label = "Work email",
                leadingIcon = { MonarchIcon(SampleIcons.Mail, contentDescription = null) },
                errorText = "Enter a valid email address.",
            )
            MonarchTextField(
                value = notes,
                onValueChange = { notes = it },
                modifier = Modifier.fillMaxWidth(),
                label = "Description",
                placeholder = "What is this template for?",
                singleLine = false,
                minLines = 3,
            )
            MonarchTextField(
                value = "",
                onValueChange = {},
                modifier = Modifier.fillMaxWidth(),
                label = "Domain",
                placeholder = "help.example.com",
                enabled = false,
            )
        }
    },
    "number-input" to {
        var seats by remember { mutableStateOf<Double?>(12.0) }
        var tooMany by remember { mutableStateOf<Double?>(80.0) }
        ShowcaseColumn {
            MonarchNumberInput(
                value = seats,
                onValueChange = { seats = it },
                modifier = Modifier.fillMaxWidth(),
                label = "Seats",
                helperText = "Billed per seat each month.",
                minimum = 1.0,
                maximum = 50.0,
            )
            MonarchNumberInput(
                value = tooMany,
                onValueChange = { tooMany = it },
                modifier = Modifier.fillMaxWidth(),
                label = "Seats on the new plan",
                minimum = 1.0,
                maximum = 50.0,
                errorText = "Enter a number from 1 to 50.",
            )
        }
    },
    "select" to {
        val regions = listOf(
            MonarchSelectOption("us", "United States"),
            MonarchSelectOption("de", "Germany"),
            MonarchSelectOption("si", "Slovenia"),
        )
        ShowcaseColumn {
            MonarchSelect(
                options = regions,
                modifier = Modifier.fillMaxWidth(),
                selected = "de",
                onSelect = {},
                label = "Data region",
                helperText = "Where your files are stored.",
            )
            MonarchSelect(
                options = regions,
                modifier = Modifier.fillMaxWidth(),
                selected = null,
                onSelect = {},
                label = "Billing country",
                placeholder = "Choose a country",
                errorText = "Choose a country to continue.",
            )
        }
    },
    "search-input" to {
        var query by remember { mutableStateOf("invoice 1042") }
        ShowcaseColumn {
            MonarchSearchBar(
                query = query,
                onQueryChange = { query = it },
                onSearch = {},
                placeholder = "Search invoices",
                onClear = { query = "" },
            )
            MonarchSearchBar(
                query = "",
                onQueryChange = {},
                onSearch = {},
                placeholder = "Search customers",
            )
        }
    },
    "combobox" to {
        val people = listOf(
            MonarchComboboxOption("ada", "Ada Lovelace", hint = "ada@example.com"),
            MonarchComboboxOption("grace", "Grace Hopper", hint = "grace@example.com"),
            MonarchComboboxOption("katherine", "Katherine Johnson", hint = "katherine@example.com"),
        )
        var assignee by remember { mutableStateOf<String?>("ada") }
        ShowcaseColumn {
            MonarchCombobox(
                label = "Assignee",
                options = people,
                selected = assignee,
                onSelectedChange = { assignee = it },
                helperText = "They get an email when the task changes.",
            )
            MonarchCombobox(
                label = "Reviewer",
                options = people,
                selected = null,
                onSelectedChange = {},
                placeholder = "Choose a reviewer",
            )
        }
    },
    "password-input" to {
        var password by remember { mutableStateOf("harbor-lantern-42") }
        ShowcaseColumn {
            MonarchPasswordInput(
                value = password,
                onValueChange = { password = it },
                label = "New password",
                helperText = "At least 12 characters.",
                purpose = MonarchPasswordInputPurpose.New,
            )
        }
    },
    "form-field" to {
        var name by remember { mutableStateOf("") }
        var sync by remember { mutableStateOf(true) }
        ShowcaseColumn {
            MonarchFormField(
                label = "Project name",
                required = true,
                helperText = "Shown on invoices.",
                errorText = "Enter a name for the project.",
            ) { controlModifier ->
                MonarchTextField(
                    value = name,
                    onValueChange = { name = it },
                    modifier = controlModifier,
                    placeholder = "Acme renewal",
                    labelVisibility = MonarchTextFieldLabelVisibility.Hidden,
                )
            }
            MonarchFormField(
                label = "Sync with calendar",
                layout = MonarchFormFieldLayout.Row,
                helperText = "Adds due dates to your calendar.",
            ) { controlModifier ->
                MonarchSwitch(checked = sync, onCheckedChange = { sync = it }, modifier = controlModifier)
            }
        }
    },
)
