package dev.monarchui.sample.sections.overlays

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import dev.monarchui.components.actions.button.MonarchButton
import dev.monarchui.components.actions.button.MonarchButtonSize
import dev.monarchui.components.actions.button.MonarchButtonVariant
import dev.monarchui.components.overlays.popover.MonarchPopover
import dev.monarchui.components.typography.text.MonarchText
import dev.monarchui.foundation.MonarchTextStyle
import dev.monarchui.sample.CatalogueCaption
import dev.monarchui.sample.CataloguePage
import dev.monarchui.theme.MonarchTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val SimulatedRemoveMillis = 800L

/** The catalogue page of Popconfirm, a recipe: a small confirmation in MonarchPopover anchored to its button. */
object PopconfirmSection : CataloguePage {
    override val title = "Popconfirm"

    @Composable
    override fun Content() {
        var open by rememberSaveable { mutableStateOf(false) }
        var removing by rememberSaveable { mutableStateOf(false) }
        var removed by rememberSaveable { mutableStateOf(false) }
        val scope = rememberCoroutineScope()
        CatalogueCaption("Remove a filter")
        if (removed) {
            MonarchText(text = "Filter removed.", style = MonarchTextStyle.Caption)
            MonarchButton(text = "Add it back", onClick = { removed = false }, variant = MonarchButtonVariant.Ghost)
        } else {
            Box {
                MonarchButton(text = "Status: open", onClick = { open = true }, size = MonarchButtonSize.Sm)
                MonarchPopover(
                    expanded = open,
                    onDismissRequest = { if (!removing) open = false },
                    label = "Remove this filter?",
                ) {
                    MonarchText(text = "Remove this filter?", style = MonarchTextStyle.Subheading, heading = true)
                    MonarchText(text = "The list shows every status again.", color = MonarchTheme.colors.textMuted)
                    Row(horizontalArrangement = Arrangement.spacedBy(MonarchTheme.spacing.space2)) {
                        MonarchButton(
                            text = "Cancel",
                            onClick = { open = false },
                            variant = MonarchButtonVariant.Ghost,
                            size = MonarchButtonSize.Sm,
                            enabled = !removing,
                        )
                        MonarchButton(
                            text = "Remove",
                            onClick = {
                                removing = true
                                scope.launch {
                                    delay(SimulatedRemoveMillis)
                                    removing = false
                                    open = false
                                    removed = true
                                }
                            },
                            variant = MonarchButtonVariant.Danger,
                            size = MonarchButtonSize.Sm,
                            loading = removing,
                        )
                    }
                }
            }
        }
    }
}
