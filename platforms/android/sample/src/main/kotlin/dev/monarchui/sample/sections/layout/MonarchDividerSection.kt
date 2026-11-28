package dev.monarchui.sample.sections.layout

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import dev.monarchui.components.layout.divider.MonarchDivider
import dev.monarchui.components.layout.divider.MonarchDividerOrientation
import dev.monarchui.components.typography.text.MonarchText
import dev.monarchui.sample.CataloguePage
import dev.monarchui.theme.MonarchTheme

/** The catalogue page of Separator. */
object MonarchDividerSection : CataloguePage {
    override val title = "Separator"

    @Composable
    override fun Content() {
        MonarchText(text = "Above")
        MonarchDivider()
        MonarchText(text = "Below")
        Row(
            modifier = Modifier.height(IntrinsicSize.Min),
            horizontalArrangement = Arrangement.spacedBy(MonarchTheme.spacing.space3),
        ) {
            MonarchText(text = "Start")
            MonarchDivider(orientation = MonarchDividerOrientation.Vertical)
            MonarchText(text = "End")
        }
    }
}
