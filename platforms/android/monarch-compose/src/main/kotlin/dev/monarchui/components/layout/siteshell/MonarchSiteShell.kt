package dev.monarchui.components.layout.siteshell

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DrawerState
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import dev.monarchui.R
import dev.monarchui.components.actions.iconbutton.MonarchIconButton
import dev.monarchui.components.layout.divider.MonarchDivider
import dev.monarchui.components.navigation.megamenu.MonarchMegaMenu
import dev.monarchui.components.navigation.megamenu.MonarchMegaMenuItem
import dev.monarchui.components.navigation.megamenu.MonarchMegaMenuLink
import dev.monarchui.components.navigation.megamenu.MonarchMegaMenuSection
import dev.monarchui.components.typography.text.MonarchText
import dev.monarchui.foundation.MonarchTextStyle
import dev.monarchui.foundation.icon.MonarchIcon
import dev.monarchui.internal.MonarchGlyphs
import dev.monarchui.internal.monarchString
import dev.monarchui.theme.MonarchTheme
import kotlinx.coroutines.launch

/**
 * The frame of a public, signed-out area of an app (a welcome or product tour): a top app bar with the brand, a
 * menu button and actions, a navigation drawer with the site's links, a scrolling body and a footer.
 *
 * Built on Material 3 `ModalNavigationDrawer` and `Scaffold`, with a top app bar row (menu `MonarchIconButton`,
 * brand, actions), the Android form of the web's site header and footer. Entries with items expand in place in
 * the drawer (a `MonarchMegaMenu`); tapping a destination calls [onNavigate] and closes the drawer. The body
 * scrolls, with the footer after the content.
 *
 * @param brand the name in the top app bar and at the top of the drawer.
 * @param nav the entries of the navigation drawer, in order.
 * @param onNavigate called with the link of the destination the user taps.
 * @param modifier the modifier of the shell.
 * @param drawerState the state of the navigation drawer.
 * @param actions the actions at the end of the top app bar, such as "Log in" and "Get started" buttons.
 * @param footer the footer after the content, normally a [MonarchSiteFooter].
 * @param openNavigationLabel the spoken name of the menu button. `null` uses "Open navigation".
 * @param navigationLabel the spoken name of the drawer. `null` uses "Site navigation".
 * @param content the body of the screen, in a scrolling column.
 */
@Composable
public fun MonarchSiteShell(
    brand: String,
    nav: List<MonarchSiteNavItem>,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier,
    drawerState: DrawerState = rememberDrawerState(DrawerValue.Closed),
    actions: (@Composable RowScope.() -> Unit)? = null,
    footer: (@Composable () -> Unit)? = null,
    openNavigationLabel: String? = null,
    navigationLabel: String? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = MonarchTheme.colors
    val scope = rememberCoroutineScope()
    val drawerName = monarchString(navigationLabel, R.string.monarch_siteshell_navigation)
    var expanded by rememberSaveable { mutableStateOf<String?>(null) }
    val items = nav.mapIndexed { index, item -> item.toMenuItem(index) }
    ModalNavigationDrawer(
        modifier = modifier.clipToBounds(),
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                modifier = Modifier.semantics { paneTitle = drawerName },
                drawerContainerColor = MonarchSiteShellTokens.barFill(colors),
                drawerContentColor = colors.textDefault,
            ) {
                Column(
                    Modifier
                        .verticalScroll(rememberScrollState())
                        .padding(MonarchSiteShellTokens.DrawerPadding),
                ) {
                    MonarchText(
                        text = brand,
                        modifier = Modifier.padding(MonarchSiteShellTokens.DrawerTitlePadding),
                        style = MonarchTextStyle.Subheading,
                    )
                    MonarchMegaMenu(
                        items = items,
                        expandedItem = expanded,
                        onExpandedItemChange = { expanded = it },
                        onNavigate = { link ->
                            scope.launch { drawerState.close() }
                            onNavigate(link)
                        },
                    )
                }
            }
        },
    ) {
        Scaffold(
            containerColor = colors.bgCanvas,
            contentColor = colors.textDefault,
            topBar = {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MonarchSiteShellTokens.barFill(colors))
                            .statusBarsPadding()
                            .heightIn(min = MonarchSiteShellTokens.BarHeight)
                            .padding(horizontal = MonarchSiteShellTokens.BarPadding),
                        horizontalArrangement = Arrangement.spacedBy(MonarchSiteShellTokens.ActionGap),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        MonarchIconButton(
                            label = monarchString(openNavigationLabel, R.string.monarch_siteshell_open_navigation),
                            onClick = { scope.launch { drawerState.open() } },
                        ) {
                            MonarchIcon(MonarchGlyphs.Menu, contentDescription = null, tint = colors.textDefault)
                        }
                        Text(
                            text = brand,
                            modifier = Modifier.weight(1f),
                            style = MonarchSiteShellTokens.brandStyle(MonarchTheme.typography),
                            color = colors.textDefault,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        actions?.invoke(this)
                    }
                    MonarchDivider()
                }
            },
        ) { padding ->
            Column(
                Modifier
                    .padding(padding)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
            ) {
                content()
                footer?.invoke()
            }
        }
    }
}

private fun MonarchSiteNavItem.toMenuItem(index: Int): MonarchMegaMenuItem =
    MonarchMegaMenuItem(
        id = "$index",
        label = label,
        link = link,
        sections = if (items.isEmpty()) {
            emptyList()
        } else {
            listOf(MonarchMegaMenuSection(null, items.map { MonarchMegaMenuLink(it.label, it.link, it.description) }))
        },
    )
