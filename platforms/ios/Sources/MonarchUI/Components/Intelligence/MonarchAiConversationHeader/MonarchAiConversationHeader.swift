import SwiftUI

/// The title of a conversation with an AI assistant, for the navigation bar:
/// the title (often generated from the first prompt), the assistant and the
/// model under it, and a title menu with Rename and the app's own actions.
///
/// ```swift
/// .toolbar {
///     ToolbarItem(placement: .principal) {
///         MonarchAiConversationHeader(
///             conversation.title,
///             agent: assistant,
///             model: "Fast model",
///             onRename: { title in try await store.rename(conversation, to: title) },
///             actions: {
///                 ShareLink(item: conversation.url)
///                 Button("Delete", systemImage: "trash", role: .destructive) { delete() }
///             }
///         )
///     }
/// }
/// ```
///
/// This is the system title-menu pattern: the title is a `Menu` whose first
/// item is Rename when `onRename` is given, followed by the `actions`. Rename
/// opens the system prompt (an alert with a text field). The new title shows
/// with a spinner while `onRename` runs; if it throws, the prompt opens again
/// with the draft and "Couldn’t rename. Try again.". An empty or unchanged
/// title is not saved. Without `onRename` and actions the title is plain text.
/// It also works at the top of a screen outside a navigation bar.
///
/// To change the model, put a `Picker` in `actions` or a
/// ``MonarchAiModelPicker`` in a toolbar item.
///
/// Accessibility: the title is a header, read with the assistant and model
/// as its value ("AI assistant: Research agent. Model: Fast model"); as a
/// menu it is a button with the hint "Shows conversation actions". The agent
/// avatar is decorative because the name is written. A failed rename is
/// announced. The title wraps to two lines and the chevron is decorative.
@_spi(Experimental) public struct MonarchAiConversationHeader<Actions: View>: View {
    private let title: String
    private let agent: MonarchAiAgentRef?
    private let model: String?
    private let isNaming: Bool
    private let labels: MonarchAiConversationHeaderLabels
    private let onRename: (@MainActor (String) async throws -> Void)?
    private let hasActions: Bool
    private let actions: Actions

    @Environment(\.monarchTheme) private var theme
    @State private var isRenaming = false
    @State private var draft = ""
    @State private var pendingTitle: String?
    @State private var hasFailed = false

    private typealias Tokens = MonarchAiConversationHeaderTokens

    /// A conversation header with app actions in its title menu.
    ///
    /// - Parameters:
    ///   - title: The conversation's title. Empty shows "New conversation".
    ///   - agent: The assistant answering, shown with its avatar.
    ///   - model: The model in use, already localised.
    ///   - isNaming: Whether the title is being generated. Shows "Naming conversation…" and hides Rename.
    ///   - labels: Replacements for the built-in strings.
    ///   - onRename: Saves a new title, trimmed and changed. Throwing opens the prompt again with an error.
    ///   - actions: The app's menu items, such as Share and Delete.
    public init(
        _ title: String,
        agent: MonarchAiAgentRef? = nil,
        model: String? = nil,
        isNaming: Bool = false,
        labels: MonarchAiConversationHeaderLabels = MonarchAiConversationHeaderLabels(),
        onRename: (@MainActor (String) async throws -> Void)? = nil,
        @ViewBuilder actions: () -> Actions
    ) {
        self.title = title
        self.agent = agent
        self.model = model
        self.isNaming = isNaming
        self.labels = labels
        self.onRename = onRename
        self.hasActions = true
        self.actions = actions()
    }

