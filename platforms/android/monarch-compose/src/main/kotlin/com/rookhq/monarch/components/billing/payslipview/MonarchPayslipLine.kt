package com.rookhq.monarch.components.billing.payslipview

import androidx.compose.runtime.Immutable
import java.math.BigDecimal

/**
 * One calculated line of a `MonarchPayslipView`.
 *
 * @property id a stable key of the line.
 * @property name the pay component, such as "Basic" or "Income tax".
 * @property kind the group the line is shown in.
 * @property amount the amount in major units, as a positive number: deductions are not negated.
 * @property note how the amount was worked out, shown under the name: "22 of 22 days".
 */
@Immutable
public class MonarchPayslipLine(
    public val id: String,
    public val name: String,
    public val kind: MonarchPayslipLineKind,
    public val amount: BigDecimal,
    public val note: String? = null,
)
