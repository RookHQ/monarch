package com.rookhq.monarch.components.intelligence.aicontextwindowmeter

import androidx.compose.runtime.Immutable

/**
 * One part of what fills the context window in a `MonarchAiContextWindowMeter`, such as the instructions, the
 * attached files or the earlier messages.
 *
 * @property label the name in the legend, already localised. Required, so a part is never told apart by colour.
 * @property tokens how many tokens the part takes. A negative count counts as zero.
 * @property id a stable key. `null` uses the position of the part.
 */
@Immutable
public class MonarchAiContextWindowPart(
    public val label: String,
    public val tokens: Long,
    public val id: String? = null,
)
