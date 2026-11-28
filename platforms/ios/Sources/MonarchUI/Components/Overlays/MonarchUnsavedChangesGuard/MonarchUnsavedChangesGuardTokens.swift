import Foundation
import MonarchTokens
import SwiftUI

/// One choice in the unsaved-changes question.
enum MonarchUnsavedChangesGuardChoice: Equatable {
    /// Save, then leave.
    case save
    /// Leave without saving.
    case discard
    /// Stay on the screen.
    case keepEditing
}

/// The resolved strings of the guard: the app's replacements, else the built-in ones.
struct MonarchUnsavedChangesGuardText: Equatable {
    let title: String
    let message: String
    let keepEditing: String
    let discard: String
    let save: String
    let saveFailed: String
    let back: String
    let saving: String
}

/// Component tokens and logic for ``MonarchUnsavedChangesGuard``.
enum MonarchUnsavedChangesGuardTokens {
    /// The SF Symbol of the guarding back button. A `backward` symbol, so it follows the reading direction.
    static let backSymbol = "chevron.backward"
    /// The gap between the buttons of the question's footer.
    static let footerSpacing: CGFloat = MonarchTokens.Space.space2
    /// The padding around the saving indicator.
    static let savingPadding: CGFloat = MonarchTokens.Space.space4
    /// The shadow of the saving indicator.
    static let savingElevation: MonarchElevationLevel = .md
    /// The surface of the saving indicator.
    static var savingFill: MonarchColorRole { \.bgRaised }
    /// The colour of the saving label.
    static var savingText: MonarchColorRole { \.textDefault }

    /// The choices offered, in order: Save and leave when there is a save action, Discard
    /// changes, and Keep editing as the cancel choice.
    static func choices(canSave: Bool) -> [MonarchUnsavedChangesGuardChoice] {
        (canSave ? [.save] : []) + [.discard, .keepEditing]
    }

    /// Whether leaving by a swipe or the back button is blocked: while there are changes or a save runs.
    static func isLocked(hasChanges: Bool, isSaving: Bool) -> Bool {
        hasChanges || isSaving
    }

    /// The message shown when saving fails: the error's description when it has one, else the fallback.
    static func failureMessage(for error: any Error, fallback: String) -> String {
        MonarchFailureMessage.message(for: error, fallback: fallback)
    }

    /// The strings shown, with the app's replacements applied.
    static func text(_ labels: MonarchUnsavedChangesGuardLabels) -> MonarchUnsavedChangesGuardText {
        MonarchUnsavedChangesGuardText(
            title: labels.title ?? MonarchUnsavedChangesGuardStrings.title,
            message: labels.message ?? MonarchUnsavedChangesGuardStrings.message,
            keepEditing: labels.keepEditing ?? MonarchUnsavedChangesGuardStrings.keepEditing,
            discard: labels.discard ?? MonarchUnsavedChangesGuardStrings.discard,
            save: labels.save ?? MonarchUnsavedChangesGuardStrings.save,
            saveFailed: labels.saveFailed ?? MonarchUnsavedChangesGuardStrings.saveFailed,
            back: labels.back ?? MonarchUnsavedChangesGuardStrings.back,
            saving: labels.saving ?? MonarchUnsavedChangesGuardStrings.saving
        )
    }
}
