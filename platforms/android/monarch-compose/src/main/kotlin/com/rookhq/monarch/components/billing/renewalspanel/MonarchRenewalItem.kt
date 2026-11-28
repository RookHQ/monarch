package com.rookhq.monarch.components.billing.renewalspanel

import androidx.compose.runtime.Immutable
import java.math.BigDecimal

/**
 * One upcoming or past renewal in a `MonarchRenewalsPanel`: a subscription,
 * contract, licence or domain reaching the end of its term. Dates are epoch
 * milliseconds (UTC); amounts are in major units.
 *
 * @property id a stable key of the item.
 * @property name what renews, such as the agreement or asset name.
 * @property status the status key, looked up in the panel's registry: open, in_progress, renewed, lost, void.
 * @property renewalDateMillis the renewal or expiry date, or `null` when it is unknown.
 * @property description a line under the name, such as the type and the customer.
 * @property daysUntil whole days until the renewal, negative when past. `null` works it out from the panel's `now`.
 * @property noticeDeadlineMillis the last day to give notice, shown under the date instead of the day count.
 * @property amount what the renewal costs or is worth, or `null` when it is unpriced.
 * @property currencyCode the ISO 4217 code of [amount].
 * @property amountNote a short note after the amount, such as "per year" or "estimated".
 * @property autoRenew `true` "Auto-renews", `false` "Manual renewal", `null` says nothing.
 * @property probability the likelihood of renewing, 0 to 100.
 * @property owner who owns the renewal.
 * @property nextAction the next step, such as "Send a quote".
 * @property nextActionDateMillis when the next step is due.
 * @property nextActionOverdue the next step is overdue: written with an "Overdue" prefix in the danger colour.
 * @property outcome how a closed renewal ended, such as "Renewed for two years".
 */
@Immutable
public class MonarchRenewalItem(
    public val id: String,
    public val name: String,
    public val status: String,
    public val renewalDateMillis: Long?,
    public val description: String? = null,
    public val daysUntil: Int? = null,
    public val noticeDeadlineMillis: Long? = null,
    public val amount: BigDecimal? = null,
    public val currencyCode: String? = null,
    public val amountNote: String? = null,
    public val autoRenew: Boolean? = null,
    public val probability: Int? = null,
    public val owner: String? = null,
    public val nextAction: String? = null,
    public val nextActionDateMillis: Long? = null,
    public val nextActionOverdue: Boolean = false,
    public val outcome: String? = null,
)
