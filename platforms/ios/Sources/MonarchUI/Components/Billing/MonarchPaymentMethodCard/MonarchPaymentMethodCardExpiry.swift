import Foundation

/// Whether a saved card can still be charged.
public enum MonarchPaymentMethodCardExpiry: Sendable, Hashable, CaseIterable {
    /// The card is valid beyond next month.
    case valid
    /// The card expires this month or within the next `soonMonths` months.
    case expiring
    /// The card's expiry month has passed.
    case expired

    /// The status of a card that is valid to the end of its expiry month.
    ///
    /// - Parameters:
    ///   - month: The expiry month, 1 to 12.
    ///   - year: The expiry year, with two or four digits.
    ///   - now: Today.
    ///   - soonMonths: How many months ahead count as expiring soon. One
    ///     means this month or next.
    ///   - calendar: The calendar that gives today's month. Gregorian by default.
    /// - Returns: The status.
    public static func status(
        month: Int,
        year: Int,
        now: Date = .now,
        soonMonths: Int = 1,
        calendar: Calendar = Calendar(identifier: .gregorian)
    ) -> MonarchPaymentMethodCardExpiry {
        let fullYear = year < 100 ? 2_000 + year : year
        let expiry = fullYear * 12 + (month - 1)
        let today = calendar.dateComponents([.year, .month], from: now)
        let current = (today.year ?? 0) * 12 + ((today.month ?? 1) - 1)
        if expiry < current { return .expired }
        if expiry - current <= soonMonths { return .expiring }
        return .valid
    }
}
