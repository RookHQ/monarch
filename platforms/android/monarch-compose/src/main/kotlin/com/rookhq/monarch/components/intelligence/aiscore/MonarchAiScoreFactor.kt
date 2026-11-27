package com.rookhq.monarch.components.intelligence.aiscore

import androidx.compose.runtime.Immutable

/**
 * One input that moved a `MonarchAiScore`.
 *
 * @property label what the input is, for example "Opened the last three emails".
 * @property impact the points it added (positive) or removed (negative).
 * @property detail a short explanation shown under the label.
 */
@Immutable
public class MonarchAiScoreFactor(
    public val label: String,
    public val impact: Double,
    public val detail: String? = null,
)
