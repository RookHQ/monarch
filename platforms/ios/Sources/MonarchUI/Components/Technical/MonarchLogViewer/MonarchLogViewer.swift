import SwiftUI

/// A streaming log: level filters with counts, search that filters or
/// highlights, follow mode that keeps the newest line in view and pauses
/// when the reader scrolls back, and a wrap toggle.
///
/// ```swift
/// MonarchLogViewer(build.lines, query: $query) {
///     ShareLink(item: build.logURL)
/// }
/// .frame(height: 420)
/// ```
///
/// Lines are oldest first; append to the array to stream. Rows are built
/// lazily and each line is a single monospaced text, so long logs stay
/// cheap. While following, every new line scrolls the log to the end.
/// Dragging toward older lines pauses following; scrolling back to the end,
/// the Follow toggle or the jump button ("3 new lines") resumes it. Each
/// state is kept inside unless the app passes a binding for it. Give the
/// viewer a height with `frame(height:)`; it never grows with its lines.
///
/// Accessibility: the log is a container named "Log". Each line is one
/// element read as its level, time, source and message. Level filters are a
/// toggle group read with their counts; Follow and Wrap are toggles.
/// Pausing and resuming following are announced. Matches are tinted and
/// underlined, so they do not rely on colour alone.
public struct MonarchLogViewer<Actions: View>: View {
    private let lines: [MonarchLogLine]
    private let levels: Binding<Set<MonarchLogLevel>>?
    private let query: Binding<String>?
    private let isFollowing: Binding<Bool>?
    private let isWrapped: Binding<Bool>?
    private let searchMode: MonarchLogViewerSearchMode
    private let showsTimestamps: Bool
    private let showsLineNumbers: Bool
    private let isLoading: Bool
    private let labels: MonarchLogViewerLabels
    private let actions: Actions

    @State private var localLevels = Set(MonarchLogLevel.allCases)
    @State private var localQuery = ""
    @State private var localFollowing = true
    @State private var localWrapped = false
    @State private var pausedAt: Int?
    @State private var hasScrolledSincePause = false

    @Environment(\.monarchTheme) private var theme
    @Environment(\.timeZone) private var timeZone

    /// A log viewer with extra controls at the end of the toolbar.
    ///
    /// - Parameters:
    ///   - lines: The lines, oldest first. Append to stream.
    ///   - levels: The levels shown. Kept inside, all four at first, when `nil`.
    ///   - query: The search text. Kept inside when `nil`.
    ///   - isFollowing: Whether the newest line is kept in view. Kept inside, on at first, when `nil`.
    ///   - isWrapped: Whether long lines wrap. Kept inside, off at first, when `nil`.
    ///   - searchMode: Whether the search hides lines without a match, or only marks matches.
    ///   - showsTimestamps: Whether the time column is shown.
    ///   - showsLineNumbers: Whether each line shows its 1-based position.
    ///   - isLoading: Whether the first lines are loading. Shown while `lines` is empty.
    ///   - labels: Replacements for the built-in strings.
    ///   - actions: Extra controls after Follow and Wrap, such as a share or clear button.
    public init(
        _ lines: [MonarchLogLine],
        levels: Binding<Set<MonarchLogLevel>>? = nil,
        query: Binding<String>? = nil,
        isFollowing: Binding<Bool>? = nil,
        isWrapped: Binding<Bool>? = nil,
        searchMode: MonarchLogViewerSearchMode = .filter,
        showsTimestamps: Bool = true,
        showsLineNumbers: Bool = false,
        isLoading: Bool = false,
        labels: MonarchLogViewerLabels = MonarchLogViewerLabels(),
        @ViewBuilder actions: () -> Actions
    ) {
        self.lines = lines
        self.levels = levels
        self.query = query
        self.isFollowing = isFollowing
        self.isWrapped = isWrapped
        self.searchMode = searchMode
        self.showsTimestamps = showsTimestamps
        self.showsLineNumbers = showsLineNumbers
        self.isLoading = isLoading
        self.labels = labels
        self.actions = actions()
    }

    private typealias Tokens = MonarchLogViewerTokens

    private var levelSelection: Binding<Set<MonarchLogLevel>> { levels ?? $localLevels }
    private var queryText: Binding<String> { query ?? $localQuery }
    private var following: Binding<Bool> { isFollowing ?? $localFollowing }
    private var wrapped: Binding<Bool> { isWrapped ?? $localWrapped }

    /// The content of the view.
    public var body: some View {
        let visible = Tokens.visible(
            lines,
            levels: levelSelection.wrappedValue,
            query: queryText.wrappedValue,
            mode: searchMode
        )
        VStack(alignment: .leading, spacing: Tokens.stackSpacing) {
            toolbar
            viewport(visible)
                .overlay(alignment: .bottom) { jumpButton }
            Text(labels.count(visible.count, lines.count))
                .monarchTextStyle(.caption)
                .foregroundStyle(theme.colors.textMuted)
        }
        .onChange(of: following.wrappedValue) { _, isOn in
            pausedAt = isOn ? nil : lines.count
            hasScrolledSincePause = false
            AccessibilityNotification.Announcement(Tokens.followAnnouncement(isFollowing: isOn, labels: labels))
                .post()
        }
    }

