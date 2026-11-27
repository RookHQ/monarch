package com.rookhq.monarch.components.intelligence.aisharedsession

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.painter.Painter

/**
 * One person or agent in a `MonarchAiSharedSession`.
 *
 * @property id a stable id: what `answering`, the queue and the callbacks refer to.
 * @property name the display name.
 * @property kind a person or an AI agent.
 * @property role what a person may do. Ignored for agents.
 * @property presence how present they are, drawn as the avatar's presence dot and always written in the list.
 * @property activity what they are doing right now, if anything.
 * @property you marks the person looking at this screen. Their role decides what they can do here.
 * @property image the picture, loaded by the app. `null` shows initials.
 */
@Immutable
public class MonarchAiSharedSessionParticipant(
    public val id: String,
    public val name: String,
    public val kind: MonarchAiSharedSessionKind = MonarchAiSharedSessionKind.Person,
    public val role: MonarchAiSharedSessionRole = MonarchAiSharedSessionRole.Editor,
    public val presence: MonarchAiSharedSessionPresence = MonarchAiSharedSessionPresence.Active,
    public val activity: MonarchAiSharedSessionActivity? = null,
    public val you: Boolean = false,
    public val image: Painter? = null,
)
