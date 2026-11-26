import MonarchTokens
import SwiftUI

/// Component tokens for ``MonarchAiWelcome``.
enum MonarchAiWelcomeTokens {
    /// The gap between the parts of the welcome.
    static let spacing: CGFloat = MonarchTokens.Space.space4
    /// Padding above and below the welcome.
    static let verticalPadding: CGFloat = MonarchTokens.Space.space6
    /// The gap between the agent's name and role.
    static let agentSpacing: CGFloat = MonarchTokens.Space.space0_5
    /// The gap between capabilities.
    static let capabilitySpacing: CGFloat = MonarchTokens.Space.space2
    /// The gap between a check mark and its capability.
    static let iconSpacing: CGFloat = MonarchTokens.Space.space2
    /// The widest the capability list grows, so the checklist stays a column: 384 points.
    static let capabilitiesMaxWidth: CGFloat = MonarchTokens.Space.space24 * 4
    /// The side of the bot mark before Dynamic Type.
    static let markSide: CGFloat = MonarchTokens.Space.space12
    /// The corner radius of the bot mark, the same as an agent avatar.
    static let markRadius: CGFloat = MonarchTokens.Radius.lg
    /// The bot symbol, the same one the agent avatar badge uses.
    static let botSymbol = MonarchAiAgentAvatarTokens.botSymbol
    /// The check mark before each capability.
    static let checkSymbol = "checkmark"
    /// The fill of the bot mark.
    static var markFill: MonarchColorRole { \.aiSubtle }
    /// The glyph of the bot mark.
    static var markGlyph: MonarchColorRole { \.aiStrong }
    /// The check mark colour.
    static var checkRole: MonarchColorRole { \.aiStrong }

    /// The capabilities that are shown: blank ones are left out.
    static func capabilities(_ capabilities: [String]) -> [String] {
        capabilities.filter { !$0.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty }
    }

    /// What VoiceOver reads for the agent line: "AI assistant: name, role".
    static func agentLabel(_ agent: MonarchAiAgentRef, prefix: String?) -> String {
        let name = [agent.name, agent.role].compactMap { $0 }.filter { !$0.isEmpty }.joined(separator: ", ")
        return MonarchAiWelcomeStrings.agentLabel(prefix: prefix ?? MonarchAiWelcomeStrings.agentPrefix, name: name)
    }
}
