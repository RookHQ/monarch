package com.rookhq.monarch.sample.sections.layout

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import com.rookhq.monarch.components.actions.button.MonarchButton
import com.rookhq.monarch.components.actions.button.MonarchButtonVariant
import com.rookhq.monarch.components.forms.formactionsbar.MonarchFormActionsBar
import com.rookhq.monarch.components.forms.switchrow.MonarchSwitchRow
import com.rookhq.monarch.components.layout.sectioncard.MonarchSectionCard
import com.rookhq.monarch.components.overlays.unsavedchangesguard.MonarchUnsavedChangesGuard
import com.rookhq.monarch.components.typography.text.MonarchText
import com.rookhq.monarch.foundation.MonarchTextStyle
import com.rookhq.monarch.foundation.MonarchTone
import com.rookhq.monarch.sample.CatalogueCaption
import com.rookhq.monarch.sample.CataloguePage
import com.rookhq.monarch.theme.MonarchTheme

/** The catalogue page of SettingsPage: a recipe of existing components. */
object SettingsPageSection : CataloguePage {
    override val title = "SettingsPage"

    @Composable
    override fun Content() {
        CatalogueCaption("Sections that open their own screens")
        var section by rememberSaveable { mutableStateOf<String?>(null) }
        var savedDigest by rememberSaveable { mutableStateOf(true) }
        var digest by rememberSaveable { mutableStateOf(true) }
        var asking by remember { mutableStateOf(false) }
        val dirty = digest != savedDigest
        when (section) {
            null -> Column {
                MonarchText(text = "Workspace settings", style = MonarchTextStyle.Title)
                ListItem(
                    headlineContent = { Text("Notifications") },
                    supportingContent = { Text("Email digests and alerts") },
                    modifier = Modifier.clickable(role = Role.Button) { section = "notifications" },
                    colors = ListItemDefaults.colors(containerColor = MonarchTheme.colors.bgSurface),
                )
                HorizontalDivider(color = MonarchTheme.colors.borderSubtle)
                ListItem(
                    headlineContent = { Text("Danger zone") },
                    supportingContent = { Text("Delete this workspace") },
                    modifier = Modifier.clickable(role = Role.Button) { section = "danger" },
                    colors = ListItemDefaults.colors(
                        containerColor = MonarchTheme.colors.bgSurface,
                        headlineColor = MonarchTheme.colors.dangerStrong,
                    ),
                )
            }
            "notifications" -> Column(verticalArrangement = Arrangement.spacedBy(MonarchTheme.spacing.space3)) {
                // Back returns to the list; with unsaved changes the guard asks first.
                BackHandler(enabled = !dirty) { section = null }
                MonarchButton(
                    text = "Settings",
                    onClick = { if (dirty) asking = true else section = null },
                    variant = MonarchButtonVariant.Ghost,
                )
                MonarchSectionCard(title = "Notifications", description = "These apply to everyone in the workspace.") {
                    MonarchSwitchRow(
                        checked = digest,
                        onCheckedChange = { digest = it },
                        label = "Weekly digest",
                        description = "A summary of activity every Monday.",
                    )
                }
                MonarchFormActionsBar(
                    dirty = dirty,
                    onSave = { savedDigest = digest /* The app saves on its server here. */ },
                    onDiscard = { digest = savedDigest },
                )
                MonarchUnsavedChangesGuard(
                    hasChanges = dirty,
                    open = asking,
                    onOpenChange = { asking = it },
                    onLeave = { section = null },
                    onSave = { savedDigest = digest },
                )
            }
            else -> Column(verticalArrangement = Arrangement.spacedBy(MonarchTheme.spacing.space3)) {
                BackHandler { section = null }
                MonarchButton(text = "Settings", onClick = { section = null }, variant = MonarchButtonVariant.Ghost)
                MonarchSectionCard(title = "Danger zone", tone = MonarchTone.Danger) {
                    MonarchText(text = "Deletes every project, file and member. This cannot be undone.")
                    MonarchButton(
                    text = "Delete workspace",
                    onClick = { /* Open the confirm dialog of the danger-zone recipe. */ },
                    variant = MonarchButtonVariant.Danger,
                )
                }
            }
        }
    }
}
