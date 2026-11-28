/// Where saving stands.
public enum MonarchSaveStatus: Sendable, Hashable, CaseIterable {
    /// Nothing to report. Nothing is drawn.
    case idle
    /// Edits are not saved yet.
    case unsaved
    /// A save is in flight.
    case saving
    /// The last save finished.
    case saved
    /// The last save failed.
    case error
}
