package com.rookhq.monarch.sample.sections.feedback

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.rookhq.monarch.components.actions.button.MonarchButton
import com.rookhq.monarch.components.datadisplay.badge.MonarchBadge
import com.rookhq.monarch.components.feedback.emptystate.MonarchEmptyState
import com.rookhq.monarch.components.typography.text.MonarchText
import com.rookhq.monarch.foundation.MonarchTone
import com.rookhq.monarch.sample.CatalogueCaption
import com.rookhq.monarch.sample.CataloguePage

/** The catalogue page of ComingSoonState: a badge, MonarchEmptyState and a list of what is planned. */
object ComingSoonStateSection : CataloguePage {
    override val title = "ComingSoonState"

    private val planned = listOf("A public page per product", "Scheduled maintenance notices", "Email subscribers")

    @Composable
    override fun Content() {
        var notified by rememberSaveable { mutableStateOf(false) }
        CatalogueCaption("A planned feature")
        MonarchBadge(text = "Coming soon", tone = MonarchTone.Info)
        MonarchEmptyState(
            title = "Status pages",
            message = "Tell customers about incidents and maintenance on a page you host with us.",
            actions = {
                MonarchButton(
                    text = if (notified) "We will let you know" else "Notify me",
                    onClick = { notified = true },
                    enabled = !notified,
                )
            },
        )
        MonarchText(text = "What is planned")
        planned.forEach { MonarchText(text = "• $it") }
    }
}
