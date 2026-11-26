package com.rookhq.monarch.components.workflow.conditioneditor

/** Why a [MonarchCondition] is incomplete. */
public enum class MonarchConditionProblem {
    /** The field is missing or no longer in the list. */
    Field,

    /** The operator needs a value and there is none. */
    Value,

    /** A number field holds text that is not a number. */
    Number,

    /** A chosen option no longer exists. */
    Option,
}
