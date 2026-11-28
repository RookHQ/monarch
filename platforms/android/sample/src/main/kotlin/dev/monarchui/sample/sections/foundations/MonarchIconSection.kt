package dev.monarchui.sample.sections.foundations

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import dev.monarchui.foundation.icon.MonarchIcon
import dev.monarchui.foundation.icon.MonarchIconSize
import dev.monarchui.sample.CataloguePage
import dev.monarchui.sampleicons.SampleIcons
import dev.monarchui.theme.MonarchTheme

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
