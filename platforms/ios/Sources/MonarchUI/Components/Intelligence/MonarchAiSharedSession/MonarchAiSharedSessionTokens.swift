import Foundation
import MonarchTokens
import SwiftUI

/// Component tokens and pure logic of ``MonarchAiSharedSession``.
enum MonarchAiSharedSessionTokens {
    /// One clause of the status line.
    struct StatusPart: Equatable {
        /// What a clause is about.
        enum Kind: Equatable {
            case answering
            case activity(MonarchAiSharedSessionActivity)
            case queue
        }

        let kind: Kind
        let text: String
        /// Whether it is worth announcing. Typing and speaking are not.
        let announces: Bool
    }

    /// The status line as clauses, as visible text and as an announcement.
    struct Status: Equatable {
        let parts: [StatusPart]
        let text: String
        let announcement: String
    }

    /// A button on a waiting request.
    enum QueueAction: Equatable {
        case moveUp(isDisabled: Bool)
        case moveDown(isDisabled: Bool)
        case withdraw
        case remove
    }

    /// The gap between the rows of the bar.
    static let spacing: CGFloat = MonarchTokens.Space.space2
    /// The gap between the parts of a row.
    static let rowSpacing: CGFloat = MonarchTokens.Space.space3
    /// The gap between buttons.
    static let actionSpacing: CGFloat = MonarchTokens.Space.space2
    /// The gap between the lines of a participant or a request.
    static let lineSpacing: CGFloat = MonarchTokens.Space.space0_5
    /// Padding inside the viewer notice and a request.
    static let insetPadding: CGFloat = MonarchTokens.Space.space2
    /// The corner radius of the viewer notice and a request.
    static let cornerRadius: CGFloat = MonarchTokens.Radius.md
    /// The size of the avatars in the stack.
    static let stackAvatarSize: MonarchAvatarSize = .sm
    /// The size of the avatars in the list.
    static let listAvatarSize: MonarchAvatarSize = .md
    /// The weight of names.
    static let nameWeight = MonarchTextStyleSpec.fontWeight(MonarchTokens.FontWeight.semibold)
    /// The SF Symbol of the viewer notice and the follow toggle.
    static let eyeSymbol = "eye"
    /// The SF Symbol of the invite button.
    static let inviteSymbol = "plus"
    /// The SF Symbol of the closed queue.
    static let queueClosedSymbol = "chevron.down"
    /// The SF Symbol of the open queue.
    static let queueOpenSymbol = "chevron.up"
    /// The fill of the viewer notice and of a request.
    static var insetBackground: MonarchColorRole { \.bgSubtle }
    /// The fill of your own request.
    static var ownRequestBackground: MonarchColorRole { \.aiSubtle }

    /// The presence dot of an avatar: active is online, idle is away, away is offline.
    static func presenceStatus(_ presence: MonarchAiSharedSessionPresence) -> MonarchPresenceStatus {
        switch presence {
        case .active: .online
        case .idle: .away
        case .away: .offline
        }
    }

    /// People (you first, the rest in order) and agents (in order).
    static func groups(
        _ participants: [MonarchAiSharedSessionParticipant]
    ) -> (
        people: [MonarchAiSharedSessionParticipant], agents: [MonarchAiSharedSessionParticipant],
        you: MonarchAiSharedSessionParticipant?
    ) {
        let people = participants.filter { $0.kind == .person }
        let agents = participants.filter { $0.kind == .agent }
        let you = people.first { $0.isYou }
        guard let you else { return (people, agents, nil) }
        return ([you] + people.filter { $0.id != you.id }, agents, you)
    }

    /// Your role, or `nil` when no person is marked as you.
    static func yourRole(_ participants: [MonarchAiSharedSessionParticipant]) -> MonarchAiSharedSessionRole? {
        participants.first { $0.kind == .person && $0.isYou }?.role
    }

    /// The display name: "Maya (you)" for you.
    static func displayName(_ participant: MonarchAiSharedSessionParticipant, labels: MonarchAiSharedSessionLabels)
        -> String
    {
        guard participant.isYou else { return participant.name }
        return labels.nameWithYou?(participant.name) ?? MonarchAiSharedSessionStrings.nameWithYou(participant.name)
    }

    /// The role of a person, or "AI agent" for an agent.
    static func kindText(_ participant: MonarchAiSharedSessionParticipant, labels: MonarchAiSharedSessionLabels)
        -> String
    {
        if participant.kind == .agent { return labels.agentKind ?? MonarchAiSharedSessionStrings.agentKind }
        return labels.role?(participant.role) ?? MonarchAiSharedSessionStrings.role(participant.role)
    }

    /// The word of a presence.
    static func presenceText(_ presence: MonarchAiSharedSessionPresence, labels: MonarchAiSharedSessionLabels) -> String
    {
        labels.presence?(presence) ?? MonarchAiSharedSessionStrings.presence(presence)
    }

    /// The word of an activity.
    static func activityText(_ activity: MonarchAiSharedSessionActivity, labels: MonarchAiSharedSessionLabels) -> String
    {
        labels.activity?(activity) ?? MonarchAiSharedSessionStrings.activity(activity)
    }

