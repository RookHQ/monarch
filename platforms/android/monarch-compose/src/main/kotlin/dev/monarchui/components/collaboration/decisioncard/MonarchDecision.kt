package dev.monarchui.components.collaboration.decisioncard

import androidx.compose.runtime.Immutable

/**
 * What a `MonarchDecisionCard` passes to `onDecide`.
 *
 * @property value the chosen value, or `null` for "No decision".
 * @property reason the reason without leading and trailing white space, or `null` when it is empty.
 */
@Immutable
public class MonarchDecision(public val value: String?, public val reason: String? = null) {
    override fun equals(other: Any?): Boolean =
        other is MonarchDecision && other.value == value && other.reason == reason

    override fun hashCode(): Int = 31 * value.hashCode() + reason.hashCode()

    override fun toString(): String = "MonarchDecision(value=$value, reason=$reason)"
}
