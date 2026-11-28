package dev.monarchui.sample.sections.navigation

import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import dev.monarchui.components.typography.text.MonarchText
import dev.monarchui.foundation.icon.MonarchIcon
import dev.monarchui.sample.CatalogueCaption
import dev.monarchui.sample.CataloguePage
import dev.monarchui.sampleicons.SampleIcons

/** The catalogue page of BottomTabBar: the Material 3 NavigationBar. */
object BottomTabBarSection : CataloguePage {
    override val title = "BottomTabBar"

    private const val INBOX = 1
    private val destinations = listOf("Home", "Inbox", "Contacts", "Settings")
    private val icons = listOf(SampleIcons.Home, SampleIcons.Inbox, SampleIcons.Users, SampleIcons.Settings)

    @Composable
    override fun Content() {
        var selected by rememberSaveable { mutableIntStateOf(0) }
        CatalogueCaption("NavigationBar with four destinations and a count")
        NavigationBar {
            destinations.forEachIndexed { index, label ->
                NavigationBarItem(
                    selected = selected == index,
                    onClick = { selected = index },
                    icon = {
                        if (index == INBOX) {
                            BadgedBox(badge = { Badge { Text("3") } }) {
                                MonarchIcon(icons[index], contentDescription = null)
                            }
                        } else {
                            MonarchIcon(icons[index], contentDescription = null)
                        }
                    },
                    label = { Text(label) },
                    modifier = if (index == INBOX) Modifier.semantics { stateDescription = "3 unread" } else Modifier,
                )
            }
        }
        MonarchText(text = "Showing ${destinations[selected]}")
    }
}
