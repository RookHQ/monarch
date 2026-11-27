import SwiftUI

/// What the AI will see with the next prompt: files, a text selection, the
/// current page, records, apps and people, each as a chip with its kind,
/// name and token weight, plus the total.
///
/// ```swift
/// MonarchAiContextChips(context, budget: 128_000) { id in
///     context.removeAll { $0.id == id }
/// }
/// ```
///
/// Chips wrap onto new rows. An item with a token count shows it in compact
/// form ("1.2K tokens") with a three-bar weight mark: its share of `budget`,
/// or of the total without one. With `onRemove`, removable items get a
/// remove button; take the item out of `items` in the handler.
///
/// Accessibility: the chips are a group named "Context". Each chip is one
/// element read as the kind, the name and the token count ("File:
/// contract.pdf, 1.2K tokens"), with the description as its hint; the full
/// name is read even when it is truncated on screen. The weight is also
/// shown by the number of filled bars and the count, not by colour alone.
/// Each remove button is a separate button named "Remove contract.pdf from
/// context", with a 44-point touch target. After a removal VoiceOver
/// announces it and moves to the next chip, or the previous one.
public struct MonarchAiContextChips: View {
    private let items: [MonarchAiContextItem]
    private let budget: Int?
    private let showsTotal: Bool?
    private let emptyText: String?
    private let locale: Locale
    private let labels: MonarchAiContextChipsLabels
    private let onRemove: (@MainActor (String) -> Void)?

    @Environment(\.monarchTheme) private var theme
    @AccessibilityFocusState private var focusedChip: String?
    @State private var pendingRemoval: PendingRemoval?

    /// Context chips.
    ///
    /// - Parameters:
    ///   - items: What the AI will see, in the order it was added.
    ///   - budget: Tokens the context may use. When set, each weight is the item's share of the budget.
    ///   - showsTotal: Whether the total is shown after the chips. By default when any item has a token count.
    ///   - emptyText: Shown when `items` is empty. Without it nothing is shown.
    ///   - locale: The locale of the compact numbers.
    ///   - labels: Replacements for the built-in strings.
    ///   - onRemove: Runs with the id of the item to remove. No remove buttons without it.
    public init(
        _ items: [MonarchAiContextItem],
        budget: Int? = nil,
        showsTotal: Bool? = nil,
        emptyText: String? = nil,
        locale: Locale = .current,
        labels: MonarchAiContextChipsLabels = MonarchAiContextChipsLabels(),
        onRemove: (@MainActor (String) -> Void)? = nil
    ) {
        self.items = items
        self.budget = budget
        self.showsTotal = showsTotal
        self.emptyText = emptyText
        self.locale = locale
        self.labels = labels
        self.onRemove = onRemove
    }

    /// The content of the view.
    public var body: some View {
        let total = MonarchAiContextChipsTokens.total(items)
        VStack(alignment: .leading, spacing: MonarchAiContextChipsTokens.totalSpacing) {
            if items.isEmpty {
                if let emptyText {
                    Text(emptyText)
                        .monarchTextStyle(.caption)
                        .foregroundStyle(theme.colors.textSubtle)
                }
            } else {
                MonarchFlowLayout(spacing: MonarchAiContextChipsTokens.spacing) {
                    ForEach(Array(items.enumerated()), id: \.element.id) { index, item in
                        chip(item, index: index, total: total)
                    }
                }
                .accessibilityElement(children: .contain)
                .accessibilityLabel(labels.label ?? MonarchAiContextChipsStrings.label)
            }
            if MonarchAiContextChipsTokens.showsTotal(items, override: showsTotal) {
                Text(MonarchAiContextChipsTokens.totalText(total, labels: labels, locale: locale))
                    .monarchTextStyle(.caption)
                    .monospacedDigit()
                    .foregroundStyle(theme.colors.textMuted)
            }
        }
        .onChange(of: items.map(\.id)) { _, ids in
            handleRemoval(remaining: ids)
        }
    }

