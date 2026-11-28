import SwiftUI

/// A collapsible, searchable tree for a JSON document, with copy path and
/// copy value for the selected node.
///
/// ```swift
/// let payload = try MonarchJsonValue(parsing: body)
/// ScrollView {
///     MonarchJsonViewer(payload, expandDepth: 2)
/// }
/// ```
///
/// Rows are a flattened model of the open part of the tree: children are
/// walked only when their container is open, and a large object or array
/// shows its children in batches behind a "Show more" row, so big payloads
/// stay fast. Rows are in a plain `VStack`: the batches bound how many rows
/// are built, and the viewer lays out the same way inside or outside a
/// scroll view. Search looks through every key and primitive
/// value, opens the containers that hold a match, marks the matched text and
/// says how many matches there are; it stops after `maxMatches`. Tapping a
/// row selects it and opens or closes it; the footer shows the selected
/// path with buttons that copy the path and the value.
///
/// Accessibility: the tree is a container named "JSON". Each row is a button
/// read as "key: value" (`{ } 3 keys` for a closed object), with
/// "expanded" or "collapsed" as its value and the selected trait on the
/// selected row. Matches are also bold, not only highlighted, and a new
/// match count is announced. Rows are at least 44 points tall.
public struct MonarchJsonViewer: View {
    private let value: MonarchJsonValue
    private let expandDepth: Int
    private let maxChildren: Int
    private let maxStringLength: Int
    private let isSearchable: Bool
    private let maxMatches: Int
    private let isCopyable: Bool
    private let rootPath: String
    private let labels: MonarchJsonViewerLabels
    private let onSelect: (@MainActor (String, MonarchJsonValue) -> Void)?
    private let onCopy: (@MainActor (String) -> Void)?

    @Environment(\.monarchTheme) private var theme
    @ScaledMetric(relativeTo: .body) private var indent = MonarchJsonViewerTokens.indent
    @ScaledMetric(relativeTo: .body) private var chevronColumn = MonarchJsonViewerTokens.chevronColumn
    @State private var baseDepth: Int?
    @State private var overrides: [String: Bool] = [:]
    @State private var limits: [String: Int] = [:]
    @State private var query = ""
    @State private var selectedID: String?

    /// A JSON viewer.
    ///
    /// - Parameters:
    ///   - value: The document. Parse text with ``MonarchJsonValue/init(parsing:)``.
    ///   - expandDepth: Levels open at first: 1 shows the root's children,
    ///     0 only the root.
    ///   - maxChildren: Children shown per object or array before a "Show
    ///     more" row; each Show more adds this many. At least 1.
    ///   - maxStringLength: Strings longer than this are cut in the tree.
    ///     The copied value is always complete.
    ///   - isSearchable: Whether the search field is shown.
    ///   - maxMatches: The search stops after this many matches.
    ///   - isCopyable: Whether the footer with the selected path and the
    ///     copy buttons is shown.
    ///   - rootPath: The first part of shown and copied paths. `$` gives
    ///     `$.users[0].name`; an empty string gives `users[0].name`.
    ///   - labels: Replacements for the built-in strings.
    ///   - onSelect: Runs with the path and value when the selection changes.
    ///   - onCopy: Runs with the copied text after a path or value is copied.
    public init(
        _ value: MonarchJsonValue,
        expandDepth: Int = 1,
        maxChildren: Int = 100,
        maxStringLength: Int = 500,
        isSearchable: Bool = true,
        maxMatches: Int = 500,
        isCopyable: Bool = true,
        rootPath: String = "$",
        labels: MonarchJsonViewerLabels = MonarchJsonViewerLabels(),
        onSelect: (@MainActor (String, MonarchJsonValue) -> Void)? = nil,
        onCopy: (@MainActor (String) -> Void)? = nil
    ) {
        self.value = value
        self.expandDepth = max(0, expandDepth)
        self.maxChildren = max(1, maxChildren)
        self.maxStringLength = max(1, maxStringLength)
        self.isSearchable = isSearchable
        self.maxMatches = max(1, maxMatches)
        self.isCopyable = isCopyable
        self.rootPath = rootPath
        self.labels = labels
        self.onSelect = onSelect
        self.onCopy = onCopy
    }

