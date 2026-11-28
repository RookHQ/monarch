package dev.monarchui.components.collaboration.activitytimeline

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.painter.Painter

/**
 * Who did something in a `MonarchActivityTimeline`.
 *
 * @property name the name, written in bold at the start of the event. Already localised.
 * @property image the avatar picture, loaded by the app. `null` shows initials.
 * @property kind a person, an automation or the system. Automations and the system get an "Automated" tag, so
 * readers can tell people from rules.
 * @property seed picks the placeholder colour of the avatar. `null` uses the name.
 */
@Immutable
public class MonarchActivityTimelineActor(
    public val name: String,
    public val image: Painter? = null,
    public val kind: MonarchActivityTimelineActorKind = MonarchActivityTimelineActorKind.Person,
    public val seed: String? = null,
)
