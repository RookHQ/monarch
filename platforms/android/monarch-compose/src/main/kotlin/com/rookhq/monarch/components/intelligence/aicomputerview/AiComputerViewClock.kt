package com.rookhq.monarch.components.intelligence.aicomputerview

import java.util.Locale
import java.util.TimeZone

/** Writes the times of MonarchAiComputerView in one locale and zone. */
internal class AiComputerViewClock(val locale: Locale, val timeZone: TimeZone) {
    fun time(millis: Long): String = MonarchAiComputerViewTokens.time(millis, locale, timeZone)
}
