package com.rookhq.monarch.components.intelligence.aicomputerview

import androidx.compose.runtime.Immutable

/**
 * One thing the agent of a `MonarchAiComputerView` did.
 *
 * @property id a stable id, given to `onSelectAction`.
 * @property kind what kind of action it was.
 * @property label what happened, in plain words, such as "Clicked Sign in". Never put typed text here.
 * @property at when it happened, in epoch milliseconds.
 * @property point where on the screenshot it happened, in screenshot pixels.
 * @property detail more about it, such as the text typed or the address opened.
 * @property sensitive the action touched something private: its label and detail are never shown or announced, and
 * the screenshot taken after it is hidden.
 */
@Immutable
public data class MonarchAiComputerAction(
    public val id: String,
    public val kind: MonarchAiComputerActionKind,
    public val label: String,
    public val at: Long,
    public val point: MonarchAiComputerPoint? = null,
    public val detail: String? = null,
    public val sensitive: Boolean = false,
)
