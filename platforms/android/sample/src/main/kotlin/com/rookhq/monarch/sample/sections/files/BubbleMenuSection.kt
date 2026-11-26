package com.rookhq.monarch.sample.sections.files

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.contextmenu.builder.item
import androidx.compose.foundation.text.contextmenu.modifier.appendTextContextMenuComponents
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import com.rookhq.monarch.components.forms.textfield.MonarchTextField
import com.rookhq.monarch.components.typography.text.MonarchText
import com.rookhq.monarch.sample.CatalogueCaption
import com.rookhq.monarch.sample.CataloguePage

/** The catalogue page of BubbleMenu: actions added to the system text selection menu. */
object BubbleMenuSection : CataloguePage {
    override val title = "BubbleMenu"

    @Composable
    override fun Content() {
        var note by remember {
            val text = "Ship the release on Friday"
            mutableStateOf(TextFieldValue(text, TextRange(text.indexOf("the"), text.indexOf(" on"))))
        }
        var comment by rememberSaveable { mutableStateOf<String?>(null) }
        CatalogueCaption("Select text, then choose Comment in the selection menu")
        MonarchTextField(
            value = note,
            onValueChange = { note = it },
            modifier = Modifier
                .fillMaxWidth()
                .appendTextContextMenuComponents {
                    if (!note.selection.collapsed) {
                        separator()
                        item(key = "comment", label = "Comment") {
                            comment = note.text.substring(note.selection.min, note.selection.max)
                            close()
                        }
                    }
                },
            label = "Note",
            singleLine = false,
        )
        MonarchText(text = comment?.let { "Comment on “$it”" } ?: "No comment yet")
    }
}
