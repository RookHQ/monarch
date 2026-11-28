package com.rookhq.monarch.components.workflow.goaltree

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.painter.Painter

/**
 * One goal in a `MonarchGoalTree`: an objective, a key result or any nested
 * target. Goals are passed flat; nesting comes from [parentId].
 *
 * @property id the stable id.
 * @property parentId the id of the parent goal. `null`, a missing goal or the goal's own id makes it a top-level goal.
 * @property title what the goal is, already localised.
 * @property progress progress as a fraction from 0 to 1. Values outside are clamped; not-a-number reads as 0.
 * @property status how the goal is doing.
 * @property ownerName the owner's name, shown as an avatar and read with the goal.
 * @property ownerImage the owner's photo, loaded by the app; `null` shows the initials of [ownerName].
 * @property meta short facts after the title, such as "weight 30% · due 31 Mar".
 * @property alignedTo a goal it supports elsewhere, shown as "Aligned to …".
 */
@Immutable
public class MonarchGoal(
    public val id: String,
    public val parentId: String? = null,
    public val title: String,
    public val progress: Float,
    public val status: MonarchGoalStatus,
    public val ownerName: String? = null,
    public val ownerImage: Painter? = null,
    public val meta: String? = null,
    public val alignedTo: String? = null,
)
