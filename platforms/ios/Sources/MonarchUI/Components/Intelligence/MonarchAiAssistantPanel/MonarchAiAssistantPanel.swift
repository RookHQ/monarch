import SwiftUI

/// Everything except the view slots, so ``emptyState(_:)`` can carry it over.
private struct MonarchAiAssistantPanelConfiguration {
    let title: String
    let conversation: MonarchAiConversation?
    let state: MonarchAiAssistantPanelSnapshot
    let isPresented: Binding<Bool>
    let prompt: Binding<String>?
    let placement: MonarchAiAssistantPanelPlacement
    let agent: MonarchAiAgentRef?
    let agentImage: Image?
    let context: [MonarchAiContextItem]
    let starters: [MonarchAiStarterPrompt]
    let labels: MonarchAiAssistantPanelLabels
    let onStop: (@MainActor () -> Void)?
    let onRemoveContext: (@MainActor (String) -> Void)?
    let onSend: (@MainActor (String) async throws -> Void)?
}

/// The conversation state the panel shows.
private struct MonarchAiAssistantPanelSnapshot {
    var messages: [MonarchAiConversationMessage] = []
    var status: MonarchAiConversationStatus = .idle
    var error: MonarchAiConversationError?
    var isOffline = false
    var queuedCount = 0
    var isSlow = false
    var agent: MonarchAiAgentRef?
}

/// The assistant surface any app mounts: the title with the agent and
/// Close, a thread over a prompt input, chips for what the assistant can see
/// on the current screen, and a welcome with starter prompts while the
/// conversation is empty.
///
/// ```swift
/// @State private var conversation = MonarchAiConversation(transport: transport)
/// @State private var showsAssistant = false
/// @State private var draft = ""
///
/// DocumentView()
///     .monarchAiAssistantPanel(isPresented: $showsAssistant) {
///         MonarchAiAssistantPanel(
///             "Assistant",
///             conversation: conversation,
///             isPresented: $showsAssistant,
///             prompt: $draft,
///             context: [MonarchAiContextItem(id: "page", kind: .page, label: "Quarterly plan")],
///             starters: [MonarchAiStarterPrompt(id: "sum", title: "Summarize this page", prompt: "Summarize this page.")],
///             headerActions: {},
///             toolbar: {},
///             footer: { MonarchAiDisclaimer() }
///         )
///     }
/// ```
///
/// On iPhone the panel is the content of a `.sheet`: a `NavigationStack`
/// with the title and Close (the cancellation action) in the navigation
/// bar, and the medium and large detents. In a regular width it is a pane
/// the app places, usually in `.inspector` or a split view column, with its
/// own header row. `View.monarchAiAssistantPanel(isPresented:panel:)`
/// picks the sheet or the inspector for you. The thread is a
/// ``MonarchAiThread`` under the context chips; the prompt input sits in
/// `.safeAreaInset(edge: .bottom)`, so SwiftUI keeps it above the
/// keyboard. While the conversation is empty a ``MonarchAiWelcome`` with
/// ``MonarchAiStarterPrompts`` shows instead; replace it with
/// ``emptyState(_:)``. The input is not focused when the panel opens, so the
/// keyboard does not cover the conversation.
///
/// Pass a ``MonarchAiConversation`` and the panel reads its messages, status,
/// error, connection, queue and agent; sending calls `send`, and a refused
/// send gives the text back to the input. Or pass the messages and state
/// yourself with `onSend`.
///
/// Accessibility: the title is a header (the navigation title in a sheet,
/// a heading in a pane, which is a container named by the title). Close is
/// named "Close assistant", has a 44-point target and answers the Escape key
/// while no reply runs; while one runs, Escape and the Magic Tap stop it.
/// The avatar is decorative; the agent's name and role are text. The input is
/// named "Message the assistant". The thread announces replies, errors and
/// notices. The starters are disabled, not hidden, while a reply runs or a
/// tool call waits for approval.
@_spi(Experimental)
public struct MonarchAiAssistantPanel<HeaderActions: View, Toolbar: View, Footer: View, Empty: View>: View {
    private let configuration: Configuration
    private let headerActions: HeaderActions
    private let toolbar: Toolbar
    private let footer: Footer
    private let empty: Empty?

