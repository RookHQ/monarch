@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class,
    dev.monarchui.ExperimentalMonarchApi::class,
)

package dev.monarchui.sample.sections.navigation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextOverflow
import dev.monarchui.components.identity.avatar.MonarchAvatar
import dev.monarchui.components.identity.avatar.MonarchAvatarSize
import dev.monarchui.components.identity.spaceswitcher.MonarchSpaceOption
import dev.monarchui.components.identity.spaceswitcher.MonarchSpaceSwitcher
import dev.monarchui.components.identity.workspaceswitcher.MonarchWorkspaceOption
import dev.monarchui.components.identity.workspaceswitcher.MonarchWorkspaceSwitcher
import dev.monarchui.components.typography.text.MonarchText
import dev.monarchui.foundation.MonarchTextStyle
import dev.monarchui.sample.CatalogueCaption
import dev.monarchui.sample.CataloguePage
import dev.monarchui.sampleicons.SampleIcons
import dev.monarchui.theme.MonarchTheme
import kotlinx.coroutines.launch

private val RailDestinations = listOf("inbox" to "Inbox", "tickets" to "Tickets", "customers" to "Customers")
private val RailIcons =
    mapOf("inbox" to SampleIcons.Inbox, "tickets" to SampleIcons.Agenda, "customers" to SampleIcons.Users)
private val RailUnread = mapOf("inbox" to 3)
private const val RailHeightSteps = 4

/** The catalogue page of RookRail: a recipe of a Material 3 navigation drawer and Monarch switchers. */
object RookRailSection : CataloguePage {
    override val title = "RookRail"

    @Composable
    override fun Content() {
        val drawerState = rememberDrawerState(DrawerValue.Closed)
        val scope = rememberCoroutineScope()
        var current by rememberSaveable { mutableStateOf("inbox") }
        var last by rememberSaveable { mutableStateOf("") }
        CatalogueCaption("The Rook navigation on a phone: open it from the menu button")
        ModalNavigationDrawer(
            drawerState = drawerState,
            drawerContent = {
                ModalDrawerSheet {
                    RailDrawerContent(current = current) { key ->
                        if (key in RailDestinations.map { it.first }) current = key else last = key
                        scope.launch { drawerState.close() }
                    }
                }
            },
            modifier = Modifier.height(MonarchTheme.spacing.space24 * RailHeightSteps),
        ) {
            Column {
                TopAppBar(
                    title = { Text(text = "Rook Tickets", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    navigationIcon = {
                        IconButton(
                            onClick = { scope.launch { drawerState.open() } },
                            modifier = Modifier.semantics { contentDescription = "Open navigation" },
                        ) {
                            Icon(SampleIcons.Menu, contentDescription = null)
                        }
                    },
                )
                MonarchText(
                    text = RailDestinations.first { it.first == current }.second,
                    modifier = Modifier.padding(MonarchTheme.spacing.space4),
                    style = MonarchTextStyle.Title,
                )
            }
        }
        if (last.isNotEmpty()) MonarchText(text = "Last action: $last")
    }
}

@Composable
private fun RailDrawerContent(current: String, onSelect: (String) -> Unit) {
    var workspace by rememberSaveable { mutableStateOf<String?>("acme") }
    var space by rememberSaveable { mutableStateOf<String?>(null) }
    Column(
        modifier = Modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = MonarchTheme.spacing.space3, vertical = MonarchTheme.spacing.space4),
        verticalArrangement = Arrangement.spacedBy(MonarchTheme.spacing.space1),
    ) {
        MonarchText(text = "Rook Tickets", style = MonarchTextStyle.Heading)
        MonarchWorkspaceSwitcher(
            workspaces = listOf(
                MonarchWorkspaceOption("acme", "Acme", handle = "acme"),
                MonarchWorkspaceOption("rook", "Rook Labs", handle = "rooklabs"),
            ),
            selection = workspace,
            onSelectionChange = { workspace = it },
        )
        MonarchSpaceSwitcher(
            spaces = listOf(MonarchSpaceOption("support", "Support"), MonarchSpaceOption("sales", "Sales")),
            selection = space,
            onSelectionChange = { space = it },
        )
        HorizontalDivider()
        RailDestinations.forEach { (key, name) ->
            val count = RailUnread[key] ?: 0
            NavigationDrawerItem(
                label = { Text(name) },
                selected = current == key,
                onClick = { onSelect(key) },
                modifier = Modifier.semantics { if (count > 0) stateDescription = "$count unread" },
                icon = { Icon(RailIcons.getValue(key), contentDescription = null) },
                badge = if (count > 0) {
                    { Text(text = "$count", modifier = Modifier.clearAndSetSemantics { }) }
                } else {
                    null
                },
            )
        }
        HorizontalDivider()
        NavigationDrawerItem(
            label = { Text("Help & support") },
            selected = false,
            onClick = { onSelect("Help & support") },
            icon = { Icon(SampleIcons.Help, contentDescription = null) },
        )
        MonarchText(text = "Workspace", style = MonarchTextStyle.Caption, color = MonarchTheme.colors.textMuted)
        NavigationDrawerItem(
            label = { Text("Members") },
            selected = false,
            onClick = { onSelect("Members, in Rook ID") },
            modifier = Modifier.semantics { stateDescription = "Opens in Rook ID" },
        )
        NavigationDrawerItem(
            label = { Text("Priya Raman") },
            selected = false,
            onClick = { onSelect("Account menu") },
            modifier = Modifier.semantics { contentDescription = "Account: Priya Raman" },
            icon = {
                MonarchAvatar(image = null, name = "Priya Raman", size = MonarchAvatarSize.Sm, decorative = true)
            },
        )
    }
}
