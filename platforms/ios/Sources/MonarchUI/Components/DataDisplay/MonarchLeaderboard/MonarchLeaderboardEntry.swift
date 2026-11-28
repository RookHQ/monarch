import Foundation

/// One ranked row of a ``MonarchLeaderboard``: who or what is ranked, the
/// figure it is ranked by, and optional context.
public struct MonarchLeaderboardEntry: Identifiable, Sendable, Hashable {
    /// A stable key.
    public var id: String
    /// Who or what is ranked: a person, team, campaign or product.
    public var name: String
    /// The figure the board ranks by.
    public var value: Double
    /// The value as shown, such as "4.2 h". `nil` writes `value` in the board's format.
    public var display: String?
    /// A second line, such as a team or "Step 2 of Spring launch".
    public var secondary: String?
    /// The avatar picture. Without it, initials from `name`.
    public var avatarURL: URL?
    /// A stable seed for the avatar's fallback colour, such as the person's id.
    public var seed: String?
    /// `circle` for people, `square` for teams, products and organisations.
    public var avatarShape: MonarchAvatarShape
    /// The change against the previous period as a ratio: 0.12 is up 12%.
    public var change: Double?
    /// A short tag after the name, such as "3 breached".
    public var badge: String?
    /// The tone of the tag.
    public var badgeTone: MonarchTone
    /// Whether this entry is the person viewing. The row is tinted and tagged "(you)".
    public var isCurrent: Bool

    /// An entry.
    ///
    /// - Parameters:
    ///   - name: Who or what is ranked, already localised.
    ///   - value: The figure the board ranks by.
    ///   - id: A stable key. By default the name.
    ///   - display: The value as shown. `nil` writes `value` in the board's format.
    ///   - secondary: A second line.
    ///   - avatarURL: The avatar picture. Without it, initials from `name`.
    ///   - seed: A stable seed for the avatar's fallback colour.
    ///   - avatarShape: `circle` for people, `square` for teams and things.
    ///   - change: The change against the previous period as a ratio.
    ///   - badge: A short tag after the name.
    ///   - badgeTone: The tone of the tag.
    ///   - isCurrent: Whether this entry is the person viewing.
    public init(
        _ name: String,
        value: Double,
        id: String? = nil,
        display: String? = nil,
        secondary: String? = nil,
        avatarURL: URL? = nil,
        seed: String? = nil,
        avatarShape: MonarchAvatarShape = .circle,
        change: Double? = nil,
        badge: String? = nil,
        badgeTone: MonarchTone = .neutral,
        isCurrent: Bool = false
    ) {
        self.id = id ?? name
        self.name = name
        self.value = value
        self.display = display
        self.secondary = secondary
        self.avatarURL = avatarURL
        self.seed = seed
        self.avatarShape = avatarShape
        self.change = change
        self.badge = badge
        self.badgeTone = badgeTone
        self.isCurrent = isCurrent
    }
}
