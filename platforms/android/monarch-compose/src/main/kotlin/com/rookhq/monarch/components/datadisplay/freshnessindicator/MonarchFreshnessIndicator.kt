package com.rookhq.monarch.components.datadisplay.freshnessindicator

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import com.rookhq.monarch.R
import com.rookhq.monarch.components.actions.button.MonarchButton
import com.rookhq.monarch.components.actions.button.MonarchButtonSize
import com.rookhq.monarch.components.actions.button.MonarchButtonVariant
import com.rookhq.monarch.internal.MonarchDefaultIcons
import com.rookhq.monarch.internal.MonarchLiveStatus
import com.rookhq.monarch.internal.MonarchShapeDefaults
import com.rookhq.monarch.internal.monarchString
import com.rookhq.monarch.internal.monarchSurface
import com.rookhq.monarch.internal.rememberMonarchRelativeClock
import com.rookhq.monarch.theme.MonarchTheme
import com.rookhq.monarch.tokens.MonarchTokens

/**
 * How fresh a data set or a sync is: "Updated 3 minutes ago", turning to
 * warning and danger as it ages, with an optional Refresh button.
 *
 * Built by Monarch from a `FlowRow` of a status dot, a `Text` and a ghost
 * `MonarchButton`: Material 3 has no equivalent. The status is worked out
 * from the age of [updatedAt] and the two limits, and worked out again every
 * minute with the relative time. The line is one TalkBack element with
 * commas between its parts; the dot is decoration, and "Out of date" or
 * "Very out of date" is written next to the time, so the colour is never the
 * only signal. A change to stale, expired or error, and the end of a refresh,
 * are announced through a polite live region.
 *
 * @param updatedAt when the data was last updated or synced, in epoch milliseconds (UTC). `null` means never.
 * @param modifier the modifier of the indicator.
 * @param variant a line of muted text, or a bordered bar.
 * @param status replaces the status worked out from the age, for example `Checking` or `Error`.
 * @param refreshing shows the refresh button as busy while a refresh the app runs is in progress. When it turns
 * `false`, "Refreshed." is announced, or "Refresh failed." in the error status.
 * @param staleAfterMillis the age after which the data is stale (warning). `null` never turns stale.
 * @param expiredAfterMillis the age after which the data is expired (danger). `null` never expires.
 * @param source names the data or its source, shown first: "Directory".
 * @param details further facts appended with middle dots: "Events on". Empty strings are left out.
 * @param error the error text for the error status, or the last error of a sync.
 * @param onRefresh adds a Refresh button. Not called while [refreshing].
 * @param labels replacements for the built-in strings.
 * @param actions further controls after the Refresh button, such as a link to the source's settings.
 */
@Composable
public fun MonarchFreshnessIndicator(
    updatedAt: Long?,
    modifier: Modifier = Modifier,
    variant: MonarchFreshnessIndicatorVariant = MonarchFreshnessIndicatorVariant.Inline,
    status: MonarchFreshnessStatus? = null,
    refreshing: Boolean = false,
    staleAfterMillis: Long? = null,
    expiredAfterMillis: Long? = null,
    source: String? = null,
    details: List<String> = emptyList(),
    error: String? = null,
    onRefresh: (() -> Unit)? = null,
    labels: MonarchFreshnessIndicatorLabels = MonarchFreshnessIndicatorLabels(),
    actions: (@Composable () -> Unit)? = null,
) {
    val themeColors = MonarchTheme.colors
    val now = minuteClock(running = updatedAt != null, times = listOf(updatedAt))
    val computed = MonarchFreshnessIndicatorTokens.status(updatedAt, now, staleAfterMillis, expiredAfterMillis)
    val resolved = status ?: computed
    val strings = freshnessStrings(labels, error)
    val time = updatedAt?.takeIf { MonarchFreshnessIndicatorTokens.showsTime(resolved, it) }
        ?.let { MonarchFreshnessIndicatorTokens.relativeTime(it, now) }
    val updated = time?.let {
        labels.updated?.invoke(it) ?: stringResource(R.string.monarch_freshnessindicator_updated, it)
    }
    val parts = freshnessParts(resolved, updated, strings) + details.filter { it.isNotEmpty() }.map { it to null }
    val all = listOfNotNull(source?.takeIf { it.isNotEmpty() }?.let { it to null }) + parts
    val flagColor = MonarchFreshnessIndicatorTokens.flagColor(resolved, themeColors)
    val line = buildAnnotatedString {
        all.forEachIndexed { index, (text, flagged) ->
            if (index > 0) append(MonarchFreshnessIndicatorTokens.SEPARATOR)
            if (index == 0 && source != null && source.isNotEmpty()) {
                val strong = FontWeight(MonarchTokens.FontWeight.semibold)
                withStyle(SpanStyle(color = themeColors.textDefault, fontWeight = strong)) {
                    append(text)
                }
            } else if (flagged == true) {
                withStyle(SpanStyle(color = flagColor, fontWeight = FontWeight(MonarchTokens.FontWeight.semibold))) {
                    append(text)
                }
            } else {
                append(text)
            }
        }
    }
    val spoken = all.joinToString(stringResource(R.string.monarch_freshnessindicator_spoken_separator)) { it.first }
    val announcement = freshnessAnnouncements(resolved, refreshing, strings)
    val colors = MonarchFreshnessIndicatorTokens.colors(variant, themeColors)
    Box(
        modifier = modifier
            .monarchSurface(colors, MonarchShapeDefaults.Container)
            .padding(MonarchFreshnessIndicatorTokens.padding(variant)),
    ) {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(MonarchFreshnessIndicatorTokens.ColumnGap),
            verticalArrangement = Arrangement.spacedBy(MonarchFreshnessIndicatorTokens.RowGap),
            itemVerticalAlignment = Alignment.CenterVertically,
        ) {
            FreshnessLine(line, spoken, MonarchFreshnessIndicatorTokens.dotColor(resolved, themeColors), colors.content)
            if (onRefresh != null) {
                MonarchButton(
                    text = strings.refresh,
                    onClick = onRefresh,
                    variant = MonarchButtonVariant.Ghost,
                    size = MonarchButtonSize.Sm,
                    leadingIcon = MonarchDefaultIcons.Retry,
                    loading = refreshing,
                    loadingLabel = strings.refreshing,
                )
            }
            actions?.invoke()
        }
        MonarchLiveStatus(text = announcement.takeIf { it.isNotEmpty() }, visible = false)
    }
}

