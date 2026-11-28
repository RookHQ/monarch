package dev.monarchui.components.forms.switchsettingslist

/** Where the save of one `MonarchSwitchSettingsRow` is. */
public enum class MonarchSwitchSettingsStatus {
    /** No save in progress; the switch shows the saved value. */
    Idle,

    /** A save is running; the switch already shows the new value. */
    Saving,

    /** The last save succeeded; "Saved" shows for a short time. */
    Saved,

    /** The last save failed; the switch shows the saved value and the error offers a retry. */
    Error,
}