    @Environment(\.monarchTheme) private var theme
    @Environment(\.horizontalSizeClass) private var horizontalSizeClass
    @Environment(\.monarchAiAssistantPanelPresentation) private var presentation
    @State private var draft = ""
    @State private var isCollapsed = false
    @State private var detent: PresentationDetent = .large

    private typealias Tokens = MonarchAiAssistantPanelTokens
    private typealias Strings = MonarchAiAssistantPanelStrings
    private typealias Configuration = MonarchAiAssistantPanelConfiguration
    private typealias Snapshot = MonarchAiAssistantPanelSnapshot

    /// A send the conversation refused, so the prompt input gives the text back.
    private struct SendRefused: Error {}

    private init(
        configuration: Configuration,
        headerActions: HeaderActions,
        toolbar: Toolbar,
        footer: Footer,
        empty: Empty?
    ) {
        self.configuration = configuration
        self.headerActions = headerActions
        self.toolbar = toolbar
        self.footer = footer
        self.empty = empty
    }

    /// Replaces the empty state (the welcome and the starters).
    ///
    /// - Parameter content: What shows while the conversation has no messages.
    /// - Returns: The panel with the empty state.
    public func emptyState<NewEmpty: View>(
        @ViewBuilder _ content: () -> NewEmpty
    ) -> MonarchAiAssistantPanel<HeaderActions, Toolbar, Footer, NewEmpty> {
        MonarchAiAssistantPanel<HeaderActions, Toolbar, Footer, NewEmpty>(
            configuration: configuration,
            headerActions: headerActions,
            toolbar: toolbar,
            footer: footer,
            empty: content()
        )
    }

    // MARK: - Body

    /// The content of the view.
    public var body: some View {
        let placement = Tokens.resolvedPlacement(
            configuration.placement,
            presentedAs: presentation,
            isCompactWidth: horizontalSizeClass == .compact
        )
        switch placement {
        case .sheet, .automatic:
            sheetChrome
        case .inline:
            inlineChrome
        }
    }

    private var snapshot: Snapshot {
        guard let conversation = configuration.conversation else { return configuration.state }
        return Snapshot(
            messages: conversation.messages,
            status: conversation.status,
            error: conversation.error,
            isOffline: !conversation.isOnline,
            queuedCount: conversation.queued.count,
            isSlow: conversation.isSlow,
            agent: conversation.agent
        )
    }

    private var agent: MonarchAiAgentRef? { configuration.agent ?? snapshot.agent }

    private var sheetChrome: some View {
        NavigationStack {
            content
                .navigationTitle(configuration.title)
                #if os(iOS)
                    .navigationBarTitleDisplayMode(.inline)
                #endif
                .toolbar {
                    ToolbarItem(placement: .cancellationAction) {
                        closeButton
                    }
                    ToolbarItem(placement: .principal) {
                        HStack(spacing: Tokens.spacing) {
                            avatar
                            titleBlock
                        }
                    }
                    ToolbarItemGroup(placement: .primaryAction) {
                        headerActions
                        collapseButton
                    }
                }
        }
        .presentationDetents([.medium, .large], selection: $detent)
    }

    private var inlineChrome: some View {
        VStack(spacing: 0) {
            HStack(spacing: Tokens.spacing) {
                avatar
                titleBlock
                    .frame(maxWidth: .infinity, alignment: .leading)
                headerActions
                collapseButton
                closeButton
            }
            .padding(.horizontal, Tokens.padding)
            .padding(.vertical, Tokens.spacing)
            if !isCollapsed {
                Divider()
                content
            }
        }
        .background(theme.colors[role: Tokens.backgroundRole])
        .accessibilityElement(children: .contain)
        .accessibilityLabel(configuration.title)
    }

    // MARK: - Header

    private var avatar: some View {
        let shown = Tokens.headerAgent(agent, labels: configuration.labels)
        return MonarchAiAgentAvatar(
            shown,
            image: configuration.agentImage,
            state: Tokens.avatarState(snapshot.status),
            size: .sm,
            isDecorative: true
        )
    }

