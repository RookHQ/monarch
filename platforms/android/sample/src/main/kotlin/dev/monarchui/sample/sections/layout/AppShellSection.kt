package dev.monarchui.sample.sections.layout

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.monarchui.components.typography.text.MonarchText
import dev.monarchui.foundation.MonarchTextStyle
import dev.monarchui.sample.CatalogueCaption
import dev.monarchui.sample.CataloguePage
import dev.monarchui.sampleicons.SampleIcons
import dev.monarchui.theme.MonarchTheme
import dev.monarchui.tokens.MonarchTokens

/**
 * The catalogue page of AppShell. An app uses NavigationSuiteScaffold, which
 * puts the bar or the rail around the content for the window size; the
 * catalogue does not depend on that library, so it shows both shells with
 * plain layout.
 */
object AppShellSection : CataloguePage {
    override val title = "AppShell"

    private val destinations = listOf("Inbox", "Customers", "Settings")

    /** The height of each shell in the page. */
    private val shellHeight = (MonarchTokens.Space.space24 * 3).dp

    @Composable
    override fun Content() {
        var current by rememberSaveable { mutableIntStateOf(0) }
        CatalogueCaption("Compact width: the content above a NavigationBar")
        Column(Modifier.fillMaxWidth().height(shellHeight)) {
            Destination(destinations[current], Modifier.weight(1f))
            NavigationBar {
                destinations.forEachIndexed { index, name ->
                    NavigationBarItem(
                        selected = current == index,
                        onClick = { current = index },
                        icon = { DestinationIcon(name) },
                        label = { Text(name) },
                    )
                }
            }
        }
        CatalogueCaption("Medium and expanded width: a NavigationRail beside the content")
        Row(Modifier.fillMaxWidth().height(shellHeight)) {
            NavigationRail {
                destinations.forEachIndexed { index, name ->
                    NavigationRailItem(
                        selected = current == index,
                        onClick = { current = index },
                        icon = { DestinationIcon(name) },
                        label = { Text(name) },
                    )
                }
            }
            Destination(destinations[current], Modifier.weight(1f))
        }
        MonarchText(
            text = "NavigationSuiteScaffold chooses between the two for the window size.",
            style = MonarchTextStyle.Caption,
            color = MonarchTheme.colors.textMuted,
        )
    }

    @Composable
    private fun Destination(name: String, modifier: Modifier) {
        Box(modifier.padding(MonarchTheme.spacing.space4)) {
            MonarchText(text = name, style = MonarchTextStyle.Title)
        }
    }

    @Composable
    private fun DestinationIcon(name: String) {
        val glyph = when (name) {
            "Inbox" -> SampleIcons.Inbox
            "Customers" -> SampleIcons.Users
            else -> SampleIcons.Settings
        }
        val icon = @Composable { Icon(glyph, contentDescription = null) }
        if (name == "Inbox") BadgedBox(badge = { Badge { Text("3") } }) { icon() } else icon()
    }
}
