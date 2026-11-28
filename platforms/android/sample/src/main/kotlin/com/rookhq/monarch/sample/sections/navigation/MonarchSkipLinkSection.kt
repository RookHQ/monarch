package com.rookhq.monarch.sample.sections.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import com.rookhq.monarch.components.actions.button.MonarchButton
import com.rookhq.monarch.components.navigation.skiplink.MonarchSkipLink
import com.rookhq.monarch.components.navigation.skiplink.monarchSkipLinkTarget
import com.rookhq.monarch.components.typography.text.MonarchText
import com.rookhq.monarch.sample.CatalogueCaption
import com.rookhq.monarch.sample.CataloguePage

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