    private var titleBlock: some View {
        VStack(alignment: .leading, spacing: Tokens.titleSpacing) {
            Text(configuration.title)
                .monarchTextStyle(.subheading)
                .foregroundStyle(theme.colors[role: Tokens.titleRole])
                .accessibilityAddTraits(.isHeader)
            if let subtitle = Tokens.subtitle(agent) {
                Text(subtitle)
                    .monarchTextStyle(.caption)
                    .foregroundStyle(theme.colors[role: Tokens.secondaryRole])
            }
        }
    }

    /// The chevron beside the close button, as on the web: it folds the panel to its header (a sheet goes to its
    /// medium height and back).
    private var collapseButton: some View {
        let collapsed = isSheet ? detent == .medium : isCollapsed
        let label = collapsed ? Strings.expand : Strings.collapse
        return Button {
            if isSheet {
                detent = detent == .medium ? .large : .medium
            } else {
                isCollapsed.toggle()
            }
        } label: {
            MonarchIcon(
                systemName: collapsed ? Tokens.expandSymbol : Tokens.collapseSymbol,
                label: label,
                size: .sm
            )
        }
        .buttonStyle(.monarch(.ghost, size: .sm))
    }

    private var isSheet: Bool {
        switch Tokens.resolvedPlacement(
            configuration.placement,
            presentedAs: presentation,
            isCompactWidth: horizontalSizeClass == .compact
        ) {
        case .sheet, .automatic: true
        case .inline: false
        }
    }

    private var closeButton: some View {
        let label = configuration.labels.close ?? Strings.close
        return Button {
            configuration.isPresented.wrappedValue = false
        } label: {
            MonarchIcon(systemName: Tokens.closeSymbol, label: label, size: .sm)
        }
        .buttonStyle(.monarch(.ghost, size: .sm))
        .keyboardShortcut(Tokens.closesOnEscape(snapshot.status) ? .cancelAction : nil)
    }

    // MARK: - Content

    private var content: some View {
        let state = snapshot
        return VStack(spacing: 0) {
            contextChips
            Group {
                if Tokens.showsWelcome(messageCount: state.messages.count) {
                    ScrollView {
                        emptyContent(status: state.status)
                            .padding(Tokens.padding)
                            .frame(maxWidth: .infinity)
                    }
                    .scrollBounceBehavior(.basedOnSize)
                } else {
                    MonarchAiThread(
                        state.messages,
                        status: state.status,
                        error: state.error,
                        isOffline: state.isOffline,
                        queuedCount: state.queuedCount,
                        isSlow: state.isSlow,
                        onStop: stopAction
                    )
                }
            }
            .frame(maxWidth: .infinity, maxHeight: .infinity)
        }
        .safeAreaInset(edge: .bottom, spacing: 0) {
            inputArea(status: state.status)
        }
    }

    /// What the assistant can see, above the thread (as on the web).
    @ViewBuilder private var contextChips: some View {
        if !configuration.context.isEmpty {
            MonarchAiContextChips(
                configuration.context,
                labels: MonarchAiContextChipsLabels(label: configuration.labels.context ?? Strings.context),
                onRemove: configuration.onRemoveContext
            )
            .padding(.horizontal, Tokens.padding)
            .padding(.vertical, Tokens.spacing)
            .frame(maxWidth: .infinity, alignment: .leading)
            .background(theme.colors[role: Tokens.backgroundRole])
        }
    }

    @ViewBuilder
    private func emptyContent(status: MonarchAiConversationStatus) -> some View {
        if let empty {
            empty
        } else {
            MonarchAiWelcome(configuration.labels.welcome ?? Strings.welcome, agent: agent) {
                MonarchAiStarterChips(
                    prompts: configuration.starters,
                    label: configuration.labels.starters ?? Strings.starters
                ) { starter in
                    select(starter)
                }
                .disabled(Tokens.areStartersDisabled(status))
            }
            .environment(\.monarchAiWelcomeHidesMark, true)
        }
    }

    private func inputArea(status: MonarchAiConversationStatus) -> some View {
        VStack(alignment: .leading, spacing: Tokens.spacing) {
            MonarchAiPromptInput(
                configuration.labels.inputLabel ?? Strings.inputLabel,
                text: configuration.prompt ?? $draft,
                isBusy: Tokens.isBusy(status),
                onStop: stopAction,
                triggerMenu: { EmptyView() },
                toolbar: { toolbar },
                footer: { footer },
                onSubmit: { text in try await submit(text) }
            )
        }
        .padding(Tokens.padding)
        .background(theme.colors[role: Tokens.backgroundRole])
    }

