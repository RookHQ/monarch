import SwiftUI

/// Lists an item's dependencies (blocked by, blocks, related to, duplicates)
/// and adds new ones: choose the relation, then search for the item.
///
/// ```swift
/// MonarchDependencyPicker(
///     links: links,
///     items: issues,
///     excludedIDs: [currentIssue.id],
///     onAdd: { relation, item in links.append(MonarchDependencyLink(id: UUID().uuidString, relation: relation, item: item)) },
///     onRemove: { link in links.removeAll { $0.id == link.id } }
/// )
/// ```
///
/// Links are grouped under their relation. Adding uses a menu `Picker` for
/// the relation and a ``MonarchCombobox``, whose searchable sheet lists the
/// items that are not excluded or already linked. For a server search, bind
/// `searchText`, update `items` and set `isSearching`.
///
/// Accessibility: each relation heading has the header trait. Each row reads
/// the item's key, title and status ("STR-12 Fix login, Status: In
/// progress"); an item with a web address is a link. Remove buttons name the
/// relation and the item. Adding and removing are announced. Every relation
/// is a word, never only a colour or an icon. Touch targets are at least 44 points.
public struct MonarchDependencyPicker: View {
    private let links: [MonarchDependencyLink]
    private let items: [MonarchDependencyItem]
    private let excludedIDs: Set<String>
    private let relations: [MonarchDependencyRelation]
    private let isReadOnly: Bool
    private let isSearching: Bool
    private let searchText: Binding<String>?
    private let labels: MonarchDependencyPickerLabels
    private let onAdd: (@MainActor (MonarchDependencyRelation, MonarchDependencyItem) -> Void)?
    private let onRemove: (@MainActor (MonarchDependencyLink) -> Void)?

    @Environment(\.monarchTheme) private var theme
    @Environment(\.isEnabled) private var isEnabled
    @State private var relation: MonarchDependencyRelation

    /// A dependency picker.
    ///
    /// - Parameters:
    ///   - links: The current links.
    ///   - items: The items that can be found by the search.
    ///   - excludedIDs: Items that cannot be linked, such as the item itself. Linked items are left out too.
    ///   - relations: The relations offered, in order.
    ///   - defaultRelation: The relation chosen at first. The first of `relations` by default.
    ///   - isReadOnly: Hides the add controls and the remove buttons.
    ///   - isSearching: Shows that a server search is running.
    ///   - searchText: The search text, for apps that search a server.
    ///   - labels: Replacements for the built-in strings.
    ///   - onAdd: Adds a link. The app saves it and updates `links`. Without it nothing can be added.
    ///   - onRemove: Removes a link. Without it rows have no remove button.
    public init(
        links: [MonarchDependencyLink],
        items: [MonarchDependencyItem] = [],
        excludedIDs: Set<String> = [],
        relations: [MonarchDependencyRelation] = MonarchDependencyRelation.allCases,
        defaultRelation: MonarchDependencyRelation? = nil,
        isReadOnly: Bool = false,
        isSearching: Bool = false,
        searchText: Binding<String>? = nil,
        labels: MonarchDependencyPickerLabels = MonarchDependencyPickerLabels(),
        onAdd: (@MainActor (MonarchDependencyRelation, MonarchDependencyItem) -> Void)? = nil,
        onRemove: (@MainActor (MonarchDependencyLink) -> Void)? = nil
    ) {
        self.links = links
        self.items = items
        self.excludedIDs = excludedIDs
        self.relations = relations
        self.isReadOnly = isReadOnly
        self.isSearching = isSearching
        self.searchText = searchText
        self.labels = labels
        self.onAdd = onAdd
        self.onRemove = onRemove
        _relation = State(
            initialValue: MonarchDependencyPickerTokens.startRelation(relations, preferred: defaultRelation)
        )
    }

    private var text: MonarchDependencyPickerText { MonarchDependencyPickerText(labels: labels) }

