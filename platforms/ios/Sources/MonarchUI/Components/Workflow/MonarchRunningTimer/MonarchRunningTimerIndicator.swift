import SwiftUI

/// The indicator form of ``MonarchRunningTimer``: a chip with the clock,
/// a stop button and a menu, for a toolbar or header.
struct MonarchRunningTimerIndicator: View {
    let isRunning: Bool
    let seconds: Int
    let label: String
    let isLongRunning: Bool
    let failure: String?
    let labels: MonarchRunningTimerLabels
    let actions: MonarchRunningTimerActions

    @Environment(\.monarchTheme) private var theme
    @Environment(\.locale) private var locale

    var body: some View {
        let appearance = MonarchRunningTimerTokens.indicatorAppearance(
            isRunning: isRunning,
            isLongRunning: isLongRunning,
            hasFailed: failure != nil
        )
        HStack(spacing: 0) {
            clock
            if let stop = actions.stop {
                separator
                Button(action: stop) {
                    part {
                        if actions.busy == .stop {
                            ProgressView().controlSize(.mini)
                        } else {
                            MonarchIcon(systemName: "stop.fill", size: .sm)
                        }
                    }
                }
                .buttonStyle(.plain)
                .disabled(actions.isBusy)
                .accessibilityLabel(MonarchRunningTimerStrings.stopAndLog(labels))
            }
            if actions.hasMenu {
                separator
                Menu {
                    if let stopEarlier = actions.stopEarlier {
                        Button(MonarchRunningTimerStrings.stoppedEarlier(labels), action: stopEarlier)
                    }
                    if let discard = actions.discard {
                        Button(MonarchRunningTimerStrings.discard(labels), role: .destructive, action: discard)
                    }
                } label: {
                    part { MonarchIcon(systemName: "chevron.down", size: .sm) }
                }
                .buttonStyle(.plain)
                .disabled(actions.isBusy)
                .accessibilityLabel(MonarchRunningTimerStrings.moreActions(labels))
            }
        }
        .fixedSize(horizontal: false, vertical: true)
        .monarchTextStyle(MonarchRunningTimerTokens.smallStyle)
        .monarchSurface(appearance, in: MonarchShapes.control)
    }

    @ViewBuilder private var clock: some View {
        let content = ViewThatFits(in: .horizontal) {
            readout(showsLabel: true)
            readout(showsLabel: false)
        }
        if let open = actions.open {
            Button(action: open) { content }
                .buttonStyle(.plain)
                .accessibilityElement(children: .ignore)
                .modifier(clockAccessibility)
                .accessibilityHint(MonarchRunningTimerStrings.openHint(labels))
                .accessibilityAddTraits(.isButton)
        } else {
            content
                .accessibilityElement(children: .ignore)
                .modifier(clockAccessibility)
        }
    }

    private var clockAccessibility: MonarchRunningTimerClockAccessibility {
        MonarchRunningTimerClockAccessibility(
            label: MonarchRunningTimerTokens.clockAccessibilityLabel(label: label, seconds: seconds, locale: locale),
            value: MonarchRunningTimerTokens.clockAccessibilityValue(
                isRunning: isRunning,
                failure: failure,
                labels: labels
            ),
            isRunning: isRunning
        )
    }

    private func readout(showsLabel: Bool) -> some View {
        part {
            HStack(spacing: MonarchRunningTimerTokens.indicatorSpacing) {
                MonarchIcon(
                    systemName: MonarchRunningTimerTokens.indicatorSymbol(
                        isLongRunning: isRunning && isLongRunning,
                        hasFailed: failure != nil
                    ),
                    size: .sm
                )
                if showsLabel {
                    Text(label)
                        .lineLimit(1)
                }
                Text(MonarchRunningTimerTokens.clockText(seconds))
                    .monospacedDigit()
                    .fixedSize()
            }
        }
    }

    private func part(@ViewBuilder _ content: () -> some View) -> some View {
        content()
            .padding(.horizontal, MonarchRunningTimerTokens.indicatorPadding)
            .monarchMinimumHitTarget()
    }

    private var separator: some View {
        Rectangle()
            .fill(theme.colors.borderSubtle)
            .frame(width: MonarchShapes.borderWidth)
            .accessibilityHidden(true)
    }
}

/// The label, value and traits VoiceOver reads for the indicator's clock.
private struct MonarchRunningTimerClockAccessibility: ViewModifier {
    let label: String
    let value: String
    let isRunning: Bool

    func body(content: Content) -> some View {
        content
            .accessibilityLabel(label)
            .accessibilityValue(value)
            .accessibilityAddTraits(isRunning ? .updatesFrequently : [])
    }
}
