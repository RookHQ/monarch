/// How the person chose to leave a screen guarded by ``MonarchUnsavedChangesGuard``.
public enum MonarchUnsavedChangesGuardLeaveReason: Sendable, Hashable, CaseIterable {
    /// Without saving: the changes are thrown away.
    case discard
    /// After the save succeeded.
    case save
}
