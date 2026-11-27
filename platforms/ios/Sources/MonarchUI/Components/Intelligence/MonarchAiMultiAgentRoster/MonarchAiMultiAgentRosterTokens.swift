import MonarchTokens
import SwiftUI

/// Component tokens and pure logic of ``MonarchAiMultiAgentRoster``.
enum MonarchAiMultiAgentRosterTokens {
    /// The gap between the rows, and between the body and the summary.
    static let spacing: CGFloat = MonarchTokens.Space.space1
    /// The gap between the parts of a row.
    static let rowSpacing: CGFloat = MonarchTokens.Space.space3
    /// The gap between the lines of text in a row.
    static let textSpacing: CGFloat = MonarchTokens.Space.space0_5
    /// Padding before and after a row's content.
    static let rowHorizontalPadding: CGFloat = MonarchTokens.Space.space2
    /// Padding above and below a row's content.
    static let rowVerticalPadding: CGFloat = MonarchTokens.Space.space2
    /// The corner radius of a tinted row.
    static let rowCornerRadius: CGFloat = MonarchTokens.Radius.md
    /// The gap between the stack and the summary.
    static let stackSpacing: CGFloat = MonarchTokens.Space.space2
    /// The size of the avatars in the list.
    static let listAvatarSize: MonarchAvatarSize = .md
    /// The size of the avatars in the stack.
    static let stackAvatarSize: MonarchAvatarSize = .sm
    /// The weight of an agent's name and of the state.
    static let nameWeight = MonarchTextStyleSpec.fontWeight(MonarchTokens.FontWeight.semibold)
    /// The order of the stack: agents that need attention first.
    static let attentionOrder: [MonarchAiAgentState] = [.error, .waiting, .working, .idle]

    /// The fill of a row: warning for waiting, danger for error, none otherwise.
    static func rowBackground(_ state: MonarchAiAgentState) -> MonarchColorRole? {
        switch state {
        case .waiting: \.warningSubtle
        case .error: \.dangerSubtle
        case .idle, .working: nil
        }
    }

    /// The colour of the state text.
    static func stateRole(_ state: MonarchAiAgentState) -> MonarchColorRole {
        switch state {
        case .idle: \.textMuted
        case .working: \.aiStrong
        case .waiting: \.warningStrong
        case .error: \.dangerStrong
        }
    }

    /// The members sorted so agents that need attention come first, keeping
    /// the given order within a state.
    static func sortedForStack(_ members: [MonarchAiMultiAgentRosterMember]) -> [MonarchAiMultiAgentRosterMember] {
        members.enumerated()
            .sorted { lhs, rhs in
                let left = attentionOrder.firstIndex(of: lhs.element.state) ?? attentionOrder.count
                let right = attentionOrder.firstIndex(of: rhs.element.state) ?? attentionOrder.count
                return left == right ? lhs.offset < rhs.offset : left < right
            }
            .map(\.element)
    }

    /// The members hidden behind "+N", with the same rule as ``MonarchAvatarGroup``.
    static func hidden(
        _ sorted: [MonarchAiMultiAgentRosterMember],
        maxVisible: Int
    ) -> [MonarchAiMultiAgentRosterMember] {
        let visible = MonarchOverflow.visibleCount(total: sorted.count, maxVisible: max(1, maxVisible))
        return Array(sorted.dropFirst(visible))
    }

    /// The name of a state.
    static func stateName(_ state: MonarchAiAgentState, labels: MonarchAiMultiAgentRosterLabels) -> String {
        labels.state?(state) ?? MonarchAiAgentAvatarStrings.state(state)
    }

    /// How many agents are in each state.
    static func counts(_ members: [MonarchAiMultiAgentRosterMember]) -> [MonarchAiAgentState: Int] {
        var counts: [MonarchAiAgentState: Int] = [:]
        for state in MonarchAiAgentState.allCases { counts[state] = 0 }
        for member in members { counts[member.state, default: 0] += 1 }
        return counts
    }

    /// The summary line: "3 agents · 2 working · 1 waiting for you · 1 with errors".
    static func summary(_ members: [MonarchAiMultiAgentRosterMember], labels: MonarchAiMultiAgentRosterLabels) -> String
    {
        let counts = counts(members)
        if let summary = labels.summary { return summary(members.count, counts) }
        var parts = [MonarchAiMultiAgentRosterStrings.agents(members.count)]
        if let working = counts[.working], working > 0 {
            parts.append(MonarchAiMultiAgentRosterStrings.working(working))
        }
        if let waiting = counts[.waiting], waiting > 0 {
            parts.append(MonarchAiMultiAgentRosterStrings.waiting(waiting))
        }
        if let errors = counts[.error], errors > 0 { parts.append(MonarchAiMultiAgentRosterStrings.errors(errors)) }
        return parts.joined(separator: MonarchAiMultiAgentRosterStrings.separator)
    }

    /// The states by member id, to compare with the next update.
    static func snapshot(_ members: [MonarchAiMultiAgentRosterMember]) -> [String: MonarchAiAgentState] {
        Dictionary(members.map { ($0.id, $0.state) }, uniquingKeysWith: { _, last in last })
    }

    /// What to announce after an update: one line per agent whose state
    /// changed or who joined, joined with ". ". `nil` when nothing changed
    /// or there was no earlier state (the first state is never announced).
    static func announcement(
        previous: [String: MonarchAiAgentState]?,
        current: [MonarchAiMultiAgentRosterMember],
        labels: MonarchAiMultiAgentRosterLabels
    ) -> String? {
        guard let previous else { return nil }
        let lines =
            current
            .filter { previous[$0.id] != $0.state }
            .map { member in
                let state = stateName(member.state, labels: labels)
                return labels.changed?(member.agent.name, state)
                    ?? MonarchAiMultiAgentRosterStrings.changed(member.agent.name, state)
            }
        return lines.isEmpty ? nil : lines.joined(separator: ". ")
    }

    /// What VoiceOver reads for a list row: the name with its kind, the
    /// role, the task and the state.
    static func rowLabel(_ member: MonarchAiMultiAgentRosterMember, labels: MonarchAiMultiAgentRosterLabels) -> String {
        let kind = labels.agentKind ?? MonarchAiAgentAvatarStrings.kind
        let parts = [
            MonarchAiMultiAgentRosterStrings.agentName(member.agent.name, kind: kind),
            member.agent.role,
            member.task,
            stateName(member.state, labels: labels),
        ]
        return parts.compactMap { $0 }.filter { !$0.isEmpty }.joined(separator: ", ")
    }

    /// What VoiceOver reads for the "+N" tile: each hidden agent with its state.
    static func hiddenLabel(_ hidden: [MonarchAiMultiAgentRosterMember], labels: MonarchAiMultiAgentRosterLabels)
        -> String
    {
        hidden
            .map { MonarchAiMultiAgentRosterStrings.hiddenAgent($0.agent.name, stateName($0.state, labels: labels)) }
            .joined(separator: "; ")
    }
}
