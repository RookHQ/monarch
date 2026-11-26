import SwiftUI

/// Typed links between documents ("Parent of", "Supersedes", "Related
/// to"), read the right way round from either side, with a sheet to add one
/// by searching and a remove button on each.
///
/// ```swift
/// MonarchDocumentRelations(
///     relations,
///     candidates: searchResults,
///     onSearch: { query in search(query) },
///     onOpen: { target in path.append(target.id) },
///     onAdd: { targetID, type in try await api.link(targetID, as: type) },
///     onRemove: { relationID in try await api.unlink(relationID) }
/// )
/// ```
///
/// Each relation is a row: the target's file type icon, the relation
/// ("Parent of", with an arrow), the target's title and description, and a
/// remove button. An incoming relation reads as the inverse of its type, so
/// the same data reads correctly from both documents. With `onOpen` the row
/// opens the target, usually by pushing it in the navigation stack. A long
/// press shows Open and Remove in a context menu.
///
/// "Add relation" opens a sheet with a picker of relation types and a
/// searchable list of `candidates`, without the documents already related.
/// The list is filtered by title, or, with `onSearch`, the app searches and
/// updates `candidates`. Choosing a document calls `onAdd` with its id and
/// the type; the sheet closes when it returns and shows the error when it
/// throws. `onRemove` works the same way, with the error under the list.
///
/// Accessibility: the list is a container named "Relations". Each row is
/// read as one element, "Parent of, Q3 plan, Finance", and is a button when
/// it opens the target. The remove button is a separate element named
/// "Remove relation to Q3 plan" with a 44-point target. Adding and removing
/// are announced. The relation's arrow is decorative: its label says it.
@_spi(Experimental) public struct MonarchDocumentRelations: View {
    private let relations: [MonarchDocumentRelation]
    private let types: [MonarchDocumentRelationType]
    private let candidates: [MonarchDocumentRelationTarget]
    private let defaultType: String?
    private let showsEmpty: Bool
    private let labels: MonarchDocumentRelationsLabels
    private let onSearch: (@MainActor (String) -> Void)?
    private let onOpen: (@MainActor (MonarchDocumentRelationTarget) -> Void)?
    private let onAdd: (@MainActor (String, String) async throws -> Void)?
    private let onRemove: (@MainActor (String) async throws -> Void)?

    @Environment(\.monarchTheme) private var theme
    @State private var isAdding = false
    @State private var removing: String?
    @State private var error: String?
    @AccessibilityFocusState private var focused: String?

    /// Document relations.
    ///
    /// - Parameters:
    ///   - relations: This document's relations.
    ///   - types: The relation types and their inverses. By default related, parent and child,
    ///     supersedes and implements, as on the web.
    ///   - candidates: Documents that can be linked, shown in the add sheet.
    ///   - defaultType: The type chosen when the sheet opens. `related-to`, or the first type, by default.
    ///   - showsEmpty: Shows "No relations yet." when there are none.
    ///   - labels: Replacements for the built-in strings.
    ///   - onSearch: Searches on the server as the person types; update `candidates` with the results.
    ///   - onOpen: Opens a related document. Without it, rows are not interactive.
    ///   - onAdd: Adds a relation: the target's id and the type's value. Shows "Add relation".
    ///     Throw to keep the sheet open with an error.
    ///   - onRemove: Removes a relation by its id. Shows a remove button on each row.
    public init(
        _ relations: [MonarchDocumentRelation],
        types: [MonarchDocumentRelationType] = MonarchDocumentRelationType.defaults,
        candidates: [MonarchDocumentRelationTarget] = [],
        defaultType: String? = nil,
        showsEmpty: Bool = false,
        labels: MonarchDocumentRelationsLabels = MonarchDocumentRelationsLabels(),
        onSearch: (@MainActor (String) -> Void)? = nil,
        onOpen: (@MainActor (MonarchDocumentRelationTarget) -> Void)? = nil,
        onAdd: (@MainActor (String, String) async throws -> Void)? = nil,
        onRemove: (@MainActor (String) async throws -> Void)? = nil
    ) {
        self.relations = relations
        self.types = types
        self.candidates = candidates
        self.defaultType = defaultType
        self.showsEmpty = showsEmpty
        self.labels = labels
        self.onSearch = onSearch
        self.onOpen = onOpen
        self.onAdd = onAdd
        self.onRemove = onRemove
    }

    private typealias Tokens = MonarchDocumentRelationsTokens
    private typealias Strings = MonarchDocumentRelationsStrings

