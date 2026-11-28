package dev.monarchui.sample.sections.technical

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import dev.monarchui.components.technical.lifecyclebadge.MonarchLifecycle
import dev.monarchui.components.technical.lifecyclebadge.MonarchLifecycleBadge
import dev.monarchui.components.typography.text.MonarchText
import dev.monarchui.foundation.MonarchTextStyle
import dev.monarchui.foundation.MonarchTone
import dev.monarchui.sample.CatalogueCaption
import dev.monarchui.sample.CataloguePage
import dev.monarchui.theme.MonarchTheme

/** The catalogue page of LifecycleBadge. */
object MonarchLifecycleBadgeSection : CataloguePage {
    override val title = "LifecycleBadge"

    @OptIn(ExperimentalLayoutApi::class)
    @Composable
    override fun Content() {
        CatalogueCaption("Built-in stages")
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(MonarchTheme.spacing.space2),
            verticalArrangement = Arrangement.spacedBy(MonarchTheme.spacing.space2),
        ) {
            MonarchLifecycle.entries.forEach { MonarchLifecycleBadge(lifecycle = it) }
        }
        CatalogueCaption("Beside a name")
        Row(
            horizontalArrangement = Arrangement.spacedBy(MonarchTheme.spacing.space2),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            MonarchText(text = "Invoices API", style = MonarchTextStyle.BodyStrong)
            MonarchLifecycleBadge(lifecycle = MonarchLifecycle.Preview)
        }
        CatalogueCaption("Custom stages")
        FlowRow(horizontalArrangement = Arrangement.spacedBy(MonarchTheme.spacing.space2)) {
            MonarchLifecycleBadge(stage = "early_access", tone = MonarchTone.Info)
            MonarchLifecycleBadge(stage = "retired", tone = MonarchTone.Danger)
            MonarchLifecycleBadge(lifecycle = MonarchLifecycle.Stable, showDot = false)
        }
    }
}
