import Foundation
import SwiftUI

/// The docs and links attached to a record (a ticket, a task, a campaign):
/// one row per doc or web link that opens it, with unlink, a space filter,
/// a doc picker with search and New doc, and a form to add a link.
///
/// ```swift
/// MonarchLinkedDocsPanel(
///     ticket.links,
///     docOptions: docs,
///     onLinkDoc: { docID in try await api.link(doc: docID, to: ticket) },
///     onAddLink: { url, title in try await api.link(url: url, title: title, to: ticket) },
///     onUnlink: { item in try await api.unlink(item.id) },
///     onOpen: { item in open(item) }
/// )
/// ```
///
/// The panel is a ``MonarchSectionCard``. `nil` items show skeleton rows.
/// The header's Add menu offers Link doc (a sheet with a searchable list of
/// `docOptions`, leaving out docs already linked, and New doc) and Add link
/// (a sheet with a web address and an optional title, checked before it is
/// sent). When the rows span more than one space a menu filters them, and a
/// line says how many the filter hides. The app updates `items` after each
/// handler returns; a thrown error shows a notice in the panel or the form.
///
/// Accessibility: the title is a header. Each row is a button named by its
/// title, with its secondary line and status as the value. Unlink is a
/// separate button named "Unlink <title>", busy while it runs. Failures are
/// shown in words and announced. The pickers are system sheets with a
/// search field, a form and Cancel.
@_spi(Experimental) public struct MonarchLinkedDocsPanel: View {
    private let items: [MonarchLinkedDocsPanelItem]?
    private let isReadOnly: Bool
    private let notice: String?
    private let errorMessage: String?
    private let isRetrying: Bool
    private let docOptions: [MonarchLinkedDocsPanelOption]
    private let isSpaceFilterShown: Bool
    private let labels: MonarchLinkedDocsPanelLabels
    private let onRetry: (@MainActor () -> Void)?
    private let onLinkDoc: (@MainActor (String) async throws -> Void)?
    private let onDocQueryChange: (@MainActor (String) -> Void)?
    private let onCreateDoc: (@MainActor () -> Void)?
    private let onAddLink: (@MainActor (URL, String) async throws -> Void)?
    private let onUnlink: (@MainActor (MonarchLinkedDocsPanelItem) async throws -> Void)?
    private let onOpen: @MainActor (MonarchLinkedDocsPanelItem) -> Void

    @Environment(\.monarchTheme) private var theme
    @Environment(\.locale) private var locale
    @State private var filter = ""
    @State private var unlinkingID: String?
    @State private var failure: String?
    @State private var isPickingDoc = false
    @State private var isAddingLink = false

    private typealias Tokens = MonarchLinkedDocsPanelTokens
    private typealias Strings = MonarchLinkedDocsPanelStrings

    /// A linked docs panel.
    ///
    /// - Parameters:
    ///   - items: What is linked to the record, or `nil` while loading.
    ///   - isReadOnly: Hides linking, adding and unlinking.
    ///   - notice: Replaces the list, such as "You don’t have access to docs in this workspace."
    ///   - errorMessage: Something went wrong loading or changing links. Shown above the list.
    ///   - isRetrying: Whether a retry is in progress.
    ///   - docOptions: The docs the picker offers. Docs already linked are left out.
    ///   - isSpaceFilterShown: Shows the space filter when the rows span more than one space.
    ///   - labels: Replacements for the built-in strings.
    ///   - onRetry: Shows "Try again" with `errorMessage`.
    ///   - onLinkDoc: Links a doc, given its id. Shows Link doc in the Add menu. Throw to show the failure.
    ///   - onDocQueryChange: Called as the picker's search text changes, to search on the server
    ///     and update `docOptions`.
    ///   - onCreateDoc: Shows New doc in the picker.
    ///   - onAddLink: Adds a link, given its address and title. Shows Add link in the Add menu. Throw to show
    ///     the failure.
    ///   - onUnlink: Unlinks a row. Shows an unlink button on rows that allow it. Throw to show the failure.
    ///   - onOpen: Opens a row.
    public init(
        _ items: [MonarchLinkedDocsPanelItem]?,
        isReadOnly: Bool = false,
        notice: String? = nil,
        errorMessage: String? = nil,
        isRetrying: Bool = false,
        docOptions: [MonarchLinkedDocsPanelOption] = [],
        isSpaceFilterShown: Bool = true,
        labels: MonarchLinkedDocsPanelLabels = MonarchLinkedDocsPanelLabels(),
        onRetry: (@MainActor () -> Void)? = nil,
        onLinkDoc: (@MainActor (String) async throws -> Void)? = nil,
        onDocQueryChange: (@MainActor (String) -> Void)? = nil,
        onCreateDoc: (@MainActor () -> Void)? = nil,
        onAddLink: (@MainActor (URL, String) async throws -> Void)? = nil,
        onUnlink: (@MainActor (MonarchLinkedDocsPanelItem) async throws -> Void)? = nil,
        onOpen: @escaping @MainActor (MonarchLinkedDocsPanelItem) -> Void
    ) {
        self.items = items
        self.isReadOnly = isReadOnly
        self.notice = notice
        self.errorMessage = errorMessage
        self.isRetrying = isRetrying
        self.docOptions = docOptions
        self.isSpaceFilterShown = isSpaceFilterShown
        self.labels = labels
        self.onRetry = onRetry
        self.onLinkDoc = onLinkDoc
        self.onDocQueryChange = onDocQueryChange
        self.onCreateDoc = onCreateDoc
        self.onAddLink = onAddLink
        self.onUnlink = onUnlink
        self.onOpen = onOpen
    }