    // MARK: - Actions

    private var stopAction: (@MainActor () -> Void)? {
        if let conversation = configuration.conversation {
            return { conversation.stop() }
        }
        return configuration.onStop
    }

    private func submit(_ text: String) async throws {
        if let conversation = configuration.conversation {
            guard conversation.send(text) else { throw SendRefused() }
        } else if let onSend = configuration.onSend {
            try await onSend(text)
        }
    }

    private func select(_ starter: MonarchAiStarterPrompt) {
        if let conversation = configuration.conversation {
            conversation.send(starter.prompt)
        } else if let onSend = configuration.onSend {
            Task { @MainActor in try? await onSend(starter.prompt) }
        }
    }
}

// MARK: - Initialisers

extension MonarchAiAssistantPanel where Empty == EmptyView {
    /// An assistant panel that shows and sends to a conversation.
    ///
    /// - Parameters:
    ///   - title: The heading, such as "Assistant", already localised.
    ///   - conversation: The conversation to show and send to.
    ///   - isPresented: Whether the panel shows. Close sets it to `false`.
    ///   - prompt: The draft. Without it the panel keeps one while it stays on screen.
    ///   - placement: The sheet or pane chrome. `automatic` follows the presenting modifier or the width.
    ///   - agent: The agent in the header and the welcome. Without it, the conversation's agent.
    ///   - agentImage: The agent's picture, loaded by the app. Initials show without it.
    ///   - context: What the assistant can see on the current screen, as chips above the thread.
    ///   - starters: Suggested first prompts while the conversation is empty. Choosing one sends it.
    ///   - labels: Replacements for the built-in strings.
    ///   - onRemoveContext: Lets the person remove a context chip by id.
    ///   - headerActions: Buttons beside Close, such as New conversation.
    ///   - toolbar: Controls in the prompt input's bar, such as a model picker.
    ///   - footer: Content under the prompt input, such as ``MonarchAiDisclaimer``.
    public init(
        _ title: String,
        conversation: MonarchAiConversation,
        isPresented: Binding<Bool>,
        prompt: Binding<String>? = nil,
        placement: MonarchAiAssistantPanelPlacement = .automatic,
        agent: MonarchAiAgentRef? = nil,
        agentImage: Image? = nil,
        context: [MonarchAiContextItem] = [],
        starters: [MonarchAiStarterPrompt] = [],
        labels: MonarchAiAssistantPanelLabels = MonarchAiAssistantPanelLabels(),
        onRemoveContext: (@MainActor (String) -> Void)? = nil,
        @ViewBuilder headerActions: () -> HeaderActions,
        @ViewBuilder toolbar: () -> Toolbar,
        @ViewBuilder footer: () -> Footer
    ) {
        self.init(
            configuration: Configuration(
                title: title,
                conversation: conversation,
                state: Snapshot(),
                isPresented: isPresented,
                prompt: prompt,
                placement: placement,
                agent: agent,
                agentImage: agentImage,
                context: context,
                starters: starters,
                labels: labels,
                onStop: nil,
                onRemoveContext: onRemoveContext,
                onSend: nil
            ),
            headerActions: headerActions(),
            toolbar: toolbar(),
            footer: footer(),
            empty: nil
        )
    }

