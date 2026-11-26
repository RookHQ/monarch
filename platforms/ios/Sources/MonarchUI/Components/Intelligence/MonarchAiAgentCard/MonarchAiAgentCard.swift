import MonarchTokens
import SwiftUI

/// What an AI agent is and what it may do: its name and role, a
/// description, its capabilities, the tools it can call, the data it can
/// see and who owns it, with optional actions such as "Start a chat".
///
/// ```swift
/// MonarchAiAgentCard(
///     agent,
///     description: "Finds and summarises sources for your documents.",
///     capabilities: ["Searches the web", "Drafts summaries"],
///     tools: [MonarchAiAgentTool("Search documents", description: "Reads files in this space")],
///     dataAccess: ["Documents in this space"],
///     owner: "Platform team"
/// ) {
///     MonarchButton("Start a chat", variant: .primary, size: .sm) { startChat() }
/// }
/// ```
///
/// The card is a ``MonarchCard``. The role line always ends with "AI
/// agent", so the card never reads as a person's profile. The `compact`
/// variant is a one-card summary (avatar, name, role, description,
/// capabilities and a tool count) for lists and pickers, and leaves out the
/// tools, data access, owner and actions. Sections without content are left
/// out.
///
/// Accessibility: the name is a header, read with the role line. Section
/// headings are headers. Each tool is one element read as its name and what
/// it does; the owner is read as "Owner, <name>". The avatar is decorative
/// and reads only a state other than idle. Actions stay separate elements.
@_spi(Experimental) public struct MonarchAiAgentCard<Actions: View>: View {
    private let agent: MonarchAiAgentRef
    private let description: String?
    private let capabilities: [String]
    private let tools: [MonarchAiAgentTool]
    private let dataAccess: [String]
    private let owner: String?
    private let state: MonarchAiAgentState
    private let variant: MonarchAiAgentCardVariant
    private let labels: MonarchAiAgentCardLabels
    private let actions: Actions

    @Environment(\.monarchTheme) private var theme
    @Environment(\.monarchAiAgentCardTrailingInset) private var trailingInset

    private typealias Tokens = MonarchAiAgentCardTokens

    /// An agent card with actions at the end.
    ///
    /// - Parameters:
    ///   - agent: The agent: name, role, codename and avatar.
    ///   - description: One or two sentences on what the agent is for.
    ///   - capabilities: Short capability phrases, shown as tags, such as "Drafts replies".
    ///   - tools: The tools the agent can call, with what each does.
    ///   - dataAccess: What data the agent can read, such as "Documents in this space".
    ///   - owner: Who owns or maintains the agent.
    ///   - state: What the agent is doing now, shown on its avatar.
    ///   - variant: Every section, or the compact summary for lists.
    ///   - labels: Replacements for the built-in strings.
    ///   - actions: Buttons or links at the end of the full card, such as "Start a chat".
    public init(
        _ agent: MonarchAiAgentRef,
        description: String? = nil,
        capabilities: [String] = [],
        tools: [MonarchAiAgentTool] = [],
        dataAccess: [String] = [],
        owner: String? = nil,
        state: MonarchAiAgentState = .idle,
        variant: MonarchAiAgentCardVariant = .full,
        labels: MonarchAiAgentCardLabels = MonarchAiAgentCardLabels(),
        @ViewBuilder actions: () -> Actions
    ) {
        self.agent = agent
        self.description = description
        self.capabilities = capabilities
        self.tools = tools
        self.dataAccess = dataAccess
        self.owner = owner
        self.state = state
        self.variant = variant
        self.labels = labels
        self.actions = actions()
    }

