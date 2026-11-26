package com.rookhq.monarch.sample.sections.technical

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import com.rookhq.monarch.components.technical.lifecyclebadge.MonarchLifecycle
import com.rookhq.monarch.components.technical.lifecyclebadge.MonarchLifecycleBadge
import com.rookhq.monarch.components.typography.text.MonarchText
import com.rookhq.monarch.foundation.MonarchTextStyle
import com.rookhq.monarch.foundation.MonarchTone
import com.rookhq.monarch.sample.CatalogueCaption
import com.rookhq.monarch.sample.CataloguePage
import com.rookhq.monarch.theme.MonarchTheme

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
