import SwiftUI

/// Every comment thread of a document or record: Open and Resolved threads
/// in a list, and one thread at a time pushed as a screen with its quote,
/// replies, Resolve or Reopen and confirmed delete.
///
/// ```swift
/// .sheet(isPresented: $showsComments) {
///     NavigationStack {
///         MonarchCommentsPanel(
///             store.threads,
///             filter: $filter,
///             selectedThreadID: $selectedThreadID,
///             onClose: { showsComments = false },
///             onReply: { id, text in try await store.reply(text, to: id) },
///             onResolvedChange: { id, isResolved in try await store.setResolved(isResolved, id) },
///             onDeleteComment: { id, commentID in try await store.delete(commentID, in: id) }
///         )
///     }
///     .presentationDetents([.medium, .large])
/// }
/// ```
///
/// Put it in a `NavigationStack`: in a `.sheet` on a phone, or in
/// `.inspector` at regular width. The panel sets the navigation title and,
/// with `onClose`, a Close button. Selecting a thread pushes it with
/// `navigationDestination(item:)`, so the system back button and the back
/// swipe return to the list and set `selectedThreadID` to `nil`. The app
/// owns `filter` and `selectedThreadID`, so it can open a thread when its
/// highlight is tapped and highlight the passage of the thread that is open.
/// The open thread is a ``MonarchCommentThread``. After a thread is
/// resolved or reopened the panel returns to the list. `threads` is `nil`
/// while loading. Times are relative and stay current; pass `now`
/// to fix them.
///
/// Accessibility: the navigation title is the screen's heading. Open and
/// Resolved are a segmented `Picker` with the system selected state. Each
/// thread is one button read as the starter, the quote, the number of
/// comments and the last activity; the avatar is decorative. Loading is one
/// element read as "Loading comments". Resolving, reopening, replies and
/// failures are announced by ``MonarchCommentThread``. Rows and buttons are
/// at least 44 points.
@_spi(Experimental) public struct MonarchCommentsPanel: View {
    private let threads: [MonarchCommentsPanelThread]?
    @Binding private var filter: MonarchCommentsPanelFilter
    @Binding private var selectedThreadID: String?
    private let errorMessage: String?
    private let now: Date?
    private let labels: MonarchCommentsPanelLabels
    private let onRetry: (@MainActor () -> Void)?
    private let onClose: (@MainActor () -> Void)?
    private let onReply: (@MainActor (String, String) async throws -> Void)?
    private let onResolvedChange: (@MainActor (String, Bool) async throws -> Void)?
    private let onDeleteComment: (@MainActor (String, String) async throws -> Void)?

    @Environment(\.monarchTheme) private var theme
    @Environment(\.locale) private var locale
    @Environment(\.calendar) private var calendar

    private typealias Tokens = MonarchCommentsPanelTokens
    private typealias Strings = MonarchCommentsPanelStrings

    /// A comments panel.
    ///
    /// - Parameters:
    ///   - threads: The threads, or `nil` while they load. Listed in this order.
    ///   - filter: Open or Resolved: which threads are listed. Changing it closes the open thread.
    ///   - selectedThreadID: The thread shown in full, or `nil` for the list. A thread not listed under the filter shows the list.
    ///   - errorMessage: Loading failed: shown instead of the list.
    ///   - now: Fixes "now" for the relative times, for tests and previews. The clock by default.
    ///   - labels: Replacements for the built-in strings, including the open thread's.
    ///   - onRetry: Adds Try again to `errorMessage`.
    ///   - onClose: Adds a Close button to the toolbar, for a sheet or an inspector.
    ///   - onReply: Posts a reply to the thread with this id. Adds the reply field to threads with `canReply`. Throw to keep the text and show a failure.
    ///   - onResolvedChange: Resolves (`true`) or reopens the thread with this id. Adds the button to threads with `canResolve`. On success the panel returns to the list; throw to show a failure.
    ///   - onDeleteComment: Deletes a comment, given the thread's id and the comment's, after a confirmation dialog. Offered on comments with `canDelete`.
    public init(
        _ threads: [MonarchCommentsPanelThread]?,
        filter: Binding<MonarchCommentsPanelFilter>,
        selectedThreadID: Binding<String?>,
        errorMessage: String? = nil,
        now: Date? = nil,
        labels: MonarchCommentsPanelLabels = MonarchCommentsPanelLabels(),
        onRetry: (@MainActor () -> Void)? = nil,
        onClose: (@MainActor () -> Void)? = nil,
        onReply: (@MainActor (String, String) async throws -> Void)? = nil,
        onResolvedChange: (@MainActor (String, Bool) async throws -> Void)? = nil,
        onDeleteComment: (@MainActor (String, String) async throws -> Void)? = nil
    ) {
        self.threads = threads
        self._filter = filter
        self._selectedThreadID = selectedThreadID
        self.errorMessage = errorMessage
        self.now = now
        self.labels = labels
        self.onRetry = onRetry
        self.onClose = onClose
        self.onReply = onReply
        self.onResolvedChange = onResolvedChange
        self.onDeleteComment = onDeleteComment
    }

