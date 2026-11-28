/// The meaning or emphasis a toned component carries: a badge, a tag, a
/// banner, a toast, a progress bar.
///
/// The names match the web and Android. `accent` and `ai` were added after
/// the first five: a `switch` over `MonarchTone` in an app needs a case for
/// each, or a `default` (see the changelog).
public enum MonarchTone: Sendable, Hashable, CaseIterable {
    /// No particular meaning.
    case neutral
    /// The product accent: emphasis without a status, such as "today", a
    /// selected filter or the newest items. It has no spoken name.
    case accent
    /// Information.
    case info
    /// Something succeeded.
    case success
    /// Something needs attention.
    case warning
    /// Something failed or is destructive.
    case danger
    /// Content made or suggested by an AI model. Say so in the text too.
    case ai
}
