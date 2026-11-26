package com.rookhq.monarch.sample.sections.foundations

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import com.rookhq.monarch.foundation.icon.MonarchIcon
import com.rookhq.monarch.foundation.icon.MonarchIconSize
import com.rookhq.monarch.sample.CataloguePage
import com.rookhq.monarch.sampleicons.SampleIcons
import com.rookhq.monarch.theme.MonarchTheme

/** The catalogue page of Icon. */
object MonarchIconSection : CataloguePage {
    override val title = "Icon"

    @Composable
    override fun Content() {
        Row(
            horizontalArrangement = Arrangement.spacedBy(MonarchTheme.spacing.space4),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            MonarchIconSize.entries.forEach { size ->
                MonarchIcon(imageVector = SampleIcons.Plus, contentDescription = "Add, ${size.name}", size = size)
            }
            MonarchIcon(
                imageVector = SampleIcons.Plus,
                contentDescription = "Add, accent",
                tint = MonarchTheme.colors.accentStrong,
            )
            MonarchIcon(imageVector = SampleIcons.ArrowBack, contentDescription = "Back, mirrors right to left")
        }
    }
}
