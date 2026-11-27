import SwiftUI

/// One message of an AI conversation, with its parts in the order the model
/// produced them: text, reasoning, tool calls and approvals, sources and
/// citations, files, plans, memories, artifacts and errors, then the time,
/// the branch switcher and the actions row.
///
/// ```swift
/// ForEach(conversation.messages) { message in
///     MonarchAiMessage(
///         message,
///         feedback: $ratings[message.id],
///         onRegenerate: { conversation.regenerate() },
///         onRetry: { conversation.retry() },
///         onApproveTool: { id, input in conversation.approveTool(id, input: input) },
///         onDenyTool: { id, reason in conversation.denyTool(id, reason: reason) }
///     )
/// }
/// ```
///
/// It renders ``MonarchAiConversationMessage`` with the Monarch AI
/// components: ``MonarchAiStreamingText`` for text, ``MonarchAiReasoning``,
/// ``MonarchAiToolCall`` (or ``MonarchAiToolApproval`` for a call that waits
/// for approval when `onApproveTool` or `onDenyTool` is set),
/// ``MonarchAiSourceList`` where the first source stands, a
/// ``MonarchAiCitation`` marker for each citation, ``MonarchAttachmentChip``
/// for files, ``MonarchAiChainOfSteps`` for a plan, ``MonarchAiMemoryChip``,
/// ``MonarchAiArtifactCard`` when `onOpenArtifact` is set, and
/// ``MonarchAiErrorMessage`` with Retry and Resume. Generative UI and edit
/// proposals need the app's own views, so they are drawn by `part` and left
/// out without it.
///
/// An assistant message has a header with the agent's avatar, name, role and
/// time. The person's message is a bubble at the trailing edge; a system
/// message is muted and centred. The actions row
/// (``MonarchAiMessageActions``) is hidden while the reply streams, while the
/// message is edited and while it is queued offline. A reply that stopped at
/// the length limit, a safety filter or a refusal ends with a notice.
///
/// Accessibility: the message is a container named by who said it ("You
/// said", "Assistant said"). The header is one element read as the name, the
/// role and the time, with the header trait; the avatar is decorative. Each
/// part keeps its own component's behaviour: streaming text announces new
/// sentences (turn off with `announces`) and the last text announces the end
/// of the reply; tool call state changes and errors are announced by their
/// components. Every button is at least 44 points.
@_spi(Experimental) public struct MonarchAiMessage<Part: View, More: View>: View {
    private let message: MonarchAiConversationMessage
    private let agent: MonarchAiAgentRef?
    private let feedback: Binding<MonarchAiMessageFeedback?>?
    private let isReadingAloud: Binding<Bool>?
    private let isEditing: Binding<Bool>?
    private let showsTimestamp: Bool
    private let showsActions: Bool
    private let isCopyable: Bool
    private let isShareable: Bool
    private let announces: Bool
    private let labels: MonarchAiMessageLabels
    private let onCopy: (@MainActor (String) -> Void)?
    private let onRegenerate: (@MainActor () -> Void)?
    private let onEditSubmit: (@MainActor (String) -> Void)?
    private let onBranch: (@MainActor (Int) -> Void)?
    private let onRetry: (@MainActor () -> Void)?
    private let onResume: (@MainActor () -> Void)?
    private let onCancelQueued: (@MainActor () -> Void)?
    private let onApproveTool: (@MainActor (_ toolCallID: String, _ input: MonarchAiJSON?) -> Void)?
    private let onDenyTool: (@MainActor (_ toolCallID: String, _ reason: String?) -> Void)?
    private let onAlwaysAllowTool: (@MainActor (_ toolName: String) -> Void)?
    private let onEditMemory: (@MainActor (_ id: String, _ text: String) -> Void)?
    private let onForgetMemory: (@MainActor (_ id: String) -> Void)?
    private let onOpenArtifact: (@MainActor (MonarchAiArtifactPart) -> Void)?
    private let onOpenSource: (@MainActor (MonarchAiSourceListItem) -> Void)?
    private let onOpenLink: (@MainActor (URL) -> Void)?
    private let drawsCustomParts: Bool
    private let part: (MonarchAiPart) -> Part
    private let hasMore: Bool
    private let more: More

