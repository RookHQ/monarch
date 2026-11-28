package com.rookhq.monarch.components.intelligence.aispendcap

/** Where AI spending stands against its limit. */
public enum class MonarchAiSpendCapState {
    /** Below the warning share. */
    Ok,

    /** At or above the warning share and below the limit. */
    Warning,

    /** At or over the limit. A limit of zero is always reached. */
    Reached,

    /** No limit is set. */
    NoLimit,
}
