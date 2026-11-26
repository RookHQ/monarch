/// Where the save of one ``MonarchSwitchSettingsRow`` stands.
public enum MonarchSwitchSettingsStatus: Sendable, Hashable, CaseIterable {
    /// Nothing is saving; the switch shows the saved value.
    case idle
    /// The save is running; the switch already shows the new value.
    case saving
    /// The save finished. Shown for a short time, then back to idle.
    case saved
    /// The save failed; the switch is back at the saved value and a retry
    /// button repeats the value that failed.
    case error
}
