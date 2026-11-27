/// Built-in strings of the switch settings list, from the
/// `MonarchSwitchSettingsList` strings table.
enum MonarchSwitchSettingsListStrings {
    /// Shown beside the spinner while a row saves.
    static var saving: String {
        MonarchStrings.string("monarch.switchSettingsList.saving", table: "MonarchSwitchSettingsList")
    }

    /// Shown and announced after a row saved.
    static var saved: String {
        MonarchStrings.string("monarch.switchSettingsList.saved", table: "MonarchSwitchSettingsList")
    }

    /// Shown and announced when a row could not save.
    static var error: String {
        MonarchStrings.string("monarch.switchSettingsList.error", table: "MonarchSwitchSettingsList")
    }

    /// The title of the retry button.
    static var retry: String {
        MonarchStrings.string("monarch.switchSettingsList.retry", table: "MonarchSwitchSettingsList")
    }

    /// The spoken name of the retry button of a row.
    static func retryLabel(_ label: String) -> String {
        MonarchStrings.string("monarch.switchSettingsList.retryLabel", table: "MonarchSwitchSettingsList", label)
    }
}
