import SwiftUI

/// The presence and turn-taking bar of an AI conversation that several
/// people and agents share: who is here, who is typing, whose request the
/// assistant is answering, what is waiting, and whether you may send.
///
/// ```swift
/// MonarchAiSharedSession(
///     session.participants,
///     answering: session.answering,
///     queue: session.queue,
///     onWithdraw: { id in session.withdraw(id) },
///     onInvite: { showInvite = true }
/// )
/// ```
///
/// The avatar stack (``MonarchAvatarGroup``) is one button that opens the
/// list of everyone in a sheet, people first with you at the top, then
/// agents. The status line says who is being answered, who else is typing,
/// speaking or reviewing, and how many requests wait. "Queue (2)" shows the
/// waiting requests, with Withdraw on your own and, for owners, Move up,
/// Move down and Remove. Viewers see a notice and an optional Request
/// access button. Data comes in through parameters and every action goes
/// out through a closure.
///
/// Accessibility: the bar is a container named "Shared session". The stack
/// is one button named "3 people and 1 agent here" whose value names each
/// visible participant with role or "AI agent", presence and activity.
/// Presence is always a word as well as a dot. Changes of who is being
/// answered, of reviewing and of the queue are announced; typing and
/// speaking are not. Queue buttons are named with the request ("Move up:
/// request 2, from Sam") and a move is announced.
@_spi(Experimental) public struct MonarchAiSharedSession: View {
    private let participants: [MonarchAiSharedSessionParticipant]
    private let answering: MonarchAiSharedSessionAnswering?
    private let queue: [MonarchAiSharedSessionRequest]
    private let maxVisible: Int
    private let following: Binding<Set<String>>?
    private let isQueueExpanded: Binding<Bool>?
    private let isAccessRequested: Bool
    private let announcesChanges: Bool
    private let labels: MonarchAiSharedSessionLabels
    private let onWithdraw: (@MainActor (String) -> Void)?
    private let onReorder: (@MainActor ([String], MonarchAiSharedSessionMove) -> Void)?
    private let onRemove: (@MainActor (String) -> Void)?
    private let onInvite: (@MainActor () -> Void)?
    private let onRequestAccess: (@MainActor () -> Void)?

    @Environment(\.monarchTheme) private var theme
    @Environment(\.locale) private var locale
    @State private var showsList = false
    @State private var isQueueOpen = false
    @State private var lastAnnouncement: String?

    /// A shared session bar.
    ///
    /// - Parameters:
    ///   - participants: Everyone in the session, people and agents, in display order. Mark one person as you.
    ///   - answering: The answer in progress: which agent, and whose request.
    ///   - queue: Requests waiting for their turn, first in line first.
    ///   - maxVisible: The avatars shown before "+N".
    ///   - following: The ids of the people you follow. Shows a Follow toggle beside every other person.
    ///   - isQueueExpanded: Whether the queue is shown, when the app owns it. `nil` lets the Queue button own it.
    ///   - isAccessRequested: Replaces a viewer's Request access button with "Access requested".
    ///   - announcesChanges: Whether status changes are announced to VoiceOver.
    ///   - labels: Replacements for the built-in strings.
    ///   - onWithdraw: Shows Withdraw on your own requests; receives the request id.
    ///   - onReorder: Shows Move up and Move down to an owner; receives the new order and the move.
    ///   - onRemove: Shows Remove on other people's requests to an owner; receives the request id.
    ///   - onInvite: Shows an Invite button that opens the product's invite flow.
    ///   - onRequestAccess: Shows Request access in a viewer's notice.
    public init(
        _ participants: [MonarchAiSharedSessionParticipant],
        answering: MonarchAiSharedSessionAnswering? = nil,
        queue: [MonarchAiSharedSessionRequest] = [],
        maxVisible: Int = 4,
        following: Binding<Set<String>>? = nil,
        isQueueExpanded: Binding<Bool>? = nil,
        isAccessRequested: Bool = false,
        announcesChanges: Bool = true,
        labels: MonarchAiSharedSessionLabels = MonarchAiSharedSessionLabels(),
        onWithdraw: (@MainActor (String) -> Void)? = nil,
        onReorder: (@MainActor ([String], MonarchAiSharedSessionMove) -> Void)? = nil,
        onRemove: (@MainActor (String) -> Void)? = nil,
        onInvite: (@MainActor () -> Void)? = nil,
        onRequestAccess: (@MainActor () -> Void)? = nil
    ) {
        self.participants = participants
        self.answering = answering
        self.queue = queue
        self.maxVisible = maxVisible
        self.following = following
        self.isQueueExpanded = isQueueExpanded
        self.isAccessRequested = isAccessRequested
        self.announcesChanges = announcesChanges
        self.labels = labels
        self.onWithdraw = onWithdraw
        self.onReorder = onReorder
        self.onRemove = onRemove
        self.onInvite = onInvite
        self.onRequestAccess = onRequestAccess
    }

