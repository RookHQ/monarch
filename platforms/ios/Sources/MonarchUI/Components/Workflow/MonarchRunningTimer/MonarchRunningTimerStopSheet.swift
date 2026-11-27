import SwiftUI

/// The sheet of "I stopped earlier": the start, a date and time picker
/// between the start and now, the length of work that will be saved, and
/// Cancel and Save.
struct MonarchRunningTimerStopSheet: View {
    let startedAt: Date
    let now: Date
    let isSaving: Bool
    let failure: String?
    let labels: MonarchRunningTimerLabels
    let onCancel: @MainActor () -> Void
    let onSave: @MainActor (Date) -> Void

    @State private var stoppedAt: Date

    @Environment(\.monarchTheme) private var theme
    @Environment(\.locale) private var locale
    @Environment(\.timeZone) private var timeZone

    init(
        startedAt: Date,
        now: Date,
        isSaving: Bool,
        failure: String?,
        labels: MonarchRunningTimerLabels,
        onCancel: @escaping @MainActor () -> Void,
        onSave: @escaping @MainActor (Date) -> Void
    ) {
        self.startedAt = startedAt
        self.now = now
        self.isSaving = isSaving
        self.failure = failure
        self.labels = labels
        self.onCancel = onCancel
        self.onSave = onSave
        _stoppedAt = State(initialValue: MonarchRunningTimerTokens.suggestedStop(startedAt: startedAt, now: now))
    }

    var body: some View {
        let error = MonarchRunningTimerTokens.stopError(stoppedAt, startedAt: startedAt, now: now, labels: labels)
        ScrollView {
            VStack(alignment: .leading, spacing: MonarchRunningTimerTokens.sheetSpacing) {
                Text(MonarchRunningTimerStrings.stoppedEarlierTitle(labels))
                    .monarchTextStyle(.heading)
                    .accessibilityAddTraits(.isHeader)
                Text(MonarchRunningTimerStrings.stoppedEarlierMessage(labels))
                    .monarchTextStyle(.body)
                    .foregroundStyle(theme.colors.textMuted)
                Text(
                    MonarchRunningTimerStrings.started(
                        MonarchRunningTimerTokens.startText(startedAt, locale: locale, timeZone: timeZone),
                        labels
                    )
                )
                .monarchTextStyle(.body)
                DatePicker(
                    MonarchRunningTimerStrings.stoppedAt(labels),
                    selection: $stoppedAt,
                    in: MonarchRunningTimerTokens.stopRange(startedAt: startedAt, now: now),
                    displayedComponents: [.date, .hourAndMinute]
                )
                .monarchTextStyle(.body)
                .disabled(isSaving)
                if let error {
                    message(error, systemImage: "exclamationmark.circle", role: \.dangerStrong)
                } else {
                    Text(
                        MonarchRunningTimerStrings.workDuration(
                            MonarchRunningTimerTokens.hoursMinutes(stoppedAt.timeIntervalSince(startedAt)),
                            labels
                        )
                    )
                    .monarchTextStyle(.caption)
                    .foregroundStyle(theme.colors.textMuted)
                }
                if let failure, !isSaving {
                    message(failure, systemImage: "exclamationmark.triangle", role: \.dangerStrong)
                }
                MonarchFlowLayout(spacing: MonarchRunningTimerTokens.actionSpacing) {
                    MonarchButton(MonarchRunningTimerStrings.cancel(labels)) { onCancel() }
                        .disabled(isSaving)
                    MonarchButton(
                        MonarchRunningTimerStrings.save(labels),
                        variant: .primary,
                        isLoading: isSaving
                    ) {
                        if error == nil { onSave(stoppedAt) }
                    }
                    .disabled(error != nil)
                }
            }
            .padding(MonarchRunningTimerTokens.sheetPadding)
            .frame(maxWidth: .infinity, alignment: .leading)
        }
        .background(theme.colors.bgSurface)
        .presentationDetents([.medium, .large])
        .interactiveDismissDisabled(isSaving)
    }

    private func message(_ text: String, systemImage: String, role: MonarchColorRole) -> some View {
        HStack(alignment: .firstTextBaseline, spacing: MonarchRunningTimerTokens.messageSpacing) {
            MonarchIcon(systemName: systemImage, size: .sm)
            Text(text)
        }
        .monarchTextStyle(.caption)
        .foregroundStyle(theme.colors[role: role])
        .accessibilityElement(children: .combine)
    }
}