    private var toolbar: some View {
        let counts = Tokens.counts(lines)
        return VStack(alignment: .leading, spacing: Tokens.toolbarSpacing) {
            MonarchLogViewerSearchField(
                label: labels.search,
                prompt: labels.searchPrompt,
                clearLabel: labels.clearSearch,
                text: queryText
            )
            MonarchToggleGroup(
                labels.levels,
                selection: levelSelection,
                options: MonarchLogLevel.allCases.map { level in
                    MonarchToggleGroupOption(
                        level,
                        label: labels.levelName(level),
                        count: (counts[level] ?? 0).formatted()
                    )
                },
                size: .sm
            )
            .environment(\.monarchToggleGroupTone, .neutral)
            // As on the web: ghost toggles that show a grey fill while on.
            MonarchFlowLayout(spacing: Tokens.toolbarSpacing) {
                Toggle(labels.follow, isOn: following)
                    .toggleStyle(.monarchButton(.ghost, size: .sm, tone: .neutral))
                Toggle(labels.wrap, isOn: wrapped)
                    .toggleStyle(.monarchButton(.ghost, size: .sm, tone: .neutral))
                actions
            }
        }
    }

    private func viewport(_ visible: [Tokens.Entry]) -> some View {
        let shape = RoundedRectangle(cornerRadius: Tokens.cornerRadius, style: .continuous)
        return Group {
            switch Tokens.content(isLoading: isLoading, total: lines.count, visible: visible.count) {
            case .loading:
                HStack(spacing: Tokens.toolbarSpacing) {
                    ProgressView()
                    Text(labels.loading).monarchTextStyle(.body)
                }
                .foregroundStyle(theme.colors.textMuted)
                .accessibilityElement(children: .combine)
                .frame(maxWidth: .infinity, maxHeight: .infinity)
            case .empty:
                MonarchEmptyState(labels.empty, systemImage: Tokens.emptySymbol, size: .sm)
                    .frame(maxWidth: .infinity, maxHeight: .infinity)
            case .noMatches:
                MonarchEmptyState(
                    labels.noMatches,
                    kind: .filtered,
                    size: .sm,
                    onClearFilters: {
                        queryText.wrappedValue = ""
                        levelSelection.wrappedValue = Set(MonarchLogLevel.allCases)
                    }
                )
                .frame(maxWidth: .infinity, maxHeight: .infinity)
            case .lines:
                scroller(visible)
            }
        }
        .frame(maxWidth: .infinity, minHeight: Tokens.minimumViewportHeight, maxHeight: .infinity)
        .background(shape.fill(theme.colors[role: Tokens.viewportBackground]))
        .clipShape(shape)
        .overlay(shape.strokeBorder(theme.colors[role: Tokens.viewportBorder], lineWidth: Tokens.borderWidth))
        .accessibilityElement(children: .contain)
        .accessibilityLabel(labels.log)
    }

    private func scroller(_ visible: [Tokens.Entry]) -> some View {
        let isWrapped = wrapped.wrappedValue
        let needle = Tokens.needle(queryText.wrappedValue)
        let numberWidth = String(lines.count).count
        return GeometryReader { proxy in
            let rowWidth = max(0, proxy.size.width - Tokens.viewportPadding * 2)
            ScrollViewReader { reader in
                ScrollView(isWrapped ? [.vertical] : [.vertical, .horizontal]) {
                    LazyVStack(alignment: .leading, spacing: 0) {
                        ForEach(visible) { entry in
                            row(
                                entry,
                                needle: needle,
                                numberWidth: numberWidth,
                                isWrapped: isWrapped,
                                minWidth: rowWidth
                            )
                        }
                        Color.clear
                            .frame(height: 1)
                            .id(Tokens.bottomID)
                            .onAppear {
                                if !following.wrappedValue && hasScrolledSincePause {
                                    following.wrappedValue = true
                                }
                            }
                    }
                    .padding(Tokens.viewportPadding)
                    .frame(width: isWrapped ? proxy.size.width : nil, alignment: .leading)
                }
                .defaultScrollAnchor(following.wrappedValue ? .bottomLeading : .topLeading)
                .simultaneousGesture(
                    DragGesture(minimumDistance: Tokens.pauseDragDistance).onChanged { value in
                        if following.wrappedValue {
                            if Tokens.isDragTowardOlder(value.translation) { following.wrappedValue = false }
                        } else {
                            hasScrolledSincePause = true
                        }
                    }
                )
                .onAppear {
                    if following.wrappedValue { reader.scrollTo(Tokens.bottomID, anchor: .bottomLeading) }
                }
                .onChange(of: visible.last?.id) {
                    if following.wrappedValue { reader.scrollTo(Tokens.bottomID, anchor: .bottomLeading) }
                }
                .onChange(of: following.wrappedValue) { _, isOn in
                    if isOn { reader.scrollTo(Tokens.bottomID, anchor: .bottomLeading) }
                }
                .onChange(of: isWrapped) {
                    if following.wrappedValue { reader.scrollTo(Tokens.bottomID, anchor: .bottomLeading) }
                }
            }
        }
    }

