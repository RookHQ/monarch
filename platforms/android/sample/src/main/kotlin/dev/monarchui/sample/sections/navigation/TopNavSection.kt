package dev.monarchui.sample.sections.navigation

import androidx.compose.foundation.layout.height
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
 * The catalogue page of TopNav. An app uses NavigationSuiteScaffold, which
 * picks the bar or the rail by window size; the catalogue does not depend on
 * that library, so it shows both forms directly.
 */
object TopNavSection : CataloguePage {
    override val title = "TopNav"

    private val destinations = listOf("Home", "Search", "Inbox")

    /** Room for the three rail items in the page. */
    private val railHeight = (MonarchTokens.Space.space24 * 3).dp

    @Composable
    override fun Content() {
        var current by rememberSaveable { mutableIntStateOf(0) }
        CatalogueCaption("Compact width: NavigationBar")
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
        CatalogueCaption("Medium and expanded width: NavigationRail")
        NavigationRail(modifier = Modifier.height(railHeight)) {
            destinations.forEachIndexed { index, name ->
                NavigationRailItem(
                    selected = current == index,
                    onClick = { current = index },
                    icon = { DestinationIcon(name) },
                    label = { Text(name) },
                )
            }
        }
        MonarchText(
            text = "NavigationSuiteScaffold switches between the two for the window size.",
            style = MonarchTextStyle.Caption,
            color = MonarchTheme.colors.textMuted,
        )
    }

    @Composable
    private fun DestinationIcon(name: String) {
        val glyph = when (name) {
            "Home" -> SampleIcons.Home
            "Search" -> SampleIcons.Search
            else -> SampleIcons.Inbox
        }
        val icon = @Composable { Icon(glyph, contentDescription = null) }
        if (name == "Inbox") BadgedBox(badge = { Badge { Text("3") } }) { icon() } else icon()
    }
}
