package dev.monarchui.sample.sections.layout

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import dev.monarchui.components.actions.button.MonarchButton
import dev.monarchui.components.actions.button.MonarchButtonSize
import dev.monarchui.components.actions.button.MonarchButtonVariant
import dev.monarchui.components.layout.sectionheader.MonarchSectionHeader
import dev.monarchui.components.layout.sectionheader.MonarchSectionHeaderSize
import dev.monarchui.components.typography.text.MonarchText
import dev.monarchui.foundation.icon.MonarchIcon
import dev.monarchui.foundation.icon.MonarchIconSize
import dev.monarchui.sample.CatalogueCaption
import dev.monarchui.sample.CataloguePage
import dev.monarchui.sampleicons.SampleIcons
import kotlinx.coroutines.delay

/** The catalogue page of SectionHeader. */
object MonarchSectionHeaderSection : CataloguePage {
    override val title = "SectionHeader"

    private const val REFRESH_MILLIS = 1500L

    @Composable
    override fun Content() {
        CatalogueCaption("Meta and a trailing link")
        MonarchSectionHeader(title = "Recent invoices", meta = "12 issued this month") {
            MonarchButton(
                text = "View all invoices",
                onClick = {},
                variant = MonarchButtonVariant.Ghost,
                size = MonarchButtonSize.Sm,
            )
        }

        CatalogueCaption("Description and an action, Md")
        MonarchSectionHeader(
            title = "Team members",
            description = "People who can sign in to this workspace.",
            size = MonarchSectionHeaderSize.Md,
        ) {
            MonarchButton(
                text = "Invite",
                onClick = {},
                variant = MonarchButtonVariant.Primary,
                size = MonarchButtonSize.Sm,
                leadingIcon = {
                    MonarchIcon(SampleIcons.Plus, contentDescription = null, size = MonarchIconSize.Sm)
                },
            )
        }

        var busy by rememberSaveable { mutableStateOf(false) }
        LaunchedEffect(busy) {
            if (busy) {
                delay(REFRESH_MILLIS)
                busy = false
            }
        }
        CatalogueCaption("Busy while the section refreshes")
        MonarchSectionHeader(title = "Traffic sources", meta = "Last 7 days", busy = busy) {
            MonarchButton(text = "Refresh", onClick = { busy = true }, size = MonarchButtonSize.Sm)
        }
        MonarchText(text = "Search, Direct, Referral")
    }
}
