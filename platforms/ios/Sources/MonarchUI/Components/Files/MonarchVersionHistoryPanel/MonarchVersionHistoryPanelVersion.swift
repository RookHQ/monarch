import SwiftUI

/// One saved version shown by ``MonarchVersionHistoryPanel``.
public struct MonarchVersionHistoryPanelVersion: Identifiable, Sendable {
    /// A stable id of the version.
    public let id: String
    /// When the version was saved.
    public let date: Date
    /// Who saved it, already localised.
    public let authorName: String
    /// The author's picture, when the app already has it.
    public let authorImage: Image?
    /// The address of the author's picture, loaded with `AsyncImage`.
    public let authorURL: URL?
    /// The name someone gave the version.
    public let note: String?
    /// Whether the version was saved automatically rather than by a person.
    public let isAutomatic: Bool

    /// A version with an author picture the app already has.
    ///
    /// - Parameters:
    ///   - id: A stable id of the version.
    ///   - date: When the version was saved.
    ///   - authorName: Who saved it, already localised.
    ///   - authorImage: The author's picture, or `nil` for initials.
    ///   - note: The name someone gave the version.
    ///   - isAutomatic: Whether it was saved automatically.
    public init(
        id: String,
        date: Date,
        authorName: String,
        authorImage: Image?,
        note: String? = nil,
        isAutomatic: Bool = false
    ) {
        self.id = id
        self.date = date
        self.authorName = authorName
        self.authorImage = authorImage
        self.authorURL = nil
        self.note = note
        self.isAutomatic = isAutomatic
    }

    /// A version whose author picture is loaded from a URL.
    ///
    /// - Parameters:
    ///   - id: A stable id of the version.
    ///   - date: When the version was saved.
    ///   - authorName: Who saved it, already localised.
    ///   - authorURL: The address of the author's picture, or `nil` for initials.
    ///   - note: The name someone gave the version.
    ///   - isAutomatic: Whether it was saved automatically.
    public init(
        id: String,
        date: Date,
        authorName: String,
        authorURL: URL? = nil,
        note: String? = nil,
        isAutomatic: Bool = false
    ) {
        self.id = id
        self.date = date
        self.authorName = authorName
        self.authorImage = nil
        self.authorURL = authorURL
        self.note = note
        self.isAutomatic = isAutomatic
    }
}
