/// The visible height step of a field. `medium` is the default.
///
/// The values are native, not the web pointer sizes: 40, 44 and 52 pt, so a
/// field matches the 44 pt touch target minimum at the default size.
public enum MonarchFieldSize: Sendable, Hashable, CaseIterable {
    /// 40 pt, for dense forms. The hit area stays 44 pt.
    case small
    /// 44 pt, the default.
    case medium
    /// 52 pt, for prominent fields such as a search bar.
    case large
}
