package com.rookhq.monarch.components.forms.textfield

/** Whether the label of a `MonarchTextField` is drawn. The same names as iOS. */
public enum class MonarchTextFieldLabelVisibility {
    /** The label floats into the outline, as in every Material text field. The default. */
    Visible,

    /**
     * The label is not drawn but stays the name TalkBack reads. For a field in
     * a row whose visible label sits next to it (a settings row, a table cell).
     */
    Hidden,
}