    /// The content of the view.
    public var body: some View {
        VStack(alignment: .leading, spacing: MonarchDependencyPickerTokens.sectionSpacing) {
            let groups = MonarchDependencyPickerTokens.groups(links, relations: relations)
            if groups.isEmpty {
                Text(text.empty)
                    .monarchTextStyle(.body)
                    .foregroundStyle(isEnabled ? theme.colors.textMuted : theme.colors.textDisabled)
            }
            ForEach(groups, id: \.relation) { group in
                VStack(alignment: .leading, spacing: MonarchDependencyPickerTokens.rowSpacing) {
                    Text(text.relation(group.relation))
                        .monarchTextStyle(.subheading)
                        .foregroundStyle(isEnabled ? theme.colors.textDefault : theme.colors.textDisabled)
                        .accessibilityAddTraits(.isHeader)
                    ForEach(group.links) { row($0) }
                }
            }
            if !isReadOnly, onAdd != nil { addControls }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
    }

    private func row(_ link: MonarchDependencyLink) -> some View {
        HStack(alignment: .center, spacing: MonarchDependencyPickerTokens.partSpacing) {
            itemContent(link.item)
                .frame(maxWidth: .infinity, alignment: .leading)
            if let onRemove, !isReadOnly {
                let relationName = text.relation(link.relation)
                let itemName = MonarchDependencyPickerTokens.name(link.item)
                Button {
                    onRemove(link)
                    AccessibilityNotification.Announcement(text.removed(relationName, itemName)).post()
                } label: {
                    Label(text.remove(relationName, itemName), systemImage: MonarchDependencyPickerTokens.removeSymbol)
                        .labelStyle(.iconOnly)
                }
                .buttonStyle(.monarch(.ghost, size: .sm))
            }
        }
    }

    @ViewBuilder private func itemContent(_ item: MonarchDependencyItem) -> some View {
        let url = MonarchDependencyPickerTokens.safeURL(item)
        let content = VStack(alignment: .leading, spacing: MonarchDependencyPickerTokens.lineSpacing) {
            HStack(alignment: .firstTextBaseline, spacing: MonarchDependencyPickerTokens.partSpacing) {
                // As the web: the status is its icon in the tone's colour, in front of the key; the name is spoken.
                if let status = item.status, let symbol = status.systemImage {
                    MonarchIcon(systemName: symbol, size: .sm)
                        .foregroundStyle(
                            theme.colors[
                                role: status.tone == .neutral ? \.textMuted : MonarchToneChrome.titleColor(status.tone)
                            ]
                        )
                        .accessibilityHidden(true)
                }
                if let key = item.key {
                    Text(key)
                        .monarchTextStyle(.code)
                        .foregroundStyle(
                            theme.colors[role: MonarchDependencyPickerTokens.keyColor(isEnabled: isEnabled)]
                        )
                }
                Text(item.title)
                    .monarchTextStyle(.body)
                    .foregroundStyle(
                        theme.colors[
                            role: MonarchDependencyPickerTokens.titleColor(isLink: url != nil, isEnabled: isEnabled)
                        ]
                    )
                    .multilineTextAlignment(.leading)
            }
            if let status = item.status, status.systemImage == nil {
                MonarchStatusBadge(status, size: .sm)
            }
        }
        .monarchMinimumHitTarget()
        let label = MonarchDependencyPickerTokens.rowLabel(item, text: text)
        if let url {
            Link(destination: url) { content }
                .accessibilityLabel(label)
        } else {
            content
                .accessibilityElement(children: .ignore)
                .accessibilityLabel(label)
        }
    }

    private var addControls: some View {
        VStack(alignment: .leading, spacing: MonarchDependencyPickerTokens.rowSpacing) {
            MonarchSelectField(
                text.relationLabel,
                selection: $relation,
                valueText: text.relation(relation)
            ) {
                ForEach(relations, id: \.self) { relation in
                    Text(text.relation(relation)).tag(relation)
                }
            }
            MonarchCombobox(
                text.searchLabel,
                selection: Binding(get: { nil }, set: { add($0) }),
                options: MonarchDependencyPickerTokens.candidates(items, links: links, excluding: excludedIDs)
                    .map(MonarchDependencyPickerTokens.option),
                prompt: text.searchPrompt,
                isLoading: isSearching,
                isClearable: false,
                searchText: searchText,
                labels: MonarchComboboxLabels(empty: text.noResults)
            )
        }
    }

    private func add(_ id: String?) {
        guard let id, let onAdd, let item = items.first(where: { $0.id == id }) else { return }
        onAdd(relation, item)
        AccessibilityNotification.Announcement(
            text.added(text.relation(relation), MonarchDependencyPickerTokens.name(item))
        )
        .post()
    }
}

#if DEBUG
    #Preview("MonarchDependencyPicker") {
        let items = [
            MonarchDependencyItem(
                id: "1",
                key: "STR-12",
                title: "Fix login",
                status: MonarchStatusDefinition("In progress", tone: .info, systemImage: "circle.lefthalf.filled")
            ),
            MonarchDependencyItem(id: "2", key: "STR-14", title: "Update the pricing page"),
        ]
        MonarchPreviewMatrix {
            VStack(alignment: .leading) {
                MonarchDependencyPicker(
                    links: [MonarchDependencyLink(id: "l1", relation: .blockedBy, item: items[0])],
                    items: items,
                    onAdd: { _, _ in },
                    onRemove: { _ in }
                )
                MonarchDependencyPicker(links: [], isReadOnly: true)
            }
        }
    }
#endif
