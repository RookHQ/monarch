package dev.monarchui.components.billing.payslipview

import androidx.compose.runtime.Immutable

/**
 * A label and a value in the details of a `MonarchPayslipView`: employee,
 * employee number, period, paid days.
 *
 * @property label the term, such as "Pay period".
 * @property value the value, already formatted.
 */
@Immutable
public class MonarchPayslipDetail(
    public val label: String,
    public val value: String,
)
