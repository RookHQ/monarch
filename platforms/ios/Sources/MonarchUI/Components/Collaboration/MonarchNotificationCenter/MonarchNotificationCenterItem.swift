import Foundation

/// One notification in a ``MonarchNotificationCenter``.
public struct MonarchNotificationCenterItem: Identifiable, Sendable, Hashable {
    /// A stable id, passed to `onMarkRead`.
    public let id: String
    /// The headline, such as "Ana Lopez mentioned you in Launch checklist", already localised.
    public let title: String
    /// A snippet or more context, on one line.
    public let message: String?
    /// When it happened.
    public let time: Date
    /// Whether it has been read. Unread items have a dot, a bold title and are read as "Unread".
    public let isRead: Bool
    /// Who caused it. Shown as an avatar with initials; no avatar without it.
    public let actorName: String?
    /// The actor's avatar picture, loaded with `AsyncImage`.
    public let actorAvatarURL: URL?
    /// A small SF Symbol after the title, such as the app or the kind of event.
    public let systemImage: String?
    /// The spoken description of the symbol. Without it the symbol is decorative.
    public let systemImageLabel: String?

    /// A notification.
    ///
    /// - Parameters:
    ///   - id: A stable id.
    ///   - title: The headline.
    ///   - message: A snippet or more context.
    ///   - time: When it happened.
    ///   - isRead: Whether it has been read.
    ///   - actorName: Who caused it, for the avatar.
    ///   - actorAvatarURL: The actor's avatar picture.
    ///   - systemImage: A small SF Symbol after the title.
    ///   - systemImageLabel: The spoken description of the symbol.
    public init(
        id: String,
        title: String,
        message: String? = nil,
        time: Date,
        isRead: Bool = false,
        actorName: String? = nil,
        actorAvatarURL: URL? = nil,
        systemImage: String? = nil,
        systemImageLabel: String? = nil
    ) {
        self.id = id
        self.title = title
        self.message = message
        self.time = time
        self.isRead = isRead
        self.actorName = actorName
        self.actorAvatarURL = actorAvatarURL
        self.systemImage = systemImage
        self.systemImageLabel = systemImageLabel
    }
}
