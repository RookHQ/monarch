package com.rookhq.monarch.sample.sections.collaboration

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.rookhq.monarch.components.actions.button.MonarchButton
import com.rookhq.monarch.components.collaboration.emojipicker.MonarchEmojiPicker
import com.rookhq.monarch.components.collaboration.reactionbar.MonarchReactionBar
import com.rookhq.monarch.components.collaboration.reactionbar.MonarchReactionBarItem
import com.rookhq.monarch.components.overlays.bottomsheet.MonarchBottomSheet
import com.rookhq.monarch.components.typography.text.MonarchText
import com.rookhq.monarch.sample.CatalogueCaption
import com.rookhq.monarch.sample.CataloguePage

/** The catalogue page of EmojiPicker: the system emoji picker, inline, in a sheet and behind a reaction bar. */
object EmojiPickerSection : CataloguePage {
    override val title = "EmojiPicker"

    private const val PickerColumns = 8
    private const val PickerRows = 4.5f

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        var picked by rememberSaveable { mutableStateOf<String?>(null) }
        var sheet by rememberSaveable { mutableStateOf(false) }
        var reactions by rememberSaveable { mutableStateOf(mapOf("👍" to 2)) }
        CatalogueCaption("Inline")
        MonarchText(text = picked?.let { "Picked $it" } ?: "Nothing picked yet")
        MonarchEmojiPicker(onEmojiPicked = { picked = it }, columns = PickerColumns, rows = PickerRows)
        CatalogueCaption("In a sheet")
        MonarchButton(text = "Choose an emoji", onClick = { sheet = true })
        if (sheet) {
            MonarchBottomSheet(onDismissRequest = { sheet = false }) {
                MonarchEmojiPicker(
                    onEmojiPicked = {
                        picked = it
                        sheet = false
                    },
                )
            }
        }
        CatalogueCaption("Behind a reaction bar's add button")
        MonarchReactionBar(
            reactions = reactions.map { (emoji, count) -> MonarchReactionBarItem(emoji, count = count) },
            onToggle = { emoji, on ->
                val count = (reactions[emoji] ?: 0) + if (on) 1 else -1
                reactions = reactions + (emoji to count)
            },
            picker = { onPick, onDismiss ->
                MonarchBottomSheet(onDismissRequest = onDismiss) { MonarchEmojiPicker(onEmojiPicked = onPick) }
            },
        )
    }
}
