import SwiftUI

/// One event of an activity timeline: its marker on the line, the sentence,
/// the time and tags, the body and the details.
struct MonarchActivityTimelineRow: View {
    let event: MonarchActivityTimelineEvent
    let now: Date
    let calendar: Calendar
    let marker: MonarchActivityTimelineMarker
    let density: MonarchActivityTimelineDensity
    let isLast: Bool
    let labels: MonarchActivityTimelineLabels
    @Binding var isExpanded: Bool

    @Environment(\.monarchTheme) private var theme
    @Environment(\.locale) private var locale
    @ScaledMetric(relativeTo: .body) private var column = MonarchActivityTimelineTokens.markerColumn
    @ScaledMetric(relativeTo: .body) private var dot = MonarchActivityTimelineTokens.dotSide

    var body: some View {
        HStack(alignment: .top, spacing: MonarchActivityTimelineTokens.columnSpacing) {
            markerView
                .frame(width: column, height: column)
                .accessibilityHidden(true)
            VStack(alignment: .leading, spacing: MonarchActivityTimelineTokens.lineSpacing) {
                summary
                if let details = event.details, !details.isEmpty {
                    detailsView(details)
                }
            }
            .frame(maxWidth: .infinity, alignment: .leading)
        }
        .padding(.bottom, isLast ? 0 : MonarchActivityTimelineTokens.rowSpacing(density))
        .background(alignment: .leading) {
            if !isLast {
                // Leading padding, so the line stays under the markers in right-to-left layouts.
                Rectangle()
                    .fill(theme.colors[role: MonarchActivityTimelineTokens.lineRole])
                    .frame(width: MonarchActivityTimelineTokens.lineWidth)
                    .padding(.top, column)
                    .padding(.leading, (column - MonarchActivityTimelineTokens.lineWidth) / 2)
            }
        }
    }

    /// The Details toggle, as on the web: an accent link that reads "Hide details" while open, over a
    /// tinted block that lines labels up with their values.
    private func detailsView(_ details: String) -> some View {
        VStack(alignment: .leading, spacing: MonarchActivityTimelineTokens.lineSpacing) {
            Button {
                isExpanded.toggle()
            } label: {
                HStack(spacing: MonarchActivityTimelineTokens.lineSpacing) {
                    Text(
                        isExpanded
                            ? MonarchActivityTimelineStrings.hideDetails : MonarchActivityTimelineStrings.showDetails
                    )
                    .monarchTextStyle(.caption)
                    MonarchIcon(systemName: isExpanded ? "chevron.up" : "chevron.down", size: .sm)
                }
                .foregroundStyle(theme.colors.textLink)
                .frame(minHeight: MonarchAccessibility.minimumHitTarget, alignment: .leading)
                .contentShape(Rectangle())
            }
            .buttonStyle(.plain)
            .accessibilityLabel(labels.details ?? MonarchActivityTimelineStrings.details)
            .accessibilityValue(
                isExpanded ? MonarchActivityTimelineStrings.hideDetails : MonarchActivityTimelineStrings.showDetails
            )
            if isExpanded {
                let rows = details.split(separator: "\n", omittingEmptySubsequences: true).map { line in
                    let parts = line.split(separator: "\t", maxSplits: 1, omittingEmptySubsequences: false)
                    return (label: parts.count > 1 ? String(parts[0]) : nil, value: String(parts.last ?? ""))
                }
                Grid(
                    alignment: .topLeading,
                    horizontalSpacing: MonarchActivityTimelineTokens.metaSpacing * 2,
                    verticalSpacing: 2
                ) {
                    ForEach(Array(rows.enumerated()), id: \.offset) { _, row in
                        GridRow {
                            if let label = row.label {
                                Text(label).foregroundStyle(theme.colors.textMuted)
                            }
                            Text(row.value).foregroundStyle(theme.colors.textDefault)
                                .gridColumnAlignment(.leading)
                        }
                        .monarchTextStyle(.caption)
                    }
                }
                .frame(maxWidth: .infinity, alignment: .leading)
                .padding(MonarchActivityTimelineTokens.metaSpacing * 2)
                .background(
                    MonarchShapes.control.fill(theme.colors.bgSubtle)
                )
                .textSelection(.enabled)
            }
        }
    }

    private var summary: some View {
        let time = MonarchActivityTimelineTokens.relativeTime(event.time, now: now, calendar: calendar, locale: locale)
        let automated = labels.automated ?? MonarchActivityTimelineStrings.automated
        return VStack(alignment: .leading, spacing: MonarchActivityTimelineTokens.lineSpacing) {
            Text(MonarchActivityTimelineTokens.sentence(event, theme: theme))
                .monarchTextStyle(.bodySmall)
                .foregroundStyle(theme.colors.textDefault)
                .multilineTextAlignment(.leading)
            MonarchFlowLayout(spacing: MonarchActivityTimelineTokens.metaSpacing) {
                Text(time)
                    .monarchTextStyle(.caption)
                    .foregroundStyle(theme.colors.textMuted)
                if MonarchActivityTimelineTokens.isAutomated(event.actor) {
                    MonarchTag(automated, size: .sm)
                }
                ForEach(Array(event.tags.enumerated()), id: \.offset) { _, tag in
                    MonarchTag(tag, size: .sm)
                }
            }
            if let body = event.body, !body.isEmpty {
                // A bordered bubble, as the web draws a comment.
                Text(body)
                    .monarchTextStyle(.body)
                    .foregroundStyle(theme.colors.textDefault)
                    .multilineTextAlignment(.leading)
                    .padding(.horizontal, MonarchActivityTimelineTokens.metaSpacing * 2)
                    .padding(.vertical, MonarchActivityTimelineTokens.metaSpacing)
                    .overlay(
                        MonarchShapes.control
                            .strokeBorder(theme.colors.borderSubtle, lineWidth: MonarchShapes.borderWidth)
                    )
            }
        }
        .accessibilityElement(children: .ignore)
        .accessibilityLabel(
            MonarchActivityTimelineTokens.accessibilityLabel(
                event,
                time: MonarchActivityTimelineTokens.absoluteTime(event.time, calendar: calendar, locale: locale),
                automated: automated
            )
        )
    }

    @ViewBuilder private var markerView: some View {
        if let systemImage = event.systemImage {
            let roles = MonarchActivityTimelineTokens.iconRoles(event.tone)
            MonarchIcon(systemName: systemImage, size: .sm)
                .foregroundStyle(theme.colors[role: roles.symbol])
                .frame(width: column, height: column)
                .background(Circle().fill(theme.colors[role: roles.fill]))
        } else if marker == .avatar, let actor = event.actor {
            MonarchAvatar(url: actor.avatarURL, name: actor.name, seed: actor.seed, size: .sm, isDecorative: true)
        } else {
            Circle()
                .fill(theme.colors[role: MonarchActivityTimelineTokens.dotRole(event.tone)])
                .frame(width: dot, height: dot)
        }
    }
}
