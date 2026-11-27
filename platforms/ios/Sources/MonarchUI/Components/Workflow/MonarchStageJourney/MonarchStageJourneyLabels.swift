/// Replacements for the built-in strings of ``MonarchStageJourney``.
/// A property that is `nil` keeps the built-in English string.
public struct MonarchStageJourneyLabels: Sendable {
    /// The name of the stage strip. Built in: "Stages".
    public var stages: String?
    /// The heading of the current-stage card. Receives the stage. Built in: "Now: Examination".
    public var now: (@Sendable (String) -> String)?
    /// The time in the current stage. Receives "3 days ago". Built in: "since 3 days ago".
    public var since: (@Sendable (String) -> String)?
    /// The state of a finished stage, read by VoiceOver. Built in: "done".
    public var done: String?
    /// The state of the current stage, read by VoiceOver. Built in: "current stage".
    public var current: String?
    /// The outcome of a successful journey. Built in: "Completed".
    public var success: String?
    /// The outcome of a closed journey. Built in: "Closed".
    public var closed: String?
    /// The heading above the moves. Built in: "Next steps".
    public var nextSteps: String?
    /// The name of a forward or exit move. Receives the stage. Built in: "Move to Granted".
    public var moveTo: (@Sendable (String) -> String)?
    /// The title of a back move. Receives the stage. Built in: "Back to Filed".
    public var backTo: (@Sendable (String) -> String)?
    /// Read after a move that asks for details. Built in: "asks for details".
    public var needsInput: String?
    /// Announced when the current stage changes. Receives the stage. Built in: "Moved to Granted".
    public var moved: (@Sendable (String) -> String)?
    /// Shown when a move fails without a localized description.
    /// Built in: "The move did not go through. Try again.".
    public var failed: String?

    /// Replacements for built-in strings. Every parameter is optional.
    public init(
        stages: String? = nil,
        now: (@Sendable (String) -> String)? = nil,
        since: (@Sendable (String) -> String)? = nil,
        done: String? = nil,
        current: String? = nil,
        success: String? = nil,
        closed: String? = nil,
        nextSteps: String? = nil,
        moveTo: (@Sendable (String) -> String)? = nil,
        backTo: (@Sendable (String) -> String)? = nil,
        needsInput: String? = nil,
        moved: (@Sendable (String) -> String)? = nil,
        failed: String? = nil
    ) {
        self.stages = stages
        self.now = now
        self.since = since
        self.done = done
        self.current = current
        self.success = success
        self.closed = closed
        self.nextSteps = nextSteps
        self.moveTo = moveTo
        self.backTo = backTo
        self.needsInput = needsInput
        self.moved = moved
        self.failed = failed
    }
}
