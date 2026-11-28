package dev.monarchui.components.intelligence.aiautofillfield

/** Where a field that AI filled stands, driven by the app for `MonarchAiAutofillField`. */
public enum class MonarchAiAutofillState {
    /** AI filled the field and the person has not checked it: Accept and Clear are offered. */
    Suggested,

    /** The person accepted the value. */
    Accepted,

    /** The person changed the value; the original suggestion is shown. */
    Edited,

    /** The person cleared the value; only the control is shown. */
    Cleared,
}
