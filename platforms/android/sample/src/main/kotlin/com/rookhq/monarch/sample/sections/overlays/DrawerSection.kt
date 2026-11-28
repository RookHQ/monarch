package com.rookhq.monarch.sample.sections.overlays

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.rookhq.monarch.components.actions.button.MonarchButton
import com.rookhq.monarch.components.datadisplay.listrow.MonarchListRow
import com.rookhq.monarch.components.overlays.bottomsheet.MonarchBottomSheet
import com.rookhq.monarch.components.typography.text.MonarchText
import com.rookhq.monarch.sample.CatalogueCaption
import com.rookhq.monarch.sample.CataloguePage

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