    private var canLinkDoc: Bool { !isReadOnly && onLinkDoc != nil }
    private var canAddLink: Bool { !isReadOnly && onAddLink != nil }

    /// The content of the view.
    public var body: some View {
        let phase = Tokens.phase(items: items, notice: notice)
        MonarchSectionCard(
            labels.title ?? Strings.title,
            state: phase == .loading ? .loading : .ready,
            labels: MonarchSectionCardLabels(loading: labels.loading ?? Strings.loading),
            content: { content(phase) },
            actions: { addMenu(phase) }
        )
        .monarchTitleBadge(phase == .list ? (items?.count ?? 0).formatted(.number.locale(locale)) : nil)
        .sheet(isPresented: $isPickingDoc) {
            MonarchLinkedDocsPanelPicker(
                options: docOptions,
                items: items ?? [],
                labels: labels,
                onQueryChange: onDocQueryChange,
                onCreateDoc: onCreateDoc,
                onSelect: { docID in
                    isPickingDoc = false
                    link(docID)
                }
            )
        }
        .sheet(isPresented: $isAddingLink) {
            MonarchLinkedDocsPanelLinkForm(labels: labels) { url, title in
                try await onAddLink?(url, title)
            }
        }
    }

    @ViewBuilder private func addMenu(_ phase: MonarchLinkedDocsPanelTokens.Phase) -> some View {
        if phase != .notice, canLinkDoc || canAddLink {
            // As on the web: "Link doc" (secondary) and "Add link" (ghost) side by side.
            HStack(spacing: Tokens.rowSpacing) {
                if canLinkDoc {
                    MonarchButton(
                        labels.linkDoc ?? Strings.linkDoc,
                        systemImage: Tokens.linkDocSymbol,
                        variant: .secondary,
                        size: .sm
                    ) { isPickingDoc = true }
                }
                if canAddLink {
                    MonarchButton(
                        labels.addLink ?? Strings.addLink,
                        systemImage: Tokens.linkSymbol,
                        variant: .ghost,
                        size: .sm
                    ) { isAddingLink = true }
                }
            }
            .fixedSize(horizontal: true, vertical: false)
        }
    }

    @ViewBuilder private func content(_ phase: MonarchLinkedDocsPanelTokens.Phase) -> some View {
        if let errorMessage, !errorMessage.isEmpty {
            if let onRetry {
                MonarchInlineErrorRetry(errorMessage, isRetrying: isRetrying, onRetry: onRetry)
            } else {
                MonarchInlineNotice(errorMessage, tone: .danger, variant: .plain)
            }
        }
        if let failure {
            MonarchInlineNotice(failure, tone: .danger, variant: .plain)
        }
        switch phase {
        case .notice:
            MonarchInlineNotice(notice ?? "", tone: .info)
        case .loading:
            EmptyView()
        case .empty:
            Text(isReadOnly ? labels.emptyReadOnly ?? Strings.emptyReadOnly : labels.empty ?? Strings.empty)
                .monarchTextStyle(.body)
                .foregroundStyle(theme.colors.textMuted)
                .fixedSize(horizontal: false, vertical: true)
        case .list:
            list(items ?? [])
        }
    }

    @ViewBuilder private func list(_ all: [MonarchLinkedDocsPanelItem]) -> some View {
        let spaces = Tokens.spaces(all, noSpace: labels.noSpace ?? Strings.noSpace, locale: locale)
        let showsFilter = Tokens.showsFilter(isEnabled: isSpaceFilterShown, spaces: spaces)
        let shown = showsFilter ? Tokens.filtered(all, key: filter, spaces: spaces) : all
        VStack(alignment: .leading, spacing: Tokens.sectionSpacing) {
            if showsFilter {
                MonarchFlowLayout(spacing: Tokens.rowSpacing) {
                    filterChip(
                        "\(labels.all ?? Strings.all) · \(all.count.formatted(.number.locale(locale)))",
                        isOn: filter.isEmpty
                    ) { filter = "" }
                    ForEach(spaces) { space in
                        filterChip(
                            "\(space.name) · \(space.count.formatted(.number.locale(locale)))",
                            isOn: filter == space.key
                        ) { filter = space.key }
                    }
                }
                .accessibilityElement(children: .contain)
                .accessibilityLabel(labels.filterBySpace ?? Strings.filterBySpace)
            }
            VStack(alignment: .leading, spacing: Tokens.rowSpacing) {
                ForEach(Array(shown.enumerated()), id: \.element.id) { index, item in
                    if index > 0 {
                        MonarchDivider()
                    }
                    row(item)
                }
            }
            if shown.count < all.count {
                HStack(spacing: Tokens.iconSpacing) {
                    let hidden = all.count - shown.count
                    Text(labels.hiddenByFilter?(hidden) ?? Strings.hiddenByFilter(hidden))
                        .monarchTextStyle(.caption)
                        .foregroundStyle(theme.colors.textMuted)
                    Button(labels.showAll ?? Strings.showAll) { filter = "" }
                        .buttonStyle(.monarchLink(.standalone))
                }
            }
        }
    }

