import Foundation

/// One app that can be granted in a ``MonarchAppAccessPicker``.
@_spi(Experimental) public struct MonarchAppAccessOption: Sendable, Hashable, Identifiable {
    /// The stable id put in the selection, such as the app's slug.
    public let id: String
    /// The app's name, already localised.
    public let name: String
    /// One line on what the app is for.
    public let description: String?
    /// Where the app's logo is. The first letter of the name is shown without one, while it loads and if it fails.
    public let logoURL: URL?
    /// A short tag after the name, such as a plan or "Beta".
    public let badge: String?
    /// Whether the app cannot be changed here, for example because everyone has it.
    public let isDisabled: Bool

    /// An app that can be granted.
    ///
    /// - Parameters:
    ///   - id: The stable id put in the selection.
    ///   - name: The app's name, already localised.
    ///   - description: One line on what the app is for.
    ///   - logoURL: Where the app's logo is.
    ///   - badge: A short tag after the name.
    ///   - isDisabled: Whether the app cannot be changed here.
    public init(
        id: String,
        name: String,
        description: String? = nil,
        logoURL: URL? = nil,
        badge: String? = nil,
        isDisabled: Bool = false
    ) {
        self.id = id
        self.name = name
        self.description = description
        self.logoURL = logoURL
        self.badge = badge
        self.isDisabled = isDisabled
    }
}
