package com.rookhq.monarch.components.intelligence.aieditproposal

import androidx.compose.runtime.Immutable

/**
 * One self-contained change in a `MonarchAiEditProposal`: a field, a
 * paragraph or a sentence.
 *
 * @property id a stable id, passed back through `onAcceptHunk` and `onRejectHunk`.
 * @property before the current text. Empty for an addition.
 * @property after the text the AI proposes. Empty for a removal.
 * @property label what the change is about, for example a field name. `null` names it "Change 2 of 3".
 * @property status the decision, when the app keeps it. `null` lets the component remember decisions itself.
 */
@Immutable
public class MonarchAiEditHunk(
    public val id: String,
    public val before: String,
    public val after: String,
    public val label: String? = null,
    public val status: MonarchAiEditHunkStatus? = null,
)
