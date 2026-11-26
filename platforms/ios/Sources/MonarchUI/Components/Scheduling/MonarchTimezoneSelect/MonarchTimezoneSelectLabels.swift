/// Replacements for the built-in strings of ``MonarchTimezoneSelect``.
///
/// Every property is `nil` by default, which keeps the built-in string.
public struct MonarchTimezoneSelectLabels: Sendable {
    /// The heading of the detected zone. Built in: "Detected".
    public var detected: String?
    /// The heading of the pinned zones. Built in: "Pinned".
    public var pinned: String?
    /// The heading of the recent zones. Built in: "Recent".
    public var recent: String?
    /// The heading of every zone, when other sections are shown. Built in: "All time zones".
    public var all: String?
    /// Shown when no zone matches the search. Built in: "No time zones match".
    public var empty: String?
    /// The strings of the combobox itself: search, cancel, clear and the field.
    public var combobox: MonarchComboboxLabels

    /// Replacements for built-in strings. Pass only the ones to replace.
    ///
    /// - Parameters:
    ///   - detected: The heading of the detected zone.
    ///   - pinned: The heading of the pinned zones.
    ///   - recent: The heading of the recent zones.
    ///   - all: The heading of every zone.
    ///   - empty: Shown when no zone matches the search.
    ///   - combobox: The strings of the combobox itself.
    public init(
        detected: String? = nil,
        pinned: String? = nil,
        recent: String? = nil,
        all: String? = nil,
        empty: String? = nil,
        combobox: MonarchComboboxLabels = MonarchComboboxLabels()
    ) {
        self.detected = detected
        self.pinned = pinned
        self.recent = recent
        self.all = all
        self.empty = empty
        self.combobox = combobox
    }
}
