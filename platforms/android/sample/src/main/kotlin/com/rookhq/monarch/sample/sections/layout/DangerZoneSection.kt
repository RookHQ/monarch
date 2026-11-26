@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class,
    androidx.compose.foundation.layout.ExperimentalLayoutApi::class,
)

package com.rookhq.monarch.sample.sections.layout

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.semantics
import com.rookhq.monarch.components.actions.button.MonarchButton
import com.rookhq.monarch.components.actions.button.MonarchButtonSize
import com.rookhq.monarch.components.actions.button.MonarchButtonVariant
import com.rookhq.monarch.components.layout.sectioncard.MonarchSectionCard
import com.rookhq.monarch.components.overlays.dialog.MonarchDialog
import com.rookhq.monarch.components.typography.text.MonarchText
import com.rookhq.monarch.foundation.MonarchTextStyle
import com.rookhq.monarch.foundation.MonarchTone
import com.rookhq.monarch.sample.CatalogueCaption
import com.rookhq.monarch.sample.CataloguePage
import com.rookhq.monarch.theme.MonarchTheme

/** The catalogue page of DangerZone: a recipe of existing components. */
object DangerZoneSection : CataloguePage {
    override val title = "DangerZone"

    @Composable
    override fun Content() {
        CatalogueCaption("A danger zone with a confirmation")
        run {
            var confirming by remember { mutableStateOf(false) }
            var deleted by remember { mutableStateOf(false) }
            MonarchSectionCard(
                title = "Danger zone",
                description = "Only workspace owners can use these actions.",
                tone = MonarchTone.Danger,
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(MonarchTheme.spacing.space3)) {
                    Column(
                        modifier = Modifier.semantics { isTraversalGroup = true },
                        verticalArrangement = Arrangement.spacedBy(MonarchTheme.spacing.space1),
                    ) {
                        MonarchText(text = "Transfer ownership", style = MonarchTextStyle.BodyStrong, heading = true)
                        MonarchText(
                            text = "Make another member the owner of this workspace.",
                            style = MonarchTextStyle.Caption,
                            color = MonarchTheme.colors.textMuted,
                        )
                        MonarchText(
                            text = "You are the only member, so there is no one to transfer to.",
                            style = MonarchTextStyle.Caption,
                            color = MonarchTheme.colors.textMuted,
                        )
                        MonarchButton(
                            text = "Transfer ownership",
                            onClick = {},
                            variant = MonarchButtonVariant.Danger,
                            size = MonarchButtonSize.Sm,
                            enabled = false,
                        )
                    }
                    HorizontalDivider(color = MonarchTheme.colors.borderSubtle)
                    Column(
                        modifier = Modifier.semantics { isTraversalGroup = true },
                        verticalArrangement = Arrangement.spacedBy(MonarchTheme.spacing.space1),
                    ) {
                        MonarchText(text = "Delete this workspace", style = MonarchTextStyle.BodyStrong, heading = true)
                        MonarchText(
                            text = "Deletes every project, file and member. This cannot be undone.",
                            style = MonarchTextStyle.Caption,
                            color = MonarchTheme.colors.textMuted,
                        )
                        MonarchButton(
                            text = if (deleted) "Workspace deleted" else "Delete workspace",
                            onClick = { confirming = true },
                            variant = MonarchButtonVariant.Danger,
                            size = MonarchButtonSize.Sm,
                            enabled = !deleted,
                        )
                    }
                }
            }
            if (confirming) {
                MonarchDialog(
                    onDismissRequest = { confirming = false },
                    title = "Delete this workspace?",
                    description = "Every project, file and member is deleted. This cannot be undone.",
                    actions = {
                        MonarchButton(
                            text = "Cancel",
                            onClick = { confirming = false },
                            variant = MonarchButtonVariant.Ghost,
                        )
                        MonarchButton(
                            text = "Delete",
                            onClick = {
                                deleted = true
                                confirming = false
                            },
                            variant = MonarchButtonVariant.Danger,
                        )
                    },
                )
            }
        }
    }
}
