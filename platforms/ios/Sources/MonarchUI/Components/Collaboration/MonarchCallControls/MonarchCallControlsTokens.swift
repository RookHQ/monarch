import MonarchTokens
import SwiftUI

/// Component tokens and pure logic of ``MonarchCallControls``.
enum MonarchCallControlsTokens {
    /// The colour of one control.
    enum Tone: Equatable, CaseIterable {
        /// A device that is on, or an action that is not running.
        case standard
        /// A device that is switched off: microphone or camera.
        case off
        /// An action in progress: screen sharing.
        case active
        /// A raised hand.
        case raised
        /// Leave.
        case danger
    }

    /// One control, in the order they are shown.
    enum Control: Equatable {
        case microphone
        case camera
        case share
        case hand
        case reactions
        case more
        case leave
    }

    /// The diameter of a round control, scaled with Dynamic Type. At least
    /// the 24-point target size of WCAG 2.2, so the row of controls stays one row on a phone.
    static let diameter: CGFloat = 34
    /// Extra padding before and after the icon of the wide Leave control.
    static let wideHorizontalPadding: CGFloat = MonarchTokens.Space.space3
    /// The gap between controls.
    static let spacing: CGFloat = MonarchTokens.Space.space1

    /// The controls that are shown: each needs its binding or action, and
    /// the minimal form leaves out the hand, reactions and More.
    static func controls(
        hasMicrophone: Bool,
        hasCamera: Bool,
        hasShare: Bool,
        hasHand: Bool,
        hasReactions: Bool,
        hasMoreItems: Bool,
        hasLeave: Bool,
        isMinimal: Bool
    ) -> [Control] {
        var result: [Control] = []
        if hasMicrophone { result.append(.microphone) }
        if hasCamera { result.append(.camera) }
        if hasShare { result.append(.share) }
        if hasHand && !isMinimal { result.append(.hand) }
        if hasReactions && !isMinimal { result.append(.reactions) }
        if hasMoreItems && !isMinimal { result.append(.more) }
        if hasLeave { result.append(.leave) }
        return result
    }

    /// The tone of a toggle for its state. A device that is off is marked,
    /// as are sharing and a raised hand while they are on.
    static func tone(_ control: Control, isOn: Bool) -> Tone {
        switch control {
        case .microphone, .camera: isOn ? .standard : .off
        case .share: isOn ? .active : .standard
        case .hand: isOn ? .raised : .standard
        case .leave: .danger
        case .reactions, .more: .standard
        }
    }

    /// The SF Symbol of a control for its state. Every state change also
    /// changes the symbol, so colour is never the only cue.
    static func symbol(_ control: Control, isOn: Bool) -> String {
        switch control {
        case .microphone: isOn ? "mic" : "mic.slash"
        case .camera: isOn ? "video" : "video.slash"
        case .share: isOn ? "arrow.up.square.fill" : "arrow.up.square"
        case .hand: isOn ? "hand.raised.fill" : "hand.raised"
        case .reactions: "face.smiling"
        case .more: "ellipsis"
        case .leave: "phone.down.fill"
        }
    }

    /// The colour roles of a control. Pure, so it is tested without a view.
    static func appearance(tone: Tone, isPressed: Bool, isEnabled: Bool) -> MonarchSurfaceAppearance {
        guard isEnabled else {
            return MonarchSurfaceAppearance(background: \.bgMuted, foreground: \.textDisabled, border: nil)
        }
        switch tone {
        case .standard:
            return MonarchSurfaceAppearance(
                background: isPressed ? \.bgEmphasis : \.bgMuted,
                foreground: \.textDefault,
                border: \.borderSubtle
            )
        case .off, .danger:
            return MonarchSurfaceAppearance(
                background: isPressed ? \.dangerSolidActive : \.dangerSolid,
                foreground: \.dangerOnSolid,
                border: nil
            )
        case .active:
            return MonarchSurfaceAppearance(
                background: isPressed ? \.successSolidActive : \.successSolid,
                foreground: \.successOnSolid,
                border: nil
            )
        case .raised:
            return MonarchSurfaceAppearance(
                background: isPressed ? \.warningSolidActive : \.warningSolid,
                foreground: \.warningOnSolid,
                border: nil
            )
        }
    }

    /// The spoken name of a reaction: "React 👍".
    static func reactionLabel(_ emoji: String, labels: MonarchCallControlsLabels) -> String {
        labels.reactWith?(emoji) ?? MonarchCallControlsStrings.reactWith(emoji)
    }

    /// The name of a layout.
    static func layoutName(_ layout: MonarchCallControlsLayout, labels: MonarchCallControlsLabels) -> String {
        labels.layoutName?(layout) ?? MonarchCallControlsStrings.layoutName(layout)
    }
}
