import Foundation

/// One piece of work an agent does in the background, for ``MonarchAiBackgroundTasks``.
///
/// ```swift
/// MonarchAiBackgroundTask(
///     "Summarize 40 contracts",
///     id: "t1",
///     status: .running,
///     agent: "Research agent",
///     progress: 0.3,
///     step: "Reading contract 12 of 40",
///     startedAt: start
/// )
/// ```
@_spi(Experimental) public struct MonarchAiBackgroundTask: Identifiable, Sendable, Hashable {
    /// The identity of the task, stable across updates.
    public let id: String
    /// What the task is, already localised.
    public var title: String
    /// Where the task is.
    public var status: MonarchAiBackgroundTaskStatus
    /// The name of the agent doing the work.
    public var agent: String?
    /// Completion from 0 to 1, when the product can measure it.
    public var progress: Double?
    /// Whether the bar is indeterminate even when `progress` is set.
    public var isIndeterminate: Bool
    /// What the agent is doing now, shown while the task is not finished.
    public var step: String?
    /// When the task started.
    public var startedAt: Date?
    /// When the task ended.
    public var endedAt: Date?
    /// A one-line outcome, shown once the task has succeeded.
    public var result: String?
    /// What went wrong, shown once the task has failed.
    public var error: String?

    /// A background task.
    ///
    /// - Parameters:
    ///   - title: What the task is, already localised.
    ///   - id: The identity of the task, stable across updates.
    ///   - status: Where the task is.
    ///   - agent: The name of the agent doing the work.
    ///   - progress: Completion from 0 to 1, when known.
    ///   - isIndeterminate: Whether the bar is indeterminate even when `progress` is set.
    ///   - step: What the agent is doing now.
    ///   - startedAt: When the task started.
    ///   - endedAt: When the task ended.
    ///   - result: A one-line outcome of a succeeded task.
    ///   - error: What went wrong in a failed task.
    public init(
        _ title: String,
        id: String,
        status: MonarchAiBackgroundTaskStatus,
        agent: String? = nil,
        progress: Double? = nil,
        isIndeterminate: Bool = false,
        step: String? = nil,
        startedAt: Date? = nil,
        endedAt: Date? = nil,
        result: String? = nil,
        error: String? = nil
    ) {
        self.title = title
        self.id = id
        self.status = status
        self.agent = agent
        self.progress = progress
        self.isIndeterminate = isIndeterminate
        self.step = step
        self.startedAt = startedAt
        self.endedAt = endedAt
        self.result = result
        self.error = error
    }
}
