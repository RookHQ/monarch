import MonarchTokens
import SwiftUI

/// The empty state of a new conversation: who the assistant is, a greeting,
/// what it can help with and where to start.
///
/// ```swift
/// MonarchAiWelcome(
///     "How can I help?",
///     agent: MonarchAiAgentRef(name: "Dossier assistant", role: "Writing assistant"),
///     description: "I can read the page you are on and the documents you share with me.",
///     capabilities: ["Summarise long documents", "Draft replies"],
///     starters: { MonarchAiStarterPrompts(starters) { send($0.prompt) } },
///     footer: { MonarchAiDisclaimer() }
/// )
/// ```
///
/// The agent is shown with ``MonarchAiAgentAvatar``; without one a bot mark
/// stands in. Put ``MonarchAiStarterPrompts`` in `starters` and
/// ``MonarchAiDisclaimer`` in `footer`. The column is centred and its text
/// wraps at every size.
///
/// Accessibility: the welcome is a container named by its title, and the
/// title is a heading. The agent line is read as one element, "AI
/// assistant: Dossier assistant, Writing assistant". The capabilities are a
/// group named "What I can help with", one element per capability; their
/// check marks and the bot mark are decorative.
public struct MonarchAiWelcome<Starters: View, Footer: View>: View {
    private let title: String
    private let agent: MonarchAiAgentRef?
    private let description: String?
    private let capabilities: [String]
    private let capabilitiesLabel: String?
    private let agentPrefix: String?
    private let starters: Starters
    private let footer: Footer

    @Environment(\.monarchTheme) private var theme
    @Environment(\.monarchAiWelcomeHidesMark) private var hidesMark
    @ScaledMetric(relativeTo: .title) private var markSide = MonarchAiWelcomeTokens.markSide

    /// A welcome with starters and a footer.
    ///
    /// - Parameters:
    ///   - title: The greeting, such as "How can I help?", already localised. It names the welcome.
    ///   - agent: The assistant answering in this conversation: its avatar, name and role.
    ///   - description: One or two sentences on what the assistant can see or do.
    ///   - capabilities: Short capability statements, shown as a checklist.
    ///   - capabilitiesLabel: Replaces the built-in spoken name "What I can help with".
    ///   - agentPrefix: Replaces the built-in "AI assistant" read before the agent's name.
    ///   - starters: Where to start, usually ``MonarchAiStarterPrompts``.
    ///   - footer: Content under the starters, usually ``MonarchAiDisclaimer``.
    public init(
        _ title: String,
        agent: MonarchAiAgentRef? = nil,
        description: String? = nil,
        capabilities: [String] = [],
        capabilitiesLabel: String? = nil,
        agentPrefix: String? = nil,
        @ViewBuilder starters: () -> Starters,
        @ViewBuilder footer: () -> Footer
    ) {
        self.title = title
        self.agent = agent
        self.description = description
        self.capabilities = capabilities
        self.capabilitiesLabel = capabilitiesLabel
        self.agentPrefix = agentPrefix
        self.starters = starters()
        self.footer = footer()
    }

    /// The content of the view.
    public var body: some View {
        VStack(spacing: MonarchAiWelcomeTokens.spacing) {
            mark
            if let agent {
                agentLine(agent)
            }
            Text(title)
                .monarchTextStyle(.heading)
                .foregroundStyle(theme.colors.textDefault)
                .accessibilityAddTraits(.isHeader)
            if let description, !description.isEmpty {
                Text(description)
                    .monarchTextStyle(.body)
                    .foregroundStyle(theme.colors.textMuted)
            }
            let shown = MonarchAiWelcomeTokens.capabilities(capabilities)
            if !shown.isEmpty {
                capabilityList(shown)
            }
            starters
                .multilineTextAlignment(.leading)
            footer
        }
        .multilineTextAlignment(.center)
        .frame(maxWidth: .infinity)
        .padding(.vertical, MonarchAiWelcomeTokens.verticalPadding)
        .accessibilityElement(children: .contain)
        .accessibilityLabel(title)
    }

    @ViewBuilder private var mark: some View {
        if hidesMark {
            EmptyView()
        } else if let agent, agent.avatar != nil {
            MonarchAiAgentAvatar(agent, size: .lg, isDecorative: true)
        } else {
            MonarchAiBotGlyph(size: .lg)
                .foregroundStyle(theme.colors[role: MonarchAiWelcomeTokens.markGlyph])
                .frame(width: markSide, height: markSide)
                .background(
                    RoundedRectangle(cornerRadius: MonarchAiWelcomeTokens.markRadius, style: .continuous)
                        .fill(theme.colors[role: MonarchAiWelcomeTokens.markFill])
                )
                .accessibilityHidden(true)
        }
    }

    private func agentLine(_ agent: MonarchAiAgentRef) -> some View {
        // "Assistant · Workspace assistant": the name in the AI colour, the role muted, on one line as on the web.
        HStack(spacing: MonarchTokens.Space.space2) {
            Text(agent.name)
                .monarchTextStyle(.caption, weight: .semibold)
                .foregroundStyle(theme.colors.aiStrong)
            if let role = agent.role, !role.isEmpty {
                Text("·")
                    .monarchTextStyle(.caption)
                    .foregroundStyle(theme.colors.textMuted)
                    .accessibilityHidden(true)
                Text(role)
                    .monarchTextStyle(.caption)
                    .foregroundStyle(theme.colors.textMuted)
            }
        }
        .accessibilityElement(children: .ignore)
        .accessibilityLabel(MonarchAiWelcomeTokens.agentLabel(agent, prefix: agentPrefix))
    }

