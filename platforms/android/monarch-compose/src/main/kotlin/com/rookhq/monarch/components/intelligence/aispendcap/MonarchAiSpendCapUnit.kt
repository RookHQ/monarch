package com.rookhq.monarch.components.intelligence.aispendcap

/** What the integer amounts of a `MonarchAiSpendCap` count. */
public enum class MonarchAiSpendCapUnit {
    /** The currency's minor unit: pence, cents; yen for a currency without one. */
    Minor,

    /** Millionths of the major unit: 12 400 000 is 12.40. For usage priced in fractions of a cent. */
    Micros,
}
