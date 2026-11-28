/// How a running timer is shown.
public enum MonarchRunningTimerVariant: Sendable, Hashable, CaseIterable {
    /// A large clock with Start and Stop and room for the fields of the
    /// next entry. The default.
    case panel
    /// A compact chip for a toolbar or header. It is hidden while no timer
    /// runs, unless asked to stay.
    case indicator
}
