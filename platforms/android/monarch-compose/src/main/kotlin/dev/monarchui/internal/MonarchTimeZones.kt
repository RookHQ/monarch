package dev.monarchui.internal

import android.content.pm.ApplicationInfo
import android.icu.util.TimeZone
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import java.util.concurrent.ConcurrentHashMap

/**
 * The device's current time zone as an ICU zone.
 *
 * `android.icu.util.TimeZone.getDefault()` caches the zone the first time it
 * is read, so it can stay stale after the person changes zone, and in tests it
 * ignores `java.util.TimeZone.setDefault`. Reading the JVM default each time
 * avoids both.
 */
internal fun currentIcuTimeZone(): TimeZone = TimeZone.getTimeZone(java.util.TimeZone.getDefault().id)

/**
 * Turns the `timeZone: String?` parameter of a component into a zone.
 *
 * Components take an IANA zone id such as `"Europe/Berlin"`, as the web does
 * and as persisted values do; `null` means the device zone, read each time it
 * is resolved, so a change of zone shows on the next composition. An id that
 * Java does not know falls back to the device zone too, and is reported once
 * per id in a debuggable app (see [rememberZoneReporter]).
 */
internal object MonarchTimeZones {
    /** The log tag of the invalid-id report. */
    const val TAG: String = "MonarchTimeZone"

    private const val GMT = "GMT"

    private val reported: MutableSet<String> = ConcurrentHashMap.newKeySet()

    /**
     * Whether [id] names a zone. `java.util.TimeZone.getTimeZone` answers GMT
     * for an id it does not know, so anything that comes back as GMT without
     * asking for it is unknown. Custom ids such as `GMT+05:30` are valid.
     */
    fun isValid(id: String): Boolean {
        if (id.isBlank()) return false
        val zone = java.util.TimeZone.getTimeZone(id)
        return zone.id != GMT || id == GMT
    }

    /** The zone [id] names, or the device zone when [id] is `null` or not valid. [onInvalid] hears an invalid id. */
    fun java(id: String?, onInvalid: (String) -> Unit = {}): java.util.TimeZone = when {
        id == null -> java.util.TimeZone.getDefault()
        isValid(id) -> java.util.TimeZone.getTimeZone(id)
        else -> {
            onInvalid(id)
            java.util.TimeZone.getDefault()
        }
    }

    /** As [java], as an ICU zone. */
    fun icu(id: String?, onInvalid: (String) -> Unit = {}): TimeZone = when {
        id == null -> currentIcuTimeZone()
        isValid(id) -> TimeZone.getTimeZone(java.util.TimeZone.getTimeZone(id).id)
        else -> {
            onInvalid(id)
            currentIcuTimeZone()
        }
    }

    /** Logs [id] as invalid the first time it is seen in this process. */
    fun report(id: String) {
        if (reported.add(id)) {
            Log.w(TAG, "Unknown time zone id \"$id\"; using the device time zone. Pass an IANA id like Europe/Berlin.")
        }
    }

    /** Forgets what was reported, for tests. */
    fun resetReports() {
        reported.clear()
    }
}

/**
 * The reporter components pass to [MonarchTimeZones]: it logs an invalid id in
 * a debuggable app and does nothing in a release build.
 */
@Composable
internal fun rememberZoneReporter(): (String) -> Unit {
    val debuggable = LocalContext.current.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0
    return if (debuggable) MonarchTimeZones::report else NoReport
}

private val NoReport: (String) -> Unit = {}

/** The java zone of a component's `timeZone` parameter; see [MonarchTimeZones]. */
@Composable
internal fun monarchTimeZone(id: String?): java.util.TimeZone = MonarchTimeZones.java(id, rememberZoneReporter())

/** The ICU zone of a component's `timeZone` parameter; see [MonarchTimeZones]. */
@Composable
internal fun monarchIcuTimeZone(id: String?): TimeZone = MonarchTimeZones.icu(id, rememberZoneReporter())
