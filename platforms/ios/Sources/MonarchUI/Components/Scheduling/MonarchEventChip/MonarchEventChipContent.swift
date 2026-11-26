import SwiftUI

/// What an event chip draws, for one pressed state. ``MonarchEventChip``
/// shows it directly, or inside a button.
struct MonarchEventChipContent: View {
    let title: String
    let time: String?
    let detail: String?
    let color: Color?
    let status: MonarchEventChipStatus
    let variant: MonarchEventChipVariant
    let isPast: Bool
    let isCompact: Bool
    let statusLabel: String
    var isPressed = false

    @ScaledMetric(relativeTo: .caption) private var dotSize = MonarchEventChipTokens.dotSize
    @Environment(\.monarchTheme) private var theme
    @Environment(\.dynamicTypeSize) private var dynamicTypeSize

    var body: some View {
        let shape = MonarchEventChipTokens.shape(variant)
        let surface = MonarchEventChipTokens.surface(variant: variant, status: status, isPressed: isPressed)
        layout
            .background {
                if let base = surface.base {
                    shape.fill(theme.colors[role: base])
                }
                if surface.tint > 0 {
                    shape.fill(eventColor.opacity(surface.tint))
                }
            }
            .overlay(alignment: .leading) {
                if variant != .dot {
                    bar
                }
            }
            .clipShape(shape)
            .overlay {
                if surface.isOutlined {
                    shape.strokeBorder(eventColor, lineWidth: MonarchShapes.borderWidth)
                }
            }
    }

    // MARK: Layouts

    @ViewBuilder private var layout: some View {
        switch variant {
        case .dot: dotLayout
        case .bar: barLayout
        case .block: blockLayout
        }
    }

    private var dotLayout: some View {
        HStack(spacing: MonarchEventChipTokens.dotSpacing) {
            dot
            timeText
            titleText(weight: MonarchEventChipTokens.titleWeight)
        }
        // One line in a month cell; at the accessibility sizes the text
        // wraps instead of being cut off.
        .lineLimit(dynamicTypeSize.isAccessibilitySize ? nil : 1)
        .padding(.vertical, MonarchEventChipTokens.dotVerticalPadding)
        .padding(.horizontal, MonarchEventChipTokens.dotHorizontalPadding)
        .frame(maxWidth: .infinity, alignment: .leading)
    }

    private var barLayout: some View {
        VStack(alignment: .leading, spacing: 0) {
            ViewThatFits(in: .horizontal) {
                HStack(alignment: .firstTextBaseline, spacing: MonarchEventChipTokens.barSpacing) {
                    titleText(weight: MonarchEventChipTokens.titleWeight)
                    Spacer(minLength: 0)
                    timeText
                    statusText
                }
                VStack(alignment: .leading, spacing: 0) {
                    // In a narrow column the name shrinks onto one line, so a word is not broken in two.
                    titleText(weight: MonarchEventChipTokens.titleWeight)
                        .lineLimit(dynamicTypeSize.isAccessibilitySize ? nil : 1)
                        .minimumScaleFactor(0.6)
                    HStack(alignment: .firstTextBaseline, spacing: MonarchEventChipTokens.barSpacing) {
                        timeText
                        statusText
                    }
                }
            }
            detailText
        }
        .tintedPadding()
        .frame(maxWidth: .infinity, alignment: .leading)
    }

