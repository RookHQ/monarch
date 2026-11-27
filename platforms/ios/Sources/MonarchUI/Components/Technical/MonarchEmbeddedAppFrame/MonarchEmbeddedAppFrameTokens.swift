import MonarchTokens
import SwiftUI

/// Component tokens for ``MonarchEmbeddedAppFrame``.
enum MonarchEmbeddedAppFrameTokens {
    /// The padding around the content and the messages.
    static let padding: CGFloat = MonarchTokens.Space.space5
    /// The gap between the lines of a message.
    static let spacing: CGFloat = MonarchTokens.Space.space3
    /// The gap between the buttons of the failure message.
    static let actionSpacing: CGFloat = MonarchTokens.Space.space2
    /// The height of the frame while it loads or failed, when the app gives none.
    static let placeholderHeight: CGFloat = MonarchTokens.Space.space24 + MonarchTokens.Space.space16
    /// The symbol of the failure message.
    static let failureSymbol = "exclamationmark.triangle"
    /// The fill of the frame.
    static var fill: MonarchColorRole { \.bgSurface }
    /// The border of the frame.
    static var border: MonarchColorRole { \.borderSubtle }
    /// The colour of supporting text.
    static var supportingColor: MonarchColorRole { \.textMuted }

    /// What VoiceOver reads as the value of the frame in a state.
    static func accessibilityValue(_ state: MonarchEmbeddedAppFrameState, labels: MonarchEmbeddedAppFrameLabels)
        -> String
    {
        switch state {
        case .loading: labels.loading ?? MonarchEmbeddedAppFrameStrings.loading
        case .ready: ""
        case .failed: labels.errorTitle ?? MonarchEmbeddedAppFrameStrings.errorTitle
        }
    }
}