    @Environment(\.monarchTheme) private var theme
    @Environment(\.locale) private var locale
    @Environment(\.timeZone) private var timeZone

    private typealias Tokens = MonarchAiMessageTokens

    /// A message that draws generative UI and edit proposals with `part` and
    /// adds items to the More menu of its actions.
    ///
    /// - Parameters:
    ///   - message: The message to show.
    ///   - agent: The agent that answered, when the message has none.
    ///   - feedback: The rating of an assistant message. Shows the thumbs when set.
    ///   - isReadingAloud: Whether the app reads the assistant message aloud. Shows Read aloud when set.
    ///   - isEditing: Whether the person's message is being edited. Shows Edit when set; Edit sets it.
    ///   - showsTimestamp: Shows the time the message was created.
    ///   - showsActions: Shows the actions row.
    ///   - isCopyable: Shows Copy when the message has text.
    ///   - isShareable: Shows Share.
    ///   - announces: Whether streaming text and errors are announced. Turn off when the conversation announces.
    ///   - labels: Replacements for the built-in strings.
    ///   - onCopy: Runs with the copied text after each copy.
    ///   - onRegenerate: Shows Regenerate on an assistant message.
    ///   - onEditSubmit: Receives the edited text; editing then ends.
    ///   - onBranch: Receives the alternative chosen in the branch switcher, counted from 0.
    ///   - onRetry: Retries a failed turn whose error can be retried.
    ///   - onResume: Resumes a dropped reply whose error can be resumed.
    ///   - onCancelQueued: Cancels a message queued offline.
    ///   - onApproveTool: Approves a tool call, with the edited input or `nil`. Shows approvals.
    ///   - onDenyTool: Denies a tool call, with the reason or `nil`. Shows approvals.
    ///   - onAlwaysAllowTool: Always allows the tool by name, from the approval.
    ///   - onEditMemory: Edits a memory by id.
    ///   - onForgetMemory: Forgets a memory by id.
    ///   - onOpenArtifact: Opens an artifact. Shows artifact cards.
    ///   - onOpenSource: Opens a source in the app instead of its address.
    ///   - onOpenLink: Opens a link in the text instead of the system browser.
    ///   - part: Draws a generative UI or edit proposal part.
    ///   - more: Extra items at the end of the More menu of the actions.
    public init(
        _ message: MonarchAiConversationMessage,
        agent: MonarchAiAgentRef? = nil,
        feedback: Binding<MonarchAiMessageFeedback?>? = nil,
        isReadingAloud: Binding<Bool>? = nil,
        isEditing: Binding<Bool>? = nil,
        showsTimestamp: Bool = true,
        showsActions: Bool = true,
        isCopyable: Bool = true,
        isShareable: Bool = false,
        announces: Bool = true,
        labels: MonarchAiMessageLabels = MonarchAiMessageLabels(),
        onCopy: (@MainActor (String) -> Void)? = nil,
        onRegenerate: (@MainActor () -> Void)? = nil,
        onEditSubmit: (@MainActor (String) -> Void)? = nil,
        onBranch: (@MainActor (Int) -> Void)? = nil,
        onRetry: (@MainActor () -> Void)? = nil,
        onResume: (@MainActor () -> Void)? = nil,
        onCancelQueued: (@MainActor () -> Void)? = nil,
        onApproveTool: (@MainActor (_ toolCallID: String, _ input: MonarchAiJSON?) -> Void)? = nil,
        onDenyTool: (@MainActor (_ toolCallID: String, _ reason: String?) -> Void)? = nil,
        onAlwaysAllowTool: (@MainActor (_ toolName: String) -> Void)? = nil,
        onEditMemory: (@MainActor (_ id: String, _ text: String) -> Void)? = nil,
        onForgetMemory: (@MainActor (_ id: String) -> Void)? = nil,
        onOpenArtifact: (@MainActor (MonarchAiArtifactPart) -> Void)? = nil,
        onOpenSource: (@MainActor (MonarchAiSourceListItem) -> Void)? = nil,
        onOpenLink: (@MainActor (URL) -> Void)? = nil,
        @ViewBuilder part: @escaping (MonarchAiPart) -> Part,
        @ViewBuilder more: () -> More
    ) {
        self.message = message
        self.agent = agent
        self.feedback = feedback
        self.isReadingAloud = isReadingAloud
        self.isEditing = isEditing
        self.showsTimestamp = showsTimestamp
        self.showsActions = showsActions
        self.isCopyable = isCopyable
        self.isShareable = isShareable
        self.announces = announces
        self.labels = labels
        self.onCopy = onCopy
        self.onRegenerate = onRegenerate
        self.onEditSubmit = onEditSubmit
        self.onBranch = onBranch
        self.onRetry = onRetry
        self.onResume = onResume
        self.onCancelQueued = onCancelQueued
        self.onApproveTool = onApproveTool
        self.onDenyTool = onDenyTool
        self.onAlwaysAllowTool = onAlwaysAllowTool
        self.onEditMemory = onEditMemory
        self.onForgetMemory = onForgetMemory
        self.onOpenArtifact = onOpenArtifact
        self.onOpenSource = onOpenSource
        self.onOpenLink = onOpenLink
        self.drawsCustomParts = Part.self != EmptyView.self
        self.part = part
        self.hasMore = More.self != EmptyView.self
        self.more = more()
    }

