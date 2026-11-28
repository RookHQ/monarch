/// Replacements for the built-in strings of ``MonarchImpersonationBanner``.
/// A property that is `nil` keeps the built-in English string.
public struct MonarchImpersonationBannerLabels: Sendable {
    /// The main line. Receives the name. Built in: "You are viewing as
    /// <name>", with the name in bold. A replacement is shown without bold.
    public var viewingAs: (@Sendable (String) -> String)?
    /// Before the start time. Built in: "Started".
    public var started: String?
    /// Before the reason. Built in: "Reason:".
    public var reason: String?
    /// The stop button. Built in: "Stop".
    public var stop: String?
    /// The spoken name of the stop button. Receives the name. Built in:
    /// "Stop viewing as <name>".
    public var stopLabel: (@Sendable (String) -> String)?
    /// Shown when stopping fails with an error that has no description.
    /// Built in: "Couldn't stop. Try again."
    public var stopError: String?

    /// Replacements for built-in strings. Every parameter is optional.
    public init(
        viewingAs: (@Sendable (String) -> String)? = nil,
        started: String? = nil,
        reason: String? = nil,
        stop: String? = nil,
        stopLabel: (@Sendable (String) -> String)? = nil,
        stopError: String? = nil
    ) {
        self.viewingAs = viewingAs
        self.started = started
        self.reason = reason
        self.stop = stop
        self.stopLabel = stopLabel
        self.stopError = stopError
    }
}
