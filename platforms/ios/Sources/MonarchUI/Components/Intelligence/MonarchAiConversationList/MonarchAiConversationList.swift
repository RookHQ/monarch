import MonarchTokens
import SwiftUI

/// The history of an AI chat: conversations grouped into Pinned, Today,
/// Previous 7 days and Older, with search, the open conversation marked,
/// and Rename, Pin and Delete on each row.
///
/// ```swift
/// NavigationStack {
///     MonarchAiConversationList(
///         history,
///         activeID: openID,
///         onNew: { startConversation() },
///         onRename: { id, title in store.rename(id, to: title) },
///         onPin: { id, isPinned in store.pin(id, isPinned) },
///         onDelete: { id in store.delete(id) },
///         onSelect: { id in openID = id }
///     )
///     .navigationTitle("History")
/// }
/// ```
///
/// A standard `List` with one `Section` per group, newest first within each
/// group. Search uses `searchable`, so place the list in a
/// `NavigationStack` or a `NavigationSplitView` sidebar to show the field.
/// Without `onSearch` the list filters titles and previews itself; with it
/// the app searches and the list shows the conversations it is given.
/// Swiping a row leads to Pin and trails to Delete; a long press opens a
/// menu with Rename, Pin and Delete. Rename opens a dialog with a text
/// field; Delete asks for confirmation first. A long history shows
/// `visibleCount` rows and a button that shows more.
///
/// Accessibility: the list is named "Conversation history" and each group
/// heading is a header. A row is a button read as its title and preview;
/// the open conversation has the selected trait and a tinted row. Swipe and menu commands are offered to
/// VoiceOver as actions. After a local search changes, the number of
/// matches is announced.
public struct MonarchAiConversationList: View {
    private let conversations: [MonarchAiConversationSummary]
    private let activeID: String?
    private let isSearchable: Bool
    private let visibleCount: Int
    private let now: Date?
    private let calendar: Calendar
    private let labels: MonarchAiConversationListLabels
    private let onSearch: (@MainActor (_ query: String) -> Void)?
    private let onNew: (@MainActor () -> Void)?
    private let onRename: (@MainActor (_ id: String, _ title: String) -> Void)?
    private let onPin: (@MainActor (_ id: String, _ isPinned: Bool) -> Void)?
    private let onDelete: (@MainActor (_ id: String) -> Void)?
    private let onSelect: @MainActor (_ id: String) -> Void

    @Environment(\.monarchTheme) private var theme
    @State private var query = ""
    @State private var shown: Int
    @State private var renaming: MonarchAiConversationSummary?
    @State private var draft = ""
    @State private var deleting: MonarchAiConversationSummary?

    /// A conversation list.
    ///
    /// - Parameters:
    ///   - conversations: The history, in any order.
    ///   - activeID: The open conversation, marked as selected.
    ///   - isSearchable: Whether the list has a search field.
    ///   - visibleCount: How many rows are shown at first, and how many "Show more" adds.
    ///   - now: The reference time for the groups. The current time when `nil`.
    ///   - calendar: The calendar and time zone of the groups.
    ///   - labels: Replaces the built-in strings.
    ///   - onSearch: Searches in the app: receives every change of the query,
    ///     and the list shows `conversations` as given.
    ///   - onNew: Adds a "New conversation" button at the top.
    ///   - onRename: Adds Rename. Receives the id and the trimmed new title.
    ///   - onPin: Adds Pin and Unpin. Receives the id and whether it is now pinned.
    ///   - onDelete: Adds Delete, after a confirmation. Receives the id.
    ///   - onSelect: Opens a conversation. Receives the id.
    public init(
        _ conversations: [MonarchAiConversationSummary],
        activeID: String? = nil,
        isSearchable: Bool = true,
        visibleCount: Int = 200,
        now: Date? = nil,
        calendar: Calendar = .current,
        labels: MonarchAiConversationListLabels = MonarchAiConversationListLabels(),
        onSearch: (@MainActor (_ query: String) -> Void)? = nil,
        onNew: (@MainActor () -> Void)? = nil,
        onRename: (@MainActor (_ id: String, _ title: String) -> Void)? = nil,
        onPin: (@MainActor (_ id: String, _ isPinned: Bool) -> Void)? = nil,
        onDelete: (@MainActor (_ id: String) -> Void)? = nil,
        onSelect: @escaping @MainActor (_ id: String) -> Void
    ) {
        self.conversations = conversations
        self.activeID = activeID
        self.isSearchable = isSearchable
        self.visibleCount = visibleCount
        self.now = now
        self.calendar = calendar
        self.labels = labels
        self.onSearch = onSearch
        self.onNew = onNew
        self.onRename = onRename
        self.onPin = onPin
        self.onDelete = onDelete
        self.onSelect = onSelect
        _shown = State(initialValue: MonarchAiConversationListTokens.step(visibleCount))
    }

