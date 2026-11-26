package com.rookhq.monarch.internal

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay

/** The intervals of the shared clock. */
internal object MonarchClock {
    /** One minute: the refresh of relative times ("5 minutes ago"). */
    const val MINUTE_MILLIS: Long = 60_000L

    /** One second: the refresh of a running timer or a call duration. */
    const val SECOND_MILLIS: Long = 1_000L

    /** How long to wait from [now] until the next whole [interval], so every row ticks at the same moment. */
    fun untilNextTick(now: Long, interval: Long): Long = interval - Math.floorMod(now, interval)
}

/**
 * The current time in epoch milliseconds, refreshed on each whole
 * [intervalMillis] while in composition: the one ticking clock for relative
 * times, timers and "live" labels.
 *
 * @param fixed a time to use instead of the clock, for tests, previews and screenshots. While it is set
 * nothing ticks.
 * @param intervalMillis how often the time is refreshed. A minute by default.
 * @param running whether the clock ticks. While `false` the last time stays, for example while nothing on
 * screen reads a relative time.
 * @param currentTime reads the clock. Replace it in tests.
 * @param intervalFor the interval for the time it is now, when it depends on the time, as for relative times
 * ([rememberMonarchRelativeClock]). Read before each tick; [intervalMillis] is used when it is `null`.
 */
@Composable
internal fun rememberMonarchClock(
    fixed: Long? = null,
    intervalMillis: Long = MonarchClock.MINUTE_MILLIS,
    running: Boolean = true,
    currentTime: () -> Long = System::currentTimeMillis,
    intervalFor: ((now: Long) -> Long)? = null,
): Long {
    var clock by remember { mutableLongStateOf(currentTime()) }
    val interval by rememberUpdatedState(intervalFor)
    LaunchedEffect(fixed, intervalMillis, running) {
        if (fixed != null || !running) return@LaunchedEffect
        clock = currentTime()
        while (true) {
            delay(MonarchClock.untilNextTick(clock, interval?.invoke(clock) ?: intervalMillis))
            clock = currentTime()
        }
    }
    return fixed ?: clock
}

/**
 * The clock for relative times ("30 seconds ago", "5 minutes ago"): it ticks as
 * often as the freshest of [times] needs ([MonarchRelativeTime.refreshMillis],
 * the web `live` interval), so a time in its first minute is redrawn every
 * 5 seconds and an old one every hour.
 *
 * @param times the times on screen, epoch milliseconds; `null` entries are skipped.
 * @param fixed a time to use instead of the clock, for tests, previews and screenshots.
 * @param running whether the clock ticks.
 * @param currentTime reads the clock. Replace it in tests.
 */
@Composable
internal fun rememberMonarchRelativeClock(
    times: List<Long?>,
    fixed: Long? = null,
    running: Boolean = true,
    currentTime: () -> Long = System::currentTimeMillis,
): Long {
    val current by rememberUpdatedState(times)
    return rememberMonarchClock(
        fixed = fixed,
        running = running,
        currentTime = currentTime,
        intervalFor = { now -> MonarchRelativeTime.refreshMillis(current, now) },
    )
}