    private func chip(_ item: MonarchAiContextItem, index: Int, total: Int) -> some View {
        let isRemovable = onRemove != nil && item.isRemovable
        let weight = MonarchAiContextChipsTokens.weight(item, total: total, budget: budget)
        let tokensText: String? = item.tokens.map { count in
            MonarchAiContextChipsTokens.tokensText(count, labels: labels, locale: locale)
        }
        var removeAction: (@MainActor () -> Void)?
        if isRemovable {
            removeAction = { remove(item, at: index) }
        }
        let removeLabel = labels.remove?(item.label) ?? MonarchAiContextChipsStrings.remove(item.label)
        return MonarchAiContextChip(
            item: item,
            kindName: item.group ?? MonarchAiContextChipsTokens.kindName(item.kind, labels: labels),
            weight: weight,
            tokensText: tokensText,
            removeLabel: removeLabel,
            onRemove: removeAction
        )
        .accessibilityFocused($focusedChip, equals: item.id)
    }

    private func remove(_ item: MonarchAiContextItem, at index: Int) {
        pendingRemoval = PendingRemoval(id: item.id, index: index, label: item.label)
        onRemove?(item.id)
    }

    private func handleRemoval(remaining ids: [String]) {
        guard let pending = pendingRemoval, !ids.contains(pending.id) else { return }
        pendingRemoval = nil
        AccessibilityNotification.Announcement(
            labels.removed?(pending.label) ?? MonarchAiContextChipsStrings.removed(pending.label)
        ).post()
        focusedChip = MonarchFocusAfterRemoval.target(removedIndex: pending.index, remaining: ids)
    }

    /// The item being removed, until it leaves `items`.
    private struct PendingRemoval: Equatable {
        let id: String
        let index: Int
        let label: String
    }
}

/// One chip: the kind icon, the name, the token count with its weight mark and the remove button.
private struct MonarchAiContextChip: View {
    let item: MonarchAiContextItem
    let kindName: String
    let weight: MonarchAiContextWeight?
    let tokensText: String?
    let removeLabel: String
    let onRemove: (@MainActor () -> Void)?

    @Environment(\.monarchTheme) private var theme
    @Environment(\.isEnabled) private var isEnabled
    @ScaledMetric(relativeTo: .body) private var labelMaxWidth = MonarchAiContextChipsTokens.labelMaxWidth

    var body: some View {
        let appearance = MonarchAiContextChipsTokens.appearance(isEnabled: isEnabled)
        let inset = MonarchAiContextChipsTokens.hitInset(isRemovable: onRemove != nil)
        HStack(spacing: 0) {
            content
                .padding(.leading, MonarchAiContextChipsTokens.horizontalPadding)
                .padding(.trailing, onRemove == nil ? MonarchAiContextChipsTokens.horizontalPadding : 0)
                .padding(.vertical, MonarchAiContextChipsTokens.verticalPadding + inset)
                .frame(minHeight: MonarchAiContextChipsTokens.minHeight + inset * 2)
                .accessibilityElement(children: .ignore)
                .accessibilityLabel(
                    MonarchAiContextChipsTokens.accessibilityLabel(
                        kind: kindName,
                        label: item.label,
                        tokens: tokensText
                    )
                )
                .accessibilityHint(item.description ?? "")
                .help(item.description ?? item.label)
            if let onRemove {
                removeButton(onRemove)
            }
        }
        .foregroundStyle(theme.colors[role: appearance.foreground])
        .background {
            if let background = appearance.background {
                Capsule().fill(theme.colors[role: background]).padding(.vertical, inset)
            }
        }
        .overlay {
            if let border = appearance.border {
                Capsule()
                    .strokeBorder(theme.colors[role: border], lineWidth: MonarchAiContextChipsTokens.borderWidth)
                    .padding(.vertical, inset)
                    .allowsHitTesting(false)
            }
        }
    }

