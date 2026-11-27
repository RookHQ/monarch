package com.rookhq.monarch.sample.sections.overlays

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.rookhq.monarch.ExperimentalMonarchApi
import com.rookhq.monarch.components.actions.button.MonarchButton
import com.rookhq.monarch.components.collaboration.messagecomposer.MonarchMessageComposer
import com.rookhq.monarch.components.collaboration.messagerow.MonarchMessageRow
import com.rookhq.monarch.components.collaboration.messagerow.MonarchMessageRowAuthor
import com.rookhq.monarch.components.collaboration.messagerow.MonarchMessageRowTimeStyle
import com.rookhq.monarch.components.datadisplay.badge.MonarchBadge
import com.rookhq.monarch.components.navigation.link.MonarchLink
import com.rookhq.monarch.components.navigation.link.MonarchLinkVariant
import com.rookhq.monarch.components.overlays.bottomsheet.MonarchBottomSheet
import com.rookhq.monarch.components.search.searchresults.MonarchSearchResult
import com.rookhq.monarch.components.search.searchresults.MonarchSearchResultGroup
import com.rookhq.monarch.components.search.searchresults.MonarchSearchResults
import com.rookhq.monarch.components.typography.text.MonarchText
import com.rookhq.monarch.foundation.MonarchTextStyle
import com.rookhq.monarch.foundation.MonarchTone
import com.rookhq.monarch.sample.CatalogueCaption
import com.rookhq.monarch.sample.CataloguePage
import com.rookhq.monarch.theme.MonarchTheme

/** The catalogue page of SupportPanel: a recipe of existing components in a bottom sheet. */
object SupportPanelSection : CataloguePage {
    override val title = "SupportPanel"

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        CatalogueCaption("Chat and help in a sheet")
        var open by rememberSaveable { mutableStateOf(false) }
        var tab by rememberSaveable { mutableIntStateOf(0) }
        val draft = rememberTextFieldState()
        var sent by remember { mutableStateOf(listOf<String>()) }
        val articles = listOf(
            MonarchSearchResultGroup(
                title = "Popular articles",
                id = "popular",
                results = listOf(
                    MonarchSearchResult(title = "Reset your password", id = "a1", snippet = "Get a reset link."),
                    MonarchSearchResult(title = "Invite your team", id = "a2", snippet = "Add people by email."),
                ),
            ),
        )
        MonarchButton(text = "Help & support", onClick = { open = true })
        if (open) {
            MonarchBottomSheet(onDismissRequest = { open = false }, title = "Help & support") {
                Column(
                    verticalArrangement = Arrangement.spacedBy(MonarchTheme.spacing.space3),
                ) {
                    PrimaryTabRow(selectedTabIndex = tab) {
                        Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text("Chat") })
                        Tab(selected = tab == 1, onClick = { tab = 1 }, text = { Text("Help") })
                    }
                    if (tab == 0) {
                        @OptIn(ExperimentalMonarchApi::class)
                        MonarchMessageRow(
                            author = MonarchMessageRowAuthor("Support"),
                            time = 1_794_614_400_000L,
                            timeStyle = MonarchMessageRowTimeStyle.Clock,
                        ) {
                            MonarchText("Ask a question and the team will reply here.")
                        }
                        sent.forEach { text ->
                            @OptIn(ExperimentalMonarchApi::class)
                            MonarchMessageRow(
                                author = MonarchMessageRowAuthor("You"),
                                time = 1_794_614_460_000L,
                                timeStyle = MonarchMessageRowTimeStyle.Clock,
                            ) {
                                MonarchText(text)
                            }
                        }
                        @OptIn(ExperimentalMonarchApi::class)
                        MonarchMessageComposer(
                            label = "Message",
                            state = draft,
                            onSend = { message -> sent = sent + message.text /* Post it to the support chat API. */ },
                            placeholder = "Write a message…",
                        )
                    } else {
                        MonarchSearchResults(
                            groups = articles,
                            onSelect = { result, _ -> /* Open result.id in the help center. */ },
                            modifier = Modifier.heightIn(max = MonarchTheme.spacing.space24 * 2),
                        )
                        MonarchText(text = "Other ways to reach us", style = MonarchTextStyle.Subheading)
                        MonarchLink(
                            text = "Email support@example.com",
                            onClick = { /* Start an ACTION_SENDTO intent with a mailto: address. */ },
                            variant = MonarchLinkVariant.Standalone,
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(MonarchTheme.spacing.space2),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            MonarchBadge(text = "All systems normal", tone = MonarchTone.Success)
                            MonarchLink(
                                text = "System status",
                                onClick = { /* Open the status page in a Custom Tab. */ },
                                external = true,
                            )
                        }
                    }
                }
            }
        }
    }
}
