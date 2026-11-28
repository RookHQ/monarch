package dev.monarchui.sample.sections.overlays

import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import dev.monarchui.components.actions.button.MonarchButton
import dev.monarchui.components.actions.button.MonarchButtonSize
import dev.monarchui.components.actions.button.MonarchButtonVariant
import dev.monarchui.components.overlays.menu.MonarchMenu
import dev.monarchui.components.overlays.menu.MonarchMenuItem
import dev.monarchui.components.typography.text.MonarchText
import dev.monarchui.foundation.MonarchTextStyle
import dev.monarchui.sample.CatalogueCaption
import dev.monarchui.sample.CataloguePage
import dev.monarchui.theme.MonarchTheme

/** The catalogue page of ContextMenu: a MonarchMenu opened by a long press, and by a visible More button. */
object ContextMenuSection : CataloguePage {
    override val title = "ContextMenu"

    @Composable
    override fun Content() {
        var menu by rememberSaveable { mutableStateOf(false) }
        var last by rememberSaveable { mutableStateOf("Touch and hold the row") }
        CatalogueCaption("Long press, with a visible way too")
        Box {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .combinedClickable(
                        onClick = { last = "Opened INV-1042" },
                        onLongClickLabel = "Show actions",
                        onLongClick = { menu = true },
                    )
                    .semantics {
                        customActions = listOf(
                            CustomAccessibilityAction("Copy link") {
                                last = "Copied the link"
                                true
                            },
                            CustomAccessibilityAction("Delete") {
                                last = "Deleted"
                                true
                            },
                        )
                    }
                    .padding(MonarchTheme.spacing.space3),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                MonarchText(text = "Invoice INV-1042", modifier = Modifier.weight(1f))
                MonarchButton(
                    text = "More",
                    onClick = { menu = true },
                    variant = MonarchButtonVariant.Ghost,
                    size = MonarchButtonSize.Sm,
                )
            }
            MonarchMenu(expanded = menu, onDismissRequest = { menu = false }) {
                MonarchMenuItem(
                    text = "Copy link",
                    onClick = {
                        last = "Copied the link"
                        menu = false
                    },
                )
                MonarchMenuItem(
                    text = "Delete",
                    onClick = {
                        last = "Deleted"
                        menu = false
                    },
                    destructive = true,
                )
            }
        }
        MonarchText(text = last, style = MonarchTextStyle.Caption, color = MonarchTheme.colors.textMuted)
    }
}