    private var content: some View {
        HStack(spacing: MonarchAiContextChipsTokens.contentSpacing) {
            // A group name stands in for the icon, in the AI colour, as on the web ("Page Opportunities").
            if item.group == nil {
                MonarchIcon(systemName: MonarchAiContextFormat.symbol(item.kind), size: .sm)
                    .foregroundStyle(isEnabled ? theme.colors.aiStrong : theme.colors.textDisabled)
            }
            if let group = item.group {
                Text(group)
                    .monarchTextStyle(.caption)
                    .foregroundStyle(isEnabled ? theme.colors.aiStrong : theme.colors.textDisabled)
                    .lineLimit(1)
            }
            MonarchAiContextLabelWidth(maxWidth: labelMaxWidth) {
                Text(item.label)
                    .monarchTextStyle(.caption, weight: MonarchAiContextChipsTokens.labelWeight)
                    .lineLimit(1)
                    .truncationMode(.middle)
            }
            if let tokensText, let weight {
                HStack(spacing: MonarchAiContextChipsTokens.contentSpacing / 2) {
                    weightMark(weight)
                    Text(tokensText)
                        .monarchTextStyle(.caption)
                        .monospacedDigit()
                        .lineLimit(1)
                        .foregroundStyle(
                            isEnabled
                                ? theme.colors[role: MonarchAiContextChipsTokens.tokensRole(weight)]
                                : theme.colors.textDisabled
                        )
                }
                .fixedSize()
            }
        }
    }

    private func weightMark(_ weight: MonarchAiContextWeight) -> some View {
        HStack(alignment: .bottom, spacing: MonarchAiContextChipsTokens.barSpacing) {
            ForEach(Array(MonarchAiContextChipsTokens.barHeights.enumerated()), id: \.offset) { index, height in
                Capsule()
                    .fill(theme.colors[role: MonarchAiContextChipsTokens.barRole(index: index, weight: weight)])
                    .frame(width: MonarchAiContextChipsTokens.barWidth, height: height)
            }
        }
    }

    private func removeButton(_ onRemove: @escaping @MainActor () -> Void) -> some View {
        Button(action: onRemove) {
            MonarchIcon(systemName: MonarchAiContextChipsTokens.removeSymbol, size: .sm)
                .padding(.trailing, MonarchAiContextChipsTokens.horizontalPadding)
                .frame(
                    minWidth: MonarchAccessibility.minimumHitTarget,
                    minHeight: MonarchAccessibility.minimumHitTarget,
                    alignment: .trailing
                )
                .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
        .padding(.leading, -MonarchAiContextChipsTokens.removeOverlap)
        .accessibilityLabel(removeLabel)
    }
}

/// Offers the name at most `maxWidth`, also when the parent asks for its ideal
/// size, so a long name truncates instead of widening the chip.
private struct MonarchAiContextLabelWidth: Layout {
    let maxWidth: CGFloat

    func sizeThatFits(proposal: ProposedViewSize, subviews: Subviews, cache: inout ()) -> CGSize {
        let width = min(proposal.width ?? maxWidth, maxWidth)
        return subviews.first?.sizeThatFits(ProposedViewSize(width: width, height: proposal.height)) ?? .zero
    }

    func placeSubviews(in bounds: CGRect, proposal: ProposedViewSize, subviews: Subviews, cache: inout ()) {
        subviews.first?.place(at: bounds.origin, anchor: .topLeading, proposal: ProposedViewSize(bounds.size))
    }
}

#if DEBUG
    #Preview("MonarchAiContextChips") {
        MonarchPreviewMatrix {
            MonarchAiContextChips(
                [
                    MonarchAiContextItem(id: "1", kind: .file, label: "Q3 contract.pdf", tokens: 12_400),
                    MonarchAiContextItem(id: "2", kind: .selection, label: "Selected text", tokens: 820),
                    MonarchAiContextItem(id: "3", kind: .page, label: "Invoices", tokens: 2_100, isRemovable: false),
                    MonarchAiContextItem(id: "4", kind: .person, label: "Dana Whitfield"),
                ],
                onRemove: { _ in }
            )
        }
    }
#endif
