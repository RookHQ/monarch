package com.rookhq.monarch.components.billing.savingspanel

import androidx.compose.runtime.Immutable
import java.math.BigDecimal

/**
 * An amount in one currency in a `MonarchSavingsPanel`. Amounts in different
 * currencies are listed side by side, never converted or added.
 *
 * @property amount the amount in major units.
 * @property currencyCode the ISO 4217 code.
 */
@Immutable
public class MonarchSavingsAmount(
    public val amount: BigDecimal,
    public val currencyCode: String,
)
