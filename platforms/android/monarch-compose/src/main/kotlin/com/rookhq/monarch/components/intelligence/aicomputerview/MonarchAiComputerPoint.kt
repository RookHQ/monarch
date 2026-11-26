package com.rookhq.monarch.components.intelligence.aicomputerview

import androidx.compose.runtime.Immutable

/**
 * A position on the screenshot of a `MonarchAiComputerView`, in the
 * screenshot's pixels from its top-left corner, in every layout direction.
 *
 * @property x pixels from the left edge.
 * @property y pixels from the top edge.
 */
@Immutable
public data class MonarchAiComputerPoint(
    public val x: Int,
    public val y: Int,
)
