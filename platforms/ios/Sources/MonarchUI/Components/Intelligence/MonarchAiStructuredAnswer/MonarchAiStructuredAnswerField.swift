import SwiftUI

/// One field of ``MonarchAiStructuredAnswer``: the label and the value, a
/// placeholder while it streams in, or a dash once the answer is complete.
struct MonarchAiStructuredAnswerField: View {
    let field: MonarchAiStructuredField
    let value: MonarchAiJSON?
    let isStreaming: Bool
    let isCard: Bool
    let pendingLabel: String
    let emptyLabel: String
    let onOpenLink: (@MainActor (URL) -> Void)?

    @Environment(\.monarchTheme) private var theme
    @Environment(\.locale) private var locale

    private typealias Tokens = MonarchAiStructuredAnswerTokens

    var body: some View {
        let state = Tokens.state(value, isStreaming: isStreaming)
        Group {
            if field.type == .markdown && state == .value {
                // Markdown keeps its own elements, so its links stay reachable.
                VStack(alignment: .leading, spacing: Tokens.itemSpacing) {
                    Text(field.label)
                        .monarchTextStyle(.caption)
                        .foregroundStyle(theme.colors.textMuted)
                    MonarchAiMarkdown(Tokens.text(value), onOpenLink: onOpenLink)
                }
                .frame(maxWidth: .infinity, alignment: .leading)
                .accessibilityElement(children: .contain)
            } else {
                LabeledContent {
                    content(state)
                } label: {
                    Text(field.label)
                }
                .labeledContentStyle(.monarch(isCard || Tokens.isWide(field) ? .stacked : .inline))
                .accessibilityElement(children: .ignore)
                .accessibilityLabel(field.label)
                .accessibilityValue(
                    Tokens.spokenValue(
                        value,
                        field: field,
                        isStreaming: isStreaming,
                        locale: locale,
                        pending: pendingLabel,
                        empty: emptyLabel
                    )
                )
            }
        }
        .padding(isCard ? Tokens.cardPadding : 0)
        .frame(maxWidth: .infinity, maxHeight: isCard ? .infinity : nil, alignment: .topLeading)
        .background {
            if isCard {
                RoundedRectangle(cornerRadius: Tokens.cardRadius, style: .continuous)
                    .fill(theme.colors[role: Tokens.Role.cardSurface])
                    .overlay(
                        RoundedRectangle(cornerRadius: Tokens.cardRadius, style: .continuous)
                            .strokeBorder(theme.colors[role: Tokens.Role.cardBorder], lineWidth: Tokens.borderWidth)
                    )
            }
        }
    }

    @ViewBuilder private func content(_ state: Tokens.State) -> some View {
        switch state {
        case .pending:
            Text(pendingLabel)
                .redacted(reason: .placeholder)
        case .empty:
            Text(Tokens.emptyMark)
                .foregroundStyle(theme.colors[role: Tokens.Role.empty])
        case .value:
            switch field.type {
            case .number:
                Text(Tokens.number(value, format: field.format, locale: locale))
                    .monospacedDigit()
            case .list:
                VStack(alignment: .leading, spacing: Tokens.itemSpacing) {
                    ForEach(Array(Tokens.items(value).enumerated()), id: \.offset) { _, item in
                        HStack(alignment: .firstTextBaseline, spacing: Tokens.bulletSpacing) {
                            Text(Tokens.bullet)
                                .foregroundStyle(theme.colors[role: Tokens.Role.bullet])
                            Text(item)
                                .fixedSize(horizontal: false, vertical: true)
                        }
                    }
                }
            case .badge:
                MonarchFlowLayout(spacing: Tokens.itemSpacing) {
                    ForEach(Array(Tokens.items(value).enumerated()), id: \.offset) { _, item in
                        MonarchBadge(item, tone: Tokens.tone(item, field: field))
                    }
                }
            case .text, .markdown:
                Text(Tokens.text(value))
                    .fixedSize(horizontal: false, vertical: true)
            }
        }
    }
}
