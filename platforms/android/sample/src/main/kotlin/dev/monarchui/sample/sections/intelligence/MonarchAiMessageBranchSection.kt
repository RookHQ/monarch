package dev.monarchui.sample.sections.intelligence

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import dev.monarchui.components.intelligence.aimessagebranch.MonarchAiMessageBranch
import dev.monarchui.components.typography.text.MonarchText
import dev.monarchui.foundation.icon.MonarchIcon
import dev.monarchui.sample.CatalogueCaption
import dev.monarchui.sample.CataloguePage
import dev.monarchui.sampleicons.SampleIcons

/** The catalogue page of AiMessageBranch. */
object MonarchAiMessageBranchSection : CataloguePage {
    override val title = "AiMessageBranch"

    private val replies = listOf(
        "The meeting moved to Thursday at ten.",
        "The meeting is now on Thursday, 10:00, in room B.",
        "Thursday 10:00, room B. The agenda is unchanged.",
    )

    @Composable
    override fun Content() {
        var selection by rememberSaveable { mutableIntStateOf(0) }
        CatalogueCaption("Three versions of a reply")
        MonarchText(text = replies[selection])
        MonarchAiMessageBranch(
            selection = selection,
            count = replies.size,
            onSelectionChange = { selection = it },
            previousIcon = { MonarchIcon(SampleIcons.ChevronLeft, contentDescription = null) },
            // The back arrow turned around; it mirrors in right-to-left layouts like the other one.
            nextIcon = {
                MonarchIcon(
                    SampleIcons.ChevronRight,
                    contentDescription = null,
                )
            },
        )
        CatalogueCaption("Disabled while a reply streams")
        MonarchAiMessageBranch(
            selection = 1,
            count = 3,
            onSelectionChange = {},
            previousIcon = { MonarchIcon(SampleIcons.ChevronLeft, contentDescription = null) },
            nextIcon = {
                MonarchIcon(
                    SampleIcons.ChevronRight,
                    contentDescription = null,
                )
            },
            enabled = false,
        )
    }
}