    private var needle: String {
        query.trimmingCharacters(in: .whitespacesAndNewlines).lowercased()
    }

    /// The content of the view.
    public var body: some View {
        let needle = needle
        let search = MonarchJsonViewerModel.search(value, needle: needle, maxMatches: maxMatches)
        let overrides = overrides
        let baseDepth = baseDepth ?? expandDepth
        let rows = MonarchJsonViewerModel.rows(
            value,
            isOpen: { id, depth in
                MonarchJsonViewerModel.isOpen(
                    id: id,
                    depth: depth,
                    overrides: overrides,
                    search: search,
                    baseDepth: baseDepth
                )
            },
            limits: limits,
            maxChildren: maxChildren,
            reveal: search.reveal
        )
        let selected = rows.first { $0.id == selectedID && $0.kind == .node } ?? rows.first
        let shape = MonarchShapes.container
        VStack(alignment: .leading, spacing: 0) {
            toolbar(needle: needle, search: search)
            divider
            // Not a LazyVStack: outside a scroll view, rows of different
            // heights (wrapped strings at large text sizes) that run past the
            // window made the lazy stack swap measured rows for estimates
            // without end, and layout never finished.
            VStack(alignment: .leading, spacing: 0) {
                ForEach(rows) { row in
                    rowView(
                        row,
                        rows: rows,
                        needle: needle,
                        isMatch: search.matched.contains(row.id),
                        isOpen: row.isExpandable
                            && MonarchJsonViewerModel.isOpen(
                                id: row.id,
                                depth: row.depth,
                                overrides: overrides,
                                search: search,
                                baseDepth: baseDepth
                            ),
                        isSelected: row.id == selected?.id
                    )
                }
            }
            .padding(.vertical, MonarchJsonViewerTokens.barPadding)
            .accessibilityElement(children: .contain)
            .accessibilityLabel(labels.resolvedLabel)
            if isCopyable, let selected {
                divider
                footer(selected)
            }
        }
        .background(shape.fill(theme.colors[role: MonarchJsonViewerTokens.surfaceRole]))
        .overlay(
            shape.strokeBorder(
                theme.colors[role: MonarchJsonViewerTokens.borderRole],
                lineWidth: MonarchShapes.borderWidth
            )
        )
        .clipShape(shape)
        .onChange(of: selected?.id) { _, _ in
            guard let selected else { return }
            onSelect?(MonarchJsonViewerModel.path(selected.segments, root: rootPath), selected.value)
        }
        .onChange(of: needle.isEmpty ? "" : labels.matchesText(search.count, isCapped: search.isCapped)) { _, text in
            guard !text.isEmpty else { return }
            AccessibilityNotification.Announcement(text).post()
        }
    }

    private var divider: some View {
        Rectangle()
            .fill(theme.colors[role: MonarchJsonViewerTokens.borderRole])
            .frame(height: MonarchShapes.borderWidth)
            .accessibilityHidden(true)
    }

    // MARK: Toolbar

    private func toolbar(needle: String, search: MonarchJsonViewerSearch) -> some View {
        VStack(alignment: .leading, spacing: MonarchJsonViewerTokens.barSpacing) {
            if isSearchable {
                MonarchJsonViewerSearchField(
                    label: labels.resolvedSearch,
                    prompt: labels.resolvedSearchPrompt,
                    text: $query,
                    clearLabel: labels.resolvedClearSearch
                )
            }
            HStack(alignment: .center, spacing: MonarchJsonViewerTokens.barSpacing) {
                if isSearchable, !needle.isEmpty {
                    Text(labels.matchesText(search.count, isCapped: search.isCapped))
                        .monarchTextStyle(.caption)
                        .foregroundStyle(theme.colors[role: MonarchJsonViewerTokens.mutedRole])
                        .accessibilityAddTraits(.updatesFrequently)
                }
                Spacer(minLength: 0)
                MonarchButton(labels.resolvedCollapseAll, variant: .ghost, size: .sm) {
                    collapseAll()
                }
            }
        }
        .padding(MonarchJsonViewerTokens.barPadding)
    }