    private var role: MonarchAiRole { message.role }
    private var resolvedAgent: MonarchAiAgentRef? { message.agent ?? agent }
    private var editing: Bool { role == .user && (isEditing?.wrappedValue ?? false) }
    private var time: String? {
        Tokens.time(message.createdAt, isShown: showsTimestamp, locale: locale, timeZone: timeZone)
    }

    private var alignment: Alignment {
        switch role {
        case .user: .trailing
        case .assistant: .leading
        case .system, .tool: .center
        }
    }

    private var horizontalAlignment: HorizontalAlignment {
        switch role {
        case .user: .trailing
        case .assistant: .leading
        case .system, .tool: .center
        }
    }

    /// The content of the view.
    public var body: some View {
        VStack(alignment: horizontalAlignment, spacing: Tokens.spacing) {
            if role == .assistant {
                header
            }
            if editing, let isEditing {
                MonarchAiUserMessageEdit(
                    message.text,
                    label: labels.editLabel,
                    sendLabel: labels.save,
                    cancelLabel: labels.cancel,
                    onCancel: { isEditing.wrappedValue = false },
                    onSubmit: { text in
                        onEditSubmit?(text)
                        isEditing.wrappedValue = false
                    }
                )
            } else if role == .user {
                HStack(spacing: 0) {
                    Spacer(minLength: Tokens.bubbleLeadingInset)
                    content
                        .padding(.horizontal, Tokens.bubblePaddingHorizontal)
                        .padding(.vertical, Tokens.bubblePaddingVertical)
                        .background(
                            UnevenRoundedRectangle(
                                topLeadingRadius: Tokens.bubbleRadius,
                                bottomLeadingRadius: Tokens.bubbleRadius,
                                bottomTrailingRadius: Tokens.bubbleTailRadius,
                                topTrailingRadius: Tokens.bubbleRadius,
                                style: .continuous
                            )
                            .fill(theme.colors[role: Tokens.bubbleFill])
                        )
                }
            } else if role == .assistant {
                content
            } else {
                content
                    .foregroundStyle(theme.colors[role: Tokens.systemText])
                    .multilineTextAlignment(.center)
            }
            if role == .user, message.isQueued {
                queuedLine
            }
            footer
        }
        .frame(maxWidth: .infinity, alignment: alignment)
        .accessibilityElement(children: .contain)
        .accessibilityLabel(Tokens.spokenRole(role, labels: labels))
    }

    // MARK: - Parts

    private var header: some View {
        let name = Tokens.name(agent: resolvedAgent, labels: labels)
        return HStack(alignment: .center, spacing: Tokens.rowSpacing) {
            MonarchAiAgentAvatar(resolvedAgent ?? MonarchAiAgentRef(name: name), size: .sm, isDecorative: true)
            MonarchFlowLayout(spacing: Tokens.rowSpacing, rowSpacing: 0, alignment: .firstTextBaseline) {
                Text(name)
                    .monarchTextStyle(.bodyStrong)
                    .foregroundStyle(theme.colors.textDefault)
                if let agentRole = resolvedAgent?.role, !agentRole.isEmpty {
                    Text(agentRole)
                        .monarchTextStyle(.caption)
                        .foregroundStyle(theme.colors[role: Tokens.metaText])
                }
                if let time {
                    Text(time)
                        .monarchTextStyle(.caption)
                        .foregroundStyle(theme.colors[role: Tokens.metaText])
                }
            }
        }
        .accessibilityElement(children: .ignore)
        .accessibilityLabel(Tokens.headerLabel(name: name, agentRole: resolvedAgent?.role, time: time))
        .accessibilityAddTraits(.isHeader)
    }

