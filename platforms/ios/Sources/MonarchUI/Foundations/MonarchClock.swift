import SwiftUI

private struct MonarchNowKey: EnvironmentKey {
    static let defaultValue: Date? = nil
}

extension EnvironmentValues {
    /// A fixed "now" for components that read the current time, or `nil` for the real clock.
    var monarchNow: Date? {
        get { self[MonarchNowKey.self] }
        set { self[MonarchNowKey.self] = newValue }
    }
}

extension View {
    /// Pins the current time that Monarch components use for "today" highlights, the month shown first and
    /// relative times such as "3 days ago", instead of the real clock.
    ///
    /// Use it in screenshot tests, previews and documentation images so they do not change with the day.
    /// A component that takes its own `today` or `now` argument uses that argument first.
    ///
    /// ```swift
    /// MonarchMiniMonth(selection: $day)
    ///     .monarchNow(Date(timeIntervalSince1970: 1_793_750_400))
    /// ```
    public func monarchNow(_ date: Date?) -> some View {
        environment(\.monarchNow, date)
    }
}
