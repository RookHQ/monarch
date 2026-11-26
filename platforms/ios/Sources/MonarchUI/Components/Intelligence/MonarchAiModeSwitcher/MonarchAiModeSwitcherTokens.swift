import MonarchTokens
import SwiftUI

/// Component tokens for ``MonarchAiModeSwitcher``.
enum MonarchAiModeSwitcherTokens {
    /// The gap between the control and the description.
    static let spacing: CGFloat = MonarchTokens.Space.space1_5
    /// The colour of the description.
    static var description: MonarchColorRole { \.textMuted }
    /// The colour of the description while the switcher is disabled.
    static var disabledDescription: MonarchColorRole { \.textDisabled }

    /// The Monarch segmented control size of a switcher size.
    static func segmentedSize(_ size: MonarchAiModeSwitcherSize) -> MonarchButtonSize {
        switch size {
        case .sm: .sm
        case .md: .md
        }
    }

    /// The SwiftUI control size of a switcher size.
    static func controlSize(_ size: MonarchAiModeSwitcherSize) -> ControlSize {
        switch size {
        case .sm: .small
        case .md: .regular
        }
    }

    /// Whether the switcher has a description line: it is asked for and at
    /// least one mode has a description, so the line keeps its place while
    /// the selection changes.
    static func describes(modes: [MonarchAiMode], showsDescription: Bool) -> Bool {
        showsDescription && modes.contains { !($0.description ?? "").isEmpty }
    }

    /// The description of the selected mode, or `nil`.
    static func description(selection: String, modes: [MonarchAiMode]) -> String? {
        guard let text = modes.first(where: { $0.id == selection })?.description, !text.isEmpty else {
            return nil
        }
        return text
    }

    /// Whether a new selection is taken: it names a mode that is not disabled.
    static func accepts(_ id: String, modes: [MonarchAiMode]) -> Bool {
        modes.contains { $0.id == id && !$0.isDisabled }
    }

    /// Whether a segment shows only its symbol.
    static func showsIconOnly(_ mode: MonarchAiMode) -> Bool {
        mode.isIconOnly && mode.systemImage != nil
    }
}
