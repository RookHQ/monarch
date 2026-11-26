/// The built-in strings of a ``MonarchUsageMeter``. Pass your own to replace
/// any of them; the rest stay the localised defaults.
///
/// ```swift
/// MonarchUsageMeterLabels(nearLimit: "Almost used up")
/// ```
public struct MonarchUsageMeterLabels: Sendable {
    /// The figures of a limited item from the formatted amounts: "8,200 of 10,000".
    public var amount: @Sendable (_ used: String, _ limit: String) -> String
    /// The share used from the formatted percentage: "82% used".
    public var percent: @Sendable (_ percent: String) -> String
    /// Shown and spoken when an item is at or above the warning level.
    public var nearLimit: String
    /// Shown and spoken when an item is above its limit.
    public var overLimit: String
    /// The reset line from the formatted date: "Resets 1 Dec 2026".
    public var resets: @Sendable (_ date: String) -> String
    /// Announced when an item reaches a level, from the item's label and the level text.
    public var announcement: @Sendable (_ label: String, _ level: String) -> String
    /// Shown beside the spinner while loading.
    public var loading: String
    /// The title of the retry button.
    public var retry: String
    /// Replaces the status line under the bar of a limited item, from the item's label, whether it is over its
    /// limit and the formatted share used. It returns the whole line, an empty string to show no line, or `nil` to
    /// keep the built-in "Near the limit · 82% used". Called only for items at or above the warning level and for
    /// items below it (`isNearOrOver` is `false` for those).
    public var statusLine:
        (@Sendable (_ label: String, _ isNearOrOver: Bool, _ isOver: Bool, _ percent: String) -> String?)?

    /// The labels. Every parameter that is `nil` uses the built-in string.
    ///
    /// - Parameters:
    ///   - amount: The figures of a limited item from the formatted amounts.
    ///   - percent: The share used from the formatted percentage.
    ///   - nearLimit: Shown and spoken when an item is at or above the warning level.
    ///   - overLimit: Shown and spoken when an item is above its limit.
    ///   - resets: The reset line from the formatted date.
    ///   - announcement: Announced when an item reaches a level.
    ///   - loading: Shown beside the spinner while loading.
    ///   - retry: The title of the retry button.
    public init(
        amount: (@Sendable (_ used: String, _ limit: String) -> String)? = nil,
        percent: (@Sendable (_ percent: String) -> String)? = nil,
        nearLimit: String? = nil,
        overLimit: String? = nil,
        resets: (@Sendable (_ date: String) -> String)? = nil,
        announcement: (@Sendable (_ label: String, _ level: String) -> String)? = nil,
        loading: String? = nil,
        retry: String? = nil
    ) {
        self.amount = amount ?? { MonarchUsageMeterStrings.amount(used: $0, limit: $1) }
        self.percent = percent ?? { MonarchUsageMeterStrings.percent($0) }
        self.nearLimit = nearLimit ?? MonarchUsageMeterStrings.nearLimit
        self.overLimit = overLimit ?? MonarchUsageMeterStrings.overLimit
        self.resets = resets ?? { MonarchUsageMeterStrings.resets($0) }
        self.announcement = announcement ?? { MonarchUsageMeterStrings.announcement(label: $0, level: $1) }
        self.loading = loading ?? MonarchUsageMeterStrings.loading
        self.retry = retry ?? MonarchUsageMeterStrings.retry
    }
}
