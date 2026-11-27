import SwiftUI

/// The panel form of ``MonarchRunningTimer``: the clock and label, the
/// fields, the buttons, and the warning and error.
struct MonarchRunningTimerPanel<Fields: View>: View {
    let isRunning: Bool
    let seconds: Int
    let label: String?
    let isLongRunning: Bool
    let warnAfter: TimeInterval?
    let failure: String?
    let labels: MonarchRunningTimerLabels
    let actions: MonarchRunningTimerActions
    let fields: Fields

    @Environment(\.monarchTheme) private var theme
    @Environment(\.locale) private var locale

    var body: some View {
        VStack(alignment: .leading, spacing: MonarchRunningTimerTokens.panelSpacing) {
            readout
            if Fields.self != EmptyView.self {
                fields
            }
            buttons
            if isLongRunning, let warnAfter {
                message(
                    MonarchRunningTimerStrings.longRunning(
                        MonarchRunningTimerTokens.warningDuration(warnAfter),
                        labels
                    ),
                    systemImage: "exclamationmark.triangle",
                    role: \.warningStrong
                )
            }
            if let failure {
                message(failure, systemImage: "exclamationmark.circle", role: \.dangerStrong)
            }
        }
        .padding(MonarchRunningTimerTokens.panelPadding)
        .frame(maxWidth: .infinity, alignment: .leading)
        .monarchSurface(MonarchRunningTimerTokens.panelAppearance, in: MonarchRunningTimerTokens.panelShape)
    }

    private var readout: some View {
        VStack(alignment: .leading) {
            Text(MonarchRunningTimerTokens.clockText(seconds))
                .monarchTextStyle(MonarchRunningTimerTokens.clockStyle)
                .monospacedDigit()
                .foregroundStyle(theme.colors[role: MonarchRunningTimerTokens.clockForeground(isRunning: isRunning)])
            if let label, !label.isEmpty {
                Text(label)
                    .monarchTextStyle(MonarchRunningTimerTokens.smallStyle)
                    .foregroundStyle(theme.colors.textMuted)
                    .lineLimit(1)
            }
        }
        .accessibilityElement(children: .ignore)
        .accessibilityLabel(
            MonarchRunningTimerTokens.clockAccessibilityLabel(label: label, seconds: seconds, locale: locale)
        )
        .accessibilityValue(MonarchRunningTimerTokens.stateText(isRunning: isRunning, labels: labels))
        .accessibilityAddTraits(isRunning ? .updatesFrequently : [])
    }

    @ViewBuilder private var buttons: some View {
        let busy = actions.busy
        MonarchFlowLayout(spacing: MonarchRunningTimerTokens.actionSpacing) {
            if let stop = actions.stop {
                MonarchButton(
                    MonarchRunningTimerStrings.stop(labels),
                    systemImage: "stop.fill",
                    variant: .danger,
                    isLoading: busy == .stop
                ) { stop() }
                .disabled(actions.isBusy && busy != .stop)
            }
            if let stopEarlier = actions.stopEarlier {
                MonarchButton(MonarchRunningTimerStrings.stoppedEarlier(labels)) { stopEarlier() }
                    .disabled(actions.isBusy)
            }
            if let discard = actions.discard {
                Button(action: discard) {
                    Label(MonarchRunningTimerStrings.discard(labels), systemImage: "trash")
                        .labelStyle(.iconOnly)
                }
                .buttonStyle(.monarch(.ghost))
                .disabled(actions.isBusy)
            }
            if let start = actions.start {
                MonarchButton(
                    MonarchRunningTimerStrings.start(labels),
                    systemImage: "play.fill",
                    variant: .primary,
                    isLoading: busy == .start || (actions.isPending && busy == nil)
                ) { start() }
                .disabled(!actions.isStartEnabled)
            }
        }
    }

    private func message(_ text: String, systemImage: String, role: MonarchColorRole) -> some View {
        HStack(alignment: .firstTextBaseline, spacing: MonarchRunningTimerTokens.messageSpacing) {
            MonarchIcon(systemName: systemImage, size: .sm)
            Text(text)
                .frame(maxWidth: .infinity, alignment: .leading)
        }
        .monarchTextStyle(MonarchRunningTimerTokens.smallStyle)
        .foregroundStyle(theme.colors[role: role])
        .accessibilityElement(children: .combine)
    }
}
