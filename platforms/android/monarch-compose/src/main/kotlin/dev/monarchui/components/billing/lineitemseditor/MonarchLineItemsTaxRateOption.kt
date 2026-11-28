package dev.monarchui.components.billing.lineitemseditor

import androidx.compose.runtime.Immutable
import java.math.BigDecimal

/**
 * A preset tax rate offered in the menu of an editable `MonarchLineItemsEditor` tax rate.
 *
 * @property value the rate as a percentage, such as 18 for 18%.
 * @property label the name in the menu, such as "GST 18%". Already localised.
 */
@Immutable
public class MonarchLineItemsTaxRateOption(
    public val value: BigDecimal,
    public val label: String,
)