    private func capabilityList(_ shown: [String]) -> some View {
        VStack(alignment: .leading, spacing: MonarchAiWelcomeTokens.capabilitySpacing) {
            ForEach(Array(shown.enumerated()), id: \.offset) { _, capability in
                HStack(alignment: .firstTextBaseline, spacing: MonarchAiWelcomeTokens.iconSpacing) {
                    MonarchIcon(systemName: MonarchAiWelcomeTokens.checkSymbol, size: .sm)
                        .foregroundStyle(theme.colors[role: MonarchAiWelcomeTokens.checkRole])
                    Text(capability)
                        .monarchTextStyle(.body)
                        .foregroundStyle(theme.colors.textDefault)
                        .frame(maxWidth: .infinity, alignment: .leading)
                }
                .multilineTextAlignment(.leading)
                .accessibilityElement(children: .combine)
            }
        }
        .fixedSize(horizontal: false, vertical: true)
        .frame(maxWidth: MonarchAiWelcomeTokens.capabilitiesMaxWidth)
        .accessibilityElement(children: .contain)
        .accessibilityLabel(capabilitiesLabel ?? MonarchAiWelcomeStrings.capabilities)
    }
}

extension MonarchAiWelcome where Footer == EmptyView {
    /// A welcome with starters and no footer.
    ///
    /// - Parameters:
    ///   - title: The greeting, already localised. It names the welcome.
    ///   - agent: The assistant answering in this conversation.
    ///   - description: One or two sentences on what the assistant can see or do.
    ///   - capabilities: Short capability statements, shown as a checklist.
    ///   - capabilitiesLabel: Replaces the built-in spoken name "What I can help with".
    ///   - agentPrefix: Replaces the built-in "AI assistant" read before the agent's name.
    ///   - starters: Where to start, usually ``MonarchAiStarterPrompts``.
    public init(
        _ title: String,
        agent: MonarchAiAgentRef? = nil,
        description: String? = nil,
        capabilities: [String] = [],
        capabilitiesLabel: String? = nil,
        agentPrefix: String? = nil,
        @ViewBuilder starters: () -> Starters
    ) {
        self.init(
            title,
            agent: agent,
            description: description,
            capabilities: capabilities,
            capabilitiesLabel: capabilitiesLabel,
            agentPrefix: agentPrefix,
            starters: starters,
            footer: { EmptyView() }
        )
    }
}

extension MonarchAiWelcome where Starters == EmptyView, Footer == EmptyView {
    /// A welcome with only the agent, the greeting, the description and the capabilities.
    ///
    /// - Parameters:
    ///   - title: The greeting, already localised. It names the welcome.
    ///   - agent: The assistant answering in this conversation.
    ///   - description: One or two sentences on what the assistant can see or do.
    ///   - capabilities: Short capability statements, shown as a checklist.
    ///   - capabilitiesLabel: Replaces the built-in spoken name "What I can help with".
    ///   - agentPrefix: Replaces the built-in "AI assistant" read before the agent's name.
    public init(
        _ title: String,
        agent: MonarchAiAgentRef? = nil,
        description: String? = nil,
        capabilities: [String] = [],
        capabilitiesLabel: String? = nil,
        agentPrefix: String? = nil
    ) {
        self.init(
            title,
            agent: agent,
            description: description,
            capabilities: capabilities,
            capabilitiesLabel: capabilitiesLabel,
            agentPrefix: agentPrefix,
            starters: { EmptyView() },
            footer: { EmptyView() }
        )
    }
}

#if DEBUG
    #Preview("MonarchAiWelcome") {
        MonarchPreviewMatrix {
            VStack {
                MonarchAiWelcome(
                    "How can I help?",
                    agent: MonarchAiAgentRef(name: "Dossier assistant", role: "Writing assistant"),
                    description: "I can read the page you are on and the documents you share with me.",
                    capabilities: ["Summarise long documents", "Draft replies in your tone"],
                    starters: {
                        MonarchAiStarterPrompts(
                            [
                                MonarchAiStarterPrompt(
                                    id: "sum",
                                    title: "Summarise this page",
                                    prompt: "Summarise this page."
                                )
                            ]
                        ) { _ in }
                    },
                    footer: { MonarchAiDisclaimer() }
                )
                MonarchAiWelcome("Ask about your projects")
            }
        }
    }
#endif

private struct MonarchAiWelcomeHidesMarkKey: EnvironmentKey {
    static let defaultValue = false
}

extension EnvironmentValues {
    /// Set by a host that already shows the agent's face, such as the assistant panel, so the welcome is text only.
    var monarchAiWelcomeHidesMark: Bool {
        get { self[MonarchAiWelcomeHidesMarkKey.self] }
        set { self[MonarchAiWelcomeHidesMarkKey.self] = newValue }
    }
}
