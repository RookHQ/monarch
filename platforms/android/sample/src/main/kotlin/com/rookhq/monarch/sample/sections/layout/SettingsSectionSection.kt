package com.rookhq.monarch.sample.sections.layout

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import com.rookhq.monarch.components.actions.button.MonarchButton
import com.rookhq.monarch.components.actions.button.MonarchButtonSize
import com.rookhq.monarch.components.actions.button.MonarchButtonVariant
import com.rookhq.monarch.components.forms.formfield.MonarchFormField
import com.rookhq.monarch.components.forms.formfield.MonarchFormFieldLayout
import com.rookhq.monarch.components.forms.select.MonarchSelect
import com.rookhq.monarch.components.forms.select.MonarchSelectOption
import com.rookhq.monarch.components.forms.switchrow.MonarchSwitchRow
import com.rookhq.monarch.components.layout.sectioncard.MonarchSectionCard
import com.rookhq.monarch.components.layout.sectioncard.MonarchSectionCardLabels
import com.rookhq.monarch.components.layout.sectioncard.MonarchSectionCardState
import com.rookhq.monarch.foundation.MonarchTone
import com.rookhq.monarch.sample.CatalogueCaption
import com.rookhq.monarch.sample.CataloguePage
import com.rookhq.monarch.theme.MonarchTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val SimulatedSaveMillis = 800L

private val Languages = listOf(
    MonarchSelectOption(value = "en", label = "English"),
    MonarchSelectOption(value = "pt", label = "Português"),
)

/** The catalogue page of SettingsSection: a recipe of MonarchSectionCard, settings rows and a save footer. */
object SettingsSectionSection : CataloguePage {
    override val title = "SettingsSection"

    @Composable
    override fun Content() {
        val scope = rememberCoroutineScope()
        var savedDigest by rememberSaveable { mutableStateOf(true) }
        var savedLanguage by rememberSaveable { mutableStateOf("en") }
        var digest by rememberSaveable { mutableStateOf(true) }
        var language by rememberSaveable { mutableStateOf("en") }
        var saving by rememberSaveable { mutableStateOf(false) }
        val dirty = digest != savedDigest || language != savedLanguage
        CatalogueCaption("Change a setting to show the save footer")
        MonarchSectionCard(
            title = "Workspace",
            description = "These settings apply to everyone in the workspace.",
            busy = saving,
            footer = {
                FlowRow(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(MonarchTheme.spacing.space2, Alignment.End),
                    verticalArrangement = Arrangement.spacedBy(MonarchTheme.spacing.space2),
                    itemVerticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = if (dirty) "You have unsaved changes." else "",
                        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                        style = MonarchTheme.typography.caption,
                        color = MonarchTheme.colors.textMuted,
                    )
                    MonarchButton(
                        text = "Discard",
                        onClick = {
                            digest = savedDigest
                            language = savedLanguage
                        },
                        size = MonarchButtonSize.Sm,
                        enabled = dirty && !saving,
                    )
                    MonarchButton(
                        text = "Save changes",
                        onClick = {
                            saving = true
                            scope.launch {
                                delay(SimulatedSaveMillis)
                                savedDigest = digest
                                savedLanguage = language
                                saving = false
                            }
                        },
                        variant = MonarchButtonVariant.Primary,
                        size = MonarchButtonSize.Sm,
                        enabled = dirty,
                        loading = saving,
                    )
                }
            },
        ) {
            MonarchSwitchRow(
                checked = digest,
                onCheckedChange = { digest = it },
                label = "Weekly digest",
                description = "A summary of activity every Monday.",
            )
            MonarchFormField(label = "Language", layout = MonarchFormFieldLayout.Row) { controlModifier ->
                MonarchSelect(
                    options = Languages,
                    selected = language,
                    onSelect = { language = it },
                    modifier = controlModifier,
                )
            }
        }
        CatalogueCaption("Danger zone")
        MonarchSectionCard(title = "Danger zone", tone = MonarchTone.Danger) {
            ListItem(
                headlineContent = { Text("Delete workspace") },
                supportingContent = { Text("Removes every project and file. This cannot be undone.") },
                trailingContent = {
                    MonarchButton(
                        text = "Delete",
                        onClick = {},
                        variant = MonarchButtonVariant.Danger,
                        size = MonarchButtonSize.Sm,
                    )
                },
                colors = ListItemDefaults.colors(containerColor = MonarchTheme.colors.bgSurface),
            )
        }
        CatalogueCaption("Loading and no access")
        MonarchSectionCard(
            title = "Billing",
            state = MonarchSectionCardState.Loading,
            labels = MonarchSectionCardLabels(loading = "Loading settings"),
        ) {}
        MonarchSectionCard(
            title = "Billing",
            state = MonarchSectionCardState.Empty,
            emptyMessage = "Ask a workspace owner to change billing.",
            labels = MonarchSectionCardLabels(emptyTitle = "You don’t have access to these settings"),
        ) {}
    }
}
