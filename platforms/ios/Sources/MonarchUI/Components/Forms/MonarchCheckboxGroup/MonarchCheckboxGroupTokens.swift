import MonarchTokens
import SwiftUI

/// Component tokens and selection logic for ``MonarchCheckboxGroup``.
enum MonarchCheckboxGroupTokens {
    /// The gap between choices in the vertical layout.
    static let itemSpacing: CGFloat = MonarchTokens.Space.space1
    /// The gap between choices in the horizontal layout.
    static let rowSpacing: CGFloat = MonarchTokens.Space.space4
    /// How far the choices are indented under "Select all".
    static let nestedIndent: CGFloat = MonarchTokens.Space.space6
    /// The gap between an option's label and its description.
    static let descriptionSpacing: CGFloat = MonarchTokens.Space.space0_5

    /// The choices select all acts on: those that are not disabled.
    static func enabledOptions(_ options: [MonarchCheckboxGroupOption]) -> [MonarchCheckboxGroupOption] {
        options.filter { !$0.isDisabled }
    }

    /// The selection with one value checked or cleared.
    static func setting(_ id: String, isOn: Bool, in selection: Set<String>) -> Set<String> {
        var next = selection
        if isOn { next.insert(id) } else { next.remove(id) }
        return next
    }

    /// Whether the horizontal layout is drawn as a stack.
    static func stacks(layout: MonarchCheckboxGroupLayout, isAccessibilitySize: Bool) -> Bool {
        layout == .vertical || isAccessibilitySize
    }

    /// The colour role of an option's description.
    static func descriptionColor(isEnabled: Bool) -> MonarchColorRole {
        isEnabled ? \.textMuted : \.textDisabled
    }
}
