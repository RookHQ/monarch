package dev.monarchui.components.collaboration.decisioncard

import androidx.compose.runtime.Immutable
import dev.monarchui.foundation.MonarchTone

/**
 * One choice of a `MonarchDecisionCard`, such as Keep, Review or Let lapse.
 *
 * @property label the short label of the choice, also shown on the badge of the read-only card.
 * @property value the stored value, passed to `onDecide`.
 * @property description one line under the label.
 * @property note shown under the choices while this one is selected: what choosing it will do.
 * @property tone the tone of the badge on the read-only card.
 * @property reasonRequired whether this choice needs a reason. `null` follows the card's `reasonRequired`.
 * @property enabled whether the choice can be chosen.
 */
@Immutable
public class MonarchDecisionOption(
    public val label: String,
    public val value: String,
    public val description: String? = null,
    public val note: String? = null,
    public val tone: MonarchTone = MonarchTone.Neutral,
    public val reasonRequired: Boolean? = null,
    public val enabled: Boolean = true,
)
