package com.rookhq.monarch.sample.sections.interaction

import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusRestorer
import com.rookhq.monarch.components.actions.button.MonarchButton
import com.rookhq.monarch.components.actions.button.MonarchButtonVariant
import com.rookhq.monarch.components.typography.text.MonarchText
import com.rookhq.monarch.foundation.MonarchTextStyle
import com.rookhq.monarch.sample.CatalogueCaption
import com.rookhq.monarch.sample.CataloguePage
import com.rookhq.monarch.theme.MonarchTheme

/** The catalogue page of RovingFocusGroup: Compose focus groups, restoration and requesters. */
object RovingFocusGroupSection : CataloguePage {
    override val title = "RovingFocusGroup"

    @Composable
    override fun Content() {
        MonarchText(
            text = "With a hardware keyboard: Tab moves between groups, the arrow keys move inside one.",
            style = MonarchTextStyle.Caption,
        )
        CatalogueCaption("A toolbar that is one Tab stop and remembers its focused button")
        FlowRow(
            modifier = Modifier.focusRestorer().focusGroup(),
            horizontalArrangement = Arrangement.spacedBy(MonarchTheme.spacing.space2),
        ) {
            listOf("Bold", "Italic", "Link", "Quote").forEach { MonarchButton(text = it, onClick = {}) }
        }

        val first = remember { FocusRequester() }
        CatalogueCaption("Moving focus from code")
        MonarchButton(text = "Focus the first plan", onClick = { first.requestFocus() })
        FlowRow(
            modifier = Modifier.focusGroup(),
            horizontalArrangement = Arrangement.spacedBy(MonarchTheme.spacing.space2),
        ) {
            MonarchButton(
                text = "Monthly",
                onClick = {},
                modifier = Modifier.focusRequester(first),
                variant = MonarchButtonVariant.Ghost,
            )
            MonarchButton(text = "Yearly", onClick = {}, variant = MonarchButtonVariant.Ghost)
        }
    }
}
