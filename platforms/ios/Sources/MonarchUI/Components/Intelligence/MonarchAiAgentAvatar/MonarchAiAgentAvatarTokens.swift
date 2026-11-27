import Foundation
import MonarchTokens
import SwiftUI

/// Component tokens for ``MonarchAiAgentAvatar``.
enum MonarchAiAgentAvatarTokens {
    /// The gap between the picture and its ring.
    static let ringGap: CGFloat = MonarchTokens.Space.px
    /// The width of the state ring.
    static let ringWidth: CGFloat = MonarchTokens.Space.space0_5
    /// The dash and the gap of the waiting ring.
    static let dash: [CGFloat] = [MonarchTokens.Space.space1, MonarchTokens.Space.space0_5]
    /// The gap between the picture and the visible state text.
    static let stateSpacing: CGFloat = MonarchTokens.Space.space2
    /// How far the badge moves out over the corner, as a share of its side: a
    /// 20 to 40 pt avatar is mostly initials.
    static func badgeOutset(_ size: MonarchAvatarSize) -> CGFloat {
        switch size {
        case .xs, .sm, .md: 0.4
        case .lg, .xl: 0.2
        }
    }
    /// The ring in the surface colour around the badge.
    static let badgeRingWidth: CGFloat = MonarchTokens.Space.px
    /// The share of the badge the glyph fills.
    static let badgeGlyphRatio: CGFloat = 0.6
    /// The SF Symbol of the badge and of the fallback without initials: a chip, which reads as a machine.
    static let botSymbol = "cpu"
    /// The time one turn of the working ring takes, in seconds.
    static let ringPeriod: TimeInterval = MonarchPulse.period

    /// The side of the badge.
    static func badgeSide(_ size: MonarchAvatarSize) -> CGFloat {
        switch size {
        case .xs, .sm: MonarchTokens.Space.space3
        case .md: MonarchTokens.Space.space4
        case .lg: MonarchTokens.Space.space5
        case .xl: MonarchTokens.Space.space6
        }
    }

    /// The corner radius of the ring: the picture's radius grown by the gap and the ring.
    static func ringRadius(_ size: MonarchAvatarSize) -> CGFloat {
        MonarchAvatarTokens.squareRadius(size) + ringGap + ringWidth
    }

    /// The text the fallback initials are made from: the codename when there is one, else the name.
    static func initialsSource(_ agent: MonarchAiAgentRef) -> String {
        let codename = agent.codename?.trimmingCharacters(in: .whitespacesAndNewlines) ?? ""
        return codename.isEmpty ? agent.name : codename
    }

    /// The symbol shown instead of initials, when the source has no letter or digit.
    static func fallbackSymbol(_ agent: MonarchAiAgentRef) -> String? {
        let initials = MonarchAvatarTokens.initials(initialsSource(agent))
        return MonarchAvatarTokens.hasReadableInitials(initials) ? nil : botSymbol
    }

    /// The URL of the agent's picture, when it is a web address.
    static func imageURL(_ agent: MonarchAiAgentRef) -> URL? {
        guard let text = agent.avatar, let url = URL(string: text),
            let scheme = url.scheme?.lowercased(), scheme == "https" || scheme == "http"
        else { return nil }
        return url
    }

    /// The ring of a state.
    enum Ring: Equatable {
        /// No ring.
        case none
        /// A solid ring.
        case solid(MonarchColorRole)
        /// A dashed ring.
        case dashed(MonarchColorRole)
        /// A ring that turns, or a solid ring with Reduce Motion.
        case moving(MonarchColorRole, MonarchColorRole)
    }

    /// The ring of a state.
    static func ring(_ state: MonarchAiAgentState) -> Ring {
        switch state {
        case .idle: .none
        case .working: .moving(\.aiSolid, \.aiBorder)
        case .waiting: .dashed(\.warningStrong)
        case .error: .solid(\.dangerStrong)
        }
    }

    /// The fill and the glyph colour of the badge.
    static func badge(_ state: MonarchAiAgentState) -> (fill: MonarchColorRole, glyph: MonarchColorRole) {
        let fill: MonarchColorRole = state == .error ? \.dangerSolid : \.aiSolid
        let glyph: MonarchColorRole = state == .error ? \.dangerOnSolid : \.aiOnSolid
        return (fill, glyph)
    }

    /// The angle of the working ring at a time, one turn per period.
    static func ringAngle(at time: TimeInterval) -> Angle {
        let phase = time.truncatingRemainder(dividingBy: ringPeriod) / ringPeriod
        return .degrees((phase < 0 ? phase + 1 : phase) * 360)
    }

    /// The name of a state.
    static func stateName(_ state: MonarchAiAgentState, labels: MonarchAiAgentAvatarLabels) -> String {
        labels.state?(state) ?? MonarchAiAgentAvatarStrings.state(state)
    }

    /// What VoiceOver reads, or `nil` when the avatar is hidden.
    ///
    /// The name, the kind and the state unless idle. A decorative avatar (its
    /// name is written beside it) is hidden, except that a state other than
    /// idle is still read on its own.
    static func accessibilityLabel(
        name: String,
        state: MonarchAiAgentState,
        isDecorative: Bool,
        labels: MonarchAiAgentAvatarLabels
    ) -> String? {
        let stateName = state == .idle ? nil : stateName(state, labels: labels)
        if isDecorative { return stateName }
        let kind = labels.kind ?? MonarchAiAgentAvatarStrings.kind
        guard let stateName else { return MonarchAiAgentAvatarStrings.label(name: name, kind: kind) }
        return MonarchAiAgentAvatarStrings.labelWithState(name: name, kind: kind, state: stateName)
    }
}