    private var content: some View {
        let blocks = Tokens.blocks(
            message.parts,
            showsArtifacts: onOpenArtifact != nil,
            drawsCustomParts: drawsCustomParts
        )
        return VStack(alignment: horizontalAlignment == .center ? .center : .leading, spacing: Tokens.spacing) {
            ForEach(blocks, id: \.self) { block in
                blockView(block)
            }
            if let notice = Tokens.finishNotice(message, labels: labels) {
                Label {
                    Text(notice)
                        .monarchTextStyle(.caption)
                        .foregroundStyle(theme.colors.textMuted)
                } icon: {
                    MonarchIcon(systemName: Tokens.finishSymbol, size: .sm)
                        .foregroundStyle(theme.colors[role: Tokens.finishSymbolColor])
                }
            }
        }
    }

    @ViewBuilder private func blockView(_ block: Tokens.Block) -> some View {
        switch block {
        case .part(let index):
            partView(message.parts[index], index: index)
        case .citations(let indices):
            MonarchFlowLayout(spacing: Tokens.citationSpacing, rowSpacing: Tokens.citationSpacing) {
                ForEach(indices, id: \.self) { index in
                    citation(at: index)
                }
            }
        case .sources:
            MonarchAiSourceList(
                MonarchAiSourceListItem.sources(of: message),
                labels: labels.sources,
                onOpen: onOpenSource
            )
        }
    }

    @ViewBuilder private func citation(at index: Int) -> some View {
        if case .citation(let citation) = message.parts[index],
            let source = Tokens.source(citation.sourceId, in: message.parts),
            let number = Tokens.sourceNumbers(message.parts)[citation.sourceId]
        {
            MonarchAiCitation(
                number,
                source: source,
                quote: citation.quote,
                labels: labels.citation,
                onOpen: openCitedSource
            )
        }
    }

    /// Opens a cited source through `onOpenSource`, or `nil` to let the citation link to its address.
    private var openCitedSource: (@MainActor (MonarchAiSourcePart) -> Void)? {
        guard let onOpenSource else { return nil }
        return { source in onOpenSource(MonarchAiSourceListItem(source)) }
    }

    /// A tool call inside a message, as the web writes it: one quiet row with the state mark, the tool's own name
    /// in code type, the state and the time it took. The full card with the input and output is MonarchAiToolCall.
    private func toolRow(_ call: MonarchAiToolCallPart) -> some View {
        let look = MonarchAiToolCallTokens.stateLook(call.state)
        let stateName = MonarchAiToolCallTokens.stateName(call.state, labels: labels.toolCall)
        let duration = MonarchAiToolCallTokens.duration(call).map {
            MonarchAiToolCallTokens.durationText($0, locale: locale, labels: labels.toolCall)
        }
        let symbol = call.state == .succeeded ? "checkmark" : look.symbol
        return MonarchFlowLayout(spacing: MonarchAiToolCallTokens.headerSpacing) {
            MonarchIcon(systemName: symbol, size: .sm)
                .foregroundStyle(theme.colors[role: MonarchTonePalette.palette(for: look.tone).strong])
                .accessibilityHidden(true)
            Text(call.name)
                .monarchTextStyle(.codeSmall)
                .foregroundStyle(theme.colors.textDefault)
            Text(stateName)
                .monarchTextStyle(.caption)
                .foregroundStyle(theme.colors.textMuted)
            if let duration {
                Text(duration)
                    .monarchTextStyle(.caption)
                    .foregroundStyle(theme.colors.textMuted)
                    .monospacedDigit()
            }
            if call.state == .failed, let error = call.error, !error.isEmpty {
                Text(error)
                    .monarchTextStyle(.caption)
                    .foregroundStyle(theme.colors.dangerStrong)
            }
        }
        .padding(.horizontal, theme.spacing.space2)
        .padding(.vertical, theme.spacing.space1)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(MonarchShapes.control.fill(theme.colors.bgSubtle))
        .overlay(
            MonarchShapes.control.strokeBorder(
                theme.colors.borderSubtle,
                lineWidth: MonarchAiToolCallTokens.borderWidth
            )
        )
        .accessibilityElement(children: .ignore)
        .accessibilityLabel(
            MonarchAiToolCallTokens.rowLabel(title: call.name, state: stateName, duration: duration)
        )
    }

