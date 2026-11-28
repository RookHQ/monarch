package dev.monarchui.components.intelligence.aihandoff

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.painter.Painter
import dev.monarchui.components.intelligence.aiconversation.MonarchAiAgentRef

/** Who takes the work in a `MonarchAiHandoff`: another agent, or a person. */
@Immutable
public sealed interface MonarchAiHandoffTarget {
    /** The display name of the target. */
    public val name: String

    /** The picture of the target, loaded by the app. `null` shows initials. */
    public val image: Painter?

    /**
     * Another AI agent takes the work. Shown as a square agent avatar.
     *
     * @property agent the agent from the conversation model.
     */
    @Immutable
    public class Agent(
        public val agent: MonarchAiAgentRef,
        override val image: Painter? = null,
    ) : MonarchAiHandoffTarget {
        override val name: String get() = agent.name
    }

    /**
     * A person takes the work. Shown as a round avatar.
     *
     * @property seed picks the fallback colour of the avatar; the name by default.
     */
    @Immutable
    public class Person(
        override val name: String,
        override val image: Painter? = null,
        public val seed: String? = null,
    ) : MonarchAiHandoffTarget
}
