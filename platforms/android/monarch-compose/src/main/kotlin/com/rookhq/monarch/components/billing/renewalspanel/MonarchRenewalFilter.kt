package com.rookhq.monarch.components.billing.renewalspanel

import androidx.compose.runtime.Immutable

/**
 * A filter option of a `MonarchRenewalsPanel`, such as a time window or a
 * group of statuses. The app filters the items.
 *
 * @property value the value passed back when the option is chosen.
 * @property label the visible label, such as "Next 30 days".
 * @property count a count shown after the label, or `null` for none.
 */
@Immutable
public class MonarchRenewalFilter(
    public val value: String,
    public val label: String,
    public val count: Int? = null,
)
