package dev.monarchui.components.billing.subscriptionstable

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import dev.monarchui.R
import dev.monarchui.components.datadisplay.statusbadge.MonarchStatusDefinition
import dev.monarchui.components.datadisplay.statusbadge.MonarchStatusRegistry
import dev.monarchui.foundation.MonarchTone

/** The defaults of `MonarchSubscriptionsTable`. */
public object MonarchSubscriptionsTableDefaults {
    /**
     * The statuses a recurring-billing product usually needs, with localised
     * labels: `draft`, `trial`, `active`, `past_due`, `paused`, `cancelled`
     * and `expired`. Add to them with `statuses().merging(…)`.
     */
    @Composable
    public fun statuses(): MonarchStatusRegistry = MonarchStatusRegistry(
        mapOf(
            "draft" to MonarchStatusDefinition(
                stringResource(R.string.monarch_subscriptionstable_status_draft),
                MonarchTone.Neutral,
            ),
            "trial" to MonarchStatusDefinition(
                stringResource(R.string.monarch_subscriptionstable_status_trial),
                MonarchTone.Info,
            ),
            "active" to MonarchStatusDefinition(
                stringResource(R.string.monarch_subscriptionstable_status_active),
                MonarchTone.Success,
            ),
            "past_due" to MonarchStatusDefinition(
                stringResource(R.string.monarch_subscriptionstable_status_past_due),
                MonarchTone.Warning,
            ),
            "paused" to MonarchStatusDefinition(
                stringResource(R.string.monarch_subscriptionstable_status_paused),
                MonarchTone.Neutral,
            ),
            "cancelled" to MonarchStatusDefinition(
                stringResource(R.string.monarch_subscriptionstable_status_cancelled),
                MonarchTone.Danger,
            ),
            "expired" to MonarchStatusDefinition(
                stringResource(R.string.monarch_subscriptionstable_status_expired),
                MonarchTone.Neutral,
            ),
        ),
    )
}