    private typealias Tokens = MonarchAiConversationListTokens

    private var visible: [MonarchAiConversationSummary] {
        onSearch == nil ? conversations.filter { Tokens.matches($0, query: query) } : conversations
    }

    /// The content of the view.
    public var body: some View {
        list
    }

    private var list: some View {
        let visible = visible
        let sections = Tokens.sections(visible, now: now ?? Date(), calendar: calendar)
        let hidden = max(0, visible.count - shown)
        return List {
            Group {
                if let onNew {
                    MonarchButton(
                        labels.newConversation,
                        systemImage: Tokens.newSymbol,
                        variant: .secondary,
                        size: .md,
                        fullWidth: true,
                        action: onNew
                    )
                    .listRowSeparator(.hidden)
                }
                if isSearchable {
                    // The web puts the search field under "New conversation", inside the list.
                    MonarchTextField(
                        labels.search,
                        text: $query,
                        prompt: labels.search,
                        labelVisibility: .hidden,
                        leading: {
                            MonarchIcon(systemName: "magnifyingglass", size: .sm)
                                .foregroundStyle(theme.colors.textMuted)
                                .accessibilityHidden(true)
                        },
                        trailing: { EmptyView() }
                    )
                    .listRowSeparator(.hidden)
                }
                if sections.isEmpty {
                    emptyState.listRowBackground(Color.clear)
                }
                ForEach(Tokens.capped(sections, limit: shown)) { section in
                    Section {
                        ForEach(section.items) { row($0) }
                    } header: {
                        Text(labels.group(section.group))
                            .monarchTextStyle(.caption)
                            .foregroundStyle(theme.colors.textMuted)
                            .accessibilityAddTraits(.isHeader)
                    }
                }
                if hidden > 0 {
                    Button(labels.showMore(min(hidden, Tokens.step(visibleCount)))) {
                        shown += Tokens.step(visibleCount)
                    }
                    .buttonStyle(.monarch(.ghost, size: .sm))
                }

            }
            .monarchRowsOnCanvas()
        }
        .listStyle(.plain)
        .monarchCanvas()
        .accessibilityLabel(labels.label)
        .onChange(of: query) { _, newQuery in
            onSearch?(newQuery)
            let count = conversations.filter { Tokens.matches($0, query: newQuery) }.count
            if let text = Tokens.announcement(
                query: newQuery,
                count: count,
                searchesLocally: onSearch == nil,
                labels: labels
            ) {
                AccessibilityNotification.Announcement(text).post()
            }
        }
        .alert(labels.renameTitle, isPresented: isRenaming) {
            TextField(labels.renameField, text: $draft)
            Button(labels.save) { saveRename() }
            Button(labels.cancel, role: .cancel) {}
        }
        .confirmationDialog(
            labels.deleteTitle(deleting.map { Tokens.title($0, untitled: labels.untitled) } ?? ""),
            isPresented: isDeleting,
            titleVisibility: .visible
        ) {
            Button(labels.deleteConfirm, role: .destructive) {
                if let deleting { onDelete?(deleting.id) }
            }
            Button(labels.cancel, role: .cancel) {}
        } message: {
            Text(labels.deleteMessage)
        }
    }

    @ViewBuilder private var emptyState: some View {
        if conversations.isEmpty {
            MonarchEmptyState(labels.empty, kind: .firstRun, size: .sm)
        } else {
            MonarchEmptyState(labels.noResults(query.trimmingCharacters(in: .whitespaces)), kind: .filtered, size: .sm)
            {
                query = ""
            }
        }
    }

