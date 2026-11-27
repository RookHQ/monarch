package com.rookhq.monarch.components.billing.money

/** How a `MonarchMoney` writes the currency. The same names exist on every platform. */
public enum class MonarchMoneyCurrencyDisplay {
    /** The locale's symbol, for example US$ for US dollars in Canada. The default. */
    Symbol,

    /** The shortest symbol, for example $. Needs Android 11 (API 30); earlier versions write [Symbol]. */
    NarrowSymbol,

    /** The ISO 4217 code: USD. */
    Code,

    /** The currency's name in the locale's plural form: US dollars. */
    Name,
}
