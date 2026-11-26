import Foundation

/// Logic for ``MonarchTypeToConfirmInput``. It has no sizes or colours of its
/// own: it is drawn by ``MonarchTextField``.
enum MonarchTypeToConfirmInputTokens {
    /// Whether typed text matches a phrase: case-sensitive, ignoring spaces
    /// and line breaks around either.
    static func matches(_ text: String, phrase: String) -> Bool {
        text.trimmingCharacters(in: .whitespacesAndNewlines) == phrase.trimmingCharacters(in: .whitespacesAndNewlines)
    }

    /// The label: the app's, or the built-in one that names the phrase.
    static func label(_ label: String?, phrase: String) -> String {
        label ?? MonarchTypeToConfirmInputStrings.label(phrase)
    }

    /// The error to show: only while the app asks for it and the text does not match.
    static func error(showsMismatch: Bool, matches: Bool, mismatchMessage: String?) -> String? {
        guard showsMismatch, !matches else { return nil }
        return mismatchMessage ?? MonarchTypeToConfirmInputStrings.mismatch
    }

    /// What to announce when the match state flips, or `nil` for silence.
    static func announcement(matches: Bool, matchAnnouncement: String?, mismatchAnnouncement: String?) -> String? {
        let text = matches ? (matchAnnouncement ?? MonarchTypeToConfirmInputStrings.matches) : mismatchAnnouncement
        guard let text, !text.isEmpty else { return nil }
        return text
    }
}
