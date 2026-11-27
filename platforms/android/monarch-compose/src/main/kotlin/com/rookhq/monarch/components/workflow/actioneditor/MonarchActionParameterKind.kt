package com.rookhq.monarch.components.workflow.actioneditor

/** The input of a [MonarchActionParameter]. */
public enum class MonarchActionParameterKind {
    /** One line of text. */
    Text,

    /** Several lines of text, such as a message. */
    TextArea,

    /** A number, with the number keyboard. */
    Number,

    /** One of the parameter's options, in a menu. */
    Select,

    /** Several of the parameter's options, in a sheet. */
    MultiSelect,

    /** Free values with the options as suggestions, in a sheet that can create values. */
    Tags,

    /** Yes or no, as a checkbox row. */
    Boolean,
}
