package com.rookhq.monarch.components.workflow.actioneditor

/** Why a [MonarchRuleAction] is incomplete. */
public sealed interface MonarchActionProblem {
    /**
     * A required, visible parameter is empty.
     *
     * @property param the parameter's key.
     */
    public data class Missing(public val param: String) : MonarchActionProblem

    /**
     * A number is not a number, or is outside the parameter's min and max.
     *
     * @property param the parameter's key.
     */
    public data class OutOfRange(public val param: String) : MonarchActionProblem

    /**
     * The type's own validate returned a message.
     *
     * @property message the message, already localised by the app.
     */
    public data class Invalid(public val message: String) : MonarchActionProblem
}
