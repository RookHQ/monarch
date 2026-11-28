import SwiftUI

/// The round marker of a step: its number, a checkmark or an exclamation
/// mark. It is decorative; the step's label and value carry its meaning.
struct MonarchStepperMarker: View {
    let status: MonarchStepperStatus
    let position: Int
    var isBusy = false

    @Environment(\.monarchTheme) private var theme
    @Environment(\.locale) private var locale
    @ScaledMetric(relativeTo: .caption) private var size = MonarchStepperTokens.markerSize

    var body: some View {
        let appearance = MonarchStepperTokens.marker(for: status)
        ZStack {
            if isBusy {
                // A still ring under Reduce Motion is drawn by ProgressView itself.
                ProgressView()
                    .controlSize(.mini)
                    .tint(theme.colors[role: appearance.foreground])
            } else if let symbol = MonarchStepperTokens.symbol(for: status) {
                MonarchIcon(systemName: symbol, size: .sm)
            } else {
                Text(MonarchStepperTokens.markerText(position: position, locale: locale))
                    .monarchTextStyle(MonarchStepperTokens.detailStyle, weight: MonarchStepperTokens.strongWeight)
                    .monospacedDigit()
                    .lineLimit(1)
                    .minimumScaleFactor(MonarchStepperMarker.smallestNumberScale)
            }
        }
        .foregroundStyle(theme.colors[role: appearance.foreground])
        .frame(width: size, height: size)
        .background {
            if let background = appearance.background {
                Circle().fill(theme.colors[role: background])
            }
        }
        .overlay {
            if let border = appearance.border {
                Circle().strokeBorder(theme.colors[role: border], lineWidth: MonarchStepperTokens.markerBorderWidth)
            }
        }
        .background {
            if let ring = MonarchStepperTokens.ring(for: status) {
                Circle()
                    .fill(theme.colors[role: ring])
                    .padding(-MonarchStepperTokens.ringWidth)
            }
        }
        .monarchAnimation(.fast, value: status)
        .accessibilityHidden(true)
    }

    /// How far a number of several digits may shrink to stay inside the
    /// marker.
    private static let smallestNumberScale: CGFloat = 0.5
}