    /// The details of a participant: kind or role, presence and activity.
    static func details(_ participant: MonarchAiSharedSessionParticipant, labels: MonarchAiSharedSessionLabels)
        -> [String]
    {
        [
            kindText(participant, labels: labels),
            presenceText(participant.presence, labels: labels),
            participant.activity.map { activityText($0, labels: labels) },
        ]
        .compactMap { $0 }
    }

    /// What VoiceOver reads for a participant: the name and the details.
    static func participantText(_ participant: MonarchAiSharedSessionParticipant, labels: MonarchAiSharedSessionLabels)
        -> String
    {
        ([displayName(participant, labels: labels)] + details(participant, labels: labels)).joined(separator: ", ")
    }

    /// The status line: who the assistant is answering, what other people
    /// are doing, and how many requests wait. Your own activity is left out.
    static func status(
        participants: [MonarchAiSharedSessionParticipant],
        answering: MonarchAiSharedSessionAnswering?,
        queueCount: Int,
        locale: Locale,
        labels: MonarchAiSharedSessionLabels
    ) -> Status {
        let byID = Dictionary(participants.map { ($0.id, $0) }, uniquingKeysWith: { first, _ in first })
        var parts: [StatusPart] = []
        if let answering {
            let agent =
                byID[answering.participantID]?.name ?? labels.assistant ?? MonarchAiSharedSessionStrings.assistant
            let requester = byID[answering.forParticipantID]
            let label = answering.label.flatMap { $0.isEmpty ? nil : $0 }
            let text: String
            if requester?.isYou == true {
                text =
                    labels.answeringYou?(agent, label)
                    ?? withLabel(MonarchAiSharedSessionStrings.answeringYou(agent: agent), label)
            } else {
                let name = requester?.name ?? labels.someone ?? MonarchAiSharedSessionStrings.someone
                text =
                    labels.answering?(agent, name, label)
                    ?? withLabel(MonarchAiSharedSessionStrings.answering(agent: agent, requester: name), label)
            }
            parts.append(StatusPart(kind: .answering, text: text, announces: true))
        }
        let formatter = ListFormatter()
        formatter.locale = locale
        for activity in MonarchAiSharedSessionActivity.allCases {
            let names = participants.filter { $0.activity == activity && !$0.isYou }.map(\.name)
            guard !names.isEmpty else { continue }
            let who =
                names.count > 2
                ? labels.several?(names.count) ?? MonarchAiSharedSessionStrings.several(names.count)
                : formatter.string(from: names) ?? names.joined(separator: ", ")
            let text =
                labels.activityLine?(activity, who, names.count)
                ?? MonarchAiSharedSessionStrings.activityLine(activity, names: who, count: names.count)
            parts.append(StatusPart(kind: .activity(activity), text: text, announces: activity == .reviewing))
        }
        if queueCount > 0 {
            let text = labels.queueCount?(queueCount) ?? MonarchAiSharedSessionStrings.queueCount(queueCount)
            parts.append(StatusPart(kind: .queue, text: text, announces: true))
        }
        return Status(
            parts: parts,
            text: parts.map(\.text).joined(separator: MonarchAiSharedSessionStrings.separator),
            announcement: parts.filter(\.announces).map(\.text).joined(separator: ". ")
        )
    }

    private static func withLabel(_ text: String, _ label: String?) -> String {
        guard let label else { return text }
        return MonarchAiSharedSessionStrings.withLabel(text, label)
    }

    /// What to announce after the status changed: the new announcement when
    /// it differs, never the first status, never an empty one.
    static func announcementChange(previous: String?, current: String) -> String? {
        guard let previous, current != previous, !current.isEmpty else { return nil }
        return current
    }

    /// The buttons of a waiting request: Move up and Move down for an owner
    /// who can reorder; Withdraw on your own request; Remove on another's
    /// request for an owner who can remove.
    static func queueActions(
        _ request: MonarchAiSharedSessionRequest,
        index: Int,
        count: Int,
        youID: String?,
        role: MonarchAiSharedSessionRole?,
        canReorder: Bool,
        canWithdraw: Bool,
        canRemove: Bool
    ) -> [QueueAction] {
        let isOwner = role == .owner
        var actions: [QueueAction] = []
        if isOwner && canReorder {
            actions.append(.moveUp(isDisabled: index == 0))
            actions.append(.moveDown(isDisabled: index >= count - 1))
        }
        let isOwn = youID != nil && request.participantID == youID
        if isOwn && canWithdraw {
            actions.append(.withdraw)
        } else if isOwner && canRemove {
            actions.append(.remove)
        }
        return actions
    }

    /// The request ids after moving the one at `from` to `to`, or `nil` when the move is out of range.
    static func reordered(_ ids: [String], from: Int, to: Int) -> [String]? {
        guard ids.indices.contains(from), ids.indices.contains(to), from != to else { return nil }
        var result = ids
        let id = result.remove(at: from)
        result.insert(id, at: to)
        return result
    }
}
