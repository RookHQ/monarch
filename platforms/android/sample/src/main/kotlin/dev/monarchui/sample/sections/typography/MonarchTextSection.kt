package dev.monarchui.sample.sections.typography

import androidx.compose.runtime.Composable
import dev.monarchui.components.typography.text.MonarchText
import dev.monarchui.foundation.MonarchTextStyle
import dev.monarchui.sample.CataloguePage
import dev.monarchui.theme.MonarchTheme

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
