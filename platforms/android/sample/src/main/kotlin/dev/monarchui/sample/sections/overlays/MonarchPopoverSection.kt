package dev.monarchui.sample.sections.overlays

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import dev.monarchui.components.actions.button.MonarchButton
import dev.monarchui.components.actions.button.MonarchButtonVariant
import dev.monarchui.components.forms.checkboxrow.MonarchCheckboxRow
import dev.monarchui.components.overlays.popover.MonarchPopover
import dev.monarchui.components.overlays.popover.MonarchPopoverSide
import dev.monarchui.components.typography.text.MonarchText
import dev.monarchui.foundation.MonarchTextStyle
import dev.monarchui.sample.CatalogueCaption
import dev.monarchui.sample.CataloguePage

/** The catalogue page of Popover. */
object MonarchPopoverSection : CataloguePage {
    override val title = "Popover"

    @Composable
    override fun Content() {
        var filterOpen by rememberSaveable { mutableStateOf(false) }
        var onlyUnpaid by rememberSaveable { mutableStateOf(true) }
        var helpOpen by rememberSaveable { mutableStateOf(false) }
        CatalogueCaption("A filter panel below its button")
        Box {
            MonarchButton(text = if (onlyUnpaid) "Filter: unpaid" else "Filter", onClick = { filterOpen = true })
            MonarchPopover(
                expanded = filterOpen,
                onDismissRequest = { filterOpen = false },
                label = "Filter invoices",
            ) {
                MonarchText(text = "Filter invoices", style = MonarchTextStyle.Subheading, heading = true)
                MonarchCheckboxRow(checked = onlyUnpaid, onCheckedChange = { onlyUnpaid = it }, label = "Only unpaid")
                MonarchButton(text = "Done", onClick = { filterOpen = false }, variant = MonarchButtonVariant.Primary)
            }
        }
        CatalogueCaption("An explanation above its button")
        Box {
            MonarchButton(text = "Why this price?", onClick = { helpOpen = true }, variant = MonarchButtonVariant.Ghost)
            MonarchPopover(
                expanded = helpOpen,
                onDismissRequest = { helpOpen = false },
                label = "Why this price",
                side = MonarchPopoverSide.Top,
            ) {
                MonarchText(text = "Why this price", style = MonarchTextStyle.Subheading, heading = true)
                MonarchText(text = "Seats added this month are charged from the day they were added.")
            }
        }
    }
}
