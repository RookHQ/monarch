package dev.monarchui.sample.sections.collaboration

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import dev.monarchui.components.actions.button.MonarchButton
import dev.monarchui.components.collaboration.emojipicker.MonarchEmojiPicker
import dev.monarchui.components.collaboration.reactionbar.MonarchReactionBar
import dev.monarchui.components.collaboration.reactionbar.MonarchReactionBarItem
import dev.monarchui.components.overlays.bottomsheet.MonarchBottomSheet
import dev.monarchui.components.typography.text.MonarchText
import dev.monarchui.sample.CatalogueCaption
import dev.monarchui.sample.CataloguePage

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