    private func row(
        _ entry: Tokens.Entry,
        needle: String,
        numberWidth: Int,
        isWrapped: Bool,
        minWidth: CGFloat
    ) -> some View {
        let levelName = labels.levelName(entry.line.level)
        let time = showsTimestamps ? entry.line.time.map { Tokens.timeText($0, timeZone: timeZone) } : nil
        return MonarchLogViewerRow(
            entry: entry,
            text: Tokens.lineText(
                entry,
                needle: needle,
                showsTimestamps: showsTimestamps,
                showsLineNumbers: showsLineNumbers,
                numberWidth: numberWidth,
                timeZone: timeZone,
                // The level is set in capitals on the line, as on the web; the filters keep their names.
                levelName: levelName.uppercased(),
                colors: theme.colors
            ),
            accessibilityText: Tokens.accessibilityLabel(entry.line, levelName: levelName, time: time),
            isWrapped: isWrapped,
            minWidth: minWidth
        )
    }

    @ViewBuilder private var jumpButton: some View {
        if !following.wrappedValue && !lines.isEmpty {
            MonarchButton(
                Tokens.jumpTitle(unseen: Tokens.unseen(total: lines.count, pausedAt: pausedAt), labels: labels),
                systemImage: Tokens.jumpSymbol,
                variant: .secondary,
                size: .sm
            ) {
                following.wrappedValue = true
            }
            .padding(Tokens.stackSpacing)
        }
    }
}

extension MonarchLogViewer where Actions == EmptyView {
    /// A log viewer.
    ///
    /// - Parameters:
    ///   - lines: The lines, oldest first. Append to stream.
    ///   - levels: The levels shown. Kept inside, all four at first, when `nil`.
    ///   - query: The search text. Kept inside when `nil`.
    ///   - isFollowing: Whether the newest line is kept in view. Kept inside, on at first, when `nil`.
    ///   - isWrapped: Whether long lines wrap. Kept inside, off at first, when `nil`.
    ///   - searchMode: Whether the search hides lines without a match, or only marks matches.
    ///   - showsTimestamps: Whether the time column is shown.
    ///   - showsLineNumbers: Whether each line shows its 1-based position.
    ///   - isLoading: Whether the first lines are loading. Shown while `lines` is empty.
    ///   - labels: Replacements for the built-in strings.
    public init(
        _ lines: [MonarchLogLine],
        levels: Binding<Set<MonarchLogLevel>>? = nil,
        query: Binding<String>? = nil,
        isFollowing: Binding<Bool>? = nil,
        isWrapped: Binding<Bool>? = nil,
        searchMode: MonarchLogViewerSearchMode = .filter,
        showsTimestamps: Bool = true,
        showsLineNumbers: Bool = false,
        isLoading: Bool = false,
        labels: MonarchLogViewerLabels = MonarchLogViewerLabels()
    ) {
        self.init(
            lines,
            levels: levels,
            query: query,
            isFollowing: isFollowing,
            isWrapped: isWrapped,
            searchMode: searchMode,
            showsTimestamps: showsTimestamps,
            showsLineNumbers: showsLineNumbers,
            isLoading: isLoading,
            labels: labels
        ) {
            EmptyView()
        }
    }
}

#if DEBUG
    #Preview("MonarchLogViewer") {
        let start = Date(timeIntervalSinceReferenceDate: 800_000_000)
        let lines = (0..<40).map { index in
            MonarchLogLine(
                id: "\(index)",
                message: index % 7 == 0
                    ? "Request failed: upstream timed out after 30 s while fetching the dependency graph"
                    : "Processed batch \(index)",
                level: MonarchLogLevel.allCases[index % 4],
                time: start.addingTimeInterval(Double(index)),
                source: index % 3 == 0 ? "api" : nil
            )
        }
        MonarchPreviewMatrix {
            VStack(alignment: .leading) {
                MonarchLogViewer(lines, query: .constant("batch"), searchMode: .highlight, showsLineNumbers: true)
                    .frame(height: 360)
                MonarchLogViewer([], isLoading: true).frame(height: 200)
                MonarchLogViewer([]).frame(height: 200)
            }
        }
    }
#endif
