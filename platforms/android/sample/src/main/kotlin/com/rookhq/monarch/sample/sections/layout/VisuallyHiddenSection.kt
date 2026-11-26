package com.rookhq.monarch.sample.sections.layout

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import com.rookhq.monarch.components.datadisplay.badge.MonarchBadge
import com.rookhq.monarch.components.typography.text.MonarchText
import com.rookhq.monarch.foundation.MonarchTone
import com.rookhq.monarch.foundation.icon.MonarchIcon
import com.rookhq.monarch.sample.CatalogueCaption
import com.rookhq.monarch.sample.CataloguePage
import com.rookhq.monarch.sampleicons.SampleIcons
import com.rookhq.monarch.theme.MonarchTheme

/** The catalogue page of VisuallyHidden: text for TalkBack set on the element itself. */
object VisuallyHiddenSection : CataloguePage {
    override val title = "VisuallyHidden"

    @Composable
    override fun Content() {
        CatalogueCaption("An icon-only button named \"Add a ticket\"")
        IconButton(onClick = {}) { MonarchIcon(SampleIcons.Plus, contentDescription = "Add a ticket") }

        CatalogueCaption("Read as \"Ticket 42, high priority, open\"")
        Row(
            modifier = Modifier.clearAndSetSemantics { contentDescription = "Ticket 42, high priority, open" },
            horizontalArrangement = Arrangement.spacedBy(MonarchTheme.spacing.space2),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            MonarchText(text = "#42")
            MonarchBadge(text = "High", tone = MonarchTone.Danger)
            MonarchBadge(text = "Open")
        }

        CatalogueCaption("A decorative icon that TalkBack skips")
        MonarchIcon(SampleIcons.Star, contentDescription = null)
    }
}
