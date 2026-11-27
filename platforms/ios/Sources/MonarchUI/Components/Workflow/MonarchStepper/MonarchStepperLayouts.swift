import SwiftUI

/// The steps in a row, joined by lines at the height of the markers.
struct MonarchStepperRow: View {
    let items: [MonarchStepperItem]
    /// Each name under its marker, so that the row fits an iPhone column.
    var isStacked = false

    @Environment(\.monarchTheme) private var theme
    @ScaledMetric(relativeTo: .caption) private var markerSize = MonarchStepperTokens.markerSize

    var body: some View {
        HStack(alignment: .top, spacing: MonarchStepperTokens.rowSpacing) {
            ForEach(items, id: \.step.id) { item in
                if isStacked { item.stacked() } else { item }
                if item.index < items.count - 1 {
                    Capsule()
                        .fill(theme.colors[role: MonarchStepperTokens.connector(after: item.status)])
                        .frame(minWidth: isStacked ? 8 : MonarchStepperTokens.connectorLength, maxWidth: .infinity)
                        .frame(height: MonarchStepperTokens.connectorThickness)
                        .padding(.top, connectorOffset)
                        .accessibilityHidden(true)
                }
            }
        }
    }

    /// The distance from the top of a step to the top of the line, which
    /// runs through the middle of the marker.
    private var connectorOffset: CGFloat {
        MonarchStepperTokens.itemPadding + (markerSize - MonarchStepperTokens.connectorThickness) / 2
    }
}

/// The steps in a column, joined by lines under the markers.
struct MonarchStepperColumn: View {
    let items: [MonarchStepperItem]

    @Environment(\.monarchTheme) private var theme
    @ScaledMetric(relativeTo: .caption) private var markerSize = MonarchStepperTokens.markerSize

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            ForEach(items, id: \.step.id) { item in
                item
                if item.index < items.count - 1 {
                    Capsule()
                        .fill(theme.colors[role: MonarchStepperTokens.connector(after: item.status)])
                        .frame(
                            width: MonarchStepperTokens.connectorThickness,
                            height: MonarchStepperTokens.connectorLength
                        )
                        .padding(.vertical, MonarchStepperTokens.connectorMargin)
                        .padding(.leading, connectorOffset)
                        .accessibilityHidden(true)
                }
            }
        }
    }

    /// The distance from the leading edge of a step to the line, which
    /// runs under the middle of the marker.
    private var connectorOffset: CGFloat {
        MonarchStepperTokens.itemPadding + (markerSize - MonarchStepperTokens.connectorThickness) / 2
    }
}

/// The compact form: the position and name of the current step above a
/// progress bar.
struct MonarchStepperCompact: View {
    let steps: [MonarchStepperStep]
    let currentIndex: Int
    var isBusy = false
    let labels: MonarchStepperLabels

    @Environment(\.monarchTheme) private var theme

    var body: some View {
        VStack(alignment: .leading, spacing: MonarchStepperTokens.compactSpacing) {
            ViewThatFits(in: .horizontal) {
                HStack(alignment: .firstTextBaseline, spacing: MonarchStepperTokens.compactSpacing) {
                    position
                    name
                }
                VStack(alignment: .leading, spacing: MonarchStepperTokens.textSpacing) {
                    position
                    name
                }
            }
            ProgressView(value: MonarchStepperTokens.fraction(currentIndex: currentIndex, count: steps.count))
                .progressViewStyle(.monarchLinear)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .accessibilityElement(children: .ignore)
        .accessibilityLabel(labels.progress ?? MonarchStepperStrings.progress)
        .accessibilityValue(
            MonarchStepperTokens.compactValue(currentIndex: currentIndex, steps: steps, isBusy: isBusy, labels: labels)
        )
    }

    /// The step the summary names: the last one when every step is done.
    private var shownIndex: Int { min(currentIndex, steps.count - 1) }

    private var position: some View {
        Text(MonarchStepperTokens.stepOf(shownIndex + 1, total: steps.count, labels: labels))
            .monarchTextStyle(MonarchStepperTokens.detailStyle, weight: MonarchStepperTokens.regularWeight)
            .foregroundStyle(theme.colors.textMuted)
    }

    @ViewBuilder private var name: some View {
        if steps.indices.contains(shownIndex) {
            Text(steps[shownIndex].label)
                .monarchTextStyle(MonarchStepperTokens.labelStyle, weight: MonarchStepperTokens.strongWeight)
                .foregroundStyle(theme.colors.textDefault)
        }
    }
}
