import SwiftUI

/// What changed between two texts or two versions of a document: added and
/// removed lines, the changed words inside them, folded unchanged runs,
/// totals, and previous and next change buttons.
///
/// ```swift
/// MonarchDiffViewer(
///     before: "Ship on Friday.\nTell support.",
///     after: "Ship on Monday.\nTell support.\nUpdate the status page.",
///     beforeLabel: "Version 3",
///     afterLabel: "Version 4"
/// )
/// ```
///
/// The layout is unified on phones and side by side at regular width; at
/// regular width a segmented switch lets the reader change it. Long lines
/// wrap. Put the view in a `ScrollView`; previous and next scroll to a change.
///
/// Accessibility: the lines are a container named "Changes from Version 3
/// to Version 4". Each line is one element read as its text after "Added"
/// or "Removed"; a side by side row reads each side after its name. The
/// totals read "1 added, 1 removed". Moving to a change announces "Change 2
/// of 5" and moves VoiceOver to it. A change is marked by a + or − sign,
/// and changed words by a strikethrough or underline, as well as by colour.
public struct MonarchDiffViewer<Actions: View>: View {
    private let lines: [MonarchDiffViewerLine]
    private let segments: [Int: [MonarchDiffSegment]]
    private let modeBinding: Binding<MonarchDiffViewerMode>?
    private let context: Int?
    private let beforeLabel: String?
    private let afterLabel: String?
    private let showsModeSwitch: Bool
    private let showsLineNumbers: Bool
    private let labels: MonarchDiffViewerLabels
    private let actions: Actions

    @State private var chosenMode: MonarchDiffViewerMode?
    @State private var expanded: Set<Int> = []
    @State private var position: Int?
    @AccessibilityFocusState private var focusedHunk: Int?
    @Environment(\.horizontalSizeClass) private var horizontalSizeClass
    @Environment(\.monarchTheme) private var theme

    /// A diff of two lists of lines or blocks, with actions in the header.
    ///
    /// - Parameters:
    ///   - before: The lines or blocks of the earlier text (for documents, one entry per paragraph).
    ///   - after: The lines or blocks of the later text.
    ///   - mode: The layout, when the app owns it. `nil` picks by width and lets the reader switch.
    ///   - granularity: Whether changed words inside changed lines are marked.
    ///   - context: Unchanged lines kept around each change; longer runs fold. `nil` never folds.
    ///   - beforeLabel: The name of the earlier side, for example a version name.
    ///   - afterLabel: The name of the later side.
    ///   - showsModeSwitch: Whether the layout switch is shown.
    ///   - showsLineNumbers: Whether line numbers are shown.
    ///   - labels: Replacements for the built-in strings.
    ///   - actions: Controls at the end of the header, such as a Restore button.
    public init(
        before: [String],
        after: [String],
        mode: Binding<MonarchDiffViewerMode>? = nil,
        granularity: MonarchDiffViewerGranularity = .word,
        context: Int? = 3,
        beforeLabel: String? = nil,
        afterLabel: String? = nil,
        showsModeSwitch: Bool = true,
        showsLineNumbers: Bool = true,
        labels: MonarchDiffViewerLabels = MonarchDiffViewerLabels(),
        @ViewBuilder actions: () -> Actions
    ) {
        let lines = MonarchTextDiff.lines(before, after)
        self.lines = lines
        self.segments = granularity == .word ? MonarchTextDiff.wordSegments(lines) : [:]
        self.modeBinding = mode
        self.context = context
        self.beforeLabel = beforeLabel
        self.afterLabel = afterLabel
        self.showsModeSwitch = showsModeSwitch
        self.showsLineNumbers = showsLineNumbers
        self.labels = labels
        self.actions = actions()
    }

