package dev.monarchui.components.billing.renewalspanel

import androidx.compose.runtime.Immutable
import java.math.BigDecimal

/**
 * A total of the renewals in view in one currency, shown above the list of a
 * `MonarchRenewalsPanel`. Amounts are never converted or added across currencies.
 *
 * @property currencyCode the ISO 4217 code of the total.
 * @property amount the sum of the priced items in this currency, in major units.
 * @property count the number of items in the total.
 * @property estimated the part of the total that is estimated rather than firm.
 */
@Immutable
public class MonarchRenewalTotal(
    public val currencyCode: String,
    public val amount: BigDecimal,
    public val count: Int,
    public val estimated: BigDecimal? = null,
)