    /// The content of the view.
    public var body: some View {
        MonarchCard {
            VStack(alignment: .leading, spacing: Tokens.sectionSpacing) {
                header
                if let description, !description.isEmpty {
                    Text(description)
                        .monarchTextStyle(.body)
                        .foregroundStyle(theme.colors.textDefault)
                        .fixedSize(horizontal: false, vertical: true)
                }
                if variant == .compact, !capabilities.isEmpty {
                    tags
                        .accessibilityElement(children: .contain)
                        .accessibilityLabel(labels.capabilities ?? MonarchAiAgentCardStrings.capabilities)
                }
                ForEach(
                    Tokens.sections(
                        variant: variant,
                        capabilities: capabilities,
                        tools: tools,
                        dataAccess: dataAccess,
                        owner: owner
                    ),
                    id: \.self
                ) { section in
                    self.section(section)
                }
                if variant == .full, Actions.self != EmptyView.self {
                    MonarchFlowLayout(spacing: Tokens.itemSpacing) { actions }
                }
            }
        }
        .accessibilityElement(children: .contain)
    }

    private var header: some View {
        HStack(alignment: .top, spacing: Tokens.headerSpacing) {
            MonarchAiAgentAvatar(agent, state: state, size: Tokens.avatarSize(variant), isDecorative: true)
            VStack(alignment: .leading, spacing: Tokens.lineSpacing) {
                Text(agent.name)
                    .monarchTextStyle(.subheading)
                    .foregroundStyle(theme.colors.textDefault)
                Text(Tokens.subtitle(agent, labels: labels))
                    .monarchTextStyle(.caption)
                    .foregroundStyle(theme.colors.textMuted)
            }
            .frame(maxWidth: .infinity, alignment: .leading)
            .accessibilityElement(children: .combine)
            .accessibilityAddTraits(.isHeader)
            if let count = Tokens.toolCount(tools.count, variant: variant, labels: labels) {
                Text(count)
                    .monarchTextStyle(.caption)
                    .foregroundStyle(theme.colors.textMuted)
                    .padding(.trailing, trailingInset)
            }
        }
    }

    private var tags: some View {
        MonarchFlowLayout(spacing: Tokens.tagSpacing) {
            ForEach(capabilities, id: \.self) { capability in
                MonarchAiTag(capability, style: .caption)
            }
        }
    }

    @ViewBuilder private func section(_ section: Tokens.Section) -> some View {
        switch section {
        case .capabilities:
            titled(labels.capabilities ?? MonarchAiAgentCardStrings.capabilities) { tags }
        case .tools:
            titled(labels.tools ?? MonarchAiAgentCardStrings.tools) {
                ForEach(tools) { tool in
                    // As on the web: the name and what it does beside a rule in the AI border colour.
                    HStack(alignment: .top, spacing: Tokens.itemSpacing) {
                        Rectangle()
                            .fill(theme.colors.aiBorder)
                            .frame(width: MonarchTokens.Space.space0_5)
                            .accessibilityHidden(true)
                        VStack(alignment: .leading, spacing: Tokens.lineSpacing) {
                            Text(tool.name)
                                .monarchTextStyle(.body, weight: .medium)
                                .foregroundStyle(theme.colors.textDefault)
                            if let description = tool.description, !description.isEmpty {
                                Text(description)
                                    .monarchTextStyle(.caption)
                                    .foregroundStyle(theme.colors.textMuted)
                            }
                        }
                        .fixedSize(horizontal: false, vertical: true)
                    }
                    .fixedSize(horizontal: false, vertical: true)
                    .accessibilityElement(children: .ignore)
                    .accessibilityLabel(Tokens.toolLabel(tool))
                }
            }
        case .dataAccess:
            titled(labels.dataAccess ?? MonarchAiAgentCardStrings.dataAccess) {
                ForEach(dataAccess, id: \.self) { item in
                    HStack(alignment: .firstTextBaseline, spacing: Tokens.itemSpacing) {
                        Circle()
                            .fill(theme.colors.textMuted)
                            .frame(width: MonarchTokens.Space.space1_5, height: MonarchTokens.Space.space1_5)
                            .accessibilityHidden(true)
                        Text(item)
                            .monarchTextStyle(.body)
                            .foregroundStyle(theme.colors.textDefault)
                            .fixedSize(horizontal: false, vertical: true)
                    }
                }
            }
        case .owner:
            if let owner {
                HStack(alignment: .center, spacing: Tokens.itemSpacing) {
                    Text(labels.owner ?? MonarchAiAgentCardStrings.owner)
                        .monarchTextStyle(.caption, weight: .semibold)
                        .foregroundStyle(theme.colors.textMuted)
                    MonarchAvatar(image: nil, name: owner, size: .xs, isDecorative: true)
                    Text(owner)
                        .monarchTextStyle(.body)
                        .foregroundStyle(theme.colors.textDefault)
                }
                .accessibilityElement(children: .ignore)
                .accessibilityLabel(Tokens.ownerLabel(owner, labels: labels))
            }
        }
    }