    /// A diff of two texts, split on line breaks, with actions in the header.
    ///
    /// - Parameters:
    ///   - before: The earlier text.
    ///   - after: The later text.
    ///   - mode: The layout, when the app owns it. `nil` picks by width and lets the reader switch.
    ///   - granularity: Whether changed words inside changed lines are marked.
    ///   - context: Unchanged lines kept around each change; longer runs fold. `nil` never folds.
    ///   - beforeLabel: The name of the earlier side, for example a version name.
    ///   - afterLabel: The name of the later side.
    ///   - showsModeSwitch: Whether the layout switch is shown.
    ///   - showsLineNumbers: Whether line numbers are shown.
    ///   - labels: Replacements for the built-in strings.
    ///   - actions: Controls at the end of the header, such as a Restore button.
    public init(
        before: String,
        after: String,
        mode: Binding<MonarchDiffViewerMode>? = nil,
        granularity: MonarchDiffViewerGranularity = .word,
        context: Int? = 3,
        beforeLabel: String? = nil,
        afterLabel: String? = nil,
        showsModeSwitch: Bool = true,
        showsLineNumbers: Bool = true,
        labels: MonarchDiffViewerLabels = MonarchDiffViewerLabels(),
        @ViewBuilder actions: () -> Actions
    ) {
        self.init(
            before: MonarchTextDiff.split(before),
            after: MonarchTextDiff.split(after),
            mode: mode,
            granularity: granularity,
            context: context,
            beforeLabel: beforeLabel,
            afterLabel: afterLabel,
            showsModeSwitch: showsModeSwitch,
            showsLineNumbers: showsLineNumbers,
            labels: labels,
            actions: actions
        )
    }

    private var isRegularWidth: Bool { horizontalSizeClass == .regular }

    private var mode: MonarchDiffViewerMode {
        MonarchDiffViewerTokens.mode(
            bound: modeBinding?.wrappedValue,
            chosen: chosenMode,
            isRegularWidth: isRegularWidth
        )
    }

    private var beforeName: String { beforeLabel ?? labels.before ?? MonarchDiffViewerStrings.before }
    private var afterName: String { afterLabel ?? labels.after ?? MonarchDiffViewerStrings.after }

    /// The content of the view.
    public var body: some View {
        let stats = MonarchTextDiff.stats(lines)
        let built = MonarchTextDiff.rows(
            lines,
            isSplit: mode == .split,
            context: MonarchDiffViewerTokens.context(context),
            expanded: expanded
        )
        VStack(alignment: .leading, spacing: MonarchDiffViewerTokens.spacing) {
            header(added: stats.added, removed: stats.removed, hunks: built.hunks)
            if stats.added + stats.removed > 0 {
                ScrollViewReader { proxy in
                    LazyVStack(alignment: .leading, spacing: 0) {
                        ForEach(Array(built.rows.enumerated()), id: \.offset) { _, row in
                            rowView(row)
                        }
                    }
                    .onChange(of: position) { _, new in
                        if let new { proxy.scrollTo(MonarchDiffViewerHunkID(index: new), anchor: .center) }
                    }
                }
                .clipShape(MonarchShapes.container)
                .overlay(
                    MonarchShapes.container.strokeBorder(
                        theme.colors[role: MonarchDiffViewerTokens.border],
                        lineWidth: MonarchShapes.borderWidth
                    )
                )
                .accessibilityElement(children: .contain)
                .accessibilityLabel(
                    MonarchDiffViewerTokens.caption(before: beforeName, after: afterName, labels: labels)
                )
            } else {
                Text(labels.noChanges ?? MonarchDiffViewerStrings.noChanges)
                    .monarchTextStyle(.body)
                    .foregroundStyle(theme.colors[role: MonarchDiffViewerTokens.muted])
            }
        }
        .onChange(of: lines) {
            expanded = []
            position = nil
        }
    }

    // MARK: Header

    private func header(added: Int, removed: Int, hunks: Int) -> some View {
        ViewThatFits(in: .horizontal) {
            HStack(spacing: MonarchDiffViewerTokens.headerSpacing) {
                statsView(added: added, removed: removed)
                Spacer(minLength: MonarchDiffViewerTokens.headerSpacing)
                controls(hunks: hunks)
            }
            VStack(alignment: .leading, spacing: MonarchDiffViewerTokens.headerSpacing) {
                statsView(added: added, removed: removed)
                MonarchFlowLayout(spacing: MonarchDiffViewerTokens.headerSpacing) {
                    controls(hunks: hunks)
                }
            }
        }
    }

