import SwiftUI

/// Shows the state of a ``MonarchAutosave``: "Unsaved changes", "Saving…",
/// "Saved · 2 minutes ago", or "Couldn't save" with a Retry button that
/// saves again.
///
/// ```swift
/// @State private var autosave = MonarchAutosave("") { text in
///     try await api.saveDraft(text)
/// }
///
/// TextEditor(text: $draft)
///     .onChange(of: draft) { autosave.update(draft) }
///     .toolbar { MonarchAutosaveIndicator(autosave) }
/// ```
///
/// It is ``MonarchSaveStatusIndicator`` fed by the autosave, so it looks and
/// reads the same. Nothing is drawn while nothing has been edited or saved.
///
/// Accessibility: the status is one element; the outcome (saved or failed)
/// is announced, and "Saving…" too with `announcesSaving`. The retry button
/// is a separate standard button with a 44-point touch target. The status
/// is said in words with an icon, never by colour alone.
public struct MonarchAutosaveIndicator<Value: Equatable & Sendable>: View {
    private let autosave: MonarchAutosave<Value>
    private let errorMessage: ((any Error) -> String)?
    private let announcesSaving: Bool
    private let labels: MonarchSaveStatusIndicatorLabels

    /// An autosave indicator.
    ///
    /// - Parameters:
    ///   - autosave: The autosave to show.
    ///   - errorMessage: Turns the save error into the visible message, such as "You are offline".
    ///     Without it, or when it returns an empty string, "Couldn't save" is shown.
    ///   - announcesSaving: Whether "Saving…" is announced too.
    ///   - labels: Replacements for the built-in strings.
    public init(
        _ autosave: MonarchAutosave<Value>,
        errorMessage: ((any Error) -> String)? = nil,
        announcesSaving: Bool = false,
        labels: MonarchSaveStatusIndicatorLabels = MonarchSaveStatusIndicatorLabels()
    ) {
        self.autosave = autosave
        self.errorMessage = errorMessage
        self.announcesSaving = announcesSaving
        self.labels = labels
    }

    /// The content of the view.
    public var body: some View {
        MonarchSaveStatusIndicator(
            autosave.status,
            savedAt: autosave.savedAt,
            errorMessage: MonarchAutosaveIndicatorTokens.errorMessage(
                status: autosave.status,
                error: autosave.error,
                transform: errorMessage
            ),
            announcesSaving: announcesSaving,
            labels: labels,
            onRetry: { autosave.retry() }
        )
    }
}

#if DEBUG
    @MainActor
    private func monarchAutosaveIndicatorPreviewEdited() -> MonarchAutosave<String> {
        let autosave = MonarchAutosave("Draft") { _ in }
        autosave.isEnabled = false
        autosave.update("Draft, edited")
        return autosave
    }

    #Preview("MonarchAutosaveIndicator") {
        MonarchPreviewMatrix {
            VStack(alignment: .leading) {
                MonarchAutosaveIndicator(MonarchAutosave("Draft", savedAt: .now.addingTimeInterval(-120)) { _ in })
                MonarchAutosaveIndicator(monarchAutosaveIndicatorPreviewEdited())
            }
        }
    }
#endif
