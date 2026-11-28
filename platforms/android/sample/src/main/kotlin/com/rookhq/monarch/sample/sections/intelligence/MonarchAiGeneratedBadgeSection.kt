package com.rookhq.monarch.sample.sections.intelligence

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import com.rookhq.monarch.components.intelligence.aigeneratedbadge.MonarchAiGeneratedBadge
import com.rookhq.monarch.components.intelligence.aigeneratedbadge.MonarchAiGeneratedBadgeKind
import com.rookhq.monarch.components.intelligence.aigeneratedbadge.MonarchAiGeneratedBadgeSize
import com.rookhq.monarch.components.typography.text.MonarchText
import com.rookhq.monarch.foundation.icon.MonarchIcon
import com.rookhq.monarch.sample.CatalogueCaption
import com.rookhq.monarch.sample.CataloguePage
import com.rookhq.monarch.sampleicons.SampleIcons
import com.rookhq.monarch.theme.MonarchTheme

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
