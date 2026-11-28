package com.rookhq.monarch.sample.sections.typography

import androidx.compose.runtime.Composable
import com.rookhq.monarch.components.typography.text.MonarchText
import com.rookhq.monarch.foundation.MonarchTextStyle
import com.rookhq.monarch.sample.CataloguePage
import com.rookhq.monarch.theme.MonarchTheme

/** The catalogue page of Text. */
object MonarchTextSection : CataloguePage {
    override val title = "Text"

    @Composable
    override fun Content() {
        MonarchTextStyle.entries.forEach { role ->
            MonarchText(text = role.name, style = role, heading = false)
        }
        MonarchText(text = "Muted body text takes a theme role.", color = MonarchTheme.colors.textMuted)
    }
}
