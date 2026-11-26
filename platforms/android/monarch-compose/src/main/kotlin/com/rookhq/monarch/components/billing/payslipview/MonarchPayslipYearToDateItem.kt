package com.rookhq.monarch.components.billing.payslipview

import androidx.compose.runtime.Immutable
import java.math.BigDecimal

/**
 * One year-to-date figure of a `MonarchPayslipView`, shown as a small tile.
 *
 * @property label what the figure is, such as "Gross" or "Tax paid".
 * @property amount the amount in major units.
 */
@Immutable
public class MonarchPayslipYearToDateItem(
    public val label: String,
    public val amount: BigDecimal,
)
