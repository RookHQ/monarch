package com.rookhq.monarch.sample.sections.typography

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.rookhq.monarch.components.typography.kbd.MonarchKbd
import com.rookhq.monarch.components.typography.text.MonarchText
import com.rookhq.monarch.sample.CatalogueCaption
import com.rookhq.monarch.sample.CataloguePage
import com.rookhq.monarch.theme.MonarchTheme

/** The catalogue page of Kbd. */
object MonarchKbdSection : CataloguePage {
    override val title = "Kbd"

    @Composable
    override fun Content() {
        CatalogueCaption("Shown only with a hardware keyboard")
        Shortcut("Search", "Ctrl+K", alwaysVisible = false)
        CatalogueCaption("Always visible")
        Shortcut("Command palette", "⇧⌘P", alwaysVisible = true)
        Shortcut("Close", "Esc", alwaysVisible = true)
    }

    @Composable
    private fun Shortcut(action: String, keys: String, alwaysVisible: Boolean) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(MonarchTheme.spacing.space2),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            MonarchText(text = action, modifier = Modifier.weight(1f))
            MonarchKbd(keys = keys, alwaysVisible = alwaysVisible)
        }
    }
}