    @ViewBuilder private func partView(_ value: MonarchAiPart, index: Int) -> some View {
        switch value {
        case .text(let text):
            MonarchAiStreamingText(
                text.text,
                isStreaming: text.state == .streaming,
                announces: announces,
                announcesCompletion: announces && index == Tokens.lastTextIndex(message.parts)
                    && Tokens.isReplyDone(message),
                labels: labels.streamingText,
                onOpenLink: onOpenLink
            )
        case .reasoning(let reasoning):
            MonarchAiReasoning(reasoning, labels: labels.reasoning)
        case .toolCall(let call):
            if Tokens.showsApproval(call, handlesApproval: onApproveTool != nil || onDenyTool != nil) {
                MonarchAiToolApproval(
                    call,
                    risk: call.risk,
                    labels: labels.toolApproval,
                    onAlwaysAllow: onAlwaysAllowTool,
                    onDeny: { reason in onDenyTool?(call.id, reason) },
                    onApprove: { input in onApproveTool?(call.id, input) }
                )
            } else {
                toolRow(call)
            }
        case .file(let file):
            MonarchAttachmentChip(
                file.name,
                size: file.size.map { Int64($0) },
                contentType: file.mediaType,
                url: Tokens.fileURL(file),
                labels: labels.attachment
            )
            .environment(\.monarchLowercaseKilobyte, true)
        case .error(let error):
            MonarchAiErrorMessage(
                MonarchAiErrorKind(code: error.code),
                message: error.message,
                retryAfter: Tokens.retryAfter(error),
                announces: announces,
                labels: labels.error,
                onRetry: error.isRetryable ? onRetry : nil,
                actions: {
                    if error.isResumable, let onResume {
                        Button(labels.resume ?? MonarchAiMessageStrings.resume, systemImage: Tokens.resumeSymbol) {
                            onResume()
                        }
                        .buttonStyle(.monarch(.secondary, size: .sm))
                    }
                }
            )
        case .memory(let memory):
            MonarchAiMemoryChip(
                memory,
                labels: labels.memory,
                onEdit: onEditMemory,
                onForget: onForgetMemory
            )
        case .plan(let plan):
            MonarchAiChainOfSteps(
                plan.steps,
                isCollapsible: false,
                announces: false,
                labels: MonarchAiChainOfStepsLabels(label: labels.plan ?? MonarchAiMessageStrings.plan)
            )
        case .artifact(let artifact):
            if let onOpenArtifact {
                MonarchAiArtifactCard(artifact.title, kind: artifact.kind, version: artifact.version) {
                    onOpenArtifact(artifact)
                }
            }
        case .ui, .editProposal:
            part(value)
        case .source, .citation:
            EmptyView()
        }
    }

    private var queuedLine: some View {
        HStack(alignment: .center, spacing: Tokens.rowSpacing) {
            Label {
                Text(labels.queued ?? MonarchAiMessageStrings.queued)
                    .monarchTextStyle(.caption)
                    .foregroundStyle(theme.colors.textMuted)
            } icon: {
                MonarchIcon(systemName: Tokens.queuedSymbol, size: .sm)
                    .foregroundStyle(theme.colors.textMuted)
            }
            .accessibilityElement(children: .combine)
            if let onCancelQueued {
                Button(labels.cancelQueued ?? MonarchAiMessageStrings.cancelQueued) {
                    onCancelQueued()
                }
                .buttonStyle(.monarch(.ghost, size: .sm))
            }
        }
    }