    private func filterChip(_ title: String, isOn: Bool, action: @escaping () -> Void) -> some View {
        Toggle(title, isOn: Binding(get: { isOn }, set: { _ in action() }))
            .toggleStyle(.monarchButton(.outline, size: .sm))
    }

    private func row(_ item: MonarchLinkedDocsPanelItem) -> some View {
        let meta = Tokens.meta(item)
        return HStack(alignment: .center, spacing: Tokens.iconSpacing) {
            Button {
                onOpen(item)
            } label: {
                HStack(alignment: .center, spacing: Tokens.iconSpacing) {
                    icon(item)
                    VStack(alignment: .leading, spacing: Tokens.lineSpacing) {
                        Text(item.title)
                            .monarchTextStyle(.body, weight: .medium)
                            .foregroundStyle(theme.colors.textDefault)
                            .multilineTextAlignment(.leading)
                        if let meta {
                            Text(meta)
                                .monarchTextStyle(.caption)
                                .foregroundStyle(theme.colors.textMuted)
                        }
                    }
                    .frame(maxWidth: .infinity, alignment: .leading)
                    if let status = item.status, !status.isEmpty {
                        MonarchBadge(status)
                    }
                }
                .frame(minHeight: MonarchAccessibility.minimumHitTarget)
                .contentShape(Rectangle())
            }
            .buttonStyle(.plain)
            .accessibilityElement(children: .ignore)
            .accessibilityLabel(item.title)
            .accessibilityValue(Tokens.accessibilityValue(meta: meta, status: item.status))
            .accessibilityAddTraits(.isButton)
            if !isReadOnly, onUnlink != nil, item.isUnlinkable {
                Button {
                    unlink(item)
                } label: {
                    if unlinkingID == item.id {
                        ProgressView().controlSize(.small)
                    } else {
                        MonarchIcon(systemName: Tokens.unlinkSymbol)
                    }
                }
                .buttonStyle(.monarch(.ghost, size: .sm))
                .accessibilityLabel(labels.unlink?(item.title) ?? Strings.unlink(item.title))
                .disabled(unlinkingID != nil && unlinkingID != item.id)
            }
        }
    }

    @ViewBuilder private func icon(_ item: MonarchLinkedDocsPanelItem) -> some View {
        switch item.kind {
        case .doc:
            if let fileName = item.fileName {
                MonarchFileTypeIcon(fileName: fileName, size: .sm, isDecorative: true)
            } else {
                // A plain grey document glyph, as on the web.
                MonarchIcon(systemName: Tokens.docSymbol)
                    .foregroundStyle(theme.colors.textMuted)
            }
        case .link:
            MonarchIcon(systemName: Tokens.linkSymbol)
                .foregroundStyle(theme.colors.textMuted)
        }
    }

    private func link(_ docID: String) {
        guard let onLinkDoc else { return }
        failure = nil
        Task { @MainActor in
            do {
                try await onLinkDoc(docID)
            } catch {
                let message = labels.linkFailed ?? Strings.linkFailed
                failure = message
                AccessibilityNotification.Announcement(message).post()
            }
        }
    }

    private func unlink(_ item: MonarchLinkedDocsPanelItem) {
        guard let onUnlink, unlinkingID == nil else { return }
        failure = nil
        unlinkingID = item.id
        Task { @MainActor in
            do {
                try await onUnlink(item)
            } catch {
                let message = labels.unlinkFailed ?? Strings.unlinkFailed
                failure = message
                AccessibilityNotification.Announcement(message).post()
            }
            unlinkingID = nil
        }
    }
}

#if DEBUG
    #Preview("MonarchLinkedDocsPanel") {
        MonarchPreviewMatrix {
            VStack {
                MonarchLinkedDocsPanel(
                    [
                        MonarchLinkedDocsPanelItem(
                            id: "1",
                            title: "Launch plan",
                            docID: "d1",
                            meta: "Updated 2 hours ago",
                            status: "Draft",
                            spaceID: "s1",
                            spaceName: "Marketing"
                        ),
                        MonarchLinkedDocsPanelItem(
                            id: "2",
                            title: "Runbook",
                            kind: .link,
                            url: URL(string: "https://www.example.com/runbook")
                        ),
                    ],
                    onLinkDoc: { _ in },
                    onAddLink: { _, _ in },
                    onUnlink: { _ in },
                    onOpen: { _ in }
                )
                MonarchLinkedDocsPanel(nil, onOpen: { _ in })
                MonarchLinkedDocsPanel([], isReadOnly: true, onOpen: { _ in })
            }
        }
    }
#endif