    private func statsView(added: Int, removed: Int) -> some View {
        HStack(spacing: MonarchDiffViewerTokens.columnSpacing) {
            Text(MonarchDiffViewerTokens.statsText(added: added))
                .foregroundStyle(theme.colors[role: MonarchDiffViewerTokens.markerRole(.added)])
            Text(MonarchDiffViewerTokens.statsText(removed: removed))
                .foregroundStyle(theme.colors[role: MonarchDiffViewerTokens.markerRole(.removed)])
        }
        .monarchTextStyle(.label, weight: MonarchDiffViewerTokens.statsWeight)
        .monospacedDigit()
        .accessibilityElement(children: .ignore)
        .accessibilityLabel(MonarchDiffViewerTokens.statsLabel(added: added, removed: removed, labels: labels))
    }

    @ViewBuilder private func controls(hunks: Int) -> some View {
        if hunks > 1 {
            MonarchButton(
                labels.previous ?? MonarchDiffViewerStrings.previous,
                systemImage: MonarchDiffViewerTokens.previousSymbol,
                variant: .ghost,
                size: .sm
            ) {
                go(to: MonarchDiffViewerTokens.previous(from: position, hunks: hunks), hunks: hunks)
            }
            MonarchButton(
                labels.next ?? MonarchDiffViewerStrings.next,
                systemImage: MonarchDiffViewerTokens.nextSymbol,
                variant: .ghost,
                size: .sm
            ) {
                go(to: MonarchDiffViewerTokens.next(from: position, hunks: hunks), hunks: hunks)
            }
        }
        if showsModeSwitch {
            Picker(labels.mode ?? MonarchDiffViewerStrings.mode, selection: modeSelection) {
                Text(labels.unified ?? MonarchDiffViewerStrings.unified).tag(MonarchDiffViewerMode.unified)
                Text(labels.split ?? MonarchDiffViewerStrings.split).tag(MonarchDiffViewerMode.split)
            }
            .pickerStyle(.segmented)
            .fixedSize()
        }
        actions
    }

    private var modeSelection: Binding<MonarchDiffViewerMode> {
        Binding(
            get: { mode },
            set: { new in
                if let modeBinding { modeBinding.wrappedValue = new } else { chosenMode = new }
            }
        )
    }

    private func go(to index: Int?, hunks: Int) {
        guard let index else { return }
        position = index
        focusedHunk = index
        AccessibilityNotification.Announcement(
            MonarchDiffViewerTokens.positionText(index, of: hunks, labels: labels)
        ).post()
    }

    // MARK: Rows

    @ViewBuilder private func rowView(_ row: MonarchDiffRow) -> some View {
        switch row {
        case .line(let index, let hunk):
            hunkRow(hunk) {
                lineCell(index, number: true)
                    .accessibilityElement(children: .ignore)
                    .accessibilityLabel(MonarchDiffViewerTokens.spokenLine(lines[index], labels: labels))
            }
        case .pair(let old, let new, let hunk):
            hunkRow(hunk) {
                HStack(alignment: .top, spacing: 0) {
                    sideCell(old)
                    Rectangle()
                        .fill(theme.colors[role: MonarchDiffViewerTokens.border])
                        .frame(width: MonarchShapes.borderWidth)
                    sideCell(new)
                }
                .fixedSize(horizontal: false, vertical: true)
                .accessibilityElement(children: .ignore)
                .accessibilityLabel(
                    MonarchDiffViewerTokens.spokenPair(
                        old: old.map { lines[$0] },
                        new: new.map { lines[$0] },
                        beforeName: beforeName,
                        afterName: afterName,
                        labels: labels
                    )
                )
            }
        case .gap(let id, let gapLines):
            MonarchButton(
                MonarchDiffViewerTokens.expandTitle(gapLines.count, labels: labels),
                systemImage: MonarchDiffViewerTokens.expandSymbol,
                variant: .ghost,
                size: .sm,
                fullWidth: true
            ) {
                expanded.insert(id)
            }
            .background(theme.colors[role: MonarchDiffViewerTokens.gapBackground])
        }
    }

