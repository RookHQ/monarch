import Foundation
import Observation

/// The state of one ``MonarchEditableCell``: viewing or editing, the drafts,
/// the pending save and the error. Kept outside the view so the rules are
/// tested without one.
@MainActor
@Observable
final class MonarchEditableCellSession {
    /// What a commit did.
    enum Outcome: Equatable {
        /// Nothing happened: a save is already running.
        case ignored
        /// The value equals the saved one; the editor closed without saving.
        case unchanged
        /// The draft was refused; the message is shown and nothing was saved.
        case invalid
        /// The save succeeded with this value; the editor closed.
        case saved(MonarchEditableCellValue)
        /// The save threw; the message is shown and the draft is kept.
        case failed
    }

    /// The typed text of a text or number editor.
    var draft = ""
    /// The day of a date editor opened on an empty cell.
    var dayDraft = Date(timeIntervalSince1970: 0)
    /// Whether the editor is open.
    private(set) var isEditing = false
    /// Whether a save is running.
    private(set) var isSaving = false
    /// The value being saved, shown by select and date cells until the save ends.
    private(set) var pending: MonarchEditableCellValue?
    /// The validation or save error to show.
    private(set) var errorMessage: String?
    /// The draft that was last refused, so leaving the field does not retry it.
    private(set) var failedDraft: String?

    /// Opens the editor with a text draft, or a day for an empty date cell.
    func begin(draft: String, day: Date? = nil) {
        guard !isEditing else { return }
        self.draft = draft
        if let day { dayDraft = day }
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

    /// Checks and saves a new value.
    ///
    /// - Parameters:
    ///   - parsed: The value read from the draft, or why it could not be read.
    ///   - check: Decides whether the value may be saved.
    ///   - message: Turns a thrown error into the message to show.
    ///   - onCommit: Persists the new value. Throwing keeps the draft and shows the message.
    /// - Returns: What the commit did.
    func commit(
        _ parsed: Result<MonarchEditableCellValue, MonarchEditableCellParseError>,
        check: @MainActor (MonarchEditableCellValue) -> MonarchEditableCellCheck,
        message: @MainActor (any Error) -> String,
        onCommit: @MainActor (MonarchEditableCellValue) async throws -> Void
    ) async -> Outcome {
        guard !isSaving else { return .ignored }
        let next: MonarchEditableCellValue
        switch parsed {
        case .failure(let error):
            refuse(error.message)
            return .invalid
        case .success(let value):
            next = value
        }
        switch check(next) {
        case .unchanged:
            end()
            return .unchanged
        case .invalid(let reason):
            refuse(reason)
            return .invalid
        case .valid(let value):
            isSaving = true
            pending = value
            errorMessage = nil
            do {
                try await onCommit(value)
                isSaving = false
                pending = nil
                end()
                return .saved(value)
            } catch {
                isSaving = false
                pending = nil
                failedDraft = draft
                errorMessage = message(error)
                return .failed
            }
        }
    }

    private func refuse(_ reason: String) {
        errorMessage = reason
        failedDraft = draft
    }

    private func end() {
        isEditing = false
        errorMessage = nil
        failedDraft = nil
        draft = ""
    }
}
