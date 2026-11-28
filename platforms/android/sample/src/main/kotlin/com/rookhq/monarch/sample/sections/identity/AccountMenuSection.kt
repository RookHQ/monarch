package com.rookhq.monarch.sample.sections.identity

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import com.rookhq.monarch.components.identity.avatar.MonarchAvatar
import com.rookhq.monarch.components.identity.avatar.MonarchAvatarSize
import com.rookhq.monarch.components.interaction.themetoggle.MonarchThemeToggle
import com.rookhq.monarch.components.interaction.themetoggle.MonarchThemeToggleSize
import com.rookhq.monarch.components.overlays.menu.MonarchMenu
import com.rookhq.monarch.components.overlays.menu.MonarchMenuDivider
import com.rookhq.monarch.components.overlays.menu.MonarchMenuItem
import com.rookhq.monarch.components.overlays.menu.MonarchMenuLabel
import com.rookhq.monarch.components.typography.text.MonarchText
import com.rookhq.monarch.foundation.MonarchTextStyle
import com.rookhq.monarch.sample.CatalogueCaption
import com.rookhq.monarch.sample.CataloguePage
import com.rookhq.monarch.theme.MonarchScheme
import com.rookhq.monarch.theme.MonarchTheme

/** The catalogue page of AccountMenu: an avatar button that opens a MonarchMenu with the account choices. */
object AccountMenuSection : CataloguePage {
    override val title = "AccountMenu"

    private val workspaces = listOf("acme" to "Acme", "globex" to "Globex")

    @Composable
    override fun Content() {
        var open by rememberSaveable { mutableStateOf(false) }
        var workspace by rememberSaveable { mutableStateOf("acme") }
        var scheme by rememberSaveable { mutableStateOf(MonarchScheme.System) }
        var last by rememberSaveable { mutableStateOf("") }
        CatalogueCaption("Account menu")
        Box {
            IconButton(
                onClick = { open = true },
                modifier = Modifier.semantics { contentDescription = "Account menu for Ada Lovelace" },
            ) {
                MonarchAvatar(image = null, name = "Ada Lovelace", size = MonarchAvatarSize.Sm, decorative = true)
            }
            MonarchMenu(expanded = open, onDismissRequest = { open = false }) {
                Column(
                    modifier = Modifier
                        .padding(horizontal = MonarchTheme.spacing.space3, vertical = MonarchTheme.spacing.space2)
                        .semantics(mergeDescendants = true) {},
                ) {
                    MonarchText(text = "Ada Lovelace", style = MonarchTextStyle.BodyStrong)
                    MonarchText(
                        text = "ada@example.com",
                        style = MonarchTextStyle.Caption,
                        color = MonarchTheme.colors.textMuted,
                    )
                }
                MonarchMenuDivider()
                listOf("Profile", "Settings").forEach { item ->
                    MonarchMenuItem(
                        text = item,
                        onClick = {
                            last = item
                            open = false
                        },
                    )
                }
                MonarchMenuDivider()
                MonarchMenuLabel("Switch workspace")
                workspaces.forEach { (id, name) ->
                    MonarchMenuItem(
                        text = name,
                        onClick = {
                            workspace = id
                            open = false
                        },
                        selected = id == workspace,
                    )
                }
                MonarchMenuDivider()
                MonarchThemeToggle(
                    scheme = scheme,
                    onSchemeChange = { scheme = it },
                    modifier = Modifier.padding(
                        horizontal = MonarchTheme.spacing.space3,
                        vertical = MonarchTheme.spacing.space2,
                    ),
                    size = MonarchThemeToggleSize.Sm,
                )
                MonarchMenuDivider()
                MonarchMenuItem(
                    text = "Sign out",
                    onClick = {
                        last = "Sign out"
                        open = false
                    },
                    destructive = true,
                )
            }
        }
        val workspaceName = workspaces.first { it.first == workspace }.second
        val chose = if (last.isNotEmpty()) " · Chose $last" else ""
        MonarchText(
            text = "Workspace: $workspaceName · Theme: ${scheme.name}$chose",
            color = MonarchTheme.colors.textMuted,
        )
    }
}