    /// The content of the view.
    public var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            if relations.isEmpty, showsEmpty {
                Text(labels.none ?? Strings.none)
                    .monarchTextStyle(.body)
                    .foregroundStyle(theme.colors[role: Tokens.mutedColor])
                    .padding(.vertical, Tokens.rowPadding)
            }
            // Compact pills that wrap, as on the web: the relation, the linked title and a remove mark.
            MonarchFlowLayout(spacing: Tokens.typeSpacing) {
                ForEach(relations, id: \.id) { relation in
                    chip(relation)
                }
            }
            if let error {
                Label {
                    Text(error)
                } icon: {
                    MonarchIcon(systemName: "exclamationmark.circle", size: .sm)
                }
                .monarchTextStyle(.caption)
                .foregroundStyle(theme.colors[role: Tokens.errorColor])
                .padding(.vertical, Tokens.rowPadding)
            }
            if onAdd != nil {
                Button {
                    error = nil
                    isAdding = true
                } label: {
                    Text(labels.add ?? Strings.add)
                        .monarchTextStyle(.label)
                        .foregroundStyle(theme.colors.textDefault)
                        .monarchMinimumHitTarget()
                }
                .buttonStyle(.plain)
                .accessibilityFocused($focused, equals: Tokens.addFocusID)
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .accessibilityElement(children: .contain)
        .accessibilityLabel(labels.label ?? Strings.label)
        .sheet(isPresented: $isAdding) {
            MonarchDocumentRelationsAddSheet(
                types: types,
                candidates: candidates,
                relations: relations,
                labels: labels,
                onSearch: onSearch,
                onAdd: { target, type in
                    try await onAdd?(target.id, type)
                    AccessibilityNotification.Announcement(Tokens.addedText(target.title, labels: labels)).post()
                },
                type: Tokens.initialType(defaultType, types: types)
            )
        }
    }

    private func chip(_ relation: MonarchDocumentRelation) -> some View {
        let type = Tokens.type(of: relation, types: types)
        let spoken = Tokens.accessibilityLabel(typeLabel: type.label, target: relation.target)
        let shape = Capsule()
        // The direction as an arrow, as on the web: a parent points up, a child down, other types out or back.
        let arrow: String =
            switch (type.value, relation.direction) {
            case ("parent-of", .outgoing): "arrow.up"
            case ("parent-of", .incoming): "arrow.down"
            case (_, .outgoing): "arrow.right"
            default: "arrow.left"
            }
        let label = HStack(spacing: Tokens.typeSpacing) {
            MonarchIcon(systemName: arrow, size: .sm)
                .foregroundStyle(theme.colors[role: Tokens.mutedColor])
                .accessibilityHidden(true)
            Text(type.label)
                .monarchTextStyle(.caption)
                .foregroundStyle(theme.colors[role: Tokens.mutedColor])
            Text(relation.target.title)
                .monarchTextStyle(.label)
                .underline()
                .foregroundStyle(onOpen == nil ? theme.colors[role: Tokens.titleColor] : theme.colors.textLink)
        }
        return HStack(spacing: 0) {
            if let onOpen {
                Button {
                    onOpen(relation.target)
                } label: {
                    label.monarchMinimumHitTarget().contentShape(Rectangle())
                }
                .buttonStyle(.plain)
                .accessibilityLabel(spoken)
            } else {
                label
                    .frame(minHeight: MonarchAccessibility.minimumHitTarget)
                    .accessibilityElement(children: .ignore)
                    .accessibilityLabel(spoken)
            }
            if onRemove != nil {
                if removing == relation.id {
                    ProgressView().frame(minWidth: MonarchAccessibility.minimumHitTarget)
                } else {
                    Button {
                        remove(relation)
                    } label: {
                        MonarchIcon(
                            systemName: Tokens.removeSymbol,
                            label: Tokens.removeLabel(relation.target.title, labels: labels),
                            size: .sm
                        )
                        .monarchMinimumHitTarget()
                    }
                    .buttonStyle(.plain)
                    .foregroundStyle(theme.colors[role: Tokens.mutedColor])
                    .disabled(removing != nil)
                    .accessibilityFocused($focused, equals: relation.id)
                }
            }
        }
        .padding(.leading, Tokens.rowPadding * 1.5)
        .overlay(shape.strokeBorder(theme.colors[role: Tokens.separatorColor], lineWidth: MonarchShapes.borderWidth))
        .contextMenu {
            if let onOpen {
                Button {
                    onOpen(relation.target)
                } label: {
                    Label(relation.target.title, systemImage: Tokens.symbol(type.value))
                }
            }
            if onRemove != nil {
                Button(role: .destructive) {
                    remove(relation)
                } label: {
                    Label(Tokens.removeLabel(relation.target.title, labels: labels), systemImage: Tokens.removeSymbol)
                }
            }
        }
    }

