/// What an AI agent is doing now.
public enum MonarchAiAgentState: String, Sendable, Hashable, CaseIterable {
    /// Not doing anything. No ring.
    case idle
    /// Working on a task. A moving ring, still with Reduce Motion.
    case working
    /// Waiting for a person: an approval or an answer. A dashed warning ring.
    case waiting
    /// Stopped with an error. A danger ring and a danger badge.
    case error
}