    /// The open thread's id while it is listed under the filter; setting it selects.
    private var shownID: Binding<String?> {
        Binding(
            get: { Tokens.shownThread(threads, filter: filter, selectedID: selectedThreadID)?.id },
            set: { selectedThreadID = $0 }
        )
    }

    /// The content of the view.
    public var body: some View {
        VStack(spacing: 0) {
            MonarchSegmentedControl(
                labels.filter ?? Strings.filter,
                selection: $filter,
                items: MonarchCommentsPanelFilter.allCases.map { value in
                    MonarchSegmentedItem(Tokens.filterTitle(value, labels: labels), value: value)
                },
                size: .sm
            )
            .padding(Tokens.filterPadding)
            timed { reference in list(now: reference) }
        }
        .background(theme.colors.bgSurface)
        .navigationTitle(labels.title ?? Strings.title)
        #if os(iOS)
            .navigationBarTitleDisplayMode(.inline)
        #endif
        .toolbar {
            // The title sits at the leading edge with the close button opposite, as on the web.
            ToolbarItem(placement: .principal) {
                Text(labels.title ?? Strings.title)
                    .monarchTextStyle(.bodyStrong)
                    .foregroundStyle(theme.colors.textDefault)
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .accessibilityAddTraits(.isHeader)
            }
            closeItem
        }
        .navigationDestination(item: shownID) { id in
            detail(id)
        }
        .onChange(of: filter) { _, _ in
            if selectedThreadID != nil { selectedThreadID = nil }
        }
    }

    // MARK: - List

    private func list(now reference: Date) -> some View {
        List {
            switch Tokens.content(threads: threads, errorMessage: errorMessage, filter: filter) {
            case .loading:
                MonarchLoadingState(labels.loading ?? Strings.loading, variant: .rows, rows: Tokens.placeholderRows)
                    .listRowSeparator(.hidden)
                    .listRowBackground(Color.clear)
            case .failed(let message):
                failure(message)
                    .listRowSeparator(.hidden)
                    .listRowBackground(Color.clear)
            case .empty:
                MonarchEmptyState(
                    Tokens.emptyTitle(filter, labels: labels),
                    message: Tokens.emptyMessage(filter, labels: labels),
                    systemImage: Tokens.emptySymbol,
                    size: .sm
                )
                .frame(maxWidth: .infinity)
                .listRowSeparator(.hidden)
                .listRowBackground(Color.clear)
            case .list:
                ForEach(Tokens.visible(threads ?? [], filter: filter)) { thread in
                    row(thread, now: reference)
                        .listRowBackground(Color.clear)
                }
            }
        }
        .listStyle(.plain)
        .scrollContentBackground(.hidden)
    }

    @ViewBuilder private func failure(_ message: String) -> some View {
        if let onRetry {
            MonarchInlineErrorRetry(message, onRetry: onRetry)
        } else {
            Text(message)
                .monarchTextStyle(.body)
                .foregroundStyle(theme.colors[role: Tokens.failure])
                .fixedSize(horizontal: false, vertical: true)
        }
    }

    private func row(_ thread: MonarchCommentsPanelThread, now reference: Date) -> some View {
        Button {
            selectedThreadID = thread.id
        } label: {
            HStack(alignment: .top, spacing: Tokens.rowSpacing) {
                if let starter = thread.comments.first {
                    MonarchAvatar(
                        url: starter.avatarURL,
                        name: starter.author,
                        seed: starter.seed,
                        size: .sm,
                        isDecorative: true
                    )
                }
                VStack(alignment: .leading, spacing: Tokens.lineSpacing) {
                    if let author = thread.comments.first?.author {
                        Text(author)
                            .monarchTextStyle(.bodyStrong)
                            .foregroundStyle(theme.colors[role: Tokens.author])
                    }
                    if let quote = thread.quote {
                        Text("\u{201C}\(quote)\u{201D}")
                            .monarchTextStyle(.body)
                            .foregroundStyle(theme.colors[role: Tokens.muted])
                            .lineLimit(Tokens.quoteLines)
                    }
                    Text(Tokens.detailLine(thread, now: reference, locale: locale, calendar: calendar, labels: labels))
                        .monarchTextStyle(.caption)
                        .foregroundStyle(theme.colors[role: Tokens.muted])
                }
                .frame(maxWidth: .infinity, alignment: .leading)
            }
            .padding(.vertical, Tokens.rowPadding)
            .monarchMinimumHitTarget()
            .contentShape(Rectangle())
        }
        // The button is one element; this replaces the label built from its texts.
        .accessibilityLabel(Tokens.rowLabel(thread, now: reference, locale: locale, calendar: calendar, labels: labels))
    }

