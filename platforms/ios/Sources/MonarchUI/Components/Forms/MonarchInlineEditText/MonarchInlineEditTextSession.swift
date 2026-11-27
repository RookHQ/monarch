import Observation

/// The state of one ``MonarchInlineEditText``: viewing or editing, the
/// draft, the pending save and the error. Kept outside the view so the
/// rules are tested without one.
@MainActor
@Observable
final class MonarchInlineEditTextSession {
    /// What a commit did.
    enum Outcome: Equatable {
        /// Nothing happened: not editing, or a save is already running.
        case ignored
        /// The draft equals the saved value; the editor closed without saving.
        case unchanged
        /// Validation refused the draft; the editor stays open with the message.
        case invalid
        /// The save succeeded with this value; the editor closed.
        case saved(String)
        /// The save threw; the editor stays open with the draft and the message.
        case failed
    }

    /// The text being edited.
    var draft = ""
    /// Whether the editor is open.
    private(set) var isEditing = false
    /// Whether a save is running.
    private(set) var isSaving = false
    /// The validation or save error to show.
    private(set) var errorMessage: String?
    /// The draft that was last refused, so leaving the field does not retry it.
    private(set) var failedDraft: String?

    /// Opens the editor on the saved value.
    func begin(with value: String) {
        guard !isEditing else { return }
        draft = value
        errorMessage = nil
        failedDraft = nil
        isEditing = true
    }

    /// Closes the editor and drops the draft. Does nothing while saving.
    ///
    /// - Returns: Whether the editor was closed.
    @discardableResult
    func cancel() -> Bool {
        guard isEditing, !isSaving else { return false }
        end()
        return true
    }

    /// Whether leaving the field should save: only while editing, not while
    /// a save runs, and not for a draft that has just been refused.
    var savesOnFocusLoss: Bool {
        isEditing && !isSaving && failedDraft != draft
    }

    /// Saves the draft.
    ///
    /// - Parameters:
    ///   - value: The saved value, to detect an unchanged draft.
    ///   - trimsWhitespace: Whether to trim the draft before comparing and saving.
    ///   - validate: Returns a message to refuse the draft, or `nil` to allow it.
    ///   - message: Turns a thrown error into the message to show.
    ///   - onSave: Persists the new value. Throwing keeps the editor open.
    /// - Returns: What the commit did.
    func commit(
        value: String,
        trimsWhitespace: Bool,
        validate: (@MainActor (String) -> String?)?,
        message: (any Error) -> String,
        onSave: @MainActor (String) async throws -> Void
    ) async -> Outcome {
        guard isEditing, !isSaving else { return .ignored }
        let next = trimsWhitespace ? draft.trimmingCharacters(in: .whitespacesAndNewlines) : draft
        if next == value {
            end()
            return .unchanged
        }
        if let refusal = validate?(next), !refusal.isEmpty {
            errorMessage = refusal
            failedDraft = draft
            return .invalid
        }
        isSaving = true
        errorMessage = nil
        do {
            try await onSave(next)
            isSaving = false
            end()
            return .saved(next)
        } catch {
            isSaving = false
            failedDraft = draft
            errorMessage = message(error)
            return .failed
        }
    }

    private func end() {
        isEditing = false
        errorMessage = nil
        failedDraft = nil
        draft = ""
    }
}
