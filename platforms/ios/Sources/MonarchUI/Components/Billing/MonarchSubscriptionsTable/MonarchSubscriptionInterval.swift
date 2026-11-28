/// The unit of a subscription's billing period. A one-time charge has no
/// interval (`nil`).
public enum MonarchSubscriptionInterval: String, Sendable, Hashable, CaseIterable {
    /// Billed by the day.
    case day
    /// Billed by the week.
    case week
    /// Billed by the month.
    case month
    /// Billed by the year.
    case year
}