    /// An assistant panel that shows messages and state the app holds.
    ///
    /// - Parameters:
    ///   - title: The heading, such as "Assistant", already localised.
    ///   - messages: The conversation, oldest first.
    ///   - isPresented: Whether the panel shows. Close sets it to `false`.
    ///   - prompt: The draft. Without it the panel keeps one while it stays on screen.
    ///   - placement: The sheet or pane chrome. `automatic` follows the presenting modifier or the width.
    ///   - status: While `submitted` or `streaming` Send becomes Stop and the avatar reads working.
    ///   - error: The last turn's error, shown and announced by the thread.
    ///   - isOffline: Shows the thread's offline notice.
    ///   - queuedCount: Messages waiting to be sent, for the offline notice.
    ///   - isSlow: Shows the thread's slow notice.
    ///   - agent: The agent in the header and the welcome.
    ///   - agentImage: The agent's picture, loaded by the app. Initials show without it.
    ///   - context: What the assistant can see on the current screen, as chips above the thread.
    ///   - starters: Suggested first prompts while the conversation is empty. Choosing one sends it.
    ///   - labels: Replacements for the built-in strings.
    ///   - onStop: Stops the running reply.
    ///   - onRemoveContext: Lets the person remove a context chip by id.
    ///   - headerActions: Buttons beside Close, such as New conversation.
    ///   - toolbar: Controls in the prompt input's bar, such as a model picker.
    ///   - footer: Content under the prompt input, such as ``MonarchAiDisclaimer``.
    ///   - onSend: Sends the typed prompt or a starter's prompt. Throw to give the typed text back.
    public init(
        _ title: String,
        messages: [MonarchAiConversationMessage],
        isPresented: Binding<Bool>,
        prompt: Binding<String>? = nil,
        placement: MonarchAiAssistantPanelPlacement = .automatic,
        status: MonarchAiConversationStatus = .idle,
        error: MonarchAiConversationError? = nil,
        isOffline: Bool = false,
        queuedCount: Int = 0,
        isSlow: Bool = false,
        agent: MonarchAiAgentRef? = nil,
        agentImage: Image? = nil,
        context: [MonarchAiContextItem] = [],
        starters: [MonarchAiStarterPrompt] = [],
        labels: MonarchAiAssistantPanelLabels = MonarchAiAssistantPanelLabels(),
        onStop: (@MainActor () -> Void)? = nil,
        onRemoveContext: (@MainActor (String) -> Void)? = nil,
        @ViewBuilder headerActions: () -> HeaderActions,
        @ViewBuilder toolbar: () -> Toolbar,
        @ViewBuilder footer: () -> Footer,
        onSend: @escaping @MainActor (String) async throws -> Void
    ) {
        self.init(
            configuration: Configuration(
                title: title,
                conversation: nil,
                state: Snapshot(
                    messages: messages,
                    status: status,
                    error: error,
                    isOffline: isOffline,
                    queuedCount: queuedCount,
                    isSlow: isSlow
                ),
                isPresented: isPresented,
                prompt: prompt,
                placement: placement,
                agent: agent,
                agentImage: agentImage,
                context: context,
                starters: starters,
                labels: labels,
                onStop: onStop,
                onRemoveContext: onRemoveContext,
                onSend: onSend
            ),
            headerActions: headerActions(),
            toolbar: toolbar(),
            footer: footer(),
            empty: nil
        )
    }
}

