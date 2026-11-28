import SwiftUI

/// The heading of one day column: the short weekday over the date, with a
/// filled circle on today. A button when it opens the day.
struct MonarchTimeGridDayHeading: View {
    let day: Date
    let isToday: Bool
    let formats: MonarchTimeGridTokens.Formats
    let labels: MonarchTimeGridLabels
    let action: (@MainActor () -> Void)?

    @Environment(\.monarchTheme) private var theme
    @ScaledMetric(relativeTo: .subheadline) private var todaySize = MonarchTimeGridTokens.todaySize

    var body: some View {
        let longDay = formats.longDay(day)
        let label = MonarchTimeGridTokens.headingLabel(longDay: longDay, isToday: isToday, labels: labels)
        if let action {
            Button(action: action) { content.monarchMinimumHitTarget() }
                .buttonStyle(.plain)
                .accessibilityLabel(label)
                .accessibilityHint(MonarchTimeGridTokens.openDayHint(longDay: longDay, labels: labels))
        } else {
            content
                .accessibilityElement(children: .ignore)
                .accessibilityLabel(label)
                .accessibilityAddTraits(.isHeader)
        }
    }

    private var content: some View {
        VStack(spacing: MonarchTimeGridTokens.headingSpacing) {
            Text(formats.weekday(day))
                .monarchTextStyle(MonarchTimeGridTokens.captionStyle)
                .foregroundStyle(theme.colors[role: MonarchTimeGridTokens.secondaryText])
            Text(formats.dayNumber(day))
                .monarchTextStyle(MonarchTimeGridTokens.dateStyle, weight: MonarchTimeGridTokens.dateWeight)
                .monospacedDigit()
                .foregroundStyle(
                    theme.colors[role: isToday ? MonarchTimeGridTokens.todayText : MonarchTimeGridTokens.dateText]
                )
                .frame(minWidth: todaySize, minHeight: todaySize)
                .background {
                    if isToday {
                        Circle().fill(theme.colors[role: MonarchTimeGridTokens.todayFill])
                    }
                }
        }
        .lineLimit(1)
        .padding(.vertical, MonarchTimeGridTokens.blockPadding)
        .frame(maxWidth: .infinity)
    }
}

/// A hatched block of unavailable time, with its label.
struct MonarchTimeGridUnavailableBlock: View {
    let label: String?
    let spokenLabel: String

    @Environment(\.monarchTheme) private var theme

    var body: some View {
        ZStack(alignment: .topLeading) {
            Rectangle().fill(theme.colors[role: MonarchTimeGridTokens.unavailableFill])
            MonarchTimeGridHatch(spacing: MonarchTimeGridTokens.hatchSpacing)
                .stroke(
                    theme.colors[role: MonarchTimeGridTokens.unavailableHatch],
                    lineWidth: MonarchTimeGridTokens.lineWidth
                )
            if let label, !label.isEmpty {
                Text(label)
                    .monarchTextStyle(MonarchTimeGridTokens.captionStyle)
                    .foregroundStyle(theme.colors[role: MonarchTimeGridTokens.unavailableText])
                    .padding(MonarchTimeGridTokens.blockPadding)
            }
        }
        .clipped()
        .allowsHitTesting(false)
        .accessibilityElement(children: .ignore)
        .accessibilityLabel(spokenLabel)
    }
}

/// Diagonal lines across a rectangle.
struct MonarchTimeGridHatch: Shape {
    let spacing: CGFloat

    func path(in rect: CGRect) -> Path {
        var path = Path()
        guard spacing > 0 else { return path }
        var x = rect.minX
        while x < rect.maxX + rect.height {
            path.move(to: CGPoint(x: x, y: rect.minY))
            path.addLine(to: CGPoint(x: x - rect.height, y: rect.maxY))
            x += spacing
        }
        return path
    }
}

/// Where a dragged event will land: a dashed outline with the new time.
struct MonarchTimeGridGhost: View {
    let text: String
    let color: Color?

    @Environment(\.monarchTheme) private var theme

    var body: some View {
        let shape = MonarchShapes.mark
        let tint = color ?? theme.colors[role: MonarchTimeGridTokens.ghostDefault]
        Text(text)
            .monarchTextStyle(MonarchTimeGridTokens.captionStyle)
            .monospacedDigit()
            .foregroundStyle(theme.colors[role: MonarchTimeGridTokens.ghostText])
            .padding(MonarchTimeGridTokens.blockPadding)
            .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .topLeading)
            .background {
                shape.fill(theme.colors[role: MonarchTimeGridTokens.ghostFill])
                shape.fill(tint.opacity(MonarchTimeGridTokens.ghostTintRatio))
            }
            .overlay {
                shape.strokeBorder(
                    tint,
                    style: StrokeStyle(
                        lineWidth: MonarchTimeGridTokens.ghostBorderWidth,
                        dash: MonarchTimeGridTokens.ghostDash
                    )
                )
            }
            .clipShape(shape)
            .allowsHitTesting(false)
            .accessibilityHidden(true)
    }
}

/// The current time: a dot at the leading edge and a line across the column.
struct MonarchTimeGridNowLine: View {
    @Environment(\.monarchTheme) private var theme

    var body: some View {
        let color = theme.colors[role: MonarchTimeGridTokens.now]
        ZStack(alignment: .leading) {
            Rectangle().fill(color).frame(height: MonarchTimeGridTokens.nowLineWidth)
            Circle().fill(color).frame(
                width: MonarchTimeGridTokens.nowDotSize,
                height: MonarchTimeGridTokens.nowDotSize
            )
        }
        .allowsHitTesting(false)
        .accessibilityHidden(true)
    }
}