    @ViewBuilder private var footer: some View {
        let branch = Tokens.branch(message)
        let actions = Tokens.showsActions(message, isShown: showsActions, isEditing: editing)
        let footerTime = role == .assistant ? nil : time
        if footerTime != nil || branch != nil || actions {
            MonarchFlowLayout(spacing: Tokens.rowSpacing, rowSpacing: 0) {
                if let footerTime {
                    Text(footerTime)
                        .monarchTextStyle(.caption)
                        .foregroundStyle(theme.colors[role: Tokens.metaText])
                }
                if let branch {
                    MonarchAiMessageBranch(
                        selection: Binding(get: { branch.index }, set: { index in onBranch?(index) }),
                        count: branch.count,
                        label: labels.versions,
                        previousLabel: labels.previousVersion,
                        nextLabel: labels.nextVersion,
                        formatPosition: labels.versionOf
                    )
                }
                if actions {
                    actionsRow
                }
            }
        }
    }

    /// Edit on the person's message sets `isEditing`; `nil` hides it.
    private var editAction: (@MainActor () -> Void)? {
        guard role == .user, let isEditing else { return nil }
        return { isEditing.wrappedValue = true }
    }

    private var actionFeedback: Binding<MonarchAiMessageFeedback?>? { role == .assistant ? feedback : nil }
    private var actionReadingAloud: Binding<Bool>? { role == .assistant ? isReadingAloud : nil }
    private var actionRegenerate: (@MainActor () -> Void)? { role == .assistant ? onRegenerate : nil }
    private var hasText: Bool { !message.text.isEmpty }

    @ViewBuilder private var actionsRow: some View {
        if hasMore {
            MonarchAiMessageActions(
                message.text,
                feedback: actionFeedback,
                isReadingAloud: actionReadingAloud,
                isCopyable: isCopyable && hasText,
                isShareable: isShareable && hasText,
                labels: labels.actions,
                onCopy: onCopy,
                onRegenerate: actionRegenerate,
                onEdit: editAction,
                more: { more }
            )
        } else {
            MonarchAiMessageActions(
                message.text,
                feedback: actionFeedback,
                isReadingAloud: actionReadingAloud,
                isCopyable: isCopyable && hasText,
                isShareable: isShareable && hasText,
                labels: labels.actions,
                onCopy: onCopy,
                onRegenerate: actionRegenerate,
                onEdit: editAction
            )
        }
    }
}

