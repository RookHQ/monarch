package com.rookhq.monarch.sample.sections.technical

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.rookhq.monarch.components.forms.checkboxrow.MonarchCheckboxRow
import com.rookhq.monarch.components.technical.embeddedappframe.MonarchEmbeddedAppFrame
import com.rookhq.monarch.components.technical.embeddedappframe.MonarchEmbeddedAppFrameLabels
import com.rookhq.monarch.components.technical.embeddedappframe.MonarchEmbeddedAppFrameState
import com.rookhq.monarch.components.typography.text.MonarchText
import com.rookhq.monarch.foundation.MonarchTextStyle
import com.rookhq.monarch.sample.CatalogueCaption
import com.rookhq.monarch.sample.CataloguePage
import com.rookhq.monarch.theme.MonarchTheme

/** The catalogue page of EmbeddedAppFrame. "Try again" on the failed page loads it. */
object MonarchEmbeddedAppFrameSection : CataloguePage {
    override val title = "EmbeddedAppFrame"

    @Composable
    override fun Content() {
        var tickets by rememberSaveable { mutableStateOf(true) }
        var summary by rememberSaveable { mutableStateOf(false) }
        var mentions by rememberSaveable { mutableStateOf(true) }
        var billing by rememberSaveable { mutableStateOf(MonarchEmbeddedAppFrameState.Error) }
        CatalogueCaption("An embedded settings page")
        MonarchEmbeddedAppFrame(
            title = "Example Desk: Email notifications",
            state = MonarchEmbeddedAppFrameState.Ready,
        ) {
            Column(Modifier.padding(MonarchTheme.spacing.space5)) {
                MonarchText(text = "Email notifications", style = MonarchTextStyle.Subheading)
                MonarchText(text = "Choose which updates reach your inbox.", color = MonarchTheme.colors.textMuted)
                MonarchCheckboxRow(
                    checked = tickets,
                    onCheckedChange = { tickets = it },
                    label = "New tickets assigned to me",
                )
                MonarchCheckboxRow(checked = summary, onCheckedChange = { summary = it }, label = "Daily summary")
                MonarchCheckboxRow(checked = mentions, onCheckedChange = { mentions = it }, label = "Mentions")
            }
        }
        CatalogueCaption("A page that never answers")
        MonarchEmbeddedAppFrame(
            title = "Example Desk: Billing",
            state = billing,
            onRetry = { billing = MonarchEmbeddedAppFrameState.Ready },
            onOpen = {},
            labels = MonarchEmbeddedAppFrameLabels(open = "Open in Example Desk"),
        ) {
            MonarchText(text = "Billing", modifier = Modifier.padding(MonarchTheme.spacing.space5))
        }
    }
}