@Composable
private fun FreshnessLine(
    line: AnnotatedString,
    spoken: String,
    dotColor: Color,
    textColor: Color,
) {
    Row(
        modifier = Modifier.clearAndSetSemantics { contentDescription = spoken },
        horizontalArrangement = Arrangement.spacedBy(MonarchFreshnessIndicatorTokens.DotGap),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val dot = MonarchFreshnessIndicatorTokens.DotSize * LocalDensity.current.fontScale
        Canvas(Modifier.size(dot)) { drawCircle(dotColor) }
        Text(text = line, style = MonarchTheme.typography.caption, color = textColor)
    }
}

/** The built-in strings, or their replacements. */
private class FreshnessStrings(
    val never: String,
    val checking: String,
    val stale: String,
    val expired: String,
    val error: String,
    val refresh: String,
    val refreshing: String,
    val refreshed: String,
    val refreshFailed: String,
)

@Composable
private fun freshnessStrings(labels: MonarchFreshnessIndicatorLabels, error: String?) = FreshnessStrings(
    never = monarchString(labels.never, R.string.monarch_freshnessindicator_never),
    checking = monarchString(labels.checking, R.string.monarch_freshnessindicator_checking),
    stale = monarchString(labels.stale, R.string.monarch_freshnessindicator_stale),
    expired = monarchString(labels.expired, R.string.monarch_freshnessindicator_expired),
    error = error ?: monarchString(labels.error, R.string.monarch_freshnessindicator_error),
    refresh = monarchString(labels.refresh, R.string.monarch_freshnessindicator_refresh),
    refreshing = monarchString(labels.refreshing, R.string.monarch_freshnessindicator_refreshing),
    refreshed = monarchString(labels.refreshed, R.string.monarch_freshnessindicator_refreshed),
    refreshFailed = monarchString(labels.refreshFailed, R.string.monarch_freshnessindicator_refresh_failed),
)

/** The parts of the line after the source; `true` marks a part drawn in the strong colour of the status. */
private fun freshnessParts(
    status: MonarchFreshnessStatus,
    updated: String?,
    strings: FreshnessStrings,
): List<Pair<String, Boolean?>> = when {
    status == MonarchFreshnessStatus.Checking -> listOf(strings.checking to null)
    updated == null && status == MonarchFreshnessStatus.Error -> listOf(strings.error to true)
    updated == null -> listOf(strings.never to null)
    else -> listOfNotNull(
        updated to null,
        when (status) {
            MonarchFreshnessStatus.Stale -> strings.stale to true
            MonarchFreshnessStatus.Expired -> strings.expired to true
            MonarchFreshnessStatus.Error -> strings.error to true
            else -> null
        },
    )
}

/**
 * The text of the polite live region: the new status after a change to
 * stale, expired or error, and the outcome when a refresh ends. Never set on
 * the first composition. The region is emptied for a frame first, so the
 * same words are announced again.
 */
@Composable
private fun freshnessAnnouncements(
    status: MonarchFreshnessStatus,
    refreshing: Boolean,
    strings: FreshnessStrings,
): String {
    var announcement by remember { mutableStateOf("") }
    val previousStatus = remember { mutableStateOf(status) }
    val wasRefreshing = remember { mutableStateOf(refreshing) }
    suspend fun announce(text: String) {
        announcement = ""
        withFrameNanos { }
        announcement = text
    }
    LaunchedEffect(status) {
        if (changed(previousStatus, status)) {
            when (status) {
                MonarchFreshnessStatus.Stale -> announce(strings.stale)
                MonarchFreshnessStatus.Expired -> announce(strings.expired)
                MonarchFreshnessStatus.Error -> announce(strings.error)
                else -> Unit
            }
        }
    }
    LaunchedEffect(refreshing) {
        if (changed(wasRefreshing, refreshing) && !refreshing) {
            announce(if (status == MonarchFreshnessStatus.Error) strings.refreshFailed else strings.refreshed)
        }
    }
    return announcement
}

/** Stores [value] and says whether it differs from the stored one. */
private fun <T> changed(previous: MutableState<T>, value: T): Boolean {
    val different = previous.value != value
    previous.value = value
    return different
}

/** The current time, redrawn as often as the freshest of [times] needs while [running], so relative times stay true. */
@Composable
private fun minuteClock(running: Boolean, times: List<Long?>): Long =
    rememberMonarchRelativeClock(times, running = running)
