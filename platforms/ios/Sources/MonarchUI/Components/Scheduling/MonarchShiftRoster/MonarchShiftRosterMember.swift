import SwiftUI

/// One person in a ``MonarchShiftRoster``.
///
/// ```swift
/// MonarchShiftRosterMember(id: "a", name: "Alex Kim", description: "Support")
/// ```
@_spi(Experimental) public struct MonarchShiftRosterMember: Identifiable {
    /// A stable identifier, unique in the roster. Assignments are keyed by it.
    public let id: String
    /// The person's name, already localised. Shown and used for the avatar's initials.
    public var name: String
    /// A second, muted line: a team, a role or an employee code.
    public var description: String?
    /// The person's picture, loaded by the app. Takes precedence over `avatarURL`.
    public var image: Image?
    /// The address of the person's picture, loaded with `AsyncImage`.
    public var avatarURL: URL?

    /// A person.
    ///
    /// - Parameters:
    ///   - id: A stable identifier, unique in the roster.
    ///   - name: The person's name, already localised.
    ///   - description: A second, muted line: a team, a role or an employee code.
    ///   - image: The person's picture, loaded by the app. `nil` shows the initials.
    ///   - avatarURL: The address of the picture, used when `image` is `nil`.
    public init(id: String, name: String, description: String? = nil, image: Image? = nil, avatarURL: URL? = nil) {
        self.id = id
        self.name = name
        self.description = description
        self.image = image
        self.avatarURL = avatarURL
    }
}
