import MonarchTokens
import SwiftUI

/// The `@` context menu of an AI prompt: documents, people, records and apps
/// that can be added to what the AI sees, grouped by kind, with loading,
/// empty and error states.
///
/// ```swift
/// .safeAreaInset(edge: .bottom) {
///     VStack(spacing: 0) {
///         MonarchAiMentionContext(query, items: results, status: status) { item in
///             context.append(item.contextItem)
///             if let query { prompt = query.replacing(in: prompt, with: "") }
///         }
///         MonarchAiContextChips(context) { id in context.removeAll { $0.id == id } }
///         composer
///     }
/// }
/// ```
///
/// It is the context preset of ``MonarchMentionPicker``: the same strip
/// above the keyboard, opened by the same ``MonarchMentionQuery``, but for
/// the four kinds of context, with token estimates, and a chosen result
/// becomes a ``MonarchAiContextItem`` (`item.contextItem`) for
/// ``MonarchAiContextChips``. Run the product's search in
/// `.task(id: query)` and leave out ids already in the context. More than
/// four results scroll.
///
/// Accessibility: each result is a button read as its label, description
/// and token estimate, dimmed and not activatable when disabled. The list is
/// a container named "Add context" with headings for its groups when more
/// than one kind is present. "Searching…", the number of results, the empty
/// text and the error are announced, so focus can stay in the prompt. Rows
/// are at least 44 points high; icons and avatars are decorative.
public struct MonarchAiMentionContext: View {
    private let query: MonarchMentionQuery?
    private let items: [MonarchAiMentionItem]
    private let status: MonarchMentionPickerStatus
    private let labels: MonarchAiMentionContextLabels
    private let onRetry: (@MainActor () -> Void)?
    private let onSelect: @MainActor (MonarchAiMentionItem) -> Void

    @Environment(\.monarchTheme) private var theme
    @Environment(\.locale) private var locale
    @ScaledMetric(relativeTo: .body) private var maxHeight =
        MonarchAiMentionContextTokens.rowHeight * MonarchAiMentionContextTokens.visibleRows

    /// A mention context menu.
    ///
    /// - Parameters:
    ///   - query: The `@` query being typed. Nothing is drawn while it is `nil`.
    ///   - items: The results of the product's search.
    ///   - status: Loading, ready or failed.
    ///   - labels: Replacements for the built-in strings.
    ///   - onRetry: Repeats a failed search. Adds a retry button to the error.
    ///   - onSelect: Receives the chosen result. Add `item.contextItem` to the context and remove the query.
    public init(
        _ query: MonarchMentionQuery?,
        items: [MonarchAiMentionItem],
        status: MonarchMentionPickerStatus = .ready,
        labels: MonarchAiMentionContextLabels = MonarchAiMentionContextLabels(),
        onRetry: (@MainActor () -> Void)? = nil,
        onSelect: @escaping @MainActor (MonarchAiMentionItem) -> Void
    ) {
        self.query = query
        self.items = items
        self.status = status
        self.labels = labels
        self.onRetry = onRetry
        self.onSelect = onSelect
    }

    private typealias Tokens = MonarchAiMentionContextTokens

    private struct Announced: Equatable {
        let status: MonarchMentionPickerStatus
        let count: Int
    }

    /// The content of the view.
    public var body: some View {
        if Tokens.isOpen(query: query, status: status, itemCount: items.count) {
            content
                .frame(maxWidth: .infinity, alignment: .leading)
                .background(theme.colors[role: Tokens.background])
                .overlay(alignment: .top) {
                    Rectangle()
                        .fill(theme.colors[role: Tokens.border])
                        .frame(height: MonarchShapes.borderWidth)
                        .accessibilityHidden(true)
                }
                .onChange(of: Announced(status: status, count: items.count), initial: true) { _, new in
                    AccessibilityNotification.Announcement(
                        Tokens.announcement(
                            status: new.status,
                            count: new.count,
                            query: query?.text ?? "",
                            labels: labels
                        )
                    )
                    .post()
                }
        }
    }

    @ViewBuilder private var content: some View {
        switch status {
        case .loading:
            VStack(alignment: .leading, spacing: 0) {
                ForEach(0..<Tokens.placeholderRows, id: \.self) { index in
                    row(
                        MonarchAiMentionItem(
                            id: "\(index)",
                            kind: .doc,
                            label: "Placeholder document",
                            description: "Placeholder path"
                        )
                    )
                }
            }
            .redacted(reason: .placeholder)
            .accessibilityElement(children: .ignore)
            .accessibilityLabel(labels.loading ?? MonarchAiMentionContextStrings.loading)
        case .failed:
            Group {
                if let onRetry {
                    MonarchInlineErrorRetry(
                        labels.error ?? MonarchAiMentionContextStrings.error,
                        retryLabel: labels.retry,
                        onRetry: onRetry
                    )
                } else {
                    message(labels.error ?? MonarchAiMentionContextStrings.error)
                }
            }
            .padding(.horizontal, Tokens.horizontalPadding)
            .frame(minHeight: MonarchAccessibility.minimumHitTarget)
        case .ready:
            if items.isEmpty {
                message(Tokens.emptyText(query: query?.text ?? "", labels: labels))
                    .padding(.horizontal, Tokens.horizontalPadding)
                    .frame(minHeight: MonarchAccessibility.minimumHitTarget)
            } else if Tokens.scrolls(itemCount: items.count) {
                ScrollView { list }.frame(maxHeight: maxHeight)
            } else {
                list
            }
        }
    }

