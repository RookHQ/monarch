/// How the current stage ends a journey.
public enum MonarchStageJourneyTerminal: Sendable, Hashable, CaseIterable {
    /// A good end: granted, won, hired.
    case success
    /// Another end: withdrawn, lost, rejected.
    case closed
}
