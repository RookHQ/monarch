/// Something the product found while checking a domain, shown by
/// ``MonarchDomainProgress``.
public struct MonarchDomainProblem: Sendable, Hashable {
    /// A machine code from the product, kept for analytics.
    public var code: String?
    /// What is wrong and how to fix it, in plain words. Already localised.
    public var message: String
    /// Whether the problem blocks the step or is a normal wait.
    public var severity: MonarchDomainProblemSeverity
    /// The step the problem belongs to. The current step when `nil`.
    public var step: MonarchDomainStep?

    /// A problem.
    ///
    /// - Parameters:
    ///   - message: What is wrong and how to fix it. Already localised.
    ///   - code: A machine code from the product.
    ///   - severity: `blocking` (shown as a warning) or `info`.
    ///   - step: The step it belongs to. The current step when `nil`.
    public init(
        _ message: String,
        code: String? = nil,
        severity: MonarchDomainProblemSeverity = .blocking,
        step: MonarchDomainStep? = nil
    ) {
        self.message = message
        self.code = code
        self.severity = severity
        self.step = step
    }
}
