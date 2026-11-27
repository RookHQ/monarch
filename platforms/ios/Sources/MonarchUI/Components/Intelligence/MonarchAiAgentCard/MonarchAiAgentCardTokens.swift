import Foundation
import MonarchTokens
import SwiftUI

/// Component tokens and pure logic for ``MonarchAiAgentCard``.
enum MonarchAiAgentCardTokens {
    /// The gap between the header, the description and the sections.
    static let sectionSpacing: CGFloat = MonarchTokens.Space.space4
    /// The gap between a section's heading and its content, and between list items.
    static let itemSpacing: CGFloat = MonarchTokens.Space.space2
    /// The gap between the avatar and the name, and between tags.
    static let headerSpacing: CGFloat = MonarchTokens.Space.space3
    /// The gap between the name and the role line, and inside a tool.
    static let lineSpacing: CGFloat = MonarchTokens.Space.space0_5
    /// The gap between the tags.
    static let tagSpacing: CGFloat = MonarchTokens.Space.space1_5
    /// The SF Symbol before each data access item.
    static let dataSymbol = "eye"
    /// The SF Symbol before each tool.
    static let toolSymbol = "wrench.and.screwdriver"
    /// The separator between the role and the kind.
    static let separator = " · "

    /// A part of the full card.
    enum Section: Equatable {
        /// The capability tags.
        case capabilities
        /// The tools with their descriptions.
        case tools
        /// The data the agent can read.
        case dataAccess
        /// The owner.
        case owner
    }

    /// The avatar size: large on the full card, medium on the compact one.
    static func avatarSize(_ variant: MonarchAiAgentCardVariant) -> MonarchAvatarSize {
        variant == .full ? .lg : .md
    }

    /// The line under the name: the role and "AI agent".
    static func subtitle(_ agent: MonarchAiAgentRef, labels: MonarchAiAgentCardLabels) -> String {
        let kind = labels.kind ?? MonarchAiAgentAvatarStrings.kind
        guard let role = agent.role?.trimmingCharacters(in: .whitespacesAndNewlines), !role.isEmpty else {
            return kind
        }
        return role + separator + kind
    }

    /// The sections the card shows, in order. The compact card shows its
    /// capabilities without a heading and no other section.
    static func sections(
        variant: MonarchAiAgentCardVariant,
        capabilities: [String],
        tools: [MonarchAiAgentTool],
        dataAccess: [String],
        owner: String?
    ) -> [Section] {
        guard variant == .full else { return [] }
        var sections: [Section] = []
        if !capabilities.isEmpty { sections.append(.capabilities) }
        if !tools.isEmpty { sections.append(.tools) }
        if !dataAccess.isEmpty { sections.append(.dataAccess) }
        if let owner, !owner.isEmpty { sections.append(.owner) }
        return sections
    }

    /// The tool count of the compact card, or `nil` without tools or on the full card.
    static func toolCount(_ count: Int, variant: MonarchAiAgentCardVariant, labels: MonarchAiAgentCardLabels) -> String?
    {
        guard variant == .compact, count > 0 else { return nil }
        return labels.toolCount?(count) ?? MonarchAiAgentCardStrings.toolCount(count)
    }

    /// The spoken tool: its name, then what it does.
    static func toolLabel(_ tool: MonarchAiAgentTool) -> String {
        [tool.name, tool.description ?? ""].filter { !$0.isEmpty }.joined(separator: ", ")
    }

    /// The spoken owner: "Owner, Platform team".
    static func ownerLabel(_ owner: String, labels: MonarchAiAgentCardLabels) -> String {
        "\(labels.owner ?? MonarchAiAgentCardStrings.owner), \(owner)"
    }
}
