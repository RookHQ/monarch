/// The kind of a ``MonarchMessageRow``.
public enum MonarchMessageRowVariant: Sendable, Hashable, CaseIterable {
    /// A message from a person or an app.
    case standard
    /// A note only teammates can see: a warning tint and a leading bar.
    /// Add a tag such as "Internal" so it does not rely on colour.
    case `internal`
    /// A centred, compact line written by the server, such as "Ana joined".
    /// No avatar, author, reactions or actions.
    case system
}
