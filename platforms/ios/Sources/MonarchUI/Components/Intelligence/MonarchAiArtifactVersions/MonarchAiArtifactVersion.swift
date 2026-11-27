import Foundation

/// One saved version of an AI artifact, shown by ``MonarchAiArtifactVersions``.
@_spi(Experimental) public struct MonarchAiArtifactVersion: Identifiable, Sendable, Hashable {
    /// The version number, counting from 1. It is the selection.
    public let version: Int
    /// When the version was made.
    public let createdAt: Date
    /// An optional name, such as "Shorter intro".
    public let label: String?
    /// The text of the version. With the text of the version before it, Compare shows a diff.
    public let content: String?

    /// The identity of the version, which is its number.
    public var id: Int { version }

    /// A version.
    ///
    /// - Parameters:
    ///   - version: The version number, counting from 1.
    ///   - createdAt: When the version was made.
    ///   - label: An optional name, already localised.
    ///   - content: The text of the version, for the diff.
    public init(_ version: Int, createdAt: Date, label: String? = nil, content: String? = nil) {
        self.version = version
        self.createdAt = createdAt
        self.label = label
        self.content = content
    }
}
