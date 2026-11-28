package com.rookhq.monarch.sample.sections.layout

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.rookhq.monarch.components.layout.divider.MonarchDivider
import com.rookhq.monarch.components.layout.divider.MonarchDividerOrientation
import com.rookhq.monarch.components.typography.text.MonarchText
import com.rookhq.monarch.sample.CataloguePage
import com.rookhq.monarch.theme.MonarchTheme

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
