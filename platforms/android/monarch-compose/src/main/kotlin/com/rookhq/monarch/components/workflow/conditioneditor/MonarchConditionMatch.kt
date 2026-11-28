package com.rookhq.monarch.components.workflow.conditioneditor

/** Whether every condition must hold, or at least one. */
public enum class MonarchConditionMatch {
    /** Every condition must hold. */
    All,

    /** At least one condition must hold. */
    Any,
}