    private func message(_ text: String) -> some View {
        Text(text)
            .monarchTextStyle(.body)
            .foregroundStyle(theme.colors.textMuted)
    }

    private var list: some View {
        let groups = Tokens.groups(items)
        // The first row you can pick is the active one, tinted as on the web.
        let activeID = items.first { !$0.isDisabled }?.id
        return VStack(alignment: .leading, spacing: 0) {
            ForEach(groups, id: \.kind) { group in
                if let heading = Tokens.heading(group.kind, groupCount: groups.count, labels: labels) {
                    Text(heading)
                        .monarchTextStyle(.caption, weight: .semibold)
                        .foregroundStyle(theme.colors.textMuted)
                        .padding(.horizontal, Tokens.horizontalPadding)
                        .padding(.top, Tokens.verticalPadding)
                        .accessibilityAddTraits(.isHeader)
                }
                ForEach(group.items) { item in
                    Button {
                        onSelect(item)
                    } label: {
                        row(item, isActive: item.id == activeID)
                    }
                    .buttonStyle(.plain)
                    .disabled(item.isDisabled)
                    .accessibilityElement(children: .ignore)
                    .accessibilityLabel(Tokens.rowLabel(item, locale: locale, labels: labels))
                    .accessibilityAddTraits(.isButton)
                }
            }
        }
        .accessibilityElement(children: .contain)
        .accessibilityLabel(labels.label ?? MonarchAiMentionContextStrings.label)
    }

    private func row(_ item: MonarchAiMentionItem, isActive: Bool = false) -> some View {
        let colors = Tokens.textColors(isDisabled: item.isDisabled)
        return HStack(spacing: Tokens.rowSpacing) {
            if item.kind == .person {
                MonarchAvatar(url: item.avatarURL, name: item.label, size: Tokens.avatarSize, isDecorative: true)
            } else {
                MonarchIcon(systemName: Tokens.symbol(item.kind), size: .sm)
                    .foregroundStyle(theme.colors[role: colors.label])
                    .frame(width: 24, height: 24)
            }
            VStack(alignment: .leading) {
                Text(item.label)
                    .monarchTextStyle(.body, weight: .medium)
                    .foregroundStyle(theme.colors[role: colors.label])
                if let description = item.description, !description.isEmpty {
                    Text(description)
                        .monarchTextStyle(.caption)
                        .foregroundStyle(theme.colors[role: colors.detail])
                }
            }
            Spacer(minLength: 0)
            if let tokens = item.tokens {
                Text(Tokens.tokensText(tokens, locale: locale, labels: labels))
                    .monarchTextStyle(.caption)
                    .foregroundStyle(theme.colors[role: colors.detail])
                    .fixedSize()
            }
        }
        .padding(.horizontal, Tokens.horizontalPadding)
        .padding(.vertical, Tokens.verticalPadding)
        .frame(minHeight: MonarchAccessibility.minimumHitTarget)
        .background(
            isActive ? theme.colors.aiSubtle : Color.clear,
            in: RoundedRectangle(cornerRadius: MonarchTokens.Radius.md)
        )
        .contentShape(Rectangle())
    }
}

#if DEBUG
    #Preview("MonarchAiMentionContext") {
        let query = MonarchMentionQuery.find(in: "Summarise @q3")
        MonarchPreviewMatrix {
            VStack {
                MonarchAiMentionContext(
                    query,
                    items: [
                        MonarchAiMentionItem(
                            id: "d1",
                            kind: .doc,
                            label: "Q3 report.pdf",
                            description: "Finance",
                            tokens: 12_400
                        ),
                        MonarchAiMentionItem(id: "p1", kind: .person, label: "Ana Lopez", description: "Design"),
                        MonarchAiMentionItem(id: "r1", kind: .record, label: "Acme Corp", isDisabled: true),
                    ]
                ) { _ in }
                MonarchAiMentionContext(query, items: [], status: .loading) { _ in }
                MonarchAiMentionContext(query, items: [], status: .failed, onRetry: {}, onSelect: { _ in })
            }
        }
    }
#endif
