import Foundation
import MonarchTokens
import SwiftUI
import Testing

@testable import MonarchUI

enum MonarchInlineEditTextFixtures: ComponentFixtures {
    static let id = "inline-edit-text"
    static let fixtures = [
        Fixture("viewing a value") {
            MonarchInlineEditText("Title", text: .constant("Quarterly plan")) { _ in }
        },
        Fixture("viewing an empty value with a prompt") {
            MonarchInlineEditText("Description", text: .constant(""), prompt: "Add a description") { _ in }
        },
        Fixture("viewing an empty value without a prompt") {
            MonarchInlineEditText("Description", text: .constant("")) { _ in }
        },
        Fixture("viewing long text") {
            MonarchInlineEditText(
                "Title",
                text: .constant("A very long title that wraps onto several lines when the text is at its largest size")
            ) { _ in }
        },
        Fixture("disabled") {
            MonarchInlineEditText("Owner", text: .constant("Read only")) { _ in }.disabled(true)
        },
        Fixture("editing a single line") {
            MonarchInlineEditText("Title", text: .constant("Quarterly plan"), startsEditing: true) { _ in }
        },
        Fixture("editing with an error from the app and custom titles") {
            MonarchInlineEditText(
                "Title",
                text: .constant("Quarterly plan"),
                error: "Someone else changed this title. Reload to see their version.",
                startsEditing: true,
                saveTitle: "Rename",
                cancelTitle: "Keep"
            ) { _ in }
        },
        Fixture("editing several lines, empty with a prompt") {
            MonarchInlineEditText(
                "Notes",
                text: .constant(""),
                prompt: "Add notes",
                isMultiline: true,
                startsEditing: true
            ) { _ in }
        },
    ]
}

@Suite("MonarchInlineEditText")
@MainActor
struct MonarchInlineEditTextTests {
    private struct Described: LocalizedError {
        var errorDescription: String? { "The title is already in use." }
    }

    private struct Plain: Error {}

    private func commit(
        _ session: MonarchInlineEditTextSession,
        value: String,
        trims: Bool = true,
        validate: (@MainActor (String) -> String?)? = nil,
        onSave: @MainActor (String) async throws -> Void = { _ in }
    ) async -> MonarchInlineEditTextSession.Outcome {
        await session.commit(
            value: value,
            trimsWhitespace: trims,
            validate: validate,
            message: { MonarchInlineEditTextTokens.failureMessage(for: $0, override: nil) },
            onSave: onSave
        )
    }

    @Test("Opening the editor starts the draft from the saved value")
    func begins() {
        let session = MonarchInlineEditTextSession()
        #expect(!session.isEditing)
        session.begin(with: "Plan")
        #expect(session.isEditing)
        #expect(session.draft == "Plan")
        session.draft = "Changed"
        session.begin(with: "Plan")
        #expect(session.draft == "Changed", "opening twice must not drop the draft")
    }

    @Test("A successful save closes the editor and reports the trimmed value")
    func saves() async {
        let session = MonarchInlineEditTextSession()
        session.begin(with: "Plan")
        session.draft = "  New plan \n"
        var saved: [String] = []
        let outcome = await commit(session, value: "Plan", onSave: { saved.append($0) })
        #expect(outcome == .saved("New plan"))
        #expect(saved == ["New plan"])
        #expect(!session.isEditing)
        #expect(!session.isSaving)
        #expect(session.errorMessage == nil)
    }

    @Test("Without trimming the draft is saved as typed")
    func savesUntrimmed() async {
        let session = MonarchInlineEditTextSession()
        session.begin(with: "Plan")
        session.draft = "Plan "
        let outcome = await commit(session, value: "Plan", trims: false)
        #expect(outcome == .saved("Plan "))
    }

    @Test("An unchanged draft closes the editor without saving")
    func unchanged() async {
        let session = MonarchInlineEditTextSession()
        session.begin(with: "Plan")
        session.draft = " Plan "
        var calls = 0
        let outcome = await commit(session, value: "Plan", onSave: { _ in calls += 1 })
        #expect(outcome == .unchanged)
        #expect(calls == 0)
        #expect(!session.isEditing)
    }

    @Test("Validation refuses a draft, keeps the editor open and does not save")
    func invalid() async {
        let session = MonarchInlineEditTextSession()
        session.begin(with: "Plan")
        session.draft = ""
        var calls = 0
        let outcome = await commit(
            session,
            value: "Plan",
            validate: { $0.isEmpty ? "Enter a title." : nil },
            onSave: { _ in calls += 1 }
        )
        #expect(outcome == .invalid)
        #expect(calls == 0)
        #expect(session.isEditing)
        #expect(session.errorMessage == "Enter a title.")
        #expect(!session.savesOnFocusLoss, "a refused draft is not retried when focus leaves")
        session.draft = "Better"
        #expect(session.savesOnFocusLoss)
        let retry = await commit(session, value: "Plan", validate: { $0.isEmpty ? "Enter a title." : nil })
        #expect(retry == .saved("Better"))
    }

