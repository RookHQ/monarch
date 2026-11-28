package dev.monarchui.sample.sections.overlays

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import dev.monarchui.components.actions.button.MonarchButton
import dev.monarchui.components.datadisplay.listrow.MonarchListRow
import dev.monarchui.components.overlays.bottomsheet.MonarchBottomSheet
import dev.monarchui.components.typography.text.MonarchText
import dev.monarchui.sample.CatalogueCaption
import dev.monarchui.sample.CataloguePage

/** The catalogue page of Drawer: MonarchBottomSheet with its grab handle and full-width list rows. */
object DrawerSection : CataloguePage {
    override val title = "Drawer"

    private val options = listOf("Share", "Duplicate", "Move to folder", "Archive")

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        var open by rememberSaveable { mutableStateOf(false) }
        var picked by rememberSaveable { mutableStateOf<String?>(null) }
        CatalogueCaption("A bottom drawer with a grab handle")
        MonarchButton(text = "Options", onClick = { open = true })
        picked?.let { MonarchText(text = "Picked: $it") }
        if (open) {
            MonarchBottomSheet(onDismissRequest = { open = false }, fullWidthContent = true) {
                Column {
                    options.forEach { option ->
                        MonarchListRow(
                            headlineContent = { MonarchText(option) },
                            onClick = {
                                picked = option
                                this@MonarchBottomSheet.dismiss()
                            },
                        )
                    }
                }
            }
        }
    }
}
