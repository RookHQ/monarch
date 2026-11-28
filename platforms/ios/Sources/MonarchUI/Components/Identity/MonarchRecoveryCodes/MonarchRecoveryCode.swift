/// One recovery code shown by ``MonarchRecoveryCodes``, optionally marked as used.
public struct MonarchRecoveryCode: Sendable, Hashable, ExpressibleByStringLiteral {
    /// The code, such as `"4f7k-2m9q"`.
    public var code: String
    /// Whether the code has been used and no longer works.
    public var isUsed: Bool

    /// A recovery code.
    ///
    /// - Parameters:
    ///   - code: The code.
    ///   - isUsed: Whether the code has been used and no longer works.
    public init(_ code: String, isUsed: Bool = false) {
        self.code = code
        self.isUsed = isUsed
    }

    /// An unused code from a string literal.
    public init(stringLiteral value: String) {
        self.init(value)
    }
}
