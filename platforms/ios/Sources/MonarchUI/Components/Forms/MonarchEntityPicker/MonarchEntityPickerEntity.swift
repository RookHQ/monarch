import SwiftUI

/// One person, record or other entity a ``MonarchEntityPicker`` offers.
public struct MonarchEntityPickerEntity: Identifiable, Sendable {
    /// The value put in the selection. Unique within the list.
    public let id: String
    /// The visible name, already localised. It names the row and is searched.
    public let name: String
    /// Secondary text under the name, such as a role or a team.
    public let description: String?
    /// Short trailing text, such as an email or a code. Searched too.
    public let hint: String?
    /// The picture, when the app already has it.
    public let image: Image?
    /// The address of the picture, loaded with `AsyncImage`.
    public let url: URL?
    /// An SF Symbol shown in the avatar instead of initials, for records such as projects.
    public let systemImage: String?
    /// A circle for people, a rounded square for records.
    public let shape: MonarchAvatarShape
    /// Extra words that are searched but never shown.
    public let keywords: String?
    /// Whether the entity cannot be chosen.
    public let isDisabled: Bool
    /// A heading the entity is listed under. Groups keep the order in which
    /// they first appear; entities without a group come first.
    public let group: String?
    /// A presence dot on the avatar, such as online or away.
    public let status: MonarchPresenceStatus?
    /// Replaces the built-in spoken name of `status`.
    public let statusLabel: String?

    /// An entity with a picture the app already has.
    ///
    /// - Parameters:
    ///   - id: The value put in the selection.
    ///   - name: The visible name, already localised.
    ///   - description: Secondary text under the name.
    ///   - hint: Short trailing text, such as an email.
    ///   - image: The picture, or `nil` for initials or the symbol.
    ///   - systemImage: An SF Symbol shown instead of initials.
    ///   - shape: A circle for people, a rounded square for records.
    ///   - keywords: Extra words that are searched but never shown.
    ///   - isDisabled: Whether the entity cannot be chosen.
    ///   - group: A heading the entity is listed under.
    ///   - status: A presence dot on the avatar.
    ///   - statusLabel: Replaces the built-in spoken name of `status`.
    public init(
        id: String,
        name: String,
        description: String? = nil,
        hint: String? = nil,
        image: Image?,
        systemImage: String? = nil,
        shape: MonarchAvatarShape = .circle,
        keywords: String? = nil,
        isDisabled: Bool = false,
        group: String? = nil,
        status: MonarchPresenceStatus? = nil,
        statusLabel: String? = nil
    ) {
        self.id = id
        self.name = name
        self.description = description
        self.hint = hint
        self.image = image
        self.url = nil
        self.systemImage = systemImage
        self.shape = shape
        self.keywords = keywords
        self.isDisabled = isDisabled
        self.group = group
        self.status = status
        self.statusLabel = statusLabel
    }

    /// An entity whose picture is loaded from a URL.
    ///
    /// - Parameters:
    ///   - id: The value put in the selection.
    ///   - name: The visible name, already localised.
    ///   - description: Secondary text under the name.
    ///   - hint: Short trailing text, such as an email.
    ///   - url: The picture's address, or `nil` for initials or the symbol.
    ///   - systemImage: An SF Symbol shown instead of initials.
    ///   - shape: A circle for people, a rounded square for records.
    ///   - keywords: Extra words that are searched but never shown.
    ///   - isDisabled: Whether the entity cannot be chosen.
    ///   - group: A heading the entity is listed under.
    ///   - status: A presence dot on the avatar.
    ///   - statusLabel: Replaces the built-in spoken name of `status`.
    public init(
        id: String,
        name: String,
        description: String? = nil,
        hint: String? = nil,
        url: URL? = nil,
        systemImage: String? = nil,
        shape: MonarchAvatarShape = .circle,
        keywords: String? = nil,
        isDisabled: Bool = false,
        group: String? = nil,
        status: MonarchPresenceStatus? = nil,
        statusLabel: String? = nil
    ) {
        self.id = id
        self.name = name
        self.description = description
        self.hint = hint
        self.image = nil
        self.url = url
        self.systemImage = systemImage
        self.shape = shape
        self.keywords = keywords
        self.isDisabled = isDisabled
        self.group = group
        self.status = status
        self.statusLabel = statusLabel
    }
}
