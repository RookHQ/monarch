package com.rookhq.monarch.components.billing.money

/** When a `MonarchMoney` writes a sign. The same names exist on every platform. */
public enum class MonarchMoneySignDisplay {
    /** A minus sign for negative amounts only. The default. */
    Automatic,

    /** A plus sign for zero and positive amounts, a minus sign for negative ones. */
    Always,

    /** A plus sign for gains and a minus sign for losses; zero has no sign. */
    ExceptZero,

    /** No sign at all: the magnitude only. */
    Never,
}
