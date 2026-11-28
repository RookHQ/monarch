import SwiftUI

/// One metered item: label, figures, meter, status line and description.
struct MonarchUsageMeterRow: View {
    let item: MonarchUsageMeterItem
    let level: MonarchUsageMeterLevel
    let labels: MonarchUsageMeterLabels

    @Environment(\.monarchTheme) private var theme
    @Environment(\.locale) private var locale

    var body: some View {
        let text = MonarchUsageMeterTokens.text(for: item, level: level, locale: locale, labels: labels)
        let roles = MonarchUsageMeterTokens.roles(for: level)
        VStack(alignment: .leading, spacing: MonarchUsageMeterTokens.itemSpacing) {
            Text(item.label)
                .monarchTextStyle(.label)
                .foregroundStyle(theme.colors.textMuted)
            figures(text.figures)
            if let limit = item.limit, level != .unlimited {
                ProgressView(value: min(max(item.used, 0), limit), total: limit)
                    .progressViewStyle(MonarchUsageMeterBarStyle(fill: roles.fill))
                    .padding(.vertical, MonarchUsageMeterTokens.textSpacing)
                HStack(alignment: .firstTextBaseline, spacing: MonarchUsageMeterTokens.iconSpacing) {
                    if let symbol = MonarchUsageMeterTokens.symbol(for: level) {
                        MonarchIcon(systemName: symbol, size: .sm)
                            .foregroundStyle(theme.colors[role: roles.status])
                    }
                    status(level: level, limit: limit, text: text)
                }
            }
            if let description = item.description {
                Text(description)
                    .monarchTextStyle(.caption)
                    .foregroundStyle(theme.colors.textMuted)
            }
        }
        .padding(MonarchUsageMeterTokens.cardPadding)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(
            RoundedRectangle(cornerRadius: MonarchUsageMeterTokens.cardRadius, style: .continuous)
                .fill(theme.colors.bgSurface)
        )
        .overlay(
            RoundedRectangle(cornerRadius: MonarchUsageMeterTokens.cardRadius, style: .continuous)
                .strokeBorder(theme.colors.borderDefault, lineWidth: 1)
        )
        .accessibilityElement(children: .ignore)
        .accessibilityLabel(item.label)
        .accessibilityValue(text.accessibilityValue)
    }

    /// The used amount large and strong, and the limit and unit after it smaller and muted, as on the web.
    private func figures(_ figures: String) -> some View {
        let used = MonarchUsageMeterTokens.number(item.used, locale: locale)
        let hasLimit = figures.hasPrefix(used) && figures.count > used.count
        return HStack(alignment: .firstTextBaseline, spacing: 0) {
            Text(hasLimit ? used : figures)
                .monarchTextStyle(.subheading, weight: .semibold)
                .foregroundStyle(theme.colors.textDefault)
            if hasLimit {
                Text(String(figures.dropFirst(used.count)))
                    .monarchTextStyle(.bodySmall)
                    .foregroundStyle(theme.colors.textMuted)
            }
        }
    }

    /// The status line: the level in the medium weight and its colour, the share used muted.
    private func status(level: MonarchUsageMeterLevel, limit: Double, text: MonarchUsageMeterTokens.ItemText)
        -> AnyView
    {
        let roles = MonarchUsageMeterTokens.roles(for: level)
        let levelText: String? =
            switch level {
            case .warning: labels.nearLimit
            case .over: labels.overLimit
            case .normal, .unlimited: nil
            }
        let percent = MonarchUsageMeterTokens.percent(used: item.used, limit: limit, locale: locale)
        let share = labels.percent(percent)
        if let line = labels.statusLine?(item.label, level == .warning || level == .over, level == .over, percent) {
            return AnyView(
                Group {
                    if !line.isEmpty {
                        Text(line)
                            .monarchTextStyle(.caption)
                            .foregroundStyle(theme.colors[role: roles.status])
                    }
                }
            )
        }
        return AnyView(
            HStack(alignment: .firstTextBaseline, spacing: 0) {
                if let levelText {
                    Text(levelText)
                        .monarchTextStyle(.caption, weight: .medium)
                        .foregroundStyle(theme.colors[role: roles.status])
                    Text(" \u{00B7} ")
                        .monarchTextStyle(.caption)
                        .foregroundStyle(theme.colors.textMuted)
                }
                Text(share)
                    .monarchTextStyle(.caption)
                    .foregroundStyle(level == .normal ? theme.colors[role: roles.status] : theme.colors.textMuted)
            }
        )
    }
}

/// The meter's bar. The shared linear progress style has one fill colour, and
/// the meter needs one per level.
private struct MonarchUsageMeterBarStyle: ProgressViewStyle {
    let fill: MonarchColorRole

    func makeBody(configuration: Configuration) -> some View {
        MonarchUsageMeterBar(fraction: configuration.fractionCompleted ?? 0, fill: fill)
    }
}

/// The track and its fill.
private struct MonarchUsageMeterBar: View {
    let fraction: Double
    let fill: MonarchColorRole

    @Environment(\.monarchTheme) private var theme

    var body: some View {
        GeometryReader { proxy in
            Capsule()
                .fill(theme.colors[role: fill])
                .frame(width: proxy.size.width * MonarchUsageMeterTokens.clamped(fraction))
                .frame(maxWidth: .infinity, alignment: .leading)
                .monarchAnimation(.base, value: fraction)
        }
        .frame(height: MonarchUsageMeterTokens.trackHeight)
        .background(Capsule().fill(theme.colors[role: MonarchUsageMeterTokens.track]))
    }
}
