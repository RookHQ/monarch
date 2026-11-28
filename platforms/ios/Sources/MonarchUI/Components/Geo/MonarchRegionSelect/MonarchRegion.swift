/// A first-level subdivision of a country: a state, province, territory or nation.
public struct MonarchRegion: Sendable, Hashable, Identifiable {
    /// The stable code to store. The built-in lists use ISO 3166-2 codes such as "US-CA".
    public let code: String
    /// The name people read, already localised.
    public let name: String

    /// The code, which identifies the region.
    public var id: String { code }

    /// A region.
    ///
    /// - Parameters:
    ///   - code: The stable code to store.
    ///   - name: The name people read, already localised.
    public init(code: String, name: String) {
        self.code = code
        self.name = name
    }
}