    private func row(_ relation: MonarchDocumentRelation) -> some View {
        let type = Tokens.type(of: relation, types: types)
        let spoken = Tokens.accessibilityLabel(typeLabel: type.label, target: relation.target)
        return HStack(spacing: Tokens.rowSpacing) {
            if let onOpen {
                Button {
                    onOpen(relation.target)
                } label: {
                    info(relation, type: type)
                }
                .buttonStyle(.plain)
                .accessibilityLabel(spoken)
            } else {
                info(relation, type: type)
                    .accessibilityElement(children: .ignore)
                    .accessibilityLabel(spoken)
            }
            if onRemove != nil {
                if removing == relation.id {
                    ProgressView()
                        .frame(minWidth: MonarchAccessibility.minimumHitTarget)
                } else {
                    Button {
                        remove(relation)
                    } label: {
                        MonarchIcon(
                            systemName: Tokens.removeSymbol,
                            label: Tokens.removeLabel(relation.target.title, labels: labels),
                            size: .sm
                        )
                        .monarchMinimumHitTarget()
                    }
                    .buttonStyle(.plain)
                    .foregroundStyle(theme.colors[role: Tokens.mutedColor])
                    .disabled(removing != nil)
                    .accessibilityFocused($focused, equals: relation.id)
                }
            }
        }
        .padding(.vertical, Tokens.rowPadding)
        .contextMenu {
            if let onOpen {
                Button {
                    onOpen(relation.target)
                } label: {
                    Label(relation.target.title, systemImage: Tokens.symbol(type.value))
                }
            }
            if onRemove != nil {
                Button(role: .destructive) {
                    remove(relation)
                } label: {
                    Label(Tokens.removeLabel(relation.target.title, labels: labels), systemImage: Tokens.removeSymbol)
                }
            }
        }
    }

    private func info(_ relation: MonarchDocumentRelation, type: (value: String, label: String)) -> some View {
        HStack(alignment: .firstTextBaseline, spacing: Tokens.rowSpacing) {
            MonarchDocumentRelationsTargetIcon(target: relation.target)
            VStack(alignment: .leading, spacing: Tokens.lineSpacing) {
                HStack(spacing: Tokens.typeSpacing) {
                    Image(systemName: Tokens.symbol(type.value))
                    Text(type.label)
                }
                .monarchTextStyle(.caption)
                .foregroundStyle(theme.colors[role: Tokens.mutedColor])
                Text(relation.target.title)
                    .monarchTextStyle(.bodyStrong)
                    .foregroundStyle(theme.colors[role: Tokens.titleColor])
                if let description = relation.target.description, !description.isEmpty {
                    Text(description)
                        .monarchTextStyle(.caption)
                        .foregroundStyle(theme.colors[role: Tokens.mutedColor])
                }
            }
            Spacer(minLength: 0)
        }
        .frame(minHeight: MonarchAccessibility.minimumHitTarget)
        .contentShape(Rectangle())
    }

    private func remove(_ relation: MonarchDocumentRelation) {
        guard let onRemove, removing == nil else { return }
        removing = relation.id
        error = nil
        Task {
            do {
                try await onRemove(relation.id)
                AccessibilityNotification.Announcement(Tokens.removedText(relation.target.title, labels: labels)).post()
                // As on the web: focus moves to the next relation's remove button, else Add.
                focused = Tokens.focusAfterRemoval(of: relation.id, in: relations.map(\.id), canAdd: onAdd != nil)
            } catch {
                self.error = Tokens.failureMessage(error, labels: labels)
            }
            removing = nil
        }
    }
}

#if DEBUG
    #Preview("MonarchDocumentRelations") {
        MonarchPreviewMatrix {
            MonarchDocumentRelations(
                [
                    MonarchDocumentRelation(
                        id: "r1",
                        type: "parent-of",
                        target: MonarchDocumentRelationTarget(
                            id: "d1",
                            title: "Q3 plan",
                            description: "Finance",
                            fileName: "plan.pdf"
                        )
                    ),
                    MonarchDocumentRelation(
                        id: "r2",
                        type: "parent-of",
                        direction: .incoming,
                        target: MonarchDocumentRelationTarget(id: "d2", title: "Roadmap")
                    ),
                ],
                onOpen: { _ in },
                onAdd: { _, _ in },
                onRemove: { _ in }
            )
        }
    }
#endif
