package com.rookhq.monarch.sample.sections.identity

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.rookhq.monarch.components.identity.devicelist.MonarchDevice
import com.rookhq.monarch.components.identity.devicelist.MonarchDeviceKind
import com.rookhq.monarch.components.identity.devicelist.MonarchDeviceList
import com.rookhq.monarch.components.identity.devicelist.MonarchDeviceListState
import com.rookhq.monarch.foundation.icon.MonarchIcon
import com.rookhq.monarch.sample.CatalogueCaption
import com.rookhq.monarch.sample.CataloguePage
import com.rookhq.monarch.sampleicons.SampleIcons
import java.io.IOException
import kotlinx.coroutines.delay

private const val HourMillis = 3_600_000L
private const val DayHours = 24L
private const val MonthDays = 90L
private const val SimulatedRequestMillis = 800L

private fun sampleDevices(now: Long) = listOf(
    MonarchDevice(
        id = "pixel",
        name = "Ana's Pixel",
        kind = MonarchDeviceKind.Mobile,
        os = "Android 16",
        browser = "Rook app",
        location = "Lisbon, Portugal",
        lastUsedMillis = now,
        firstSeenMillis = now - MonthDays * DayHours * HourMillis,
        current = true,
    ),
    MonarchDevice(
        id = "mac",
        name = "MacBook Pro",
        os = "macOS",
        browser = "Safari",
        location = "Porto, Portugal",
        lastUsedMillis = now - DayHours * HourMillis,
    ),
    MonarchDevice(
        id = "tablet",
        name = "Galaxy Tab",
        kind = MonarchDeviceKind.Tablet,
        os = "Android 14",
        lastUsedMillis = now - 2 * DayHours * HourMillis,
        blocked = true,
    ),
)

/** The catalogue page of DeviceList. */
object MonarchDeviceListSection : CataloguePage {
    override val title = "DeviceList"

    @Composable
    override fun Content() {
        val now = remember { System.currentTimeMillis() }
        var removed by rememberSaveable { mutableStateOf(setOf<String>()) }
        var attempts by rememberSaveable { mutableIntStateOf(0) }
        CatalogueCaption("Remove asks first; the first removal fails")
        MonarchDeviceList(
            devices = sampleDevices(now).filterNot { it.id in removed },
            now = now,
            onRemove = { id ->
                delay(SimulatedRequestMillis)
                if (attempts++ == 0) throw IOException("No connection")
                removed = removed + id
            },
            deviceIcon = { MonarchIcon(imageVector = SampleIcons.Devices, contentDescription = null) },
        )
        CatalogueCaption("Loading, empty and failed")
        MonarchDeviceList(devices = emptyList(), state = MonarchDeviceListState.Loading)
        MonarchDeviceList(devices = emptyList())
        MonarchDeviceList(devices = emptyList(), state = MonarchDeviceListState.Failed, onRetry = {})
    }
}
