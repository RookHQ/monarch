package dev.monarchui.sample.sections.feedback

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import dev.monarchui.components.actions.button.MonarchButton
import dev.monarchui.components.actions.button.MonarchButtonVariant
import dev.monarchui.components.datadisplay.card.MonarchCard
import dev.monarchui.components.feedback.emptystate.MonarchEmptyState
import dev.monarchui.components.feedback.emptystate.MonarchEmptyStateKind
import dev.monarchui.components.feedback.emptystate.MonarchEmptyStateSize
import dev.monarchui.foundation.icon.MonarchIcon
import dev.monarchui.sample.CatalogueCaption
import dev.monarchui.sample.CataloguePage
import dev.monarchui.sampleicons.SampleIcons

/** The catalogue page of EmptyState. */
object MonarchEmptyStateSection : CataloguePage {
    override val title = "EmptyState"

    @Composable
    override fun Content() {
        var created by rememberSaveable { mutableIntStateOf(0) }
        var cleared by rememberSaveable { mutableIntStateOf(0) }
        CatalogueCaption("First run")
        MonarchCard(modifier = Modifier.fillMaxWidth()) {
            MonarchEmptyState(
                modifier = Modifier.fillMaxWidth(),
                title = if (created == 0) "No invoices yet" else "Created $created invoices",
                message = "Invoices you create appear here.",
                icon = { MonarchIcon(SampleIcons.Inbox, contentDescription = null) },
                actions = {
                    MonarchButton(
                        text = "Create invoice",
                        onClick = { created++ },
                        variant = MonarchButtonVariant.Primary,
                    )
                },
            )
        }
        CatalogueCaption("Filtered, small")
        MonarchCard(modifier = Modifier.fillMaxWidth()) {
            MonarchEmptyState(
                modifier = Modifier.fillMaxWidth(),
                title = "No matching tickets",
                kind = MonarchEmptyStateKind.Filtered,
                size = MonarchEmptyStateSize.Sm,
                message = "Filters cleared $cleared times.",
                onClearFilters = { cleared++ },
            )
        }
        CatalogueCaption("Success")
        MonarchEmptyState(
            modifier = Modifier.fillMaxWidth(),
            title = "All caught up",
            kind = MonarchEmptyStateKind.Success,
            icon = { MonarchIcon(SampleIcons.Inbox, contentDescription = null) },
            tip = "New tickets appear here as they arrive.",
        )
    }
}