    // MARK: - Thread

    @ViewBuilder private func detail(_ id: String) -> some View {
        if let thread = Tokens.shownThread(threads, filter: filter, selectedID: id) {
            let actions = Tokens.actions(
                thread,
                hasReply: onReply != nil,
                hasResolve: onResolvedChange != nil,
                hasDelete: onDeleteComment != nil
            )
            ScrollView {
                MonarchCommentThread(
                    thread.comments,
                    quote: thread.quote,
                    isResolved: thread.isResolved,
                    now: now,
                    labels: labels.thread,
                    onResolvedChange: actions.resolve ? resolveAction(thread.id) : nil,
                    onDelete: actions.delete ? deleteAction(thread.id) : nil,
                    onReply: actions.reply ? replyAction(thread.id) : nil
                )
                .padding(.vertical, Tokens.threadPadding)
            }
            // A new thread starts with an empty reply field and no stale failure.
            .id(thread.id)
            .navigationTitle(Tokens.commentCount(thread.comments.count, labels: labels))
            #if os(iOS)
                .navigationBarTitleDisplayMode(.inline)
            #endif
            .toolbar { closeItem }
        }
    }

    @ToolbarContentBuilder private var closeItem: some ToolbarContent {
        if let onClose {
            ToolbarItem(placement: .primaryAction) {
                Button(action: onClose) {
                    MonarchIcon(systemName: "xmark", label: labels.close ?? Strings.close, size: .sm)
                        .foregroundStyle(theme.colors.textDefault)
                }
            }
            .monarchFlat()
        }
    }

    // MARK: - Actions

    // Each action captures only sendable values: the app's main-actor
    // function, the thread's id and the selection binding.

    private func replyAction(_ id: String) -> @MainActor (String) async throws -> Void {
        let onReply = onReply
        return { text in try await onReply?(id, text) }
    }

    private func resolveAction(_ id: String) -> @MainActor (Bool) async throws -> Void {
        let onResolvedChange = onResolvedChange
        let selection = $selectedThreadID
        return { value in
            try await onResolvedChange?(id, value)
            // Still looking at it: back to the list. MonarchCommentThread announces the change.
            if selection.wrappedValue == id { selection.wrappedValue = nil }
        }
    }

    private func deleteAction(_ id: String) -> @MainActor (String) async throws -> Void {
        let onDeleteComment = onDeleteComment
        return { comment in try await onDeleteComment?(id, comment) }
    }

    // MARK: - Time

    /// Fixed to `now` when given, on the relative-time schedule otherwise.
    @ViewBuilder private func timed<V: View>(@ViewBuilder _ make: @escaping (Date) -> V) -> some View {
        if let now {
            make(now)
        } else {
            TimelineView(MonarchRelativeTimeSchedule(dates: (threads ?? []).compactMap(\.updatedAt))) { context in
                make(context.date)
            }
        }
    }
}

#if DEBUG
    #Preview("MonarchCommentsPanel") {
        let now = Date(timeIntervalSinceReferenceDate: 800_000_000)
        let threads = [
            MonarchCommentsPanelThread(
                id: "t1",
                comments: [
                    MonarchCommentThreadComment(
                        id: "c1",
                        author: "Priya Raman",
                        body: "Should this say billed every quarter?",
                        createdAt: now.addingTimeInterval(-3_000)
                    ),
                    MonarchCommentThreadComment(
                        id: "c2",
                        author: "Tom Becker",
                        body: "Yes. Monthly billing was retired.",
                        createdAt: now.addingTimeInterval(-1_200)
                    ),
                ],
                quote: "Billed monthly, cancel any time."
            )
        ]
        MonarchPreviewMatrix {
            NavigationStack {
                MonarchCommentsPanel(
                    threads,
                    filter: .constant(.open),
                    selectedThreadID: .constant(nil),
                    now: now,
                    onClose: {}
                )
            }
            .frame(height: 360)
            NavigationStack {
                MonarchCommentsPanel(threads, filter: .constant(.resolved), selectedThreadID: .constant(nil), now: now)
            }
            .frame(height: 360)
        }
    }
#endif