    @ViewBuilder private func hunkRow(_ hunk: Int?, @ViewBuilder content: () -> some View) -> some View {
        if let hunk {
            content()
                .overlay(alignment: .leading) {
                    if hunk == position {
                        Rectangle()
                            .fill(theme.colors[role: MonarchDiffViewerTokens.currentBar])
                            .frame(width: MonarchDiffViewerTokens.currentBarWidth)
                    }
                }
                .accessibilityFocused($focusedHunk, equals: hunk)
                .id(MonarchDiffViewerHunkID(index: hunk))
        } else {
            content()
        }
    }

    @ViewBuilder private func sideCell(_ index: Int?) -> some View {
        if let index {
            lineCell(index, number: false)
        } else {
            Color.clear
                .frame(maxWidth: .infinity, maxHeight: .infinity)
                .background(theme.colors[role: MonarchDiffViewerTokens.gapBackground])
        }
    }

    /// One line: its numbers, its marker and its text. `number` is true for
    /// the unified layout, which shows both numbers.
    private func lineCell(_ index: Int, number: Bool) -> some View {
        let line = lines[index]
        let digits = MonarchDiffViewerTokens.numberDigits(lines)
        return HStack(alignment: .firstTextBaseline, spacing: MonarchDiffViewerTokens.columnSpacing) {
            if showsLineNumbers {
                if number {
                    numberText(line.oldNumber, digits: digits)
                    numberText(line.newNumber, digits: digits)
                } else {
                    numberText(line.kind == .added ? line.newNumber : line.oldNumber, digits: digits)
                }
            }
            Text(MonarchDiffViewerTokens.marker(line.kind))
                .foregroundStyle(theme.colors[role: MonarchDiffViewerTokens.markerRole(line.kind)])
            Text(attributed(index))
                .foregroundStyle(theme.colors[role: MonarchDiffViewerTokens.text])
                .frame(maxWidth: .infinity, alignment: .leading)
                .fixedSize(horizontal: false, vertical: true)
        }
        .monarchTextStyle(.code)
        .padding(.horizontal, MonarchDiffViewerTokens.lineHorizontalPadding)
        .padding(.vertical, MonarchDiffViewerTokens.lineVerticalPadding)
        .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .topLeading)
        .background(MonarchDiffViewerTokens.lineBackground(line.kind).map { theme.colors[role: $0] })
    }

    private func numberText(_ number: Int?, digits: Int) -> some View {
        Text(MonarchDiffViewerTokens.numberText(number, digits: digits))
            .foregroundStyle(theme.colors[role: MonarchDiffViewerTokens.number])
    }

    /// The text of a line, with changed words filled and struck through
    /// (removed) or underlined (added).
    private func attributed(_ index: Int) -> AttributedString {
        let line = lines[index]
        guard line.kind != .same, let segments = segments[index] else {
            return AttributedString(line.text.isEmpty ? " " : line.text)
        }
        var result = AttributedString()
        for segment in MonarchDiffViewerTokens.visibleSegments(segments, forKind: line.kind) {
            var piece = AttributedString(segment.text)
            if let role = MonarchDiffViewerTokens.wordBackground(segment.kind) {
                piece[AttributeScopes.SwiftUIAttributes.BackgroundColorAttribute.self] = theme.colors[role: role]
                if segment.kind == .removed {
                    piece[AttributeScopes.SwiftUIAttributes.StrikethroughStyleAttribute.self] = .single
                } else {
                    piece[AttributeScopes.SwiftUIAttributes.UnderlineStyleAttribute.self] = .single
                }
            }
            result += piece
        }
        return result.characters.isEmpty ? AttributedString(" ") : result
    }
}

