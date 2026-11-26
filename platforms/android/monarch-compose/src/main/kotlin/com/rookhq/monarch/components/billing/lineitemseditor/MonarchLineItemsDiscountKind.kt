package com.rookhq.monarch.components.billing.lineitemseditor

/** How a `MonarchLineItemsDiscount` is given. */
public enum class MonarchLineItemsDiscountKind {
    /** A percentage of the subtotal, such as 5 for 5%. Over 100 counts as 100. */
    Percent,

    /** A fixed amount in major units of the editor's currency. */
    Amount,
}
