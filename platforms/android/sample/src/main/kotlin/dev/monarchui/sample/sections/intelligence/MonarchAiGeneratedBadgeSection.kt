package dev.monarchui.sample.sections.intelligence

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import dev.monarchui.components.intelligence.aigeneratedbadge.MonarchAiGeneratedBadge
import dev.monarchui.components.intelligence.aigeneratedbadge.MonarchAiGeneratedBadgeKind
import dev.monarchui.components.intelligence.aigeneratedbadge.MonarchAiGeneratedBadgeSize
import dev.monarchui.components.typography.text.MonarchText
import dev.monarchui.foundation.icon.MonarchIcon
import dev.monarchui.sample.CatalogueCaption
import dev.monarchui.sample.CataloguePage
import dev.monarchui.sampleicons.SampleIcons
import dev.monarchui.theme.MonarchTheme

/** The catalogue page of AiGeneratedBadge. */
object MonarchAiGeneratedBadgeSection : CataloguePage {
    override val title = "AiGeneratedBadge"

    @Composable
    override fun Content() {
        MonarchAiGeneratedBadgeSize.entries.forEach { size ->
            CatalogueCaption(size.name)
            MonarchAiGeneratedBadgeKind.entries.forEach { kind -> MonarchAiGeneratedBadge(kind = kind, size = size) }
        }
        CatalogueCaption("With the model and time, read by TalkBack")
        MonarchAiGeneratedBadge(
            kind = MonarchAiGeneratedBadgeKind.Assisted,
            model = "gemini-2.5-pro",
            dateMillis = System.currentTimeMillis(),
            icon = { MonarchIcon(SampleIcons.Bot, contentDescription = null) },
        )
        CatalogueCaption("Icon only, in a dense row")
        Row(
            horizontalArrangement = Arrangement.spacedBy(MonarchTheme.spacing.space2),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            MonarchText(text = "Quarterly summary")
            MonarchAiGeneratedBadge(
                kind = MonarchAiGeneratedBadgeKind.Edited,
                size = MonarchAiGeneratedBadgeSize.Sm,
                iconOnly = true,
                icon = { MonarchIcon(SampleIcons.Bot, contentDescription = null) },
            )
        }
    }
}
