import Foundation
import MonarchTokens
import SwiftUI
import Testing

@testable import MonarchUI

private struct UnsavedChangesFixtureForm: View {
    let hasChanges: Bool
    var canSave = true
    var guardsBackButton = true
    var labels = MonarchUnsavedChangesGuardLabels()

    private var save: (@MainActor () async throws -> Void)? {
        guard canSave else { return nil }
        return {}
    }

    var body: some View {
        NavigationStack {
            VStack(alignment: .leading) {
                Text("Edit the contact").monarchTextStyle(.heading)
                Text(hasChanges ? "Changed" : "Saved").monarchTextStyle(.body)
            }
            .navigationTitle("Contact")
            .monarchUnsavedChangesGuard(
                isPresented: .constant(false),
                hasChanges: hasChanges,
                labels: labels,
                guardsBackButton: guardsBackButton,
                onSave: save,
                onLeave: { _ in }
            )
        }
        .frame(minHeight: MonarchTokens.Space.space24 * 2)
    }
}

private struct SaveFailure: LocalizedError {
    var errorDescription: String? { "The server is offline." }
}

private struct SilentFailure: Error {}

enum MonarchUnsavedChangesGuardFixtures: ComponentFixtures {
    static let id = "unsaved-changes-guard"
    static let fixtures = [
        Fixture("with changes, save offered") {
            UnsavedChangesFixtureForm(hasChanges: true)
        },
        Fixture("without changes") {
            UnsavedChangesFixtureForm(hasChanges: false)
        },
        Fixture("with changes, no save, back button kept") {
            UnsavedChangesFixtureForm(hasChanges: true, canSave: false, guardsBackButton: false)
        },
        Fixture("custom labels") {
            UnsavedChangesFixtureForm(
                hasChanges: true,
                labels: MonarchUnsavedChangesGuardLabels(title: "Leave the draft?", back: "Contacts")
            )
        },
    ]
}

@Suite("MonarchUnsavedChangesGuard")
struct MonarchUnsavedChangesGuardTests {
    @Test("Save and leave comes first only when there is a save action; Keep editing is last")
    func choices() {
        #expect(MonarchUnsavedChangesGuardTokens.choices(canSave: true) == [.save, .discard, .keepEditing])
        #expect(MonarchUnsavedChangesGuardTokens.choices(canSave: false) == [.discard, .keepEditing])
    }

    @Test("Leaving is blocked while there are changes or a save runs")
    func locked() {
        #expect(MonarchUnsavedChangesGuardTokens.isLocked(hasChanges: true, isSaving: false))
        #expect(MonarchUnsavedChangesGuardTokens.isLocked(hasChanges: false, isSaving: true))
        #expect(MonarchUnsavedChangesGuardTokens.isLocked(hasChanges: true, isSaving: true))
        #expect(!MonarchUnsavedChangesGuardTokens.isLocked(hasChanges: false, isSaving: false))
    }

    @Test("A failed save shows the error's description, or the fallback when it has none")
    func failureMessage() {
        #expect(
            MonarchUnsavedChangesGuardTokens.failureMessage(for: SaveFailure(), fallback: "Try again.")
                == "The server is offline."
        )
        #expect(
            MonarchUnsavedChangesGuardTokens.failureMessage(for: SilentFailure(), fallback: "Try again.")
                == "Try again."
        )
    }

    @Test("The built-in strings come from the table")
    func builtIn() {
        let text = MonarchUnsavedChangesGuardTokens.text(MonarchUnsavedChangesGuardLabels())
        #expect(text.title == "Discard unsaved changes?")
        #expect(text.message == "Your changes have not been saved. If you leave now, they will be lost.")
        #expect(text.keepEditing == "Keep editing")
        #expect(text.discard == "Discard changes")
        #expect(text.save == "Save and leave")
        #expect(text.saveFailed == "Your changes could not be saved. Try again.")
        #expect(text.back == "Back")
        #expect(text.saving == "Saving")
    }

    @Test("Every built-in string can be replaced")
    func replaced() {
        let text = MonarchUnsavedChangesGuardTokens.text(
            MonarchUnsavedChangesGuardLabels(
                title: "T",
                message: "M",
                keepEditing: "K",
                discard: "D",
                save: "S",
                saveFailed: "F",
                back: "B",
                saving: "G"
            )
        )
        #expect(
            text
                == MonarchUnsavedChangesGuardText(
                    title: "T",
                    message: "M",
                    keepEditing: "K",
                    discard: "D",
                    save: "S",
                    saveFailed: "F",
                    back: "B",
                    saving: "G"
                )
        )
    }

    @Test("The back symbol follows the reading direction")
    func backSymbol() {
        #expect(MonarchUnsavedChangesGuardTokens.backSymbol.hasSuffix(".backward"))
        #expect(MonarchUnsavedChangesGuardTokens.savingFill == \MonarchColorTokens.bgRaised)
    }
}
