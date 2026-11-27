package com.rookhq.monarch.components.intelligence.airelatedentities

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.painter.Painter

/**
 * One entity the assistant found related to the current content.
 *
 * @property id a stable id, used by relations.
 * @property label the name shown, for example "Jordan Lee".
 * @property kind what it is; written in a badge.
 * @property weight how strongly it relates, from 0 to 1. Sorts the list, fills the relevance bar and sizes the
 * graph ring. `null` sorts after weighted entities.
 * @property image a picture for the avatar, loaded by the app. `null` shows the initials.
 */
@Immutable
public class MonarchAiRelatedEntity(
    public val id: String,
    public val label: String,
    public val kind: MonarchAiRelatedEntityKind,
    public val weight: Double? = null,
    public val image: Painter? = null,
)
