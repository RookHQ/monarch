import MonarchTokens
import SwiftUI

/// The `/` menu of a composer or a document editor: a grouped list of
/// things to insert, with icons, labels and descriptions, shown in a strip
/// above the keyboard while someone types a slash command.
///
/// ```swift
/// .safeAreaInset(edge: .bottom) {
///     VStack(spacing: 0) {
///         MonarchSlashMenu(query, items: commands, groupOrder: ["Text", "Lists"]) { item in
///             if let query { text = query.replacing(in: text, with: markdown[item.id] ?? "") }
///         }
///         composer
///     }
/// }
/// ```
///
/// iOS gives no caret position to place a list beside it, so the menu is a
/// full-width strip to put directly above the text field, in the bottom
/// safe-area inset that holds the composer, where it stays above the
/// keyboard. Find the query with
/// ``MonarchSlashMenuQuery/find(in:caret:trigger:position:maxLength:)`` as
/// the text changes and pass every item: the menu filters them by what was
/// typed (labels that start with it first, then labels that contain it,
/// then ids, descriptions and keywords) and groups them under their
/// headings, in `groupOrder` first. It draws nothing while `query` is `nil`.
/// More than four rows scroll.
///
/// Accessibility: each item is a button named by its label and
/// description, dimmed and not activatable when disabled. The list is a
/// container named "Insert block" with headings for its groups. The number
/// of options, and "Nothing matches" when there are none, are announced as
/// they change, so focus can stay in the text field. Rows are at least 44
/// points high and grow with Dynamic Type. Icons are decorative.
public struct MonarchSlashMenu: View {
    private let query: MonarchSlashMenuQuery?
    private let items: [MonarchSlashMenuItem]
    private let groupOrder: [String]
    private let labels: MonarchSlashMenuLabels
    @Environment(\.monarchAiTone) private var aiTone
    private let onSelect: @MainActor (MonarchSlashMenuItem) -> Void

    @Environment(\.monarchTheme) private var theme
    @ScaledMetric(relativeTo: .body) private var rowHeight = MonarchSlashMenuTokens.rowHeight
    private let visibleRows: CGFloat

    /// A slash menu.
    ///
    /// - Parameters:
    ///   - query: The command being typed. Nothing is drawn while it is `nil`.
    ///   - items: Every item the menu offers. The menu filters them by the query.
    ///   - groupOrder: Group headings in display order. Groups not listed follow in the order they first appear.
    ///   - visibleRows: How many rows show before the list scrolls. About five and a half by default, so the cut
    ///     last row shows there is more.
    ///   - labels: Replacements for the built-in strings.
    ///   - onSelect: Receives the chosen item. Replace the query with
    ///     ``MonarchSlashMenuQuery/replacing(in:with:)`` and set the query to `nil`.
    public init(
        _ query: MonarchSlashMenuQuery?,
        items: [MonarchSlashMenuItem],
        groupOrder: [String] = [],
        visibleRows: CGFloat = 4.5,
        labels: MonarchSlashMenuLabels = MonarchSlashMenuLabels(),
        onSelect: @escaping @MainActor (MonarchSlashMenuItem) -> Void
    ) {
        self.visibleRows = visibleRows
        self.query = query
        self.items = items
        self.groupOrder = groupOrder
        self.labels = labels
        self.onSelect = onSelect
    }

    /// Filters items the way the menu does: labels that start with the query
    /// first, then labels that contain it, then matches in the id, the
    /// description or the keywords. Ignores case and diacritics; an empty
    /// query keeps every item.
    ///
    /// - Parameters:
    ///   - items: The items to filter.
    ///   - query: What was typed after the trigger.
    /// - Returns: The matching items, best first.
    public static func filter(_ items: [MonarchSlashMenuItem], query: String) -> [MonarchSlashMenuItem] {
        MonarchSlashMenuTokens.filter(items, query: query)
    }

    private typealias Tokens = MonarchSlashMenuTokens

    /// The content of the view.
    public var body: some View {
        if let query {
            let shown = Tokens.filter(items, query: query.text)
            content(query: query, shown: shown)
                .frame(maxWidth: .infinity, alignment: .leading)
                .clipShape(panel)
                .monarchElevation(.md, fill: Tokens.background, in: panel)
                .overlay {
                    panel
                        .strokeBorder(theme.colors[role: Tokens.border], lineWidth: MonarchShapes.borderWidth)
                        .allowsHitTesting(false)
                        .accessibilityHidden(true)
                }
                .onChange(of: shown.map(\.id), initial: true) { _, ids in
                    AccessibilityNotification.Announcement(
                        Tokens.announcement(count: ids.count, query: query, labels: labels)
                    )
                    .post()
                }
        }
    }

    private var panel: RoundedRectangle {
        RoundedRectangle(cornerRadius: MonarchTokens.Radius.lg, style: .continuous)
    }

