package com.rookhq.monarch.components.workflow.conditioneditor

/** The kind of value a [MonarchConditionField] holds; it decides the operators and the value input. */
public enum class MonarchConditionFieldType {
    /** Free text: a text field. */
    Text,

    /** A number: a text field with the number keyboard. */
    Number,

    /** A calendar day: a button that opens the Material 3 date picker. */
    Date,

    /** One of the field's options: a menu. */
    Select,

    /** Several of the field's options: a checklist in a sheet. */
    MultiSelect,

    /** Free values, with the field's options as suggestions: a checklist in a sheet that can create values. */
    Tags,

    /** Yes or no: the operator alone ("is yes", "is no"). */
    Boolean,
}
