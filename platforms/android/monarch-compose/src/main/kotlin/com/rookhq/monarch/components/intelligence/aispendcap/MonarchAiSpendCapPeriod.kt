package com.rookhq.monarch.components.intelligence.aispendcap

/** The window an AI spending limit applies to; spending starts again from zero each period. */
public enum class MonarchAiSpendCapPeriod {
    /** One day. */
    Day,

    /** One week. */
    Week,

    /** One month. */
    Month,
}
