package dev.monarchui.components.billing.renewalspanel

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import dev.monarchui.R
import dev.monarchui.components.datadisplay.statusbadge.MonarchStatusDefinition
import dev.monarchui.components.datadisplay.statusbadge.MonarchStatusRegistry
import dev.monarchui.foundation.MonarchTone

/** The defaults of `MonarchRenewalsPanel`. */
public object MonarchRenewalsPanelDefaults {
    /** Open renewals this many days away or fewer are marked "Due soon". */
    public const val URGENT_WITHIN_DAYS: Int = 14

    /**
     * The statuses a renewals workflow usually needs, with localised labels:
     * `open`, `in_progress`, `renewed`, `lost` and `void`. Only `open` and
     * `in_progress` count as open, so only they are marked "Due soon". Add to
     * them with `statuses().merging(…)`.
     */
    @Composable
    public fun statuses(): MonarchStatusRegistry = MonarchStatusRegistry(
        mapOf(
            "open" to MonarchStatusDefinition(
                stringResource(R.string.monarch_renewalspanel_status_open),
                MonarchTone.Info,
            ),
            "in_progress" to MonarchStatusDefinition(
                stringResource(R.string.monarch_renewalspanel_status_in_progress),
                MonarchTone.Warning,
            ),
            "renewed" to MonarchStatusDefinition(
                stringResource(R.string.monarch_renewalspanel_status_renewed),
                MonarchTone.Success,
            ),
            "lost" to MonarchStatusDefinition(
                stringResource(R.string.monarch_renewalspanel_status_lost),
                MonarchTone.Danger,
            ),
            "void" to MonarchStatusDefinition(
                stringResource(R.string.monarch_renewalspanel_status_void),
                MonarchTone.Neutral,
            ),
        ),
    )
}
