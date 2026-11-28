@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class,
    androidx.compose.foundation.layout.ExperimentalLayoutApi::class,
)

package com.rookhq.monarch.sample.sections.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import com.rookhq.monarch.components.identity.avatar.MonarchAvatar
import com.rookhq.monarch.components.identity.avatar.MonarchAvatarSize
import com.rookhq.monarch.components.layout.notificationsbell.MonarchNotificationsBell
import com.rookhq.monarch.components.overlays.menu.MonarchMenu
import com.rookhq.monarch.components.overlays.menu.MonarchMenuDivider
import com.rookhq.monarch.components.overlays.menu.MonarchMenuItem
import com.rookhq.monarch.components.search.searchbar.MonarchSearchBar
import com.rookhq.monarch.components.typography.text.MonarchText
import com.rookhq.monarch.foundation.icon.MonarchIcon
import com.rookhq.monarch.sample.CatalogueCaption
import com.rookhq.monarch.sample.CataloguePage
import com.rookhq.monarch.sampleicons.SampleIcons
import com.rookhq.monarch.theme.MonarchTheme

/** The catalogue page of IdentityBar: a recipe of existing components. */
object IdentityBarSection : CataloguePage {
    override val title = "IdentityBar"

    @Composable
    override fun Content() {
        var last by rememberSaveable { mutableStateOf("") }
        CatalogueCaption("The identity bar of a Rook app on a phone")
        run {
            var query by rememberSaveable { mutableStateOf("") }
            var accountOpen by remember { mutableStateOf(false) }
            Column {
                TopAppBar(
                    title = {
                        Text(
                            text = "Rook Tickets",
                            modifier = Modifier.semantics { heading() },
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    },
                    actions = {
                        MonarchNotificationsBell(onClick = { last = "Notifications" }, unreadCount = 3) {
                            MonarchIcon(
                                SampleIcons.Bell,
                                contentDescription = null,
                            )
                        }
                        Box {
                            IconButton(
                                onClick = { accountOpen = true },
                                modifier = Modifier.semantics { contentDescription = "Account: Priya Raman" },
                            ) {
                                MonarchAvatar(
                                    image = null,
                                    name = "Priya Raman",
                                    size = MonarchAvatarSize.Sm,
                                    decorative = true,
                                )
                            }
                            MonarchMenu(expanded = accountOpen, onDismissRequest = { accountOpen = false }) {
                                MonarchText(
                                    text = "priya@example.com",
                                    modifier = Modifier.padding(
                                        horizontal = MonarchTheme.spacing.space3,
                                        vertical = MonarchTheme.spacing.space2,
                                    ),
                                    color = MonarchTheme.colors.textMuted,
                                )
                                MonarchMenuDivider()
                                MonarchMenuItem(
                                    text = "Profile and preferences",
                                    onClick = { accountOpen = false },
                                )
                                MonarchMenuItem(text = "Rook account", onClick = { accountOpen = false })
                                MonarchMenuItem(
                                    text = "Sign out",
                                    onClick = { accountOpen = false },
                                    destructive = true,
                                )
                            }
                        }
                    },
                )
                MonarchSearchBar(
                    query = query,
                    onQueryChange = { query = it },
                    onSearch = { last = "Search: $it" },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = MonarchTheme.spacing.space4),
                    placeholder = "Search Rook Tickets",
                )
            }
        }
        if (last.isNotEmpty()) MonarchText(text = "Last action: $last")
    }
}
