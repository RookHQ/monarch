package com.rookhq.monarch.components.forms.formfield

/** Where a `MonarchFormField` puts its label relative to the control. */
public enum class MonarchFormFieldLayout {
    /** The label above the control, the help or error text under it. The default; for text fields and pickers. */
    Stack,

    /** The label and help text at the start, the control at the end, the error under both. For a switch. */
    Row,
}