    private func row(_ conversation: MonarchAiConversationSummary) -> some View {
        let title = Tokens.title(conversation, untitled: labels.untitled)
        let isActive = conversation.id == activeID
        return Button {
            onSelect(conversation.id)
        } label: {
            HStack(spacing: Tokens.markerSpacing) {
                if conversation.isPinned {
                    MonarchIcon(systemName: Tokens.pinnedMarkSymbol, size: .sm)
                        .foregroundStyle(theme.colors.textMuted)
                        .accessibilityHidden(true)
                }
                VStack(alignment: .leading, spacing: Tokens.textSpacing) {
                    Text(title)
                        .monarchTextStyle(.body)
                        .foregroundStyle(isActive ? theme.colors.accentStrong : theme.colors.textDefault)
                        .lineLimit(2)
                    if let preview = conversation.preview, !preview.isEmpty {
                        Text(preview)
                            .monarchTextStyle(.caption)
                            .foregroundStyle(theme.colors.textMuted)
                            .lineLimit(1)
                    }
                }
                .frame(maxWidth: .infinity, alignment: .leading)
                if isActive, hasCommands {
                    // The "..." of the web row, a menu with the same commands as the swipe actions.
                    Menu {
                        commands(for: conversation)
                    } label: {
                        MonarchIcon(systemName: Tokens.moreSymbol, size: .sm)
                            .foregroundStyle(theme.colors.textMuted)
                            .monarchMinimumHitTarget()
                    }
                    .menuStyle(.button)
                    .buttonStyle(.plain)
                }
            }
            .fixedSize(horizontal: false, vertical: true)
            .monarchMinimumHitTarget()
            .padding(.horizontal, theme.spacing.space2)
            .background(
                RoundedRectangle(cornerRadius: MonarchTokens.Radius.md, style: .continuous)
                    .fill(isActive ? theme.colors.accentSubtle : Color.clear)
            )
            .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
        .listRowSeparator(.hidden)
        .accessibilityElement(children: .ignore)
        .accessibilityLabel(Tokens.accessibilityLabel(title: title, preview: conversation.preview))
        .accessibilityAddTraits(isActive ? [.isButton, .isSelected] : .isButton)
        .swipeActions(edge: .leading, allowsFullSwipe: true) {
            if let onPin {
                Button {
                    onPin(conversation.id, !conversation.isPinned)
                } label: {
                    commandLabel(pinTitle(conversation), pinSymbol(conversation))
                }
                .tint(theme.colors.accentSolid)
            }
        }
        .swipeActions(edge: .trailing, allowsFullSwipe: false) {
            if onDelete != nil {
                Button {
                    deleting = conversation
                } label: {
                    commandLabel(labels.delete, Tokens.deleteSymbol)
                }
                .tint(theme.colors.dangerSolid)
            }
        }
        .contextMenu {
            commands(for: conversation)
        }
    }

    private var hasCommands: Bool { onRename != nil || onPin != nil || onDelete != nil }

    @ViewBuilder private func commands(for conversation: MonarchAiConversationSummary) -> some View {
        if onRename != nil {
            Button {
                draft = conversation.title
                renaming = conversation
            } label: {
                commandLabel(labels.rename, Tokens.renameSymbol)
            }
        }
        if let onPin {
            Button {
                onPin(conversation.id, !conversation.isPinned)
            } label: {
                commandLabel(pinTitle(conversation), pinSymbol(conversation))
            }
        }
        if onDelete != nil {
            Button(role: .destructive) {
                deleting = conversation
            } label: {
                commandLabel(labels.delete, Tokens.deleteSymbol)
            }
        }
    }

    private func commandLabel(_ title: String, _ symbol: String) -> some View {
        Label(title, systemImage: symbol)
    }

    private func pinTitle(_ conversation: MonarchAiConversationSummary) -> String {
        conversation.isPinned ? labels.unpin : labels.pin
    }

    private func pinSymbol(_ conversation: MonarchAiConversationSummary) -> String {
        conversation.isPinned ? Tokens.unpinSymbol : Tokens.pinSymbol
    }

    private var isRenaming: Binding<Bool> {
        Binding(get: { renaming != nil }, set: { if !$0 { renaming = nil } })
    }

    private var isDeleting: Binding<Bool> {
        Binding(get: { deleting != nil }, set: { if !$0 { deleting = nil } })
    }

    private func saveRename() {
        guard let renaming, let title = Tokens.renamed(draft, original: renaming.title) else { return }
        onRename?(renaming.id, title)
    }
}

#if DEBUG
    #Preview("MonarchAiConversationList") {
        let now = Date()
        let day = MonarchAiConversationListTokens.day
        MonarchPreviewMatrix {
            NavigationStack {
                MonarchAiConversationList(
                    [
                        MonarchAiConversationSummary(id: "1", title: "Quarterly plan", updatedAt: now, isPinned: true),
                        MonarchAiConversationSummary(
                            id: "2",
                            title: "Summarise the support tickets",
                            updatedAt: now,
                            preview: "Here are the five themes from this week."
                        ),
                        MonarchAiConversationSummary(id: "3", title: "", updatedAt: now.addingTimeInterval(-day * 3)),
                        MonarchAiConversationSummary(
                            id: "4",
                            title: "Trip ideas",
                            updatedAt: now.addingTimeInterval(-day * 30)
                        ),
                    ],
                    activeID: "2",
                    onNew: {},
                    onRename: { _, _ in },
                    onPin: { _, _ in },
                    onDelete: { _ in },
                    onSelect: { _ in }
                )
            }
            .frame(height: MonarchAiConversationListTokens.previewHeight)
        }
    }
#endif
