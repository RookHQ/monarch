/// The severity of a ``MonarchLogLine``, lowest first.
public enum MonarchLogLevel: Sendable, Hashable, CaseIterable {
    /// Detail for developers.
    case debug
    /// Normal operation. The level of a line that has none.
    case info
    /// Something unexpected that did not stop the work.
    case warn
    /// Something failed.
    case error
}
