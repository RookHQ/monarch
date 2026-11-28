package com.rookhq.monarch.components.workflow.approvaltimeline

import androidx.compose.ui.unit.Dp
import java.util.Locale
import java.util.TimeZone

/** What every step of one timeline shares. */
internal class MonarchApprovalTimelineContext(
    val now: Long,
    val absoluteAfterMillis: Long,
    val zone: TimeZone,
    val locale: Locale,
    val showAvatars: Boolean,
    val markerSize: Dp,
    val strings: MonarchApprovalTimelineStrings,
)