extension MonarchAiMessage where Part == EmptyView, More == EmptyView {
    /// A message without app-drawn parts or extra menu items. Generative UI
    /// and edit proposal parts are left out.
    ///
    /// - Parameters:
    ///   - message: The message to show.
    ///   - agent: The agent that answered, when the message has none.
    ///   - feedback: The rating of an assistant message. Shows the thumbs when set.
    ///   - isReadingAloud: Whether the app reads the assistant message aloud. Shows Read aloud when set.
    ///   - isEditing: Whether the person's message is being edited. Shows Edit when set; Edit sets it.
    ///   - showsTimestamp: Shows the time the message was created.
    ///   - showsActions: Shows the actions row.
    ///   - isCopyable: Shows Copy when the message has text.
    ///   - isShareable: Shows Share.
    ///   - announces: Whether streaming text and errors are announced. Turn off when the conversation announces.
    ///   - labels: Replacements for the built-in strings.
    ///   - onCopy: Runs with the copied text after each copy.
    ///   - onRegenerate: Shows Regenerate on an assistant message.
    ///   - onEditSubmit: Receives the edited text; editing then ends.
    ///   - onBranch: Receives the alternative chosen in the branch switcher, counted from 0.
    ///   - onRetry: Retries a failed turn whose error can be retried.
    ///   - onResume: Resumes a dropped reply whose error can be resumed.
    ///   - onCancelQueued: Cancels a message queued offline.
    ///   - onApproveTool: Approves a tool call, with the edited input or `nil`. Shows approvals.
    ///   - onDenyTool: Denies a tool call, with the reason or `nil`. Shows approvals.
    ///   - onAlwaysAllowTool: Always allows the tool by name, from the approval.
    ///   - onEditMemory: Edits a memory by id.
    ///   - onForgetMemory: Forgets a memory by id.
    ///   - onOpenArtifact: Opens an artifact. Shows artifact cards.
    ///   - onOpenSource: Opens a source in the app instead of its address.
    ///   - onOpenLink: Opens a link in the text instead of the system browser.
    public init(
        _ message: MonarchAiConversationMessage,
        agent: MonarchAiAgentRef? = nil,
        feedback: Binding<MonarchAiMessageFeedback?>? = nil,
        isReadingAloud: Binding<Bool>? = nil,
        isEditing: Binding<Bool>? = nil,
        showsTimestamp: Bool = true,
        showsActions: Bool = true,
        isCopyable: Bool = true,
        isShareable: Bool = false,
        announces: Bool = true,
        labels: MonarchAiMessageLabels = MonarchAiMessageLabels(),
        onCopy: (@MainActor (String) -> Void)? = nil,
        onRegenerate: (@MainActor () -> Void)? = nil,
        onEditSubmit: (@MainActor (String) -> Void)? = nil,
        onBranch: (@MainActor (Int) -> Void)? = nil,
        onRetry: (@MainActor () -> Void)? = nil,
        onResume: (@MainActor () -> Void)? = nil,
        onCancelQueued: (@MainActor () -> Void)? = nil,
        onApproveTool: (@MainActor (_ toolCallID: String, _ input: MonarchAiJSON?) -> Void)? = nil,
        onDenyTool: (@MainActor (_ toolCallID: String, _ reason: String?) -> Void)? = nil,
        onAlwaysAllowTool: (@MainActor (_ toolName: String) -> Void)? = nil,
        onEditMemory: (@MainActor (_ id: String, _ text: String) -> Void)? = nil,
        onForgetMemory: (@MainActor (_ id: String) -> Void)? = nil,
        onOpenArtifact: (@MainActor (MonarchAiArtifactPart) -> Void)? = nil,
        onOpenSource: (@MainActor (MonarchAiSourceListItem) -> Void)? = nil,
        onOpenLink: (@MainActor (URL) -> Void)? = nil
    ) {
        self.init(
            message,
            agent: agent,
            feedback: feedback,
            isReadingAloud: isReadingAloud,
            isEditing: isEditing,
            showsTimestamp: showsTimestamp,
            showsActions: showsActions,
            isCopyable: isCopyable,
            isShareable: isShareable,
            announces: announces,
            labels: labels,
            onCopy: onCopy,
            onRegenerate: onRegenerate,
            onEditSubmit: onEditSubmit,
            onBranch: onBranch,
            onRetry: onRetry,
            onResume: onResume,
            onCancelQueued: onCancelQueued,
            onApproveTool: onApproveTool,
            onDenyTool: onDenyTool,
            onAlwaysAllowTool: onAlwaysAllowTool,
            onEditMemory: onEditMemory,
            onForgetMemory: onForgetMemory,
            onOpenArtifact: onOpenArtifact,
            onOpenSource: onOpenSource,
            onOpenLink: onOpenLink,
            part: { _ in EmptyView() },
            more: { EmptyView() }
        )
    }
}

#if DEBUG
    #Preview("MonarchAiMessage") {
        MonarchPreviewMatrix {
            VStack(alignment: .leading) {
                MonarchAiMessage(
                    MonarchAiConversationMessage(
                        id: "u1",
                        role: .user,
                        parts: [.text(MonarchAiTextPart(text: "Which invoices are overdue?"))],
                        createdAt: "2026-03-10T09:41:00.000Z"
                    ),
                    isEditing: .constant(false)
                )
                MonarchAiMessage(
                    MonarchAiConversationMessage(
                        id: "a1",
                        role: .assistant,
                        parts: [
                            .reasoning(MonarchAiReasoningPart(text: "Check the ledger.", durationMs: 2_000)),
                            .toolCall(MonarchAiToolCallPart(id: "t1", name: "searchInvoices", state: .succeeded)),
                            .text(MonarchAiTextPart(text: "Two invoices are overdue.")),
                            .source(MonarchAiSourcePart(id: "s1", title: "Ledger", kind: .record)),
                            .citation(MonarchAiCitationPart(id: "c1", sourceId: "s1")),
                        ],
                        agent: MonarchAiAgentRef(name: "Ledger", role: "Finance agent"),
                        createdAt: "2026-03-10T09:41:05.000Z",
                        finishReason: .length
                    ),
                    feedback: .constant(.up),
                    onRegenerate: {}
                )
            }
        }
    }
#endif