    private func titled(_ title: String, @ViewBuilder content: () -> some View) -> some View {
        VStack(alignment: .leading, spacing: Tokens.itemSpacing) {
            Text(title)
                .monarchTextStyle(.caption, weight: .semibold)
                .foregroundStyle(theme.colors.textMuted)
                .accessibilityAddTraits(.isHeader)
            content()
        }
    }
}

extension MonarchAiAgentCard where Actions == EmptyView {
    /// An agent card.
    ///
    /// - Parameters:
    ///   - agent: The agent: name, role, codename and avatar.
    ///   - description: One or two sentences on what the agent is for.
    ///   - capabilities: Short capability phrases, shown as tags, such as "Drafts replies".
    ///   - tools: The tools the agent can call, with what each does.
    ///   - dataAccess: What data the agent can read, such as "Documents in this space".
    ///   - owner: Who owns or maintains the agent.
    ///   - state: What the agent is doing now, shown on its avatar.
    ///   - variant: Every section, or the compact summary for lists.
    ///   - labels: Replacements for the built-in strings.
    public init(
        _ agent: MonarchAiAgentRef,
        description: String? = nil,
        capabilities: [String] = [],
        tools: [MonarchAiAgentTool] = [],
        dataAccess: [String] = [],
        owner: String? = nil,
        state: MonarchAiAgentState = .idle,
        variant: MonarchAiAgentCardVariant = .full,
        labels: MonarchAiAgentCardLabels = MonarchAiAgentCardLabels()
    ) {
        self.init(
            agent,
            description: description,
            capabilities: capabilities,
            tools: tools,
            dataAccess: dataAccess,
            owner: owner,
            state: state,
            variant: variant,
            labels: labels,
            actions: { EmptyView() }
        )
    }
}

#if DEBUG
    #Preview("MonarchAiAgentCard") {
        let agent = MonarchAiAgentRef(id: "research", name: "Research agent", role: "Finds sources")
        MonarchPreviewMatrix {
            VStack {
                MonarchAiAgentCard(
                    agent,
                    description: "Finds and summarises sources for your documents.",
                    capabilities: ["Searches the web", "Drafts summaries"],
                    tools: [MonarchAiAgentTool("Search documents", description: "Reads files in this space")],
                    dataAccess: ["Documents in this space"],
                    owner: "Platform team",
                    state: .working
                ) {
                    MonarchButton("Start a chat", variant: .primary, size: .sm) {}
                }
                MonarchAiAgentCard(
                    agent,
                    capabilities: ["Drafts summaries"],
                    tools: [MonarchAiAgentTool("Search documents")],
                    variant: .compact
                )
            }
        }
    }
#endif

private struct MonarchAiAgentCardTrailingInsetKey: EnvironmentKey {
    static let defaultValue: CGFloat = 0
}

extension EnvironmentValues {
    /// Room kept free at the end of the first line, for a mark a picker draws over the card's corner.
    var monarchAiAgentCardTrailingInset: CGFloat {
        get { self[MonarchAiAgentCardTrailingInsetKey.self] }
        set { self[MonarchAiAgentCardTrailingInsetKey.self] = newValue }
    }
}
