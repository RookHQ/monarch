import Foundation

/// A subscription's price normalised to one month in the reporting
/// currency (monthly recurring revenue), as the billing system worked it out.
public enum MonarchSubscriptionMonthly: Sendable, Hashable {
    /// The monthly amount, in major units of `currencyCode`.
    case amount(Decimal, currencyCode: String)
    /// No exchange rate was available, so the subscription is left out of
    /// the monthly total. Shown as "Not converted" for an active or past-due
    /// subscription, and as a dash otherwise.
    case notConverted
}