extension MonarchAiAssistantPanel
where HeaderActions == EmptyView, Toolbar == EmptyView, Footer == EmptyView, Empty == EmptyView {
    /// An assistant panel that shows and sends to a conversation, without
    /// header actions, a toolbar or a footer.
    ///
    /// - Parameters:
    ///   - title: The heading, such as "Assistant", already localised.
    ///   - conversation: The conversation to show and send to.
    ///   - isPresented: Whether the panel shows. Close sets it to `false`.
    ///   - prompt: The draft. Without it the panel keeps one while it stays on screen.
    ///   - placement: The sheet or pane chrome. `automatic` follows the presenting modifier or the width.
    ///   - agent: The agent in the header and the welcome. Without it, the conversation's agent.
    ///   - agentImage: The agent's picture, loaded by the app. Initials show without it.
    ///   - context: What the assistant can see on the current screen, as chips above the thread.
    ///   - starters: Suggested first prompts while the conversation is empty. Choosing one sends it.
    ///   - labels: Replacements for the built-in strings.
    ///   - onRemoveContext: Lets the person remove a context chip by id.
    public init(
        _ title: String,
        conversation: MonarchAiConversation,
        isPresented: Binding<Bool>,
        prompt: Binding<String>? = nil,
        placement: MonarchAiAssistantPanelPlacement = .automatic,
        agent: MonarchAiAgentRef? = nil,
        agentImage: Image? = nil,
        context: [MonarchAiContextItem] = [],
        starters: [MonarchAiStarterPrompt] = [],
        labels: MonarchAiAssistantPanelLabels = MonarchAiAssistantPanelLabels(),
        onRemoveContext: (@MainActor (String) -> Void)? = nil
    ) {
        self.init(
            title,
            conversation: conversation,
            isPresented: isPresented,
            prompt: prompt,
            placement: placement,
            agent: agent,
            agentImage: agentImage,
            context: context,
            starters: starters,
            labels: labels,
            onRemoveContext: onRemoveContext,
            headerActions: { EmptyView() },
            toolbar: { EmptyView() },
            footer: { EmptyView() }
        )
    }

    /// An assistant panel that shows messages and state the app holds,
    /// without header actions, a toolbar or a footer.
    ///
    /// - Parameters:
    ///   - title: The heading, such as "Assistant", already localised.
    ///   - messages: The conversation, oldest first.
    ///   - isPresented: Whether the panel shows. Close sets it to `false`.
    ///   - prompt: The draft. Without it the panel keeps one while it stays on screen.
    ///   - placement: The sheet or pane chrome. `automatic` follows the presenting modifier or the width.
    ///   - status: While `submitted` or `streaming` Send becomes Stop and the avatar reads working.
    ///   - error: The last turn's error, shown and announced by the thread.
    ///   - isOffline: Shows the thread's offline notice.
    ///   - queuedCount: Messages waiting to be sent, for the offline notice.
    ///   - isSlow: Shows the thread's slow notice.
    ///   - agent: The agent in the header and the welcome.
    ///   - agentImage: The agent's picture, loaded by the app. Initials show without it.
    ///   - context: What the assistant can see on the current screen, as chips above the thread.
    ///   - starters: Suggested first prompts while the conversation is empty. Choosing one sends it.
    ///   - labels: Replacements for the built-in strings.
    ///   - onStop: Stops the running reply.
    ///   - onRemoveContext: Lets the person remove a context chip by id.
    ///   - onSend: Sends the typed prompt or a starter's prompt. Throw to give the typed text back.
    public init(
        _ title: String,
        messages: [MonarchAiConversationMessage],
        isPresented: Binding<Bool>,
        prompt: Binding<String>? = nil,
        placement: MonarchAiAssistantPanelPlacement = .automatic,
        status: MonarchAiConversationStatus = .idle,
        error: MonarchAiConversationError? = nil,
        isOffline: Bool = false,
        queuedCount: Int = 0,
        isSlow: Bool = false,
        agent: MonarchAiAgentRef? = nil,
        agentImage: Image? = nil,
        context: [MonarchAiContextItem] = [],
        starters: [MonarchAiStarterPrompt] = [],
        labels: MonarchAiAssistantPanelLabels = MonarchAiAssistantPanelLabels(),
        onStop: (@MainActor () -> Void)? = nil,
        onRemoveContext: (@MainActor (String) -> Void)? = nil,
        onSend: @escaping @MainActor (String) async throws -> Void
    ) {
        self.init(
            title,
            messages: messages,
            isPresented: isPresented,
            prompt: prompt,
            placement: placement,
            status: status,
            error: error,
            isOffline: isOffline,
            queuedCount: queuedCount,
            isSlow: isSlow,
            agent: agent,
            agentImage: agentImage,
            context: context,
            starters: starters,
            labels: labels,
            onStop: onStop,
            onRemoveContext: onRemoveContext,
            headerActions: { EmptyView() },
            toolbar: { EmptyView() },
            footer: { EmptyView() },
            onSend: onSend
        )
    }
}

#if DEBUG
    #Preview("MonarchAiAssistantPanel") {
        MonarchPreviewMatrix {
            MonarchAiAssistantPanel(
                "Assistant",
                messages: [],
                isPresented: .constant(true),
                placement: .inline,
                agent: MonarchAiAgentRef(name: "Dossier", role: "Workspace assistant"),
                context: [MonarchAiContextItem(id: "page", kind: .page, label: "Quarterly plan")],
                starters: [
                    MonarchAiStarterPrompt(id: "sum", title: "Summarize this page", prompt: "Summarize this page.")
                ],
                onSend: { _ in }
            )
            .frame(height: MonarchAiAssistantPanelTokens.previewHeight)
        }
    }
#endif
