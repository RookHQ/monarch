@file:OptIn(ExperimentalMonarchApi::class)

package dev.monarchui.sample.sections.identity

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import dev.monarchui.ExperimentalMonarchApi
import dev.monarchui.components.identity.devicelist.MonarchDeviceKind
import dev.monarchui.components.identity.sessionlist.MonarchSession
import dev.monarchui.components.identity.sessionlist.MonarchSessionList
import dev.monarchui.components.identity.sessionlist.MonarchSessionListState
import dev.monarchui.foundation.icon.MonarchIcon
import dev.monarchui.sample.CatalogueCaption
import dev.monarchui.sample.CataloguePage
import dev.monarchui.sampleicons.SampleIcons
import java.io.IOException
import kotlinx.coroutines.delay

private const val HourMillis = 3_600_000L
private const val DayHours = 24L
private const val SimulatedRequestMillis = 800L

private fun sampleSessions(now: Long) = listOf(
    MonarchSession(
        id = "this",
        label = "Rook app on Android",
        kind = MonarchDeviceKind.Mobile,
        detail = "Lisbon, Portugal",
        signedInMillis = now - 12 * DayHours * HourMillis,
        lastActiveMillis = now,
        current = true,
    ),
    MonarchSession(
        id = "mac",
        label = "Chrome on macOS",
        detail = "Porto, Portugal",
        signedInMillis = now - 40 * DayHours * HourMillis,
        lastActiveMillis = now - 2 * HourMillis,
    ),
    MonarchSession(
        id = "ipad",
        label = "Safari on iPad",
        kind = MonarchDeviceKind.Tablet,
        detail = "Madrid, Spain",
        signedInMillis = now - 3 * DayHours * HourMillis,
        lastActiveMillis = now - DayHours * HourMillis,
    ),
)

/** The catalogue page of SessionList. */
object MonarchSessionListSection : CataloguePage {
    override val title = "SessionList"

    @Composable
    override fun Content() {
        val now = remember { System.currentTimeMillis() }
        var signedOut by rememberSaveable { mutableStateOf(setOf<String>()) }
        var attempts by rememberSaveable { mutableIntStateOf(0) }
        CatalogueCaption("Sign out asks first; the first sign-out fails")
        MonarchSessionList(
            sessions = sampleSessions(now).filterNot { it.id in signedOut },
            now = now,
            footnote = "Apps you keep open stay signed in for 30 days.",
            onSignOut = { id ->
                delay(SimulatedRequestMillis)
                if (attempts++ == 0) throw IOException("No connection")
                signedOut = signedOut + id
            },
            onSignOutOthers = { ids ->
                delay(SimulatedRequestMillis)
                signedOut = signedOut + ids
            },
            sessionIcon = { MonarchIcon(imageVector = SampleIcons.Devices, contentDescription = null) },
        )
        CatalogueCaption("Loading, empty and failed")
        MonarchSessionList(sessions = emptyList(), state = MonarchSessionListState.Loading)
        MonarchSessionList(sessions = emptyList())
        MonarchSessionList(sessions = emptyList(), state = MonarchSessionListState.Failed, onRetry = {})
    }
}
