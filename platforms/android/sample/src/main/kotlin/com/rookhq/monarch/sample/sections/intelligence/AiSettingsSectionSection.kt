@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class,
    androidx.compose.foundation.layout.ExperimentalLayoutApi::class,
)

package com.rookhq.monarch.sample.sections.intelligence

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import com.rookhq.monarch.components.forms.select.MonarchSelect
import com.rookhq.monarch.components.forms.select.MonarchSelectOption
import com.rookhq.monarch.components.forms.switchrow.MonarchSwitchRow
import com.rookhq.monarch.components.layout.sectioncard.MonarchSectionCard
import com.rookhq.monarch.components.technical.secretfield.MonarchSecretField
import com.rookhq.monarch.components.typography.text.MonarchText
import com.rookhq.monarch.foundation.MonarchTextStyle
import com.rookhq.monarch.sample.CatalogueCaption
import com.rookhq.monarch.sample.CataloguePage
import com.rookhq.monarch.theme.MonarchTheme

/** The catalogue page of AiSettingsSection: a recipe of existing components. */
object AiSettingsSectionSection : CataloguePage {
    override val title = "AiSettingsSection"

    @Composable
    override fun Content() {
        CatalogueCaption("Workspace AI settings with a save footer")
        run {
            var savedApps by remember { mutableStateOf(setOf("crm")) }
            var apps by remember { mutableStateOf(setOf("crm")) }
            var savedSharing by remember { mutableStateOf(false) }
            var sharing by remember { mutableStateOf(false) }
            var savedRetention by remember { mutableStateOf("90d") }
            var retention by remember { mutableStateOf("90d") }
            var savedProvider by remember { mutableStateOf("") }
            var provider by remember { mutableStateOf("") }
            var apiKey by remember { mutableStateOf("") }
            val dirty = apps != savedApps || sharing != savedSharing || retention != savedRetention ||
                provider != savedProvider || apiKey.isNotEmpty()
            MonarchSectionCard(
                title = "AI",
                description = "Choose where AI is available in this workspace and how it handles your data.",
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
                                apps = savedApps
                                sharing = savedSharing
                                retention = savedRetention
                                provider = savedProvider
                                apiKey = ""
                            },
                            size = MonarchButtonSize.Sm,
                            enabled = dirty,
                        )
                        MonarchButton(
                            text = "Save changes",
                            onClick = {
                                // The app saves the draft here; a new key is sent only when one was typed.
                                savedApps = apps
                                savedSharing = sharing
                                savedRetention = retention
                                savedProvider = provider
                                apiKey = ""
                            },
                            variant = MonarchButtonVariant.Primary,
                            size = MonarchButtonSize.Sm,
                            enabled = dirty,
                        )
                    }
                },
            ) {
                MonarchText(text = "Use AI in", style = MonarchTextStyle.Subheading, heading = true)
                listOf("crm" to "CRM", "desk" to "Help desk").forEach { (id, label) ->
                    MonarchSwitchRow(
                        checked = id in apps,
                        onCheckedChange = { on -> apps = if (on) apps + id else apps - id },
                        label = label,
                    )
                }
                MonarchSwitchRow(
                    checked = sharing,
                    onCheckedChange = { sharing = it },
                    label = "Share data to improve AI",
                    description = "Off keeps prompts and responses private to this workspace.",
                )
                MonarchFormField(
                    label = "Conversation history",
                    helperText = "How long AI conversations are kept before they are deleted.",
                ) { controlModifier ->
                    MonarchSelect(
                        options = listOf(
                            MonarchSelectOption("30d", "30 days"),
                            MonarchSelectOption("90d", "90 days"),
                            MonarchSelectOption("forever", "Until deleted"),
                        ),
                        selected = retention,
                        onSelect = { retention = it },
                        modifier = controlModifier,
                    )
                }
                MonarchFormField(
                    label = "AI provider",
                    helperText = "Use the built-in provider or bring your own key.",
                ) { controlModifier ->
                    MonarchSelect(
                        options = listOf(
                            MonarchSelectOption("", "Built-in"),
                            MonarchSelectOption("example", "Example AI"),
                        ),
                        selected = provider,
                        onSelect = {
                            provider = it
                            apiKey = ""
                        },
                        modifier = controlModifier,
                    )
                }
                if (provider.isNotEmpty()) {
                    MonarchSecretField(
                        value = apiKey,
                        label = "Example AI API key",
                        onValueChange = { apiKey = it },
                        helperText = "Stored encrypted. Leave blank to keep the saved key.",
                        saved = provider == savedProvider,
                    )
                }
            }
        }
        CatalogueCaption("Read only")
        run {
            MonarchSectionCard(
                title = "AI",
                description = "Only workspace admins can change these settings.",
            ) {
                MonarchSwitchRow(checked = true, onCheckedChange = {}, label = "CRM", enabled = false)
                MonarchSwitchRow(
                    checked = false,
                    onCheckedChange = {},
                    label = "Share data to improve AI",
                    enabled = false,
                )
            }
        }
    }
}
