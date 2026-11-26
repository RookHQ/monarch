package com.rookhq.monarch.components.intelligence.aiusagemeter

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.semantics
import com.rookhq.monarch.R
import com.rookhq.monarch.components.billing.usagemeter.MonarchUsageLevel
import com.rookhq.monarch.components.billing.usagemeter.MonarchUsageMeter
import com.rookhq.monarch.components.billing.usagemeter.MonarchUsageMeterItem
import com.rookhq.monarch.internal.MonarchLiveStatus
import com.rookhq.monarch.internal.monarchString
import java.text.NumberFormat

/**
 * How much AI the person has used against their quotas: one meter per scope
 * (this turn, the conversation, this month), in tokens or credits.
 *
 * A preset of `MonarchUsageMeter`, which draws each scope with a Material 3
 * `LinearProgressIndicator`, writes the figures with the unit, and writes
 * "Near the limit" or "Over the limit" so the level never depends on colour.
 * The meter is one TalkBack group named by [title], or "AI usage" without one.
 * When a scope reaches the warning share or goes over its limit while the
 * meter is on screen, the most serious one is announced once through a polite
 * live region; the state on first appearance is not announced.
 *
 * @param scopes the quotas, in order.
 * @param modifier the modifier of the meter.
 * @param title a visible heading, already localised.
 * @param period the period measured, shown under the title.
 * @param resetsAtMillis when the quotas reset, in epoch milliseconds (UTC), shown under the title.
 * @param warningAt the share of a limit at which a scope is near the limit.
 * @param loading shows a progress indicator and "Loading usage" instead of the scopes.
 * @param error a message shown instead of the scopes. It wins over [loading].
 * @param onRetry adds a "Try again" button under the error.
 * @param labels replaces any of the built-in strings, including those of `MonarchUsageMeter`.
 */
@Composable
public fun MonarchAiUsageMeter(
    scopes: List<MonarchAiUsageScope>,
    modifier: Modifier = Modifier,
    title: String? = null,
    period: String? = null,
    resetsAtMillis: Long? = null,
    warningAt: Double = 0.8,
    loading: Boolean = false,
    error: String? = null,
    onRetry: (() -> Unit)? = null,
    labels: MonarchAiUsageMeterLabels = MonarchAiUsageMeterLabels(),
) {
    val name = title ?: monarchString(labels.label, R.string.monarch_aiusagemeter_label)
    val items = scopes.mapIndexed { index, scope ->
        MonarchUsageMeterItem(
            label = scope.label,
            used = MonarchAiUsageMeterTokens.used(scope),
            limit = scope.limit,
            unit = unitText(scope, labels),
            id = scope.id ?: index.toString(),
        )
    }
    val announcement = thresholdAnnouncement(scopes, warningAt, labels, active = !loading && error == null)
    Box(
        modifier = modifier.semantics {
            isTraversalGroup = true
            contentDescription = name
        },
    ) {
        MonarchUsageMeter(
            items = items,
            modifier = Modifier.fillMaxWidth(),
            title = title,
            period = period,
            resetsAtMillis = resetsAtMillis,
            warningAt = warningAt,
            loading = loading,
            error = error,
            onRetry = onRetry,
            labels = labels.meter,
        )
        // Always present, so crossing a threshold is announced once as a change of this live region.
        MonarchLiveStatus(text = announcement.takeIf { it.isNotEmpty() }, visible = false)
    }
}

@Composable
private fun unitText(scope: MonarchAiUsageScope, labels: MonarchAiUsageMeterLabels): String {
    val count = MonarchAiUsageMeterTokens.unitCount(scope)
    labels.unit?.let { return it(scope.unit, count) }
    val resource = when (scope.unit) {
        MonarchAiUsageUnit.Tokens -> R.plurals.monarch_aiusagemeter_tokens
        MonarchAiUsageUnit.Credits -> R.plurals.monarch_aiusagemeter_credits
    }
    return pluralStringResource(resource, MonarchAiUsageMeterTokens.quantity(count))
}

/**
 * The text of the polite live region: the notice of the most serious scope, once the most serious state has
 * changed since the meter first appeared. Empty before that, and when no scope is near its limit.
 */
@Composable
private fun thresholdAnnouncement(
    scopes: List<MonarchAiUsageScope>,
    warningAt: Double,
    labels: MonarchAiUsageMeterLabels,
    active: Boolean,
): String {
    val worst = if (active) MonarchAiUsageMeterTokens.worst(scopes, warningAt) else null
    val level = worst?.let { MonarchAiUsageMeterTokens.level(it, warningAt) }
    val key = worst?.let { "${it.id ?: scopes.indexOf(it)}:${it.label}:$level" }
    val first = remember { key }
    var changed by remember { mutableStateOf(false) }
    LaunchedEffect(key) { if (key != first) changed = true }
    if (!changed || worst == null) return ""
    return when (level) {
        MonarchUsageLevel.Over ->
            labels.overLimit?.invoke(worst.label) ?: stringResource(
                R.string.monarch_aiusagemeter_over_limit,
                worst.label,
            )
        else -> {
            val locale = LocalConfiguration.current.locales[0]
            val share = MonarchAiUsageMeterTokens.used(worst) / (MonarchAiUsageMeterTokens.limit(worst) ?: 1.0)
            val percent = remember(locale) { NumberFormat.getPercentInstance(locale) }.format(share)
            labels.nearLimit?.invoke(worst.label, percent)
                ?: stringResource(R.string.monarch_aiusagemeter_near_limit, worst.label, percent)
        }
    }
}
