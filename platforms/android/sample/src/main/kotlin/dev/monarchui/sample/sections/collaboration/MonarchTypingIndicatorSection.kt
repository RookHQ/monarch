package dev.monarchui.sample.sections.collaboration

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import dev.monarchui.components.actions.button.MonarchButton
import dev.monarchui.components.actions.button.MonarchButtonSize
import dev.monarchui.components.collaboration.typingindicator.MonarchTypingIndicator
import dev.monarchui.sample.CatalogueCaption
import dev.monarchui.sample.CataloguePage
import dev.monarchui.theme.MonarchTheme

/** The catalogue page of TypingIndicator. */
object MonarchTypingIndicatorSection : CataloguePage {
    override val title = "TypingIndicator"

    private val people = listOf("Ana", "Ben", "Chen")

    @Composable
    override fun Content() {
        var count by rememberSaveable { mutableIntStateOf(1) }
        CatalogueCaption("Live")
        MonarchTypingIndicator(names = people.take(count))
        Row(horizontalArrangement = Arrangement.spacedBy(MonarchTheme.spacing.space2)) {
            MonarchButton(text = "Fewer", onClick = { count-- }, size = MonarchButtonSize.Sm, enabled = count > 0)
            MonarchButton(
                text = "More",
                onClick = { count++ },
                size = MonarchButtonSize.Sm,
                enabled = count < people.size,
            )
        }
        CatalogueCaption("One, two and several")
        MonarchTypingIndicator(names = people.take(1), announceChanges = false)
        MonarchTypingIndicator(names = people.take(2), announceChanges = false)
        MonarchTypingIndicator(names = people, announceChanges = false)
    }
}
