/// The kind of a move offered from the current stage.
public enum MonarchStageJourneyTransitionKind: Sendable, Hashable, CaseIterable {
    /// A next step. The first is the primary button. The default.
    case forward
    /// Leaves the pipeline early, such as withdrawn or lost. Shown quietly.
    case exit
    /// Undoes the last move, shown as "Back to …".
    case back
}
