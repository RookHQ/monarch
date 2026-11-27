/// One line in the feature list of a ``MonarchPlanCard``.
///
/// A string literal is an included feature:
///
/// ```swift
/// let features: [MonarchPlanCardFeature] = ["Unlimited projects", .init("Single sign-on", isIncluded: false)]
/// ```
public struct MonarchPlanCardFeature: Sendable, Hashable, ExpressibleByStringLiteral {
    /// What the plan offers, such as "Unlimited projects". Already localised.
    public let label: String
    /// Short extra detail after the label, such as "up to 10 seats".
    public let hint: String?
    /// Whether the plan includes the feature. `false` lists it with a dash.
    public let isIncluded: Bool

    /// A feature.
    ///
    /// - Parameters:
    ///   - label: What the plan offers, already localised.
    ///   - hint: Short extra detail after the label.
    ///   - isIncluded: Whether the plan includes the feature.
    public init(_ label: String, hint: String? = nil, isIncluded: Bool = true) {
        self.label = label
        self.hint = hint
        self.isIncluded = isIncluded
    }

    /// An included feature from a string literal.
    public init(stringLiteral value: String) {
        self.init(value)
    }
}
