/// Replacements for the built-in strings of ``MonarchConsentHistory``.
/// A property that is `nil` keeps the built-in English string.
public struct MonarchConsentHistoryLabels: Sendable {
    /// How a method reads. Return `nil` to keep the built-in text. Built in:
    /// "Accepted all", "Rejected all", "Custom choice", "Browser privacy
    /// signal", "Acknowledged", "Withdrew consent".
    public var method: (@Sendable (MonarchConsentMethod) -> String?)?
    /// How a regime reads. Return `nil` to keep the built-in text. Built in:
    /// "Asked before use (opt-in)", "Could refuse (opt-out)", "Notice only".
    public var regime: (@Sendable (MonarchConsentRegime) -> String?)?
    /// The name of the method line. Built in: "Choice".
    public var methodTitle: String?
    /// The name of the policy version line. Built in: "Policy version".
    public var policyVersionTitle: String?
    /// The name of the regime line. Built in: "Rules applied".
    public var regimeTitle: String?
    /// The name of the categories list. Built in: "Categories".
    public var categoriesTitle: String?
    /// After a category that was allowed. Built in: "Allowed".
    public var allowed: String?
    /// After a category that was refused. Built in: "Not allowed".
    public var denied: String?
    /// The extra line when a privacy signal was present but the method was
    /// another one. Built in: "Browser privacy signal present".
    public var signalPresent: String?
    /// The title of the empty state. Built in: "No consent choices recorded".
    public var emptyTitle: String?
    /// The message of the empty state. Built in: "Choices about cookies and
    /// similar storage will appear here once they are made."
    public var emptyMessage: String?
    /// Shown when the export fails. Built in: "The download did not start.
    /// Try again."
    public var exportFailed: String?

    /// Replacements for built-in strings. Every parameter is optional.
    public init(
        method: (@Sendable (MonarchConsentMethod) -> String?)? = nil,
        regime: (@Sendable (MonarchConsentRegime) -> String?)? = nil,
        methodTitle: String? = nil,
        policyVersionTitle: String? = nil,
        regimeTitle: String? = nil,
        categoriesTitle: String? = nil,
        allowed: String? = nil,
        denied: String? = nil,
        signalPresent: String? = nil,
        emptyTitle: String? = nil,
        emptyMessage: String? = nil,
        exportFailed: String? = nil
    ) {
        self.method = method
        self.regime = regime
        self.methodTitle = methodTitle
        self.policyVersionTitle = policyVersionTitle
        self.regimeTitle = regimeTitle
        self.categoriesTitle = categoriesTitle
        self.allowed = allowed
        self.denied = denied
        self.signalPresent = signalPresent
        self.emptyTitle = emptyTitle
        self.emptyMessage = emptyMessage
        self.exportFailed = exportFailed
    }
}
