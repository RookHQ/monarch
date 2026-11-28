/// Built-in strings of the device list, from the `MonarchDeviceList` strings table.
enum MonarchDeviceListStrings {
    private static let table = "MonarchDeviceList"

    private static func string(_ key: String) -> String {
        MonarchStrings.string("monarch.deviceList.\(key)", table: table)
    }

    private static func string(_ key: String, _ argument: String) -> String {
        MonarchStrings.string("monarch.deviceList.\(key)", table: table, argument)
    }

    /// The card title.
    static var title: String { string("title") }
    /// The line under the title.
    static var description: String { string("description") }
    /// The spoken name of the list.
    static var listLabel: String { string("listLabel") }
    /// The name of a device without one.
    static var unnamed: String { string("unnamed") }
    /// The badge of the current device.
    static var current: String { string("current") }
    /// The badge of a blocked device.
    static var blocked: String { string("blocked") }
    /// Before the last-used time.
    static var lastUsed: String { string("lastUsed") }
    /// Before the first-seen time.
    static var firstSeen: String { string("firstSeen") }
    /// The remove button.
    static var remove: String { string("remove") }
    /// The spoken name of a row's remove button.
    static func removeLabel(_ name: String) -> String { string("removeLabel", name) }
    /// The confirmation title.
    static func confirmTitle(_ name: String) -> String { string("confirmTitle", name) }
    /// The confirmation message.
    static var confirmMessage: String { string("confirmMessage") }
    /// The cancel button.
    static var cancel: String { string("cancel") }
    /// Announced after a removal.
    static func removed(_ name: String) -> String { string("removed", name) }
    /// Shown when a removal fails.
    static func removeFailed(_ name: String) -> String { string("removeFailed", name) }
    /// Read while loading.
    static var loading: String { string("loading") }
    /// The empty state title.
    static var emptyTitle: String { string("emptyTitle") }
    /// The empty state message.
    static var emptyMessage: String { string("emptyMessage") }
    /// The error state title.
    static var errorTitle: String { string("errorTitle") }
}
