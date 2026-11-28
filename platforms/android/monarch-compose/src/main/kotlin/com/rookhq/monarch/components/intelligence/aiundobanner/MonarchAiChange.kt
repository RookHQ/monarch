package com.rookhq.monarch.components.intelligence.aiundobanner

import androidx.compose.runtime.Immutable

/**
 * One field the AI changed, shown in a `MonarchAiUndoBanner` as the label,
 * the old value struck through and the new value.
 *
 * @property label the field's name: "Status".
 * @property before the value before the change: "Open".
 * @property after the value the AI set: "Closed".
 */
@Immutable
public class MonarchAiChange(
    public val label: String,
    public val before: String,
    public val after: String,
)
