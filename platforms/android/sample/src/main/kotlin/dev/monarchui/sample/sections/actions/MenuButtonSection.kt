package dev.monarchui.sample.sections.actions

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.collapse
import androidx.compose.ui.semantics.expand
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import dev.monarchui.components.actions.button.MonarchButton
import dev.monarchui.components.actions.button.MonarchButtonVariant
import dev.monarchui.components.overlays.menu.MonarchMenu
import dev.monarchui.components.overlays.menu.MonarchMenuDivider
import dev.monarchui.components.overlays.menu.MonarchMenuItem
import dev.monarchui.components.typography.text.MonarchText
import dev.monarchui.foundation.icon.MonarchIcon
import dev.monarchui.foundation.icon.MonarchIconSize
import dev.monarchui.sample.CatalogueCaption
import dev.monarchui.sample.CataloguePage
import dev.monarchui.sampleicons.SampleIcons
import dev.monarchui.theme.MonarchTheme

/** The catalogue page of MenuButton: a MonarchButton that opens a MonarchMenu. */
object MenuButtonSection : CataloguePage {
    override val title = "MenuButton"

    @Composable
    override fun Content() {
        var expanded by rememberSaveable { mutableStateOf(false) }
        var last by rememberSaveable { mutableStateOf("Nothing yet") }
        CatalogueCaption("A button with a chevron that opens a menu")
        Box {
            MonarchButton(
                text = "Actions",
                onClick = { expanded = true },
                modifier = Modifier.semantics {
                    stateDescription = if (expanded) "Expanded" else "Collapsed"
                    if (expanded) {
                        collapse {
                            expanded = false
                            true
                        }
                    } else {
                        expand {
                            expanded = true
                            true
                        }
                    }
                },
                variant = MonarchButtonVariant.Secondary,
                trailingIcon = {
                    MonarchIcon(SampleIcons.ChevronDown, null, size = MonarchIconSize.Sm)
                },
            )
            MonarchMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                listOf("Rename", "Duplicate", "Move to folder").forEach { action ->
                    MonarchMenuItem(
                        text = action,
                        onClick = {
                            last = action
                            expanded = false
                        },
                    )
                }
                MonarchMenuDivider()
                MonarchMenuItem(
                    text = "Delete",
                    onClick = {
                        last = "Delete"
                        expanded = false
                    },
                    destructive = true,
                )
            }
        }
        MonarchText(text = "Last action: $last", color = MonarchTheme.colors.textMuted)
        CatalogueCaption("Disabled: the menu does not open")
        MonarchButton(text = "Actions", onClick = {}, enabled = false)
    }
}
