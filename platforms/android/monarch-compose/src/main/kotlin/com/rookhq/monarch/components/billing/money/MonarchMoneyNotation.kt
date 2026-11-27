package com.rookhq.monarch.components.billing.money

/** How a `MonarchMoney` writes its digits. The same names exist on every platform. */
public enum class MonarchMoneyNotation {
    /** Every digit: $1,250,000.00. The default. */
    Standard,

    /** The short form of the locale: $1.3M. TalkBack reads the full amount. */
    Compact,
}
