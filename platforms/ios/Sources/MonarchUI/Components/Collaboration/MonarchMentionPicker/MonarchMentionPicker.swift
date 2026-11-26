import SwiftUI

/// Suggestions for an @mention, shown in a strip above the keyboard while
/// someone types a mention.
///
/// ```swift
/// .safeAreaInset(edge: .bottom) {
///     VStack(spacing: 0) {
///         MonarchMentionPicker(query, items: results, status: status) { item in
///             if let query { text = query.replacing(in: text, with: "@\(item.label)") }
///         }
///         composer
///     }
/// }
/// ```
///
/// iOS gives no caret position to place a list beside it, so the picker is
/// a full-width strip to put directly above the text field, in the bottom
/// safe-area inset that holds the composer, where it stays above the
/// keyboard. Find the query with ``MonarchMentionQuery/find(in:caret:triggers:)``
/// as the text changes, run the product's search in `.task(id: query)`
/// (which cancels the previous search), and pass the results and the
/// status. The picker draws nothing while `query` is `nil`, or when a query
/// with a space found nothing. People and channels are grouped under
/// headings when both are present. More than four suggestions scroll.
///
/// Accessibility: each suggestion is a button named by its label and
/// detail, dimmed and not activatable when disabled. The list is a
/// container named "Suggestions" with headings for its groups. The number
/// of results, "Searching…", "No matches" and the error are announced as
/// they change, so focus can stay in the text field. Rows are at least 44
/// points high and grow with Dynamic Type. Avatars are decorative.
public struct MonarchMentionPicker: View {
    private let query: MonarchMentionQuery?
    private let items: [MonarchMentionItem]
    private let status: MonarchMentionPickerStatus
    private let labels: MonarchMentionPickerLabels
    private let onRetry: (@MainActor () -> Void)?
    private let onSelect: @MainActor (MonarchMentionItem) -> Void

    @Environment(\.monarchTheme) private var theme
    @ScaledMetric(relativeTo: .body) private var maxHeight =
        MonarchMentionPickerTokens.rowHeight * MonarchMentionPickerTokens.visibleRows

    /// A mention picker.
    ///
    /// - Parameters:
    ///   - query: The mention being typed. Nothing is drawn while it is `nil`.
    ///   - items: The suggestions from the product's search.
    ///   - status: Loading, ready or failed.
    ///   - labels: Replacements for the built-in strings.
    ///   - onRetry: Repeats a failed search. Adds a retry button to the error.
    ///   - onSelect: Receives the chosen suggestion. Replace the query with the mention and set the query to `nil`.
    public init(
        _ query: MonarchMentionQuery?,
        items: [MonarchMentionItem],
        status: MonarchMentionPickerStatus = .ready,
        labels: MonarchMentionPickerLabels = MonarchMentionPickerLabels(),
        onRetry: (@MainActor () -> Void)? = nil,
        onSelect: @escaping @MainActor (MonarchMentionItem) -> Void
    ) {
        self.query = query
        self.items = items
        self.status = status
        self.labels = labels
        self.onRetry = onRetry
        self.onSelect = onSelect
    }

    private typealias Tokens = MonarchMentionPickerTokens

    private struct Announced: Equatable {
        let status: MonarchMentionPickerStatus
        let count: Int
    }

    /// The content of the view.
    public var body: some View {
        if MonarchMentionSuggestions.isOpen(query: query, status: status, itemCount: items.count) {
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
                        Tokens.announcement(status: new.status, count: new.count, labels: labels)
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
                        MonarchMentionItem(id: "\(index)", label: "Placeholder name", detail: "placeholder@example.com")
                    )
                }
            }
            .redacted(reason: .placeholder)
            .accessibilityElement(children: .ignore)
            .accessibilityLabel(labels.loading ?? MonarchMentionPickerStrings.loading)
        case .failed:
            Group {
                if let onRetry {
                    MonarchInlineErrorRetry(
                        labels.error ?? MonarchMentionPickerStrings.error,
                        retryLabel: labels.retry,
                        onRetry: onRetry
                    )
                } else {
                    Text(labels.error ?? MonarchMentionPickerStrings.error)
                        .monarchTextStyle(.body)
                        .foregroundStyle(theme.colors.textMuted)
                }
            }
            .padding(.horizontal, Tokens.horizontalPadding)
            .frame(minHeight: MonarchAccessibility.minimumHitTarget)
        case .ready:
            if items.isEmpty {
                Text(labels.empty ?? MonarchMentionPickerStrings.empty)
                    .monarchTextStyle(.body)
                    .foregroundStyle(theme.colors.textMuted)
                    .padding(.horizontal, Tokens.horizontalPadding)
                    .frame(minHeight: MonarchAccessibility.minimumHitTarget)
            } else if Tokens.scrolls(itemCount: items.count) {
                ScrollView { list }.frame(maxHeight: maxHeight)
            } else {
                list
            }
        }
    }

    private var list: some View {
        let groups = Tokens.groups(items)
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
                        row(item)
                    }
                    .buttonStyle(.plain)
                    .disabled(item.isDisabled)
                }
            }
        }
        .accessibilityElement(children: .contain)
        .accessibilityLabel(labels.label ?? MonarchMentionPickerStrings.label)
    }

    private func row(_ item: MonarchMentionItem) -> some View {
        let colors = Tokens.textColors(isDisabled: item.isDisabled)
        return HStack(spacing: Tokens.rowSpacing) {
            if item.kind == .channel {
                MonarchAvatar(
                    image: nil,
                    name: item.label,
                    systemImage: Tokens.channelSymbol,
                    shape: .square,
                    size: Tokens.avatarSize,
                    isDecorative: true
                )
            } else {
                MonarchAvatar(url: item.avatarURL, name: item.label, size: Tokens.avatarSize, isDecorative: true)
            }
            VStack(alignment: .leading) {
                Text(item.label)
                    .monarchTextStyle(.body, weight: .medium)
                    .foregroundStyle(theme.colors[role: colors.label])
                if let detail = item.detail, !detail.isEmpty {
                    Text(detail)
                        .monarchTextStyle(.caption)
                        .foregroundStyle(theme.colors[role: colors.detail])
                }
            }
            Spacer(minLength: 0)
        }
        .padding(.horizontal, Tokens.horizontalPadding)
        .padding(.vertical, Tokens.verticalPadding)
        .frame(minHeight: MonarchAccessibility.minimumHitTarget)
        .contentShape(Rectangle())
    }
}

#if DEBUG
    #Preview("MonarchMentionPicker") {
        let query = MonarchMentionQuery.find(in: "Thanks @an")
        MonarchPreviewMatrix {
            VStack {
                MonarchMentionPicker(
                    query,
                    items: [
                        MonarchMentionItem(id: "u1", label: "Ana Lopez", detail: "Design"),
                        MonarchMentionItem(id: "u2", label: "Andre Kim", detail: "No access", isDisabled: true),
                        MonarchMentionItem(id: "c1", label: "announcements", kind: .channel, detail: "214 members"),
                    ]
                ) { _ in }
                MonarchMentionPicker(query, items: [], status: .loading) { _ in }
                MonarchMentionPicker(query, items: [], status: .failed, onRetry: {}, onSelect: { _ in })
            }
        }
    }
#endif
