import SwiftUI

/// One step of the approval timeline: its marker on the rail, the label and
/// status, who decided and when, who decides next, the comment and the note.
struct MonarchApprovalTimelineRow: View {
    let step: MonarchApprovalTimelineStep
    let now: Date
    let absoluteAfter: TimeInterval
    let calendar: Calendar
    let showsAvatars: Bool
    let isLast: Bool
    let labels: MonarchApprovalTimelineLabels

    @Environment(\.monarchTheme) private var theme
    @Environment(\.locale) private var locale
    @ScaledMetric(relativeTo: .body) private var markerSide = MonarchApprovalTimelineTokens.markerSide

    private typealias Tokens = MonarchApprovalTimelineTokens

    var body: some View {
        HStack(alignment: .top, spacing: Tokens.columnSpacing) {
            marker
            content
        }
        .padding(.bottom, isLast ? 0 : Tokens.stepSpacing)
        .background(alignment: .topLeading) {
            if !isLast {
                // Leading padding, so the rail stays under the marker in right-to-left layouts.
                Capsule()
                    .fill(theme.colors[role: Tokens.railRole(step.status)])
                    .frame(width: Tokens.railWidth)
                    .padding(.top, markerSide + Tokens.railGap)
                    .padding(.bottom, Tokens.railGap)
                    .padding(.leading, (markerSide - Tokens.railWidth) / 2)
            }
        }
        .accessibilityElement(children: .ignore)
        .accessibilityLabel(
            Tokens.accessibilityLabel(
                step,
                time: step.date.map { Tokens.absoluteTime($0, calendar: calendar, locale: locale) },
                labels: labels
            )
        )
    }

    private var marker: some View {
        let style = Tokens.marker(step.status)
        return ZStack {
            Circle().fill(theme.colors[role: style.fill])
            Circle()
                .strokeBorder(
                    theme.colors[role: style.border],
                    style: StrokeStyle(
                        lineWidth: Tokens.markerBorderWidth,
                        dash: style.isDashed ? Tokens.markerDash : []
                    )
                )
            if let systemImage = style.systemImage {
                Image(systemName: systemImage)
                    .resizable()
                    .scaledToFit()
                    .fontWeight(.bold)
                    .foregroundStyle(theme.colors[role: style.symbol])
                    .padding(markerSide * Tokens.glyphInsetRatio)
            }
        }
        .frame(width: markerSide, height: markerSide)
        .accessibilityHidden(true)
    }

    private var content: some View {
        VStack(alignment: .leading, spacing: Tokens.lineSpacing) {
            MonarchFlowLayout(spacing: Tokens.metaSpacing * 2, rowSpacing: 0, alignment: .firstTextBaseline) {
                Text(step.label)
                    .monarchTextStyle(.bodyStrong)
                    .foregroundStyle(theme.colors[role: Tokens.labelRole(step.status)])
                Text(labels.status(step.status))
                    .monarchTextStyle(.caption, weight: Tokens.isStatusEmphasised(step.status) ? .semibold : .regular)
                    .foregroundStyle(theme.colors[role: Tokens.statusRole(step.status)])
            }
            if Tokens.isDecided(step.status), step.actor != nil || step.date != nil {
                decisionLine
            }
            if let line = Tokens.approversLine(step, labels: labels) {
                metaText(line)
            }
            if let line = Tokens.delegationLine(step, labels: labels) {
                metaText(line)
            }
            if let comment = step.comment, !comment.isEmpty {
                Text(comment)
                    .monarchTextStyle(.caption)
                    .foregroundStyle(theme.colors.textDefault)
                    .multilineTextAlignment(.leading)
                    .padding(.horizontal, Tokens.commentHorizontalPadding)
                    .padding(.vertical, Tokens.commentVerticalPadding)
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .background(
                        RoundedRectangle(cornerRadius: Tokens.commentRadius, style: .continuous)
                            .fill(theme.colors.bgMuted)
                    )
            }
            if let note = step.note, !note.isEmpty {
                metaText(note)
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
    }

    private var decisionLine: some View {
        MonarchFlowLayout(spacing: Tokens.metaSpacing) {
            if let actor = step.actor {
                if showsAvatars {
                    MonarchAvatar(
                        url: actor.avatarURL,
                        name: actor.name,
                        seed: actor.seed,
                        size: .sm,
                        isDecorative: true
                    )
                }
                metaText(actor.name)
            }
            if step.actor != nil, step.date != nil {
                metaText(Tokens.metaDot)
            }
            if let date = step.date {
                metaText(
                    Tokens.timeText(date, now: now, absoluteAfter: absoluteAfter, calendar: calendar, locale: locale)
                )
            }
        }
    }

    private func metaText(_ text: String) -> some View {
        Text(text)
            .monarchTextStyle(.caption)
            .foregroundStyle(theme.colors.textMuted)
            .multilineTextAlignment(.leading)
    }
}
