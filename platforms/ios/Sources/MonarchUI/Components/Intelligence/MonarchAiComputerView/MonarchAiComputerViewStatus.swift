/// Where an agent's session in ``MonarchAiComputerView`` is.
@_spi(Experimental) public enum MonarchAiComputerViewStatus: String, Sendable, Hashable, CaseIterable {
    /// The session is starting.
    case starting
    /// The agent is acting.
    case working
    /// The person paused the agent.
    case paused
    /// The agent waits for the person, for example to sign in.
    case waitingForYou
    /// The person has taken control.
    case takenOver
    /// The task is finished.
    case done
    /// The task failed.
    case failed
    /// The person stopped the agent.
    case stopped
}