    private typealias Tokens = MonarchAiSharedSessionTokens

    private var status: Tokens.Status {
        Tokens.status(
            participants: participants,
            answering: answering,
            queueCount: queue.count,
            locale: locale,
            labels: labels
        )
    }

    private var queueOpen: Bool {
        isQueueExpanded?.wrappedValue ?? isQueueOpen
    }

    private func toggleQueue() {
        if let isQueueExpanded {
            isQueueExpanded.wrappedValue.toggle()
        } else {
            isQueueOpen.toggle()
        }
    }

    /// The content of the view.
    public var body: some View {
        let status = status
        let role = Tokens.yourRole(participants)
        VStack(alignment: .leading, spacing: Tokens.spacing) {
            ViewThatFits(in: .horizontal) {
                HStack(spacing: Tokens.rowSpacing) {
                    stack(status: status)
                    statusLine(status)
                }
                VStack(alignment: .leading, spacing: Tokens.spacing) {
                    stack(status: status)
                    statusLine(status)
                }
            }
            if !queue.isEmpty || onInvite != nil {
                MonarchFlowLayout(spacing: Tokens.actionSpacing) {
                    if !queue.isEmpty { queueToggle }
                    if let onInvite {
                        MonarchButton(
                            labels.invite ?? MonarchAiSharedSessionStrings.invite,
                            systemImage: Tokens.inviteSymbol,
                            size: .sm,
                            action: onInvite
                        )
                    }
                }
            }
            if role == .viewer { viewerNotice }
            if !queue.isEmpty && queueOpen { queueList(role: role) }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .accessibilityElement(children: .contain)
        .accessibilityLabel(labels.label ?? MonarchAiSharedSessionStrings.label)
        .onChange(of: status.announcement, initial: true) { _, announcement in
            if announcesChanges, let text = Tokens.announcementChange(previous: lastAnnouncement, current: announcement)
            {
                AccessibilityNotification.Announcement(text).post()
            }
            lastAnnouncement = announcement
        }
    }

    @ViewBuilder private func stack(status: Tokens.Status) -> some View {
        if !participants.isEmpty {
            let groups = Tokens.groups(participants)
            let visible = MonarchOverflow.visibleCount(total: participants.count, maxVisible: max(1, maxVisible))
            Button {
                showsList = true
            } label: {
                MonarchAvatarGroup(
                    "",
                    participants,
                    maxVisible: max(1, maxVisible),
                    size: Tokens.stackAvatarSize,
                    content: { participant in
                        MonarchAiSharedSessionFace(
                            participant: participant,
                            isAnswering: answering?.participantID == participant.id,
                            size: Tokens.stackAvatarSize
                        )
                    }
                )
                .monarchMinimumHitTarget()
            }
            .buttonStyle(.plain)
            .accessibilityElement(children: .ignore)
            .accessibilityLabel(
                labels.here?(groups.people.count, groups.agents.count)
                    ?? MonarchAiSharedSessionStrings.here(people: groups.people.count, agents: groups.agents.count)
            )
            .accessibilityValue(
                participants.prefix(visible).map { Tokens.participantText($0, labels: labels) }.joined(separator: "; ")
            )
            .accessibilityHint(
                labels.showAll?(participants.count) ?? MonarchAiSharedSessionStrings.showAll(participants.count)
            )
            .accessibilityAddTraits(.isButton)
            .sheet(isPresented: $showsList) {
                MonarchAiSharedSessionList(
                    participants: participants,
                    answering: answering,
                    answeringText: status.parts.first { $0.kind == .answering }?.text,
                    following: following,
                    labels: labels,
                    onDone: { showsList = false }
                )
                .presentationDetents([.medium, .large])
                .presentationDragIndicator(.visible)
            }
        }
    }

    private func statusLine(_ status: Tokens.Status) -> some View {
        Text(status.parts.isEmpty ? labels.quiet ?? MonarchAiSharedSessionStrings.quiet : status.text)
            .monarchTextStyle(.caption)
            .foregroundStyle(status.parts.isEmpty ? theme.colors.textSubtle : theme.colors.textMuted)
            .frame(maxWidth: .infinity, alignment: .leading)
    }

    private var queueToggle: some View {
        MonarchButton(
            labels.queueToggle?(queue.count) ?? MonarchAiSharedSessionStrings.queueToggle(queue.count),
            trailingSystemImage: queueOpen ? Tokens.queueOpenSymbol : Tokens.queueClosedSymbol,
            variant: .ghost,
            size: .sm
        ) {
            toggleQueue()
        }
        .accessibilityAddTraits(queueOpen ? .isSelected : [])
    }

    private var viewerNotice: some View {
        let shape = RoundedRectangle(cornerRadius: Tokens.cornerRadius, style: .continuous)
        return MonarchFlowLayout(spacing: Tokens.actionSpacing) {
            HStack(alignment: .firstTextBaseline, spacing: Tokens.actionSpacing) {
                MonarchIcon(systemName: Tokens.eyeSymbol, size: .sm)
                Text(labels.viewerNotice ?? MonarchAiSharedSessionStrings.viewerNotice)
                    .monarchTextStyle(.caption)
            }
            .foregroundStyle(theme.colors.textMuted)
            .accessibilityElement(children: .combine)
            if isAccessRequested {
                Text(labels.accessRequested ?? MonarchAiSharedSessionStrings.accessRequested)
                    .monarchTextStyle(.caption, weight: Tokens.nameWeight)
                    .foregroundStyle(theme.colors.textDefault)
            } else if let onRequestAccess {
                MonarchButton(
                    labels.requestAccess ?? MonarchAiSharedSessionStrings.requestAccess,
                    size: .sm,
                    action: onRequestAccess
                )
            }
        }
        .padding(Tokens.insetPadding)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(shape.fill(theme.colors[role: Tokens.insetBackground]))
    }

    private func queueList(role: MonarchAiSharedSessionRole?) -> some View {
        let byID = Dictionary(participants.map { ($0.id, $0) }, uniquingKeysWith: { first, _ in first })
        let youID = Tokens.groups(participants).you?.id
        return VStack(alignment: .leading, spacing: Tokens.spacing) {
            ForEach(Array(queue.enumerated()), id: \.element.id) { index, request in
                let asker = byID[request.participantID]
                let askerName = asker?.name ?? labels.someone ?? MonarchAiSharedSessionStrings.someone
                let isOwn = youID != nil && request.participantID == youID
                let shape = RoundedRectangle(cornerRadius: Tokens.cornerRadius, style: .continuous)
                VStack(alignment: .leading, spacing: Tokens.lineSpacing) {
                    VStack(alignment: .leading, spacing: Tokens.lineSpacing) {
                        Text(asker.map { Tokens.displayName($0, labels: labels) } ?? askerName)
                            .monarchTextStyle(.caption, weight: Tokens.nameWeight)
                            .foregroundStyle(theme.colors.textDefault)
                        Text(request.preview)
                            .monarchTextStyle(.body)
                            .foregroundStyle(theme.colors.textDefault)
                            .lineLimit(2)
                            .truncationMode(.tail)
                    }
                    .accessibilityElement(children: .combine)
                    let actions = Tokens.queueActions(
                        request,
                        index: index,
                        count: queue.count,
                        youID: youID,
                        role: role,
                        canReorder: onReorder != nil,
                        canWithdraw: onWithdraw != nil,
                        canRemove: onRemove != nil
                    )
                    if !actions.isEmpty {
                        MonarchFlowLayout(spacing: Tokens.actionSpacing) {
                            ForEach(Array(actions.enumerated()), id: \.offset) { _, action in
                                queueButton(action, request: request, index: index, requester: askerName)
                            }
                        }
                    }
                }
                .padding(Tokens.insetPadding)
                .frame(maxWidth: .infinity, alignment: .leading)
                .background(
                    shape.fill(theme.colors[role: isOwn ? Tokens.ownRequestBackground : Tokens.insetBackground])
                )
            }
        }
        .accessibilityElement(children: .contain)
        .accessibilityLabel(labels.queueTitle ?? MonarchAiSharedSessionStrings.queueTitle)
    }

    @ViewBuilder private func queueButton(
        _ action: Tokens.QueueAction,
        request: MonarchAiSharedSessionRequest,
        index: Int,
        requester: String
    ) -> some View {
        switch action {
        case .moveUp(let isDisabled):
            actionButton(labels.moveUp ?? MonarchAiSharedSessionStrings.moveUp, requester: requester, index: index) {
                move(index, to: index - 1)
            }
            .disabled(isDisabled)
        case .moveDown(let isDisabled):
            actionButton(labels.moveDown ?? MonarchAiSharedSessionStrings.moveDown, requester: requester, index: index)
            {
                move(index, to: index + 1)
            }
            .disabled(isDisabled)
        case .withdraw:
            actionButton(labels.withdraw ?? MonarchAiSharedSessionStrings.withdraw, requester: requester, index: index)
            {
                onWithdraw?(request.id)
            }
        case .remove:
            actionButton(labels.remove ?? MonarchAiSharedSessionStrings.remove, requester: requester, index: index) {
                onRemove?(request.id)
            }
        }
    }

    private func actionButton(
        _ title: String,
        requester: String,
        index: Int,
        action: @escaping @MainActor () -> Void
    ) -> some View {
        MonarchButton(title, variant: .ghost, size: .sm, action: action)
            .accessibilityLabel(
                labels.requestAction?(title, requester, index + 1)
                    ?? MonarchAiSharedSessionStrings.requestAction(title, requester: requester, position: index + 1)
            )
    }

    private func move(_ index: Int, to target: Int) {
        guard let onReorder, let ids = Tokens.reordered(queue.map(\.id), from: index, to: target) else { return }
        let request = queue[index]
        onReorder(ids, MonarchAiSharedSessionMove(id: request.id, from: index, to: target))
        if announcesChanges {
            let requester =
                participants.first { $0.id == request.participantID }?.name
                ?? labels.someone ?? MonarchAiSharedSessionStrings.someone
            AccessibilityNotification.Announcement(
                labels.moved?(requester, target + 1, queue.count)
                    ?? MonarchAiSharedSessionStrings.moved(requester, position: target + 1, total: queue.count)
            )
            .post()
        }
    }
}

#if DEBUG
    #Preview("MonarchAiSharedSession") {
        let participants = [
            MonarchAiSharedSessionParticipant(id: "maya", name: "Maya", role: .owner, isYou: true),
            MonarchAiSharedSessionParticipant(id: "sam", name: "Sam Lee", activity: .typing),
            MonarchAiSharedSessionParticipant(id: "ra", name: "Research agent", kind: .agent),
        ]
        MonarchPreviewMatrix {
            MonarchAiSharedSession(
                participants,
                answering: MonarchAiSharedSessionAnswering(participantID: "ra", forParticipantID: "sam"),
                queue: [MonarchAiSharedSessionRequest(id: "r1", participantID: "maya", preview: "Draft the summary")],
                onWithdraw: { _ in },
                onInvite: {}
            )
        }
    }
#endif
