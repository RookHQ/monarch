package dev.monarchui.sample.sections.datagrid

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import dev.monarchui.components.actions.button.MonarchButton
import dev.monarchui.components.actions.button.MonarchButtonVariant
import dev.monarchui.components.overlays.dialog.MonarchDialog
import dev.monarchui.components.overlays.menu.MonarchMenu
import dev.monarchui.components.overlays.menu.MonarchMenuDivider
import dev.monarchui.components.overlays.menu.MonarchMenuItem
import dev.monarchui.components.typography.text.MonarchText
import dev.monarchui.sample.CatalogueCaption
import dev.monarchui.sample.CataloguePage
import dev.monarchui.sampleicons.SampleIcons
import dev.monarchui.theme.MonarchTheme

/** The catalogue page of RowActionsMenu: a more menu with a confirmed delete, and rows that swipe to archive. */
object RowActionsMenuSection : CataloguePage {
    override val title = "RowActionsMenu"

    private val all = listOf("Acme Ltd", "Globex", "Initech", "Umbrella")

    @Composable
    override fun Content() {
        var companies by rememberSaveable { mutableStateOf(all) }
        var confirming by rememberSaveable { mutableStateOf<String?>(null) }
        CatalogueCaption("More menu on each row; swipe from the end to archive")
        companies.forEach { company ->
            key(company) {
                CompanyRow(
                    company = company,
                    onArchive = { companies = companies - company },
                    onDelete = { confirming = company },
                )
            }
        }
        if (companies.size < all.size) {
            MonarchButton(text = "Restore all", onClick = { companies = all }, variant = MonarchButtonVariant.Ghost)
        }
        confirming?.let { company ->
            MonarchDialog(
                onDismissRequest = { confirming = null },
                title = "Delete $company?",
                description = "Its contacts and notes are deleted with it.",
                actions = {
                    MonarchButton(
                        text = "Cancel",
                        onClick = { confirming = null },
                        variant = MonarchButtonVariant.Ghost,
                    )
                    MonarchButton(
                        text = "Delete",
                        onClick = {
                            companies = companies - company
                            confirming = null
                        },
                        variant = MonarchButtonVariant.Danger,
                    )
                },
            )
        }
    }

    @Composable
    private fun CompanyRow(company: String, onArchive: () -> Unit, onDelete: () -> Unit) {
        var expanded by remember { mutableStateOf(false) }
        SwipeToDismissBox(
            state = rememberSwipeToDismissBoxState(),
            backgroundContent = {
                Box(
                    modifier = Modifier.fillMaxSize().background(MonarchTheme.colors.warningSubtle),
                    contentAlignment = Alignment.CenterEnd,
                ) {
                    MonarchText(
                        text = "Archive",
                        modifier = Modifier.padding(MonarchTheme.spacing.space4),
                        color = MonarchTheme.colors.warningStrong,
                    )
                }
            },
            enableDismissFromStartToEnd = false,
            onDismiss = { onArchive() },
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MonarchTheme.colors.bgSurface)
                    .padding(start = MonarchTheme.spacing.space4)
                    .semantics {
                        customActions = listOf(
                            CustomAccessibilityAction("Archive") {
                                onArchive()
                                true
                            },
                        )
                    },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                MonarchText(text = company, modifier = Modifier.weight(1f))
                Box {
                    IconButton(onClick = { expanded = true }) {
                        Icon(
                            SampleIcons.More,
                            contentDescription = "Actions for $company",
                        )
                    }
                    MonarchMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        MonarchMenuItem(text = "Edit", onClick = { expanded = false })
                        MonarchMenuItem(
                            text = "Archive",
                            onClick = {
                                expanded = false
                                onArchive()
                            },
                        )
                        MonarchMenuDivider()
                        MonarchMenuItem(
                            text = "Delete",
                            onClick = {
                                expanded = false
                                onDelete()
                            },
                            destructive = true,
                        )
                    }
                }
            }
        }
    }
}
