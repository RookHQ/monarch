package com.rookhq.monarch.sample.sections.navigation

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import com.rookhq.monarch.components.actions.button.MonarchButton
import com.rookhq.monarch.components.datadisplay.badge.MonarchBadge
import com.rookhq.monarch.components.typography.text.MonarchText
import com.rookhq.monarch.foundation.MonarchTextStyle
import com.rookhq.monarch.foundation.MonarchTone
import com.rookhq.monarch.sample.CatalogueCaption
import com.rookhq.monarch.sample.CataloguePage
import com.rookhq.monarch.theme.MonarchTheme
import kotlinx.coroutines.launch

/** The catalogue page of SidebarNav: a modal navigation drawer with sections, a count and a nested group. */
object SidebarNavSection : CataloguePage {
    override val title = "SidebarNav"

    @Composable
    override fun Content() {
        var current by rememberSaveable { mutableStateOf("Inbox") }
        var reportsOpen by rememberSaveable { mutableStateOf(true) }
        val drawerState = rememberDrawerState(DrawerValue.Closed)
        val scope = rememberCoroutineScope()
        CatalogueCaption("A modal navigation drawer")
        ModalNavigationDrawer(
            drawerState = drawerState,
            drawerContent = {
                ModalDrawerSheet {
                    Column(
                        modifier = Modifier
                            .verticalScroll(rememberScrollState())
                            .padding(MonarchTheme.spacing.space3),
                    ) {
                        MonarchText(
                            text = "Work",
                            style = MonarchTextStyle.Label,
                            color = MonarchTheme.colors.textMuted,
                            heading = true,
                        )
                        NavigationDrawerItem(
                            label = { Text("Inbox") },
                            selected = current == "Inbox",
                            onClick = { current = "Inbox" },
                            badge = { MonarchBadge(text = "12", tone = MonarchTone.Info) },
                        )
                        NavigationDrawerItem(
                            label = { Text("Reports") },
                            selected = false,
                            onClick = { reportsOpen = !reportsOpen },
                            modifier = Modifier.semantics {
                                stateDescription = if (reportsOpen) "Expanded" else "Collapsed"
                            },
                        )
                        if (reportsOpen) {
                            listOf("Revenue", "Churn").forEach { name ->
                                NavigationDrawerItem(
                                    label = { Text(name) },
                                    selected = current == name,
                                    onClick = { current = name },
                                    modifier = Modifier.padding(start = MonarchTheme.spacing.space4),
                                )
                            }
                        }
                        HorizontalDivider()
                        NavigationDrawerItem(
                            label = { Text("Settings") },
                            selected = current == "Settings",
                            onClick = { current = "Settings" },
                        )
                        MonarchButton(text = "Close", onClick = { scope.launch { drawerState.close() } })
                    }
                }
            },
        ) {
            Column {
                MonarchText(text = "Current page: $current")
                MonarchButton(text = "Open the drawer", onClick = { scope.launch { drawerState.open() } })
            }
        }
    }
}