    /// The content of the view.
    public var body: some View {
        let canRename = onRename != nil && !isNaming
        VStack(alignment: .leading, spacing: Tokens.lineSpacing) {
            HStack(spacing: Tokens.itemSpacing) {
                Text(Tokens.displayTitle(title, isNaming: isNaming, pendingTitle: pendingTitle, labels: labels))
                    .monarchTextStyle(.bodyStrong)
                    .foregroundStyle(theme.colors[role: isNaming ? Tokens.secondaryRole : Tokens.titleRole])
                    .lineLimit(Tokens.titleLineLimit)
                    .accessibilityAddTraits(.isHeader)
                if isNaming || pendingTitle != nil {
                    ProgressView()
                        .controlSize(.small)
                        .tint(theme.colors[role: Tokens.secondaryRole])
                }
                if canRename {
                    Button {
                        startRenaming()
                    } label: {
                        MonarchIcon(systemName: Tokens.renameSymbol, size: .sm)
                            .foregroundStyle(theme.colors[role: Tokens.titleRole])
                            .frame(
                                minWidth: MonarchAccessibility.minimumHitTarget,
                                minHeight: MonarchAccessibility.minimumHitTarget
                            )
                            .contentShape(Rectangle())
                    }
                    .buttonStyle(.plain)
                    .accessibilityLabel(labels.rename ?? MonarchAiConversationHeaderStrings.rename)
                }
                if hasActions {
                    Spacer(minLength: 0)
                    Menu {
                        actions
                    } label: {
                        MonarchIcon(systemName: Tokens.menuSymbol, size: .sm)
                            .foregroundStyle(theme.colors[role: Tokens.secondaryRole])
                            .frame(
                                minWidth: MonarchAccessibility.minimumHitTarget,
                                minHeight: MonarchAccessibility.minimumHitTarget
                            )
                            .contentShape(Rectangle())
                    }
                    .menuStyle(.button)
                    .buttonStyle(.plain)
                    .accessibilityLabel(labels.menuHint ?? MonarchAiConversationHeaderStrings.menuHint)
                }
            }
            if Tokens.subtitle(agent: agent, model: model) != nil {
                HStack(spacing: Tokens.itemSpacing) {
                    if agent != nil {
                        // The agent is its glyph on the AI tint, then its name in the AI colour, as on the web.
                        MonarchAiBotGlyph(size: .sm)
                            .foregroundStyle(theme.colors.aiStrong)
                            .frame(width: Tokens.agentTile, height: Tokens.agentTile)
                            .background(
                                RoundedRectangle(cornerRadius: Tokens.agentTile * 0.28, style: .continuous)
                                    .fill(theme.colors.aiSubtle)
                            )
                            .accessibilityHidden(true)
                        if let name = agent?.name.trimmingCharacters(in: .whitespacesAndNewlines), !name.isEmpty {
                            Text(name)
                                .monarchTextStyle(.caption, weight: .medium)
                                .foregroundStyle(theme.colors.aiStrong)
                                .lineLimit(1)
                        }
                    }
                    if let model = model?.trimmingCharacters(in: .whitespacesAndNewlines), !model.isEmpty {
                        Text(model)
                            .monarchTextStyle(.caption)
                            .foregroundStyle(theme.colors[role: Tokens.secondaryRole])
                            .lineLimit(1)
                    }
                }
                .accessibilityElement(children: .ignore)
                .accessibilityLabel(Tokens.accessibilityValue(agent: agent, model: model, labels: labels))
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .accessibilityElement(children: .contain)
        .alert(labels.rename ?? MonarchAiConversationHeaderStrings.rename, isPresented: $isRenaming) {
            TextField(labels.titleLabel ?? MonarchAiConversationHeaderStrings.titleLabel, text: $draft)
            Button(labels.cancel ?? MonarchAiConversationHeaderStrings.cancel, role: .cancel) {
                hasFailed = false
            }
            Button(labels.save ?? MonarchAiConversationHeaderStrings.save) {
                save()
            }
        } message: {
            if hasFailed {
                Text(labels.renameFailed ?? MonarchAiConversationHeaderStrings.renameFailed)
            }
        }
    }

    private func startRenaming() {
        draft = title.trimmingCharacters(in: .whitespacesAndNewlines)
        hasFailed = false
        isRenaming = true
    }

    private func save() {
        guard let onRename, let next = Tokens.renamedTitle(draft, current: title) else {
            hasFailed = false
            return
        }
        pendingTitle = next
        Task { @MainActor in
            do {
                try await onRename(next)
                pendingTitle = nil
                hasFailed = false
            } catch {
                pendingTitle = nil
                hasFailed = true
                let message = labels.renameFailed ?? MonarchAiConversationHeaderStrings.renameFailed
                AccessibilityNotification.Announcement(message).post()
                isRenaming = true
            }
        }
    }
}

extension MonarchAiConversationHeader where Actions == EmptyView {
    /// A conversation header without app actions; the title menu holds only Rename.
    ///
    /// - Parameters:
    ///   - title: The conversation's title. Empty shows "New conversation".
    ///   - agent: The assistant answering, shown with its avatar.
    ///   - model: The model in use, already localised.
    ///   - isNaming: Whether the title is being generated.
    ///   - labels: Replacements for the built-in strings.
    ///   - onRename: Saves a new title. Without it the title is plain text.
    public init(
        _ title: String,
        agent: MonarchAiAgentRef? = nil,
        model: String? = nil,
        isNaming: Bool = false,
        labels: MonarchAiConversationHeaderLabels = MonarchAiConversationHeaderLabels(),
        onRename: (@MainActor (String) async throws -> Void)? = nil
    ) {
        self.title = title
        self.agent = agent
        self.model = model
        self.isNaming = isNaming
        self.labels = labels
        self.onRename = onRename
        self.hasActions = false
        self.actions = EmptyView()
    }
}

#if DEBUG
    #Preview("MonarchAiConversationHeader") {
        MonarchPreviewMatrix {
            VStack {
                MonarchAiConversationHeader(
                    "Quarterly planning notes",
                    agent: MonarchAiAgentRef(name: "Research agent"),
                    model: "Fast model",
                    onRename: { _ in },
                    actions: {
                        Button("Delete", systemImage: "trash", role: .destructive) {}
                    }
                )
                MonarchAiConversationHeader("", isNaming: true)
                MonarchAiConversationHeader("")
            }
        }
    }
#endif
