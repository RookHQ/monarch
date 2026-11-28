package dev.monarchui.sample.sections.layout

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import dev.monarchui.components.datadisplay.badge.MonarchBadge
import dev.monarchui.components.typography.text.MonarchText
import dev.monarchui.foundation.MonarchTone
import dev.monarchui.foundation.icon.MonarchIcon
import dev.monarchui.sample.CatalogueCaption
import dev.monarchui.sample.CataloguePage
import dev.monarchui.sampleicons.SampleIcons
import dev.monarchui.theme.MonarchTheme

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
