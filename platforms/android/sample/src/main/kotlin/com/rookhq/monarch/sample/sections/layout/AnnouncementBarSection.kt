package com.rookhq.monarch.sample.sections.layout

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.rookhq.monarch.components.actions.button.MonarchButton
import com.rookhq.monarch.components.actions.button.MonarchButtonVariant
import com.rookhq.monarch.components.feedback.calloutbanner.MonarchCalloutBanner
import com.rookhq.monarch.components.feedback.calloutbanner.MonarchCalloutBannerPlacement
import com.rookhq.monarch.components.navigation.link.MonarchLink
import com.rookhq.monarch.components.typography.text.MonarchText
import com.rookhq.monarch.foundation.MonarchTextStyle
import com.rookhq.monarch.foundation.MonarchTone
import com.rookhq.monarch.sample.CatalogueCaption
import com.rookhq.monarch.sample.CataloguePage
import com.rookhq.monarch.theme.MonarchTheme

/**
 * The catalogue page of AnnouncementBar, a recipe: a dismissible
 * MonarchCalloutBanner in the page placement under the top app bar.
 */
object AnnouncementBarSection : CataloguePage {
    override val title = "AnnouncementBar"

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        var dismissed by rememberSaveable { mutableStateOf(false) }
        var opened by rememberSaveable { mutableIntStateOf(0) }
        CatalogueCaption("Under the top app bar")
        Column {
            TopAppBar(title = { Text("Inbox") })
            if (!dismissed) {
                MonarchCalloutBanner(
                    message = "Shared inboxes are now available on every plan.",
                    placement = MonarchCalloutBannerPlacement.Page,
                    action = { MonarchLink(text = "Read the release notes", onClick = { opened++ }) },
                    onDismiss = { dismissed = true },
                )
            }
        }
        if (opened > 0) {
            MonarchText(
                text = "Release notes opened $opened times",
                style = MonarchTextStyle.Caption,
                color = MonarchTheme.colors.textMuted,
            )
        }
        if (dismissed) {
            MonarchButton(text = "Show it again", onClick = { dismissed = false }, variant = MonarchButtonVariant.Ghost)
        }
        CatalogueCaption("A maintenance window, which cannot be dismissed")
        MonarchCalloutBanner(
            message = "Billing is read-only on Sunday from 02:00 to 04:00 UTC.",
            tone = MonarchTone.Warning,
            placement = MonarchCalloutBannerPlacement.Page,
        )
    }
}