    // MARK: Rows

    @ViewBuilder
    private func rowView(
        _ row: MonarchJsonViewerRow,
        rows: [MonarchJsonViewerRow],
        needle: String,
        isMatch: Bool,
        isOpen: Bool,
        isSelected: Bool
    ) -> some View {
        switch row.kind {
        case .more:
            Button {
                showMore(row, rows: rows)
            } label: {
                Text(labels.showMoreText(next: min(maxChildren, row.remaining), remaining: row.remaining))
                    .monarchTextStyle(.label)
                    .foregroundStyle(theme.colors[role: MonarchJsonViewerTokens.moreRole])
                    .multilineTextAlignment(.leading)
                    .padding(.leading, indentation(row.depth) + chevronColumn + MonarchJsonViewerTokens.rowSpacing)
                    .padding(.trailing, MonarchJsonViewerTokens.rowHorizontalPadding)
                    .frame(maxWidth: .infinity, minHeight: MonarchAccessibility.minimumHitTarget, alignment: .leading)
                    .contentShape(Rectangle())
            }
            .buttonStyle(.plain)
        case .node:
            let summary = MonarchJsonViewerModel.summary(row.value, maxStringLength: maxStringLength, labels: labels)
            let rowShape = RoundedRectangle(cornerRadius: MonarchJsonViewerTokens.rowCornerRadius, style: .continuous)
            Button {
                activate(row, isOpen: isOpen)
            } label: {
                HStack(alignment: .firstTextBaseline, spacing: MonarchJsonViewerTokens.rowSpacing) {
                    Group {
                        if let symbol = MonarchJsonViewerTokens.chevron(isExpandable: row.isExpandable, isOpen: isOpen)
                        {
                            MonarchIcon(systemName: symbol, size: .sm)
                                .foregroundStyle(theme.colors[role: MonarchJsonViewerTokens.mutedRole])
                        } else {
                            Spacer(minLength: 0)
                        }
                    }
                    .frame(width: chevronColumn)
                    Text(line(row, summary: summary, needle: isMatch ? needle : ""))
                        .monarchTextStyle(.code)
                        .multilineTextAlignment(.leading)
                        .frame(maxWidth: .infinity, alignment: .leading)
                }
                .padding(.leading, indentation(row.depth))
                .padding(.trailing, MonarchJsonViewerTokens.rowHorizontalPadding)
                .padding(.vertical, MonarchJsonViewerTokens.rowVerticalPadding)
                .frame(maxWidth: .infinity, minHeight: MonarchAccessibility.minimumHitTarget, alignment: .leading)
                .background {
                    if isSelected {
                        rowShape.fill(theme.colors[role: MonarchJsonViewerTokens.selectedRole])
                    }
                }
                .contentShape(Rectangle())
            }
            .buttonStyle(.plain)
            .accessibilityLabel(MonarchJsonViewerModel.accessibilityLabel(name: row.name, summary: summary))
            .accessibilityValue(row.isExpandable ? (isOpen ? labels.resolvedExpanded : labels.resolvedCollapsed) : "")
            .accessibilityAddTraits(isSelected ? .isSelected : [])
        }
    }

    private func indentation(_ depth: Int) -> CGFloat {
        CGFloat(depth) * indent + MonarchJsonViewerTokens.rowHorizontalPadding
    }

    /// The row text: the key, a colon and the value summary, each in its
    /// colour, with matched text highlighted and bold.
    private func line(_ row: MonarchJsonViewerRow, summary: String, needle: String) -> AttributedString {
        var text = AttributedString()
        if let name = row.name {
            text += marked(
                MonarchJsonViewerModel.name(name),
                role: MonarchJsonViewerTokens.keyRole(name),
                needle: needle
            )
            text += marked(": ", role: MonarchJsonViewerTokens.mutedRole, needle: "")
        }
        let isContainer = row.isExpandable || row.type == .object || row.type == .array
        text += marked(summary, role: MonarchJsonViewerTokens.valueRole(row.type), needle: isContainer ? "" : needle)
        return text
    }