    @ViewBuilder private func content(query: MonarchSlashMenuQuery, shown: [MonarchSlashMenuItem]) -> some View {
        if shown.isEmpty {
            Text(Tokens.emptyText(query, labels: labels))
                .monarchTextStyle(.body)
                .foregroundStyle(theme.colors.textMuted)
                .padding(.horizontal, Tokens.horizontalPadding)
                .frame(minHeight: MonarchAccessibility.minimumHitTarget)
        } else if Tokens.scrolls(itemCount: shown.count) {
            ScrollView { list(shown) }.frame(maxHeight: rowHeight * visibleRows)
        } else {
            list(shown)
        }
    }

    private func list(_ shown: [MonarchSlashMenuItem]) -> some View {
        let groups = Tokens.groups(shown, order: groupOrder)
        return VStack(alignment: .leading, spacing: 0) {
            ForEach(Array(groups.enumerated()), id: \.offset) { index, group in
                if index > 0 {
                    Rectangle()
                        .fill(theme.colors[role: Tokens.border])
                        .frame(height: MonarchShapes.borderWidth)
                        .padding(.top, Tokens.verticalPadding)
                        .accessibilityHidden(true)
                }
                if let heading = group.heading {
                    Text(heading)
                        .monarchTextStyle(.caption, weight: .semibold)
                        .foregroundStyle(theme.colors[role: Tokens.heading])
                        .padding(.horizontal, Tokens.horizontalPadding)
                        .padding(.top, Tokens.verticalPadding)
                        .accessibilityAddTraits(.isHeader)
                }
                ForEach(group.items) { item in
                    Button {
                        onSelect(item)
                    } label: {
                        row(item, isActive: item.id == shown.first(where: { !$0.isDisabled })?.id)
                    }
                    .buttonStyle(.plain)
                    .disabled(item.isDisabled)
                    .accessibilityLabel(Tokens.accessibilityLabel(item))
                }
            }
        }
        .accessibilityElement(children: .contain)
        .accessibilityLabel(labels.label ?? MonarchSlashMenuStrings.label)
    }

    private func row(_ item: MonarchSlashMenuItem, isActive: Bool = false) -> some View {
        let colors = Tokens.colors(isDisabled: item.isDisabled)
        return HStack(spacing: Tokens.rowSpacing) {
            if let systemImage = item.systemImage {
                MonarchIcon(systemName: systemImage)
                    .foregroundStyle(iconColor(isActive: isActive, isDisabled: item.isDisabled, role: colors.icon))
                    .frame(width: Tokens.iconTile, height: Tokens.iconTile)
                    .background(theme.colors[role: Tokens.background], in: tile)
                    .overlay(tile.strokeBorder(theme.colors[role: Tokens.border], lineWidth: MonarchShapes.borderWidth))
            }
            VStack(alignment: .leading) {
                Text(item.label)
                    .monarchTextStyle(.label)
                    .foregroundStyle(theme.colors[role: colors.label])
                if let description = item.description, !description.isEmpty {
                    Text(description)
                        .monarchTextStyle(.caption)
                        .foregroundStyle(theme.colors[role: colors.description])
                }
            }
            Spacer(minLength: 0)
        }
        .padding(.horizontal, Tokens.horizontalPadding - MonarchTokens.Space.space1)
        .padding(.vertical, Tokens.verticalPadding)
        .frame(minHeight: MonarchAccessibility.minimumHitTarget)
        .background(
            isActive ? (aiTone ? theme.colors.aiSubtle : theme.colors.accentSubtle) : Color.clear,
            in: tile
        )
        .padding(.horizontal, MonarchTokens.Space.space1)
        .contentShape(Rectangle())
    }

    private var tile: RoundedRectangle {
        RoundedRectangle(cornerRadius: MonarchTokens.Radius.md, style: .continuous)
    }

    private func iconColor(isActive: Bool, isDisabled: Bool, role: MonarchColorRole) -> Color {
        guard !isDisabled else { return theme.colors[role: role] }
        if aiTone { return theme.colors.aiStrong }
        return isActive ? theme.colors.accentStrong : theme.colors[role: role]
    }
}

#if DEBUG
    #Preview("MonarchSlashMenu") {
        let items = [
            MonarchSlashMenuItem(
                id: "heading",
                label: "Heading",
                description: "Large section title",
                systemImage: "textformat.size",
                group: "Text",
                keywords: ["h2"]
            ),
            MonarchSlashMenuItem(
                id: "bullets",
                label: "Bulleted list",
                description: "A simple list",
                systemImage: "list.bullet",
                group: "Lists"
            ),
            MonarchSlashMenuItem(
                id: "table",
                label: "Table",
                description: "Not available in replies",
                systemImage: "tablecells",
                group: "Insert",
                isDisabled: true
            ),
        ]
        MonarchPreviewMatrix {
            VStack {
                MonarchSlashMenu(MonarchSlashMenuQuery.find(in: "/"), items: items, groupOrder: ["Text"]) { _ in }
                MonarchSlashMenu(MonarchSlashMenuQuery.find(in: "/zzz"), items: items) { _ in }
            }
        }
    }
#endif
