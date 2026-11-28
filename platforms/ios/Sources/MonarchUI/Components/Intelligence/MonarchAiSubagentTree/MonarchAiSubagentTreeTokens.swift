import Foundation
import MonarchTokens
import SwiftUI

/// Component tokens and pure logic of ``MonarchAiSubagentTree``.
enum MonarchAiSubagentTreeTokens {
    /// The gap between the header, the tree and the action bar.
    static let spacing: CGFloat = MonarchTokens.Space.space3
    /// The gap between rows.
    static let rowSpacing: CGFloat = MonarchTokens.Space.space1
    /// The gap between the lines of a row.
    static let lineSpacing: CGFloat = MonarchTokens.Space.space0_5
    /// The gap between the parts of a line.
    static let itemSpacing: CGFloat = MonarchTokens.Space.space2
    /// How far each level is indented.
    static let childIndent: CGFloat = MonarchTokens.Space.space6
    /// Padding inside a row.
    static let rowPadding: CGFloat = MonarchTokens.Space.space2
    /// Padding inside the action bar.
    static let barPadding: CGFloat = MonarchTokens.Space.space3
    /// The width of the bar that marks the selected row.
    static let selectionBarWidth: CGFloat = MonarchTokens.Space.space1
    /// The chevron of an agent with sub-agents.
    static let chevronSymbol = "chevron.forward"
    /// The turn of an open chevron, a quarter turn.
    static let openRotation: Double = 90
    /// The fill of the selected row.
    static var selectedFill: MonarchColorRole { \.bgSubtle }
    /// The bar that marks the selected row, so selection is not colour alone.
    static var selectionBar: MonarchColorRole { \.textDefault }
    /// The fill of the action bar.
    static var barFill: MonarchColorRole { \.bgSubtle }

    /// The chevron's turn: none when closed, a quarter turn towards the
    /// content when open, mirrored in right-to-left layouts.
    static func chevronRotation(isExpanded: Bool, layoutDirection: LayoutDirection) -> Angle {
        guard isExpanded else { return .zero }
        return .degrees(layoutDirection == .rightToLeft ? -openRotation : openRotation)
    }

    // MARK: Statuses

    /// Every status in the order the header lists them.
    static let headerOrder: [MonarchAiSubagentStatus] = [.working, .needsInput, .waiting, .done, .failed, .stopped]

    /// The tone and symbol of a status, so it is never told by colour alone.
    static func look(_ status: MonarchAiSubagentStatus) -> (tone: MonarchTone, symbol: String) {
        switch status {
        case .working: (.ai, "arrow.triangle.2.circlepath")
        case .needsInput: (.warning, "questionmark.circle")
        case .waiting: (.neutral, "clock")
        case .done: (.success, "checkmark.circle")
        case .failed: (.danger, "xmark.octagon")
        case .stopped: (.neutral, "minus.circle")
        }
    }

    /// The ring of the agent's avatar: turning while working, dashed while it
    /// waits for a person or to start, red once failed, none otherwise.
    static func avatarState(_ status: MonarchAiSubagentStatus) -> MonarchAiAgentState {
        switch status {
        case .working: .working
        case .needsInput, .waiting: .waiting
        case .failed: .error
        case .done, .stopped: .idle
        }
    }

    /// Whether the agent is active: working or waiting for a person.
    static func isActive(_ status: MonarchAiSubagentStatus) -> Bool {
        status == .working || status == .needsInput
    }

    /// Whether the agent has ended: done, failed or stopped.
    static func hasEnded(_ status: MonarchAiSubagentStatus) -> Bool {
        status == .done || status == .failed || status == .stopped
    }

    // MARK: Tree

    /// Every agent in reading order, parent first.
    static func flattened(_ agents: [MonarchAiSubagent]) -> [MonarchAiSubagent] {
        agents.flatMap { [$0] + flattened($0.children) }
    }

    /// The agents of the whole tree by status, and the total.
    static func counts(_ agents: [MonarchAiSubagent]) -> (total: Int, byStatus: [MonarchAiSubagentStatus: Int]) {
        let all = flattened(agents)
        return (all.count, Dictionary(grouping: all, by: \.status).mapValues(\.count))
    }

    /// The agents open at first: every agent with sub-agents that is active or
    /// has an active agent somewhere under it, so nothing active is hidden.
    static func defaultExpanded(_ agents: [MonarchAiSubagent]) -> Set<String> {
        var out = Set<String>()
        @discardableResult
        func walk(_ agent: MonarchAiSubagent) -> Bool {
            var active = isActive(agent.status)
            for child in agent.children {
                let childIsActive = walk(child)
                active = active || childIsActive
            }
            if active, !agent.children.isEmpty { out.insert(agent.id) }
            return active
        }
        for agent in agents { walk(agent) }
        return out
    }

    /// The agent with an id, anywhere in the tree.
    static func agent(_ id: String?, in agents: [MonarchAiSubagent]) -> MonarchAiSubagent? {
        guard let id else { return nil }
        return flattened(agents).first { $0.id == id }
    }

    /// The expanded set after an agent is opened or closed.
    static func toggled(_ expanded: Set<String>, id: String, isExpanded: Bool) -> Set<String> {
        var next = expanded
        if isExpanded { next.insert(id) } else { next.remove(id) }
        return next
    }

    /// The agents whose status is news since the last look: a changed status,
    /// or a new agent that already needs input or has failed. `nil` before the
    /// first look, so the first statuses are never announced.
    static func changes(
        from previous: [String: MonarchAiSubagentStatus]?,
        to agents: [MonarchAiSubagent]
    ) -> [MonarchAiSubagent] {
        guard let previous else { return [] }
        return flattened(agents).filter { agent in
            guard let before = previous[agent.id] else {
                return agent.status == .needsInput || agent.status == .failed
            }
            return before != agent.status
        }
    }