    private func marked(_ string: String, role: MonarchColorRole, needle: String) -> AttributedString {
        var text = AttributedString(string)
        text.foregroundColor = theme.colors[role: role]
        for range in MonarchJsonViewerModel.matchRanges(in: string, needle: needle) {
            guard let lower = AttributedString.Index(range.lowerBound, within: text),
                let upper = AttributedString.Index(range.upperBound, within: text)
            else { continue }
            text[lower..<upper].backgroundColor = theme.colors[role: MonarchJsonViewerTokens.matchRole]
            text[lower..<upper].inlinePresentationIntent = .stronglyEmphasized
        }
        return text
    }

    // MARK: Footer

    private func footer(_ selected: MonarchJsonViewerRow) -> some View {
        let path = MonarchJsonViewerModel.path(selected.segments, root: rootPath)
        let shownPath = path.isEmpty ? MonarchJsonViewerModel.idRoot : path
        return ViewThatFits(in: .horizontal) {
            HStack(alignment: .center, spacing: MonarchJsonViewerTokens.barSpacing) {
                pathText(shownPath)
                Spacer(minLength: 0)
                copyButtons(path: shownPath, value: selected.value)
            }
            VStack(alignment: .leading, spacing: MonarchJsonViewerTokens.barSpacing) {
                pathText(shownPath)
                HStack(spacing: MonarchJsonViewerTokens.barSpacing) {
                    copyButtons(path: shownPath, value: selected.value)
                }
            }
        }
        .padding(MonarchJsonViewerTokens.barPadding)
    }

    private func pathText(_ path: String) -> some View {
        Text(path)
            .monarchTextStyle(.code)
            .foregroundStyle(theme.colors[role: MonarchJsonViewerTokens.mutedRole])
            .lineLimit(2)
            .truncationMode(.middle)
            .textSelection(.enabled)
            .accessibilityLabel(labels.resolvedSelectedPath)
            .accessibilityValue(path)
    }

    @ViewBuilder
    private func copyButtons(path: String, value: MonarchJsonValue) -> some View {
        MonarchCopyButton(labels.resolvedCopyPath, value: path, variant: .ghost, size: .sm, onCopy: onCopy)
            .fixedSize()
        MonarchCopyButton(
            labels.resolvedCopyValue,
            value: MonarchJsonViewerModel.copyText(value),
            variant: .ghost,
            size: .sm,
            onCopy: onCopy
        )
        .fixedSize()
    }

    // MARK: Actions

    private func activate(_ row: MonarchJsonViewerRow, isOpen: Bool) {
        selectedID = row.id
        guard row.isExpandable else { return }
        overrides[row.id] = !isOpen
    }

    private func showMore(_ row: MonarchJsonViewerRow, rows: [MonarchJsonViewerRow]) {
        guard let parentID = row.parentID else { return }
        limits[parentID] = row.shown + maxChildren
        if let container = rows.first(where: { $0.id == parentID }),
            let first = MonarchJsonViewerModel.firstRevealedID(after: row, in: container.value)
        {
            selectedID = first
        }
    }

    private func collapseAll() {
        baseDepth = min(1, expandDepth)
        overrides = [:]
        limits = [:]
        query = ""
        selectedID = nil
    }
}

#if DEBUG
    #Preview("MonarchJsonViewer") {
        let payload: MonarchJsonValue = [
            "id": "evt_1",
            "type": "invoice.paid",
            "amount": 4_200,
            "paid": true,
            "refund": nil,
            "lines": [["sku": "A1", "qty": 2], ["sku": "B7", "qty": 1]],
            "customer name": "Ada Lovelace",
        ]
        MonarchPreviewMatrix {
            VStack(alignment: .leading) {
                MonarchJsonViewer(payload, expandDepth: 2)
                MonarchJsonViewer(["a", "b", "c", "d"], maxChildren: 2, isSearchable: false, isCopyable: false)
            }
        }
    }
#endif
