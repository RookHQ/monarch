import Foundation

/// What was accepted in ``MonarchTermsAcceptance``, for proof of acceptance.
public struct MonarchTermsAcceptanceDetails: Sendable, Hashable {
    /// The version of each document that has one, keyed by the document id.
    public let versions: [String: String]
    /// When the box changed.
    public let date: Date

    /// Acceptance details.
    ///
    /// - Parameters:
    ///   - versions: The version of each document that has one, keyed by the document id.
    ///   - date: When the box changed.
    public init(versions: [String: String], date: Date) {
        self.versions = versions
        self.date = date
    }
}
