package dev.monarchui.sample.sections.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import dev.monarchui.components.actions.button.MonarchButton
import dev.monarchui.components.navigation.skiplink.MonarchSkipLink
import dev.monarchui.components.navigation.skiplink.monarchSkipLinkTarget
import dev.monarchui.components.typography.text.MonarchText
import dev.monarchui.sample.CatalogueCaption
import dev.monarchui.sample.CataloguePage

/** The catalogue page of SkipLink. Activating it moves focus past the toolbar buttons to the content. */
object MonarchSkipLinkSection : CataloguePage {
    override val title = "SkipLink"

    @Composable
    override fun Content() {
        val main = remember { FocusRequester() }
        MonarchSkipLink(target = main)
        CatalogueCaption("Navigation")
        listOf("Inbox", "Assigned", "Archive").forEach { MonarchButton(text = it, onClick = {}) }
        CatalogueCaption("Main content")
        MonarchText(text = "Main content starts here.", modifier = Modifier.monarchSkipLinkTarget(main))
    }
}