/// The scroll target of a change of ``MonarchDiffViewer``.
private struct MonarchDiffViewerHunkID: Hashable {
    let index: Int
}

extension MonarchDiffViewer where Actions == EmptyView {
    /// A diff of two lists of lines or blocks.
    ///
    /// - Parameters:
    ///   - before: The lines or blocks of the earlier text (for documents, one entry per paragraph).
    ///   - after: The lines or blocks of the later text.
    ///   - mode: The layout, when the app owns it. `nil` picks by width and lets the reader switch.
    ///   - granularity: Whether changed words inside changed lines are marked.
    ///   - context: Unchanged lines kept around each change; longer runs fold. `nil` never folds.
    ///   - beforeLabel: The name of the earlier side, for example a version name.
    ///   - afterLabel: The name of the later side.
    ///   - showsModeSwitch: Whether the layout switch is shown.
    ///   - showsLineNumbers: Whether line numbers are shown.
    ///   - labels: Replacements for the built-in strings.
    public init(
        before: [String],
        after: [String],
        mode: Binding<MonarchDiffViewerMode>? = nil,
        granularity: MonarchDiffViewerGranularity = .word,
        context: Int? = 3,
        beforeLabel: String? = nil,
        afterLabel: String? = nil,
        showsModeSwitch: Bool = true,
        showsLineNumbers: Bool = true,
        labels: MonarchDiffViewerLabels = MonarchDiffViewerLabels()
    ) {
        self.init(
            before: before,
            after: after,
            mode: mode,
            granularity: granularity,
            context: context,
            beforeLabel: beforeLabel,
            afterLabel: afterLabel,
            showsModeSwitch: showsModeSwitch,
            showsLineNumbers: showsLineNumbers,
            labels: labels,
            actions: { EmptyView() }
        )
    }

    /// A diff of two texts, split on line breaks.
    ///
    /// - Parameters:
    ///   - before: The earlier text.
    ///   - after: The later text.
    ///   - mode: The layout, when the app owns it. `nil` picks by width and lets the reader switch.
    ///   - granularity: Whether changed words inside changed lines are marked.
    ///   - context: Unchanged lines kept around each change; longer runs fold. `nil` never folds.
    ///   - beforeLabel: The name of the earlier side, for example a version name.
    ///   - afterLabel: The name of the later side.
    ///   - showsModeSwitch: Whether the layout switch is shown.
    ///   - showsLineNumbers: Whether line numbers are shown.
    ///   - labels: Replacements for the built-in strings.
    public init(
        before: String,
        after: String,
        mode: Binding<MonarchDiffViewerMode>? = nil,
        granularity: MonarchDiffViewerGranularity = .word,
        context: Int? = 3,
        beforeLabel: String? = nil,
        afterLabel: String? = nil,
        showsModeSwitch: Bool = true,
        showsLineNumbers: Bool = true,
        labels: MonarchDiffViewerLabels = MonarchDiffViewerLabels()
    ) {
        self.init(
            before: before,
            after: after,
            mode: mode,
            granularity: granularity,
            context: context,
            beforeLabel: beforeLabel,
            afterLabel: afterLabel,
            showsModeSwitch: showsModeSwitch,
            showsLineNumbers: showsLineNumbers,
            labels: labels,
            actions: { EmptyView() }
        )
    }
}

#if DEBUG
    #Preview("MonarchDiffViewer") {
        MonarchPreviewMatrix {
            VStack(alignment: .leading) {
                MonarchDiffViewer(
                    before: "Ship on Friday.\nTell support.\nKeep the old banner.",
                    after: "Ship on Monday.\nTell support.\nUpdate the status page.",
                    beforeLabel: "Version 3",
                    afterLabel: "Version 4"
                )
                MonarchDiffViewer(
                    before: "One\nTwo",
                    after: "One\nTwo, changed",
                    mode: .constant(.split),
                    showsLineNumbers: false
                )
                MonarchDiffViewer(before: "Same", after: "Same")
            }
        }
    }
#endif