    @ViewBuilder private var blockLayout: some View {
        if isCompact {
            // The title wins in a narrow column: the time is dropped before the title is cut.
            ViewThatFits(in: .horizontal) {
                HStack(alignment: .firstTextBaseline, spacing: MonarchEventChipTokens.compactSpacing) {
                    timeText
                    titleText(weight: MonarchEventChipTokens.blockTitleWeight)
                }
                .fixedSize(horizontal: true, vertical: false)
                titleText(weight: MonarchEventChipTokens.blockTitleWeight)
            }
            .lineLimit(1)
            .padding(.leading, MonarchEventChipTokens.tintedLeadingPadding)
            .padding(.trailing, MonarchEventChipTokens.tintedTrailingPadding)
            .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .leading)
        } else {
            VStack(alignment: .leading, spacing: 0) {
                titleText(weight: MonarchEventChipTokens.blockTitleWeight)
                timeText
                statusText
                detailText
            }
            .tintedPadding()
            .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .topLeading)
        }
    }

    // MARK: Parts

    private var eventColor: Color {
        color ?? theme.colors[role: MonarchEventChipTokens.defaultColor]
    }

    private var markerOpacity: Double {
        isPast ? MonarchEventChipTokens.pastMarkerRatio : 1
    }

    private func titleText(weight: Font.Weight) -> some View {
        Text(title)
            .monarchTextStyle(MonarchEventChipTokens.textStyle, weight: weight)
            // A long word in a narrow column shrinks a little instead of breaking inside the word.
            .minimumScaleFactor(0.7)
            .strikethrough(MonarchEventChipTokens.isStruckThrough(status))
            .foregroundStyle(theme.colors[role: MonarchEventChipTokens.titleRole(status: status, isPast: isPast)])
    }

    @ViewBuilder private var timeText: some View {
        if let time, !time.isEmpty {
            Text(time)
                .monarchTextStyle(MonarchEventChipTokens.textStyle)
                .monospacedDigit()
                .foregroundStyle(theme.colors[role: MonarchEventChipTokens.secondaryText])
        }
    }

    @ViewBuilder private var statusText: some View {
        if MonarchEventChipTokens.showsStatus(status: status, variant: variant, isCompact: isCompact) {
            Text(statusLabel)
                .monarchTextStyle(MonarchEventChipTokens.textStyle, weight: MonarchEventChipTokens.titleWeight)
                .textCase(.uppercase)
                .foregroundStyle(theme.colors[role: MonarchEventChipTokens.secondaryText])
        }
    }

    @ViewBuilder private var detailText: some View {
        if let detail, MonarchEventChipTokens.showsDetail(detail, variant: variant, isCompact: isCompact) {
            Text(detail)
                .monarchTextStyle(MonarchEventChipTokens.textStyle)
                .foregroundStyle(theme.colors[role: MonarchEventChipTokens.secondaryText])
        }
    }

    @ViewBuilder private var dot: some View {
        let muted = theme.colors[role: MonarchEventChipTokens.mutedMarker]
        Group {
            switch MonarchEventChipTokens.marker(status: status, variant: variant) {
            case .filled:
                Circle().fill(eventColor)
            case .hollow, .dashed:
                Circle().strokeBorder(eventColor, lineWidth: MonarchEventChipTokens.dotStroke)
            case .muted:
                Circle().strokeBorder(muted, lineWidth: MonarchEventChipTokens.dotStroke)
            }
        }
        .frame(width: dotSize, height: dotSize)
        .opacity(markerOpacity)
    }

    @ViewBuilder private var bar: some View {
        let muted = theme.colors[role: MonarchEventChipTokens.mutedMarker]
        Group {
            switch MonarchEventChipTokens.marker(status: status, variant: variant) {
            case .filled:
                Rectangle().fill(eventColor)
            case .hollow, .dashed:
                MonarchEventChipBarLine().stroke(
                    eventColor,
                    style: StrokeStyle(
                        lineWidth: MonarchEventChipTokens.barWidth,
                        dash: MonarchEventChipTokens.barDash
                    )
                )
            case .muted:
                Rectangle().fill(muted)
            }
        }
        .frame(width: MonarchEventChipTokens.barWidth)
        .opacity(markerOpacity)
    }
}

/// A vertical line down the middle of its frame, for the dashed bar.
private struct MonarchEventChipBarLine: Shape {
    func path(in rect: CGRect) -> Path {
        var path = Path()
        path.move(to: CGPoint(x: rect.midX, y: rect.minY))
        path.addLine(to: CGPoint(x: rect.midX, y: rect.maxY))
        return path
    }
}

extension View {
    /// The padding of a bar or block chip: the content starts past the bar.
    fileprivate func tintedPadding() -> some View {
        padding(.vertical, MonarchEventChipTokens.tintedVerticalPadding)
            .padding(.leading, MonarchEventChipTokens.tintedLeadingPadding)
            .padding(.trailing, MonarchEventChipTokens.tintedTrailingPadding)
    }
}
