import MonarchTokens
import SwiftUI

/// Component tokens for ``MonarchAiCodeBlock``.
enum MonarchAiCodeBlockTokens {
    /// The gap between the code and the action row.
    static let spacing: CGFloat = MonarchTokens.Space.space2
    /// The gap between actions.
    static let actionSpacing: CGFloat = MonarchTokens.Space.space2

    /// The name of the code: the title, else "<Language> code", else "Code". The same name
    /// the code block gives its region and its copy button.
    static func name(title: String?, language: String?, labels: MonarchAiCodeBlockLabels) -> String {
        MonarchCodeBlockTokens.name(title: title, language: language, labels: labels.codeBlock)
    }

    /// The title of the apply button.
    static func applyTitle(_ labels: MonarchAiCodeBlockLabels) -> String {
        labels.apply ?? MonarchAiCodeBlockStrings.apply
    }

    /// The title of the insert button.
    static func insertTitle(_ labels: MonarchAiCodeBlockLabels) -> String {
        labels.insert ?? MonarchAiCodeBlockStrings.insert
    }

    /// The spoken name of an action: its title and the name of the code, so two blocks'
    /// buttons can be told apart.
    static func actionLabel(_ title: String, target: String, labels: MonarchAiCodeBlockLabels) -> String {
        labels.actionLabel?(title, target) ?? MonarchAiCodeBlockStrings.actionLabel(title, target: target)
    }

    /// The hint of an action: empty when it can be used, else why it cannot.
    static func actionHint(isIncomplete: Bool, labels: MonarchAiCodeBlockLabels) -> String {
        isIncomplete ? labels.incompleteHint ?? MonarchAiCodeBlockStrings.incompleteHint : ""
    }

    /// Whether Apply and Insert can be used: not while the code is still arriving.
    static func actionsEnabled(isIncomplete: Bool) -> Bool {
        !isIncomplete
    }

    /// Whether the action row is shown: when there is an action or a custom control.
    static func showsActionRow(hasApply: Bool, hasInsert: Bool, hasCustomActions: Bool) -> Bool {
        hasApply || hasInsert || hasCustomActions
    }
}
