package com.rookhq.monarch.components.datadisplay.periodselector

/** The limits of a custom period: [minDate] and [latest] are UTC midnight; [maxDays] is the longest range. */
internal class PeriodSelectorLimits(val minDate: Long?, val latest: Long, val maxDays: Int?)
