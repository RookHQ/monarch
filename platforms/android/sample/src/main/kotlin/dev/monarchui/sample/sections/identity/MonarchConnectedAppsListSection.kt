package dev.monarchui.sample.sections.identity

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import dev.monarchui.components.identity.connectedappslist.MonarchConnectedApp
import dev.monarchui.components.identity.connectedappslist.MonarchConnectedAppsList
import dev.monarchui.components.identity.connectedappslist.MonarchConnectedAppsListState
import dev.monarchui.sample.CatalogueCaption
import dev.monarchui.sample.CataloguePage
import java.io.IOException
import kotlinx.coroutines.delay

private const val HourMillis = 3_600_000L
private const val DaysAgo = 72L
private const val SimulatedRequestMillis = 800L

private fun sampleApps(now: Long) = listOf(
    MonarchConnectedApp(
        id = "notes",
        name = "Notes",
        domain = "notes.example.com",
        verified = true,
        workspace = "Acme",
        scopes = listOf("See your email address", "See your name and profile picture"),
        connectedAtMillis = now - DaysAgo * HourMillis,
        lastUsedAtMillis = now - 2 * HourMillis,
    ),
    MonarchConnectedApp(
        id = "boards",
        name = "Boards",
        domain = "boards.example.com",
        scopes = listOf("See your calendar"),
        connectedAtMillis = now - DaysAgo * HourMillis * 2,
    ),
    MonarchConnectedApp(id = "sheets", name = "Sheets", suspended = true),
)

/** The catalogue page of ConnectedAppsList. */
object MonarchConnectedAppsListSection : CataloguePage {
    override val title = "ConnectedAppsList"

    @Composable
    override fun Content() {
        val now = remember { System.currentTimeMillis() }
        var removed by rememberSaveable { mutableStateOf(setOf<String>()) }
        var attempts by rememberSaveable { mutableIntStateOf(0) }
        CatalogueCaption("Remove access asks first; removing Boards fails the first time")
        MonarchConnectedAppsList(
            apps = sampleApps(now).filterNot { it.id in removed },
            now = now,
            onRevoke = { app ->
                delay(SimulatedRequestMillis)
                if (app.id == "boards" && attempts++ == 0) throw IOException("The server did not answer. Try again.")
                removed = removed + app.id
            },
        )
        CatalogueCaption("Loading, empty and failed")
        MonarchConnectedAppsList(apps = emptyList(), state = MonarchConnectedAppsListState.Loading)
        MonarchConnectedAppsList(apps = emptyList())
        MonarchConnectedAppsList(apps = emptyList(), state = MonarchConnectedAppsListState.Error, onRetry = {})
    }
}