    /// The status of every agent by id.
    static func statuses(_ agents: [MonarchAiSubagent]) -> [String: MonarchAiSubagentStatus] {
        Dictionary(flattened(agents).map { ($0.id, $0.status) }, uniquingKeysWith: { first, _ in first })
    }

    // MARK: Text

    /// The name of a status.
    static func statusName(_ status: MonarchAiSubagentStatus, labels: MonarchAiSubagentTreeLabels) -> String {
        labels.status?(status) ?? MonarchAiSubagentTreeStrings.status(status)
    }

    /// The header: "4 agents: 2 working, 1 needs you, 1 done", or the empty text.
    static func summary(_ agents: [MonarchAiSubagent], locale: Locale, labels: MonarchAiSubagentTreeLabels) -> String {
        let counts = counts(agents)
        guard counts.total > 0 else { return labels.empty ?? MonarchAiSubagentTreeStrings.empty }
        let total =
            labels.agentCount?(counts.total)
            ?? MonarchAiSubagentTreeStrings.agentCount(counts.total, counts.total.formatted(.number.locale(locale)))
        let parts = headerOrder.compactMap { status -> String? in
            guard let count = counts.byStatus[status], count > 0 else { return nil }
            return labels.statusCount?(status, count)
                ?? MonarchAiSubagentTreeStrings.statusCount(status, count, count.formatted(.number.locale(locale)))
        }
        let breakdown = parts.formatted(.list(type: .and, width: .narrow).locale(locale))
        return MonarchAiSubagentTreeStrings.summary(total, breakdown)
    }

    /// The facts of a row: steps, tool calls and the time it ran.
    static func meta(
        _ agent: MonarchAiSubagent,
        locale: Locale,
        timeZone: TimeZone,
        labels: MonarchAiSubagentTreeLabels
    ) -> [String] {
        var out: [String] = []
        if let done = agent.stepsDone {
            if let steps = labels.steps {
                out.append(steps(done, agent.stepsTotal))
            } else if let total = agent.stepsTotal {
                out.append(
                    MonarchAiSubagentTreeStrings.stepsOf(
                        done.formatted(.number.locale(locale)),
                        total.formatted(.number.locale(locale))
                    )
                )
            } else {
                out.append(MonarchAiSubagentTreeStrings.steps(done, done.formatted(.number.locale(locale))))
            }
        }
        if let calls = agent.toolCalls {
            out.append(
                labels.toolCalls?(calls)
                    ?? MonarchAiSubagentTreeStrings.toolCalls(calls, calls.formatted(.number.locale(locale)))
            )
        }
        if let start = agent.startedAt {
            let startText = time(start, locale: locale, timeZone: timeZone)
            if let end = agent.endedAt {
                let endText = time(end, locale: locale, timeZone: timeZone)
                out.append(labels.ran?(startText, endText) ?? MonarchAiSubagentTreeStrings.ran(startText, endText))
            } else {
                out.append(labels.started?(startText) ?? MonarchAiSubagentTreeStrings.started(startText))
            }
        }
        return out
    }

    /// A time of day in the locale and time zone.
    static func time(_ date: Date, locale: Locale, timeZone: TimeZone) -> String {
        var style = Date.FormatStyle(date: .omitted, time: .omitted).locale(locale).monarchTime()
        style.timeZone = timeZone
        return date.formatted(style)
    }

    /// What VoiceOver reads for a row: name, status, role, activity, facts and summary.
    static func rowLabel(
        _ agent: MonarchAiSubagent,
        locale: Locale,
        timeZone: TimeZone,
        labels: MonarchAiSubagentTreeLabels
    ) -> String {
        var parts = [agent.name, statusName(agent.status, labels: labels)]
        if let role = agent.role, !role.isEmpty { parts.append(role) }
        if agent.status == .working, let activity = agent.activity, !activity.isEmpty { parts.append(activity) }
        parts += meta(agent, locale: locale, timeZone: timeZone, labels: labels)
        if hasEnded(agent.status), let summary = agent.summary, !summary.isEmpty { parts.append(summary) }
        return parts.joined(separator: ", ")
    }

    /// The announcement for the agents whose status changed.
    static func announcement(_ changed: [MonarchAiSubagent], labels: MonarchAiSubagentTreeLabels) -> String? {
        guard !changed.isEmpty else { return nil }
        return changed.map { agent in
            let status = statusName(agent.status, labels: labels)
            return labels.announce?(agent.name, status) ?? MonarchAiSubagentTreeStrings.announce(agent.name, status)
        }
        .joined(separator: " ")
    }

    // MARK: Actions

    /// Which actions the selected agent offers, given the handlers the app passed.
    struct Actions: Equatable {
        var respond = false
        var open = false
        var openPart = false
        var stop = false

        var isEmpty: Bool { !(respond || open || openPart || stop) }
    }

    /// The actions of an agent: Respond while it needs input, Open always,
    /// Show in conversation when it has a part or message, Stop while it is
    /// waiting or working. Each only with its handler.
    static func actions(
        for agent: MonarchAiSubagent,
        hasRespond: Bool,
        hasOpen: Bool,
        hasOpenPart: Bool,
        hasStop: Bool
    ) -> Actions {
        Actions(
            respond: hasRespond && agent.status == .needsInput,
            open: hasOpen,
            openPart: hasOpenPart && (agent.partId != nil || agent.messageId != nil),
            stop: hasStop && (agent.status == .waiting || agent.status == .working)
        )
    }
}
