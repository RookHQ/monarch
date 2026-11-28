package dev.monarchui.sample.sections.collaboration

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import dev.monarchui.components.collaboration.reactionbar.MonarchReactionBar
import dev.monarchui.components.collaboration.reactionbar.MonarchReactionBarItem
import dev.monarchui.components.collaboration.reactionbar.MonarchReactionBarSize
import dev.monarchui.foundation.icon.MonarchIcon
import dev.monarchui.sample.CatalogueCaption
import dev.monarchui.sample.CataloguePage
import dev.monarchui.sampleicons.SampleIcons

/** The catalogue page of ReactionBar: tap a chip to react, or add one from the menu. */
object MonarchReactionBarSection : CataloguePage {
    override val title = "ReactionBar"

    private const val THUMBS = 3

    private val Names = mapOf("👍" to "thumbs up", "🎉" to "party popper", "❤️" to "red heart", "👀" to "eyes")

    @Composable
    override fun Content() {
        // Emoji to (count, reacted), as the app would keep them.
        var counts by rememberSaveable { mutableStateOf(mapOf("👍" to (THUMBS to true), "🎉" to (1 to false))) }
        val reactions = counts.map { (emoji, state) ->
            MonarchReactionBarItem(emoji, count = state.first, reacted = state.second, label = Names[emoji])
        }
        val toggle: (String, Boolean) -> Unit = { emoji, on ->
            val (count, _) = counts[emoji] ?: (0 to false)
            counts = counts + (emoji to ((count + if (on) 1 else -1) to on))
        }
        CatalogueCaption("Under a comment")
        MonarchReactionBar(reactions, onToggle = toggle, quickReactions = Names.keys.toList())
        CatalogueCaption("Under a post, with an add icon")
        MonarchReactionBar(
            reactions,
            onToggle = toggle,
            size = MonarchReactionBarSize.Md,
            quickReactions = Names.keys.toList(),
            addIcon = { MonarchIcon(SampleIcons.React, contentDescription = null) },
        )
        CatalogueCaption("Read-only")
        MonarchReactionBar(reactions)
    }
}
