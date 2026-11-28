package dev.monarchui.sample.sections.datadisplay

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import dev.monarchui.components.actions.button.MonarchButton
import dev.monarchui.components.charts.barlist.MonarchBarListSort
import dev.monarchui.components.datadisplay.leaderboard.MonarchLeaderboard
import dev.monarchui.components.datadisplay.leaderboard.MonarchLeaderboardEntry
import dev.monarchui.components.identity.avatar.MonarchAvatarShape
import dev.monarchui.components.typography.text.MonarchText
import dev.monarchui.foundation.MonarchTone
import dev.monarchui.sample.CatalogueCaption
import dev.monarchui.sample.CataloguePage

/** The catalogue page of Leaderboard. */
object MonarchLeaderboardSection : CataloguePage {
    override val title = "Leaderboard"

    private const val Total = 24

    private val agents = listOf(
        MonarchLeaderboardEntry(name = "Dev Patel", value = 28.0, change = -0.04, secondary = "Tier 1"),
        MonarchLeaderboardEntry(name = "Ana Lima", value = 42.0, change = 0.12, current = true, secondary = "Tier 2"),
        MonarchLeaderboardEntry(name = "Ben Okafor", value = 42.0, change = 0.0),
        MonarchLeaderboardEntry(
            name = "Chloé Martin",
            value = 31.0,
            change = 0.08,
            badge = "3 breached",
            badgeTone = MonarchTone.Danger,
        ),
    )

    private val teams = listOf(
        team(name = "Billing", hours = 4.2),
        team(name = "Support", hours = 6.8),
        team(name = "Sales", hours = 9.1),
    )

    private fun team(name: String, hours: Double) = MonarchLeaderboardEntry(
        name = name,
        value = hours,
        display = "$hours h",
        avatarShape = MonarchAvatarShape.Square,
    )

    @Composable
    override fun Content() {
        var selected by rememberSaveable { mutableStateOf("nobody") }
        var failed by rememberSaveable { mutableStateOf(true) }
        CatalogueCaption("Tickets solved this week, top three highlighted")
        MonarchLeaderboard(
            entries = agents,
            sort = MonarchBarListSort.Descending,
            label = "Top agents by tickets solved",
            valueLabel = "tickets solved",
            changeComparison = "vs last week",
            highlightTop = true,
            total = Total,
            onShowMore = { selected = "the full report" },
            onSelect = { selected = it.name },
        )
        MonarchText(text = "Opened: $selected")
        CatalogueCaption("Fastest first reply, smallest first")
        MonarchLeaderboard(entries = teams, sort = MonarchBarListSort.Ascending, valueLabel = "to first reply")
        CatalogueCaption("Loading and failure")
        MonarchLeaderboard(entries = emptyList(), loading = true, loadingRows = 2)
        if (failed) {
            MonarchLeaderboard(
                entries = emptyList(),
                errorMessage = "The ranking could not be loaded.",
                onRetry = { failed = false },
            )
        } else {
            MonarchButton(text = "Fail again", onClick = { failed = true })
        }
    }
}
