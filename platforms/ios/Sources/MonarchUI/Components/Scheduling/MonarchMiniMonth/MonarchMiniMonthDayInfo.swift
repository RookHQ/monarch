/// Extra information for one day of a ``MonarchMiniMonth``: the dots under
/// the day number and what they mean.
///
/// ```swift
/// MonarchMiniMonthDayInfo(dotCount: 2, description: "2 events")
/// MonarchMiniMonthDayInfo(dots: [.success], description: "Available")
/// ```
///
/// Dots are decorative, so say in `description` what they mean.
public struct MonarchMiniMonthDayInfo: Sendable, Hashable {
    /// The tone of each dot. `nil` is the theme's accent.
    let tones: [MonarchTone?]

    /// What the dots mean, for people who cannot see them, such as
    /// "3 events" or "Available".
    public let description: String?

    /// Dots in the theme's accent colour, for events with no particular
    /// meaning.
    ///
    /// - Parameters:
    ///   - dotCount: The number of dots. At most three are drawn.
    ///   - description: What the dots mean, already localised.
    public init(dotCount: Int, description: String? = nil) {
        self.tones = Array(repeating: nil, count: max(0, dotCount))
        self.description = description
    }

    /// Dots in the colours of tones, such as success for an available day.
    ///
    /// - Parameters:
    ///   - dots: The tone of each dot, in order. At most three are drawn.
    ///   - description: What the dots mean, already localised.
    public init(dots: [MonarchTone], description: String? = nil) {
        self.tones = dots
        self.description = description
    }
}
