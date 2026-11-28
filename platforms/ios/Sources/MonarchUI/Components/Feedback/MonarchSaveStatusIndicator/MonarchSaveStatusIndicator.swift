import SwiftUI

/// A small inline status for saving: "Saving…", "Saved · 2 minutes ago" or
/// "Couldn't save" with a retry button.
///
/// ```swift
/// MonarchSaveStatusIndicator(model.saveStatus, savedAt: model.savedAt) {
///     model.save()
/// }
/// ```
///
/// Put it in an editor's toolbar or next to a form's buttons. Nothing is
/// drawn while the status is `idle`.
///
/// Accessibility: the icon and the words are a standard `Label`, read with
/// the time as one element; the icon is decorative and the words carry the
/// meaning. When the status turns saved or error the outcome is announced;
/// "Saving…" is announced only with `announcesSaving`, so frequent autosaves
/// stay quiet. The retry button is a separate standard button with a
/// 44-point touch target.
public struct MonarchSaveStatusIndicator: View {
    private let status: MonarchSaveStatus
    private let savedAt: Date?
    private let errorMessage: String?
    private let announcesSaving: Bool
    private let labels: MonarchSaveStatusIndicatorLabels
    private let onRetry: (@MainActor () -> Void)?

    @Environment(\.monarchTheme) private var theme
    @Environment(\.locale) private var locale

    /// A save status.
    ///
    /// - Parameters:
    ///   - status: Where saving stands.
    ///   - savedAt: When the last save finished, shown as relative time after "Saved".
    ///   - errorMessage: Replaces "Couldn't save" with the reason, such as "You are offline".
    ///   - announcesSaving: Whether "Saving…" is announced too.
    ///   - labels: Replacements for the built-in strings.
    ///   - onRetry: Retries a failed save. Adds a retry button to the error status.
    public init(
        _ status: MonarchSaveStatus,
        savedAt: Date? = nil,
        errorMessage: String? = nil,
        announcesSaving: Bool = false,
        labels: MonarchSaveStatusIndicatorLabels = MonarchSaveStatusIndicatorLabels(),
        onRetry: (@MainActor () -> Void)? = nil
    ) {
        self.status = status
        self.savedAt = savedAt
        self.errorMessage = errorMessage
        self.announcesSaving = announcesSaving
        self.labels = labels
        self.onRetry = onRetry
    }

    /// The content of the view.
    public var body: some View {
        HStack(alignment: .firstTextBaseline, spacing: MonarchSaveStatusIndicatorTokens.spacing) {
            if let message = MonarchSaveStatusIndicatorTokens.message(
                status,
                errorMessage: errorMessage,
                labels: labels
            ) {
                MonarchRelativeTimeClock(dates: [savedAt]) { current in
                    line(message: message, now: current)
                }
                if MonarchSaveStatusIndicatorTokens.showsRetry(status, hasHandler: onRetry != nil), let onRetry {
                    MonarchButton(
                        labels.retry ?? MonarchSaveStatusIndicatorStrings.retry,
                        variant: .ghost,
                        size: .sm,
                        action: onRetry
                    )
                }
            }
        }
        .onChange(of: status) { old, new in
            if let message = MonarchSaveStatusIndicatorTokens.announcement(
                from: old,
                to: new,
                announcesSaving: announcesSaving,
                errorMessage: errorMessage,
                labels: labels
            ) {
                AccessibilityNotification.Announcement(message).post()
            }
        }
    }

    private func line(message: String, now: Date) -> some View {
        let time =
            MonarchSaveStatusIndicatorTokens.showsTime(status, savedAt: savedAt)
            ? savedAt.map { MonarchSaveStatusIndicatorTokens.relativeText($0, now: now, locale: locale) } : nil
        return HStack(alignment: .firstTextBaseline, spacing: MonarchSaveStatusIndicatorTokens.spacing) {
            Label {
                Text(message)
                    .foregroundStyle(theme.colors[role: MonarchSaveStatusIndicatorTokens.text(status)])
            } icon: {
                icon.foregroundStyle(theme.colors[role: MonarchSaveStatusIndicatorTokens.icon(status)])
            }
            if let time {
                Text(MonarchSaveStatusIndicatorTokens.separator)
                Text(time).monospacedDigit()
            }
        }
        .monarchTextStyle(MonarchSaveStatusIndicatorTokens.textStyle)
        .foregroundStyle(theme.colors[role: MonarchSaveStatusIndicatorTokens.muted])
        .accessibilityElement(children: .ignore)
        .accessibilityLabel(MonarchSaveStatusIndicatorTokens.accessibilityLabel(message: message, time: time))
    }

    @ViewBuilder private var icon: some View {
        if status == .saving {
            ProgressView().controlSize(.mini).tint(theme.colors[role: MonarchSaveStatusIndicatorTokens.muted])
        } else if status == .unsaved {
            Circle()
                .fill(theme.colors[role: MonarchSaveStatusIndicatorTokens.icon(status)])
                .frame(
                    width: MonarchSaveStatusIndicatorTokens.dotDiameter,
                    height: MonarchSaveStatusIndicatorTokens.dotDiameter
                )
                .padding(MonarchSaveStatusIndicatorTokens.dotDiameter)
        } else if let symbol = MonarchSaveStatusIndicatorTokens.symbol(status) {
            MonarchIcon(systemName: symbol, size: MonarchSaveStatusIndicatorTokens.iconSize)
        }
    }
}

#if DEBUG
    #Preview("MonarchSaveStatusIndicator") {
        MonarchPreviewMatrix {
            VStack(alignment: .leading) {
                MonarchSaveStatusIndicator(.unsaved)
                MonarchSaveStatusIndicator(.saving)
                MonarchSaveStatusIndicator(.saved, savedAt: .now.addingTimeInterval(-120))
                MonarchSaveStatusIndicator(.error) {}
            }
        }
    }
#endif
