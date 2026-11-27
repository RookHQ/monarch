import SwiftUI

/// A person ``MonarchAssigneePicker`` can assign. The app maps its own user
/// records to this shape.
public struct MonarchAssigneePerson: Identifiable, Sendable {
    /// The stable id stored in the selection.
    public let id: String
    /// The person's name.
    public let name: String
    /// The email shown under the name and matched by search.
    public let email: String?
    /// Short trailing text, such as "3 open" or "Away".
    public let hint: String?
    /// The avatar image the app has already loaded.
    public let image: Image?
    /// The avatar's address, loaded with `AsyncImage`.
    public let url: URL?
    /// Whether the person cannot be assigned now.
    public let isDisabled: Bool
    /// A heading the person is listed under, such as "In this team". Groups appear in the order they first occur.
    public let group: String?
    /// A presence dot on the avatar.
    public let status: MonarchPresenceStatus?

    /// A person with an avatar image the app has already loaded.
    ///
    /// - Parameters:
    ///   - id: The stable id stored in the selection.
    ///   - name: The person's name.
    ///   - email: Shown under the name and matched by search.
    ///   - hint: Short trailing text, such as "3 open".
    ///   - image: The avatar image. Initials are shown without one.
    ///   - isDisabled: Whether the person cannot be assigned now.
    ///   - group: A heading the person is listed under.
    ///   - status: A presence dot on the avatar.
    public init(
        id: String,
        name: String,
        email: String? = nil,
        hint: String? = nil,
        image: Image?,
        isDisabled: Bool = false,
        group: String? = nil,
        status: MonarchPresenceStatus? = nil
    ) {
        self.id = id
        self.name = name
        self.email = email
        self.hint = hint
        self.image = image
        self.url = nil
        self.isDisabled = isDisabled
        self.group = group
        self.status = status
    }

    /// A person with an avatar loaded from an address.
    ///
    /// - Parameters:
    ///   - id: The stable id stored in the selection.
    ///   - name: The person's name.
    ///   - email: Shown under the name and matched by search.
    ///   - hint: Short trailing text, such as "3 open".
    ///   - url: The avatar's address. Initials are shown while it loads, on failure and without one.
    ///   - isDisabled: Whether the person cannot be assigned now.
    ///   - group: A heading the person is listed under.
    ///   - status: A presence dot on the avatar.
    public init(
        id: String,
        name: String,
        email: String? = nil,
        hint: String? = nil,
        url: URL? = nil,
        isDisabled: Bool = false,
        group: String? = nil,
        status: MonarchPresenceStatus? = nil
    ) {
        self.id = id
        self.name = name
        self.email = email
        self.hint = hint
        self.image = nil
        self.url = url
        self.isDisabled = isDisabled
        self.group = group
        self.status = status
    }
}
