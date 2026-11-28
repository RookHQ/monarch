import MonarchTokens
import SwiftUI

/// Component tokens for ``MonarchRecoveryCodes``.
enum MonarchRecoveryCodesTokens {
    /// The gap between the parts of the component.
    static let sectionSpacing: CGFloat = MonarchTokens.Space.space3
    /// The gap between codes in the grid.
    static let gridSpacing: CGFloat = MonarchTokens.Space.space2
    /// Padding inside the code panel.
    static let panelPadding: CGFloat = MonarchTokens.Space.space3
    /// The corner radius of the code panel.
    static let panelRadius: CGFloat = MonarchTokens.Radius.md
    /// The width of the panel border.
    static let borderWidth: CGFloat = MonarchShapes.borderWidth
    /// The least width of a code column before Dynamic Type: 112 points, two columns on a phone.
    static let columnMinWidth: CGFloat = MonarchTokens.Space.space24 + MonarchTokens.Space.space4
    /// The gap between buttons.
    static let actionSpacing: CGFloat = MonarchTokens.Space.space2
    /// The gap between the error icon and the error text.
    static let iconSpacing: CGFloat = MonarchTokens.Space.space1
    /// The SF Symbol of the show button.
    static let revealSymbol = "eye"
    /// The SF Symbol of the hide button.
    static let concealSymbol = "eye.slash"
    /// The SF Symbol of the share button.
    static let saveSymbol = "square.and.arrow.up"
    /// The SF Symbol of the save-to-Files button.
    static let saveToFilesSymbol = "square.and.arrow.down"
    /// The SF Symbol of the regenerate button.
    static let regenerateSymbol = "arrow.clockwise"
    /// The SF Symbol beside the hidden-codes hint.
    static let concealedSymbol = "lock"
    /// The SF Symbol before the acknowledgement error.
    static let errorSymbol = "exclamationmark.circle"
    /// The file name used when the person saves the codes, without a heading.
    static let defaultFileName = "recovery-codes.txt"

    /// The codes that still work, in order.
    static func unused(_ codes: [MonarchRecoveryCode]) -> [String] {
        codes.filter { !$0.isUsed }.map(\.code)
    }

    /// The copied and saved text: an optional heading line, a blank line,
    /// then one unused code per line.
    static func text(_ codes: [MonarchRecoveryCode], heading: String?) -> String {
        var lines: [String] = []
        if let heading, !heading.isEmpty { lines += [heading, ""] }
        lines += unused(codes)
        return lines.joined(separator: "\n")
    }

    /// Whether the count line is shown: once any code is used.
    static func showsCount(_ codes: [MonarchRecoveryCode]) -> Bool {
        codes.contains { $0.isUsed }
    }

    /// Whether the show and hide button is shown: by default when the codes start hidden.
    static func showsRevealToggle(startsRevealed: Bool, override: Bool?) -> Bool {
        override ?? !startsRevealed
    }

    /// A value that changes when the set of codes changes, so a new set needs a new acknowledgement.
    static func signature(_ codes: [MonarchRecoveryCode]) -> [String] {
        codes.map(\.code)
    }

    /// The colour role of a code: used codes are subtle and struck through.
    static func codeRole(isUsed: Bool) -> MonarchColorRole {
        isUsed ? \.textSubtle : \.textDefault
    }

    /// What VoiceOver reads after a code.
    static func codeValue(isUsed: Bool, usedLabel: String) -> String {
        isUsed ? usedLabel : ""
    }

    /// What pressing Continue does.
    enum ContinueResult: Equatable {
        /// Call the acknowledgement handler.
        case acknowledge
        /// Show the missing-acknowledgement error.
        case showError
    }

    /// The result of pressing Continue.
    static func continueResult(isAcknowledged: Bool) -> ContinueResult {
        isAcknowledged ? .acknowledge : .showError
    }
}
