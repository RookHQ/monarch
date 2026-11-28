/// The standard incident stages, in the order an incident usually moves
/// through them. ``MonarchIncidentTimeline`` gives each a built-in label and
/// tone; products can add their own stages by key.
public enum MonarchIncidentStage: String, Sendable, Hashable, CaseIterable {
    /// The cause is not known yet. Danger tone.
    case investigating
    /// The cause is known and a fix is in progress. Warning tone.
    case identified
    /// A fix is out and being watched. Info tone.
    case monitoring
    /// The incident is over. Success tone.
    case resolved
}
