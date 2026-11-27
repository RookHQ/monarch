package com.rookhq.monarch.components.intelligence.aispendcap

import androidx.compose.runtime.Immutable

/**
 * The settings of an AI spending limit that a person can change; `onSave` of `MonarchAiSpendCap` receives them.
 *
 * @property limit the limit as an integer in the component's unit, or `null` for no limit.
 * @property warnAt the share of the limit, from 0 to 1, at which to warn.
 * @property behavior what happens when the limit is reached.
 */
@Immutable
public data class MonarchAiSpendCapSettings(
    public val limit: Long?,
    public val warnAt: Double,
    public val behavior: MonarchAiSpendCapBehavior,
)
