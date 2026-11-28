import SwiftUI

/// One visible span: a chevron when it has children, and a button that
/// selects it, with its facts and its bar on the shared time scale.
struct MonarchAiTraceViewRow: View {
    let row: MonarchAiTraceViewTokens.Row
    let totals: MonarchAiTraceViewTokens.Totals
    let isSelected: Bool
    let currency: String
    let labels: MonarchAiTraceViewLabels
    var isCompact = false
    let onToggle: @MainActor (String) -> Void
    let onSelect: @MainActor (String) -> Void

    @Environment(\.monarchTheme) private var theme
    @Environment(\.locale) private var locale
    @Environment(\.layoutDirection) private var layoutDirection

    private typealias Tokens = MonarchAiTraceViewTokens

    var body: some View {
        let span = row.span
        let na = labels.notAvailable ?? MonarchAiTraceViewStrings.notAvailable
        Button {
            onSelect(span.id)
        } label: {
            Group {
                if isCompact {
                    compactBody(span, na: na)
                } else {
                    HStack(spacing: 0) {
                        HStack(spacing: 0) {
                            Color.clear.frame(width: Tokens.childIndent * CGFloat(row.level - 1), height: 1)
                            Color.clear.frame(width: 24, height: 1)  // the chevron's room, so names line up
                            if span.status == .error {
                                MonarchIcon(systemName: Tokens.errorSymbol, size: .sm)
                                    .foregroundStyle(theme.colors[role: Tokens.errorText])
                            }
                            Text(span.name)
                                .monarchTextStyle(.body, weight: .medium)
                                .foregroundStyle(theme.colors.textDefault)
                                .lineLimit(1)
                            Spacer(minLength: 0)
                        }
                        .frame(width: Tokens.columns[0], alignment: .leading)
                        cell(Tokens.kindName(span.kind, labels: labels), width: Tokens.columns[1], alignment: .leading)
                        cell(
                            Tokens.spanDuration(span, locale: locale, labels: labels),
                            width: Tokens.columns[2],
                            alignment: .trailing
                        )
                        cell(
                            Tokens.tokens(span).map { $0.formatted(.number.locale(locale)) } ?? na,
                            width: Tokens.columns[3],
                            alignment: .trailing
                        )
                        cell(
                            span.costMicros.map {
                                Tokens.costText($0, currency: span.currency ?? currency, locale: locale, labels: labels)
                            } ?? na,
                            width: Tokens.columns[4],
                            alignment: .trailing
                        )
                        bar(span)
                            .padding(.horizontal, Tokens.rowPadding)
                            .frame(width: Tokens.columns[5])
                    }
                }
            }
            .padding(.vertical, Tokens.rowPadding)
            .frame(minHeight: MonarchAccessibility.minimumHitTarget)
            .background(isSelected ? theme.colors[role: Tokens.selectedFill] : .clear)
            .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
        .overlay(alignment: .leading) {
            // The chevron sits over the span cell, outside the row button, so it toggles without selecting.
            if row.hasChildren {
                Button {
                    onToggle(span.id)
                } label: {
                    MonarchIcon(systemName: Tokens.chevronSymbol, size: .sm)
                        .foregroundStyle(theme.colors.textMuted)
                        .rotationEffect(
                            Tokens.chevronRotation(isExpanded: row.isExpanded, layoutDirection: layoutDirection)
                        )
                        .monarchAnimation(.fast, value: row.isExpanded)
                        .frame(width: 24, height: MonarchAccessibility.minimumHitTarget)
                        .contentShape(Rectangle())
                }
                .buttonStyle(.plain)
                .padding(.leading, Tokens.childIndent * CGFloat(row.level - 1))
                .accessibilityLabel(
                    row.isExpanded
                        ? labels.collapse?(span.name) ?? MonarchAiTraceViewStrings.collapse(span.name)
                        : labels.expand?(span.name) ?? MonarchAiTraceViewStrings.expand(span.name)
                )
            }
        }
        .accessibilityElement(children: .contain)
        .accessibilityLabel(Tokens.rowLabel(span, currency: currency, locale: locale, labels: labels))
        .accessibilityAddTraits(isSelected ? [.isButton, .isSelected] : .isButton)
    }

    /// The row on a width that cannot hold the table: the name with its kind and duration, then tokens and cost, then
    /// the bar across the full width, so the timeline is never scrolled out of sight.
    private func compactBody(_ span: MonarchAiTraceSpan, na: String) -> some View {
        VStack(alignment: .leading, spacing: Tokens.rowPadding) {
            HStack(spacing: Tokens.rowPadding) {
                Color.clear.frame(width: Tokens.childIndent * CGFloat(row.level - 1) + 24, height: 1)
                if span.status == .error {
                    MonarchIcon(systemName: Tokens.errorSymbol, size: .sm)
                        .foregroundStyle(theme.colors[role: Tokens.errorText])
                }
                Text(span.name)
                    .monarchTextStyle(.body, weight: .medium)
                    .foregroundStyle(theme.colors.textDefault)
                    .lineLimit(1)
                Spacer(minLength: 0)
                Text(Tokens.spanDuration(span, locale: locale, labels: labels))
                    .monarchTextStyle(.body)
                    .foregroundStyle(theme.colors.textDefault)
                    .monospacedDigit()
                    .lineLimit(1)
            }
            HStack(spacing: Tokens.rowPadding) {
                Color.clear.frame(width: Tokens.childIndent * CGFloat(row.level - 1) + 24, height: 1)
                Text(
                    [
                        Tokens.kindName(span.kind, labels: labels),
                        Tokens.tokens(span).map { $0.formatted(.number.locale(locale)) },
                        span.costMicros.map {
                            Tokens.costText($0, currency: span.currency ?? currency, locale: locale, labels: labels)
                        },
                    ].compactMap { $0 }.joined(separator: " · ")
                )
                .monarchTextStyle(.caption)
                .foregroundStyle(theme.colors.textMuted)
                .monospacedDigit()
                .lineLimit(1)
                Spacer(minLength: 0)
            }
            bar(span)
                .padding(.leading, Tokens.childIndent * CGFloat(row.level - 1) + 24)
        }
        .padding(.horizontal, Tokens.rowPadding)
    }

    private func cell(_ text: String, width: CGFloat, alignment: Alignment) -> some View {
        Text(text)
            .monarchTextStyle(.body)
            .foregroundStyle(theme.colors.textDefault)
            .monospacedDigit()
            .lineLimit(1)
            .padding(.horizontal, Tokens.rowPadding)
            .frame(width: width, alignment: alignment)
    }

    /// The span's bar on the shared scale. Leading padding mirrors in
    /// right-to-left layouts, so the bar starts at the leading edge in both.
    private func bar(_ span: MonarchAiTraceSpan) -> some View {
        let place = Tokens.bar(span, totals: totals)
        let shape = RoundedRectangle(cornerRadius: Tokens.barRadius, style: .continuous)
        return GeometryReader { proxy in
            shape
                .fill(theme.colors.soft(Tokens.barFill(span), by: MonarchBarChartTokens.softening))
                .frame(width: max(Tokens.minimumBarWidth, proxy.size.width * place.width))
                .padding(.leading, proxy.size.width * place.offset)
                .frame(maxWidth: .infinity, alignment: .leading)
        }
        .frame(height: Tokens.trackHeight)
        .background(shape.fill(theme.colors[role: Tokens.track]))
        .accessibilityHidden(true)
    }
}

/// The heading row of the trace table.
struct MonarchAiTraceViewTableHeader: View {
    let labels: MonarchAiTraceViewLabels
    @Environment(\.monarchTheme) private var theme

    private typealias Tokens = MonarchAiTraceViewTokens

    var body: some View {
        let names = [
            MonarchAiTraceViewStrings.columnSpan,
            labels.kindName ?? MonarchAiTraceViewStrings.kindName,
            labels.duration ?? MonarchAiTraceViewStrings.columnDuration,
            labels.totalTokens ?? MonarchAiTraceViewStrings.totalTokens,
            labels.totalCost ?? MonarchAiTraceViewStrings.totalCost,
            MonarchAiTraceViewStrings.columnTimeline,
        ]
        HStack(spacing: 0) {
            ForEach(Array(names.enumerated()), id: \.offset) { index, name in
                Text(name)
                    .monarchTextStyle(.caption)
                    .foregroundStyle(theme.colors.textMuted)
                    .padding(.horizontal, Tokens.rowPadding)
                    .frame(
                        width: Tokens.columns[index],
                        alignment: index == 0 || index == 1 || index == 5 ? .leading : .trailing
                    )
            }
        }
        .padding(.vertical, Tokens.rowPadding)
        .background(theme.colors.bgSubtle)
        .accessibilityHidden(true)
    }
}
