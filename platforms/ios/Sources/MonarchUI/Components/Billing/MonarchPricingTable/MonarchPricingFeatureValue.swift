/// What one plan offers for one feature of a ``MonarchPricingTable``.
///
/// A Boolean literal is included or not included; a string literal is text:
///
/// ```swift
/// let values: [String: MonarchPricingFeatureValue] = ["free": false, "team": true, "business": "Unlimited"]
/// ```
public enum MonarchPricingFeatureValue: Sendable, Hashable, ExpressibleByBooleanLiteral, ExpressibleByStringLiteral {
    /// A check, read as "Included".
    case included
    /// A dash, read as "Not included". Also the value of a plan missing from `values`.
    case notIncluded
    /// Text shown as is, such as "10 seats" or "Email and chat".
    case text(String)

    /// `true` is included and `false` is not included.
    public init(booleanLiteral value: Bool) {
        self = value ? .included : .notIncluded
    }

    /// Text shown as is.
    public init(stringLiteral value: String) {
        self = .text(value)
    }
}
