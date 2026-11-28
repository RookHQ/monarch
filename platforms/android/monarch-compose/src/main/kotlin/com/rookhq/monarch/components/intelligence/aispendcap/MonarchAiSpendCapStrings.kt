package com.rookhq.monarch.components.intelligence.aispendcap

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.rookhq.monarch.R

/** The built-in or replaced text of a period in running text. */
@Composable
internal fun monarchAiSpendCapPeriod(period: MonarchAiSpendCapPeriod, labels: MonarchAiSpendCapLabels): String =
    labels.period?.invoke(period) ?: stringResource(
        when (period) {
            MonarchAiSpendCapPeriod.Day -> R.string.monarch_aispendcap_period_day
            MonarchAiSpendCapPeriod.Week -> R.string.monarch_aispendcap_period_week
            MonarchAiSpendCapPeriod.Month -> R.string.monarch_aispendcap_period_month
        },
    )

/** The built-in or replaced text of a state. */
@Composable
internal fun monarchAiSpendCapState(state: MonarchAiSpendCapState, labels: MonarchAiSpendCapLabels): String =
    labels.state?.invoke(state) ?: stringResource(
        when (state) {
            MonarchAiSpendCapState.Ok -> R.string.monarch_aispendcap_state_ok
            MonarchAiSpendCapState.Warning -> R.string.monarch_aispendcap_state_warning
            MonarchAiSpendCapState.Reached -> R.string.monarch_aispendcap_state_reached
            MonarchAiSpendCapState.NoLimit -> R.string.monarch_aispendcap_state_no_limit
        },
    )

/** The built-in or replaced one-line summary of a behavior. */
@Composable
internal fun monarchAiSpendCapSummary(behavior: MonarchAiSpendCapBehavior, labels: MonarchAiSpendCapLabels): String =
    labels.behaviorSummary?.invoke(behavior) ?: stringResource(
        if (behavior == MonarchAiSpendCapBehavior.Pause) {
            R.string.monarch_aispendcap_behavior_summary_pause
        } else {
            R.string.monarch_aispendcap_behavior_summary_notify
        },
    )

/** The built-in or replaced name of a behaviour choice. */
@Composable
internal fun monarchAiSpendCapOption(behavior: MonarchAiSpendCapBehavior, labels: MonarchAiSpendCapLabels): String =
    labels.behaviorOption?.invoke(behavior) ?: stringResource(
        if (behavior == MonarchAiSpendCapBehavior.Pause) {
            R.string.monarch_aispendcap_behavior_option_pause
        } else {
            R.string.monarch_aispendcap_behavior_option_notify
        },
    )

/** The built-in or replaced description of a behaviour choice. */
@Composable
internal fun monarchAiSpendCapDescription(
    behavior: MonarchAiSpendCapBehavior,
    labels: MonarchAiSpendCapLabels,
): String =
    labels.behaviorDescription?.invoke(behavior) ?: stringResource(
        if (behavior == MonarchAiSpendCapBehavior.Pause) {
            R.string.monarch_aispendcap_behavior_description_pause
        } else {
            R.string.monarch_aispendcap_behavior_description_notify
        },
    )

/** The amount line: spent of the limit, or spent without a limit, in the period. */
@Composable
internal fun monarchAiSpendCapAmount(
    spent: String,
    limit: String?,
    period: String,
    labels: MonarchAiSpendCapLabels,
): String = if (limit == null) {
    labels.spentNoLimit?.invoke(spent, period)
        ?: stringResource(R.string.monarch_aispendcap_spent_no_limit, spent, period)
} else {
    labels.spentOfLimit?.invoke(spent, limit, period)
        ?: stringResource(R.string.monarch_aispendcap_spent_of_limit, spent, limit, period)
}
