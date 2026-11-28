import SwiftUI

/// One row of a ``MonarchTeamCalendar``: a person.
///
/// Like every Monarch component that shows a picture, a member takes the
/// photo as an `Image` or as a URL loaded with `AsyncImage`. The initials of
/// the name are shown while it loads, when it fails and when there is none.
///
/// ```swift
/// MonarchTeamCalendarMember("Alex Kim", id: "alex", description: "Design")
/// ```
@_spi(Experimental) public struct MonarchTeamCalendarMember: Identifiable {
    /// A stable id; entries refer to it with `memberID`. Also picks the avatar's fallback colour.
    public let id: String
    /// The person's name, already localised.
    public let name: String
    /// The photo, when the app loaded it.
    public let image: Image?
    /// The photo's address, loaded with `AsyncImage`.
    public let url: URL?
    /// A second line, such as a role or a team.
    public let description: String?

    /// A member whose photo is loaded from a URL.
    ///
    /// - Parameters:
    ///   - name: The person's name, already localised.
    ///   - id: A stable id; entries refer to it with `memberID`.
    ///   - url: The photo, loaded with `AsyncImage`. Initials without it.
    ///   - description: A second line, such as a role or a team.
    public init(_ name: String, id: String, url: URL? = nil, description: String? = nil) {
        self.name = name
        self.id = id
        self.image = nil
        self.url = url
        self.description = description
    }

    /// A member whose photo the app has already loaded.
    ///
    /// - Parameters:
    ///   - name: The person's name, already localised.
    ///   - id: A stable id; entries refer to it with `memberID`.
    ///   - image: The photo. Initials when it is `nil`.
    ///   - description: A second line, such as a role or a team.
    public init(_ name: String, id: String, image: Image?, description: String? = nil) {
        self.name = name
        self.id = id
        self.image = image
        self.url = nil
        self.description = description
    }
}