    @Test("A failed save keeps the editor open with the draft and the error's message")
    func fails() async {
        let session = MonarchInlineEditTextSession()
        session.begin(with: "Plan")
        session.draft = "Taken"
        let outcome = await commit(session, value: "Plan", onSave: { _ in throw Described() })
        #expect(outcome == .failed)
        #expect(session.isEditing)
        #expect(!session.isSaving)
        #expect(session.draft == "Taken")
        #expect(session.errorMessage == "The title is already in use.")
        #expect(!session.savesOnFocusLoss)
    }

    @Test("While a save runs, a second commit and a cancel are ignored")
    func pending() async {
        let session = MonarchInlineEditTextSession()
        session.begin(with: "Plan")
        session.draft = "New"
        var observed: [MonarchInlineEditTextSession.Outcome] = []
        var cancelled = true
        let outcome = await commit(
            session,
            value: "Plan",
            onSave: { _ in
                #expect(session.isSaving)
                #expect(!session.savesOnFocusLoss)
                observed.append(await commit(session, value: "Plan"))
                cancelled = session.cancel()
            }
        )
        #expect(observed == [.ignored])
        #expect(!cancelled)
        #expect(outcome == .saved("New"))
    }

    @Test("Cancel closes the editor and drops the draft; a commit when not editing is ignored")
    func cancels() async {
        let session = MonarchInlineEditTextSession()
        #expect(!session.cancel())
        #expect(await commit(session, value: "Plan") == .ignored)
        session.begin(with: "Plan")
        session.draft = "Draft"
        #expect(session.cancel())
        #expect(!session.isEditing)
        session.begin(with: "Plan")
        #expect(session.draft == "Plan")
    }

    @Test("A failed save uses the error's description, then the override, then the built-in sentence")
    func failureMessage() {
        #expect(
            MonarchInlineEditTextTokens.failureMessage(for: Described(), override: "Nope")
                == "The title is already in use."
        )
        #expect(MonarchInlineEditTextTokens.failureMessage(for: Plain(), override: "Nope") == "Nope")
        #expect(MonarchInlineEditTextTokens.failureMessage(for: Plain(), override: nil) == "Couldn’t save. Try again.")
        #expect(MonarchInlineEditTextTokens.failureMessage(for: Plain(), override: "") == "Couldn’t save. Try again.")
    }

    @Test("The editor's own error is shown before the app's")
    func shownError() {
        #expect(MonarchInlineEditTextTokens.shownError(local: "Local", external: "External") == "Local")
        #expect(MonarchInlineEditTextTokens.shownError(local: nil, external: "External") == "External")
        #expect(MonarchInlineEditTextTokens.shownError(local: "", external: "") == nil)
        #expect(MonarchInlineEditTextTokens.shownError(local: nil, external: nil) == nil)
    }

    @Test("VoiceOver reads the value or the prompt, the edit hint and the error")
    func accessibility() {
        #expect(MonarchInlineEditTextTokens.viewText(value: "Plan", prompt: "Untitled") == "Plan")
        #expect(MonarchInlineEditTextTokens.viewText(value: "", prompt: "Untitled") == "Untitled")
        #expect(MonarchInlineEditTextTokens.viewText(value: "", prompt: nil).isEmpty)
        #expect(MonarchInlineEditTextTokens.editHint(nil) == "Edit")
        #expect(MonarchInlineEditTextTokens.editHint("") == "Edit")
        #expect(MonarchInlineEditTextTokens.editHint("Rename") == "Rename")
        #expect(MonarchInlineEditTextTokens.fieldHint(error: "Too long") == "Error: Too long")
        #expect(MonarchInlineEditTextTokens.fieldHint(error: nil).isEmpty)
        #expect(MonarchInlineEditTextTokens.fieldHint(error: "").isEmpty)
    }

    @Test("Colours follow the state")
    func colors() {
        #expect(
            MonarchInlineEditTextTokens.viewForeground(isEmpty: false, isEnabled: true)
                == \MonarchColorTokens.textDefault
        )
        #expect(
            MonarchInlineEditTextTokens.viewForeground(isEmpty: true, isEnabled: true) == \MonarchColorTokens.textSubtle
        )
        #expect(
            MonarchInlineEditTextTokens.viewForeground(isEmpty: false, isEnabled: false)
                == \MonarchColorTokens.textDisabled
        )
        #expect(
            MonarchInlineEditTextTokens.viewForeground(isEmpty: true, isEnabled: false)
                == \MonarchColorTokens.textDisabled
        )
        #expect(MonarchInlineEditTextTokens.minHeight >= MonarchAccessibility.minimumHitTarget)
    }

    @Test("The built-in strings resolve")
    func strings() {
        #expect(MonarchInlineEditTextStrings.edit == "Edit")
        #expect(MonarchInlineEditTextStrings.save == "Save")
        #expect(MonarchInlineEditTextStrings.cancel == "Cancel")
        #expect(MonarchInlineEditTextStrings.saving == "Saving")
        #expect(MonarchInlineEditTextStrings.saveFailed == "Couldn’t save. Try again.")
        #expect(MonarchInlineEditTextStrings.error("Too long") == "Error: Too long")
    }

    #if os(iOS)
        // The touch target applies on the simulator.
        @Test("The text in view mode is at least as tall as the touch target")
        func hitTarget() {
            let view = MonarchInlineEditText("Title", text: .constant("Plan")) { _ in }
            #expect(Host.layout(view).height >= MonarchAccessibility.minimumHitTarget)
        }
    #endif
}
