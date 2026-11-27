import SwiftUI

/// The "type the name to confirm" field for an action that cannot be undone.
///
/// It is a ``MonarchTextField`` that knows the phrase. Keep the destructive
/// button disabled until ``matches(_:phrase:)`` is true, or track the state
/// with `onMatchChange`.
///
/// ```swift
/// MonarchTypeToConfirmInput(phrase: "acme-site", text: $typed)
/// Button("Delete site", role: .destructive) { delete() }
///     .disabled(!MonarchTypeToConfirmInput.matches(typed, phrase: "acme-site"))
/// ```
///
/// Matching is case-sensitive and ignores spaces around the text. Paste is
/// allowed. Autocorrection and capitalisation are off.
///
/// Accessibility: the field is read as a text field with the label, which
/// names the phrase. When the text starts to match, "Text matches" is
/// announced. The mismatch error is an icon and text and is announced by the
/// text field; it appears only when `showsMismatch` is set, never while the
/// person is still typing unless the app asks.
public struct MonarchTypeToConfirmInput: View {
    private let phrase: String
    @Binding private var text: String
    private let label: String?
    private let helper: String?
    private let showsMismatch: Bool
    private let mismatchMessage: String?
    private let matchAnnouncement: String?
    private let mismatchAnnouncement: String?
    private let onMatchChange: (@MainActor (Bool) -> Void)?

    /// A type-to-confirm input.
    ///
    /// - Parameters:
    ///   - phrase: The exact text the person must type, usually the record's own name.
    ///   - text: The typed text.
    ///   - label: The visible label. By default "Type “phrase” to confirm".
    ///   - helper: Supporting text under the field, such as what is about to be deleted.
    ///   - showsMismatch: Whether to show the error while the text does not match, typically after a
    ///     submit attempt.
    ///   - mismatchMessage: Replaces the built-in "The text does not match."
    ///   - matchAnnouncement: Replaces the built-in "Text matches" announcement. Pass an empty string for silence.
    ///   - mismatchAnnouncement: Announced when the text stops matching. Nothing by default.
    ///   - onMatchChange: Runs each time the match state flips, not for the first state.
    public init(
        phrase: String,
        text: Binding<String>,
        label: String? = nil,
        helper: String? = nil,
        showsMismatch: Bool = false,
        mismatchMessage: String? = nil,
        matchAnnouncement: String? = nil,
        mismatchAnnouncement: String? = nil,
        onMatchChange: (@MainActor (Bool) -> Void)? = nil
    ) {
        self.phrase = phrase
        _text = text
        self.label = label
        self.helper = helper
        self.showsMismatch = showsMismatch
        self.mismatchMessage = mismatchMessage
        self.matchAnnouncement = matchAnnouncement
        self.mismatchAnnouncement = mismatchAnnouncement
        self.onMatchChange = onMatchChange
    }

    /// Whether typed text matches a phrase: case-sensitive, ignoring spaces
    /// and line breaks around either. The rule the component uses.
    public nonisolated static func matches(_ text: String, phrase: String) -> Bool {
        MonarchTypeToConfirmInputTokens.matches(text, phrase: phrase)
    }

    /// The content of the view.
    public var body: some View {
        let matches = MonarchTypeToConfirmInputTokens.matches(text, phrase: phrase)
        MonarchTextField(
            MonarchTypeToConfirmInputTokens.label(label, phrase: phrase),
            text: $text,
            helper: helper,
            error: MonarchTypeToConfirmInputTokens.error(
                showsMismatch: showsMismatch,
                matches: matches,
                mismatchMessage: mismatchMessage
            )
        )
        .monarchHelperAbove()
        .environment(\.monarchFieldLabelCode, label == nil ? phrase : nil)
        .autocorrectionDisabled()
        #if os(iOS)
            .textInputAutocapitalization(.never)
        #endif
        .onChange(of: matches) { _, nowMatches in
            let announcement = MonarchTypeToConfirmInputTokens.announcement(
                matches: nowMatches,
                matchAnnouncement: matchAnnouncement,
                mismatchAnnouncement: mismatchAnnouncement
            )
            if let announcement {
                AccessibilityNotification.Announcement(announcement).post()
            }
            onMatchChange?(nowMatches)
        }
    }
}

#if DEBUG
    #Preview("MonarchTypeToConfirmInput") {
        MonarchPreviewMatrix {
            VStack(alignment: .leading) {
                MonarchTypeToConfirmInput(phrase: "acme-site", text: .constant(""), helper: "This deletes the site.")
                MonarchTypeToConfirmInput(phrase: "acme-site", text: .constant("acme"), showsMismatch: true)
                MonarchTypeToConfirmInput(phrase: "acme-site", text: .constant("acme-site")).disabled(true)
            }
        }
    }
#endif
