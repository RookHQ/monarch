import SwiftUI

/// A switch for whether automatic assignment may give someone new work.
///
/// It is a standard `Toggle` whose label is a status word ("Available" or
/// "Away") with a mark, and an optional line that explains the effect. The
/// app saves the change: set `isPending` while it is being saved, and roll
/// the binding back if saving fails.
///
/// ```swift
/// MonarchAvailabilityToggle(isAvailable: $isAvailable, isPending: isSaving, showsHint: true)
/// ```
///
/// Do not show it while the value is not known yet. Disable it with the
/// standard `disabled(_:)` modifier when the person may not change it.
///
/// Accessibility: VoiceOver reads the standard switch, named "Available for
/// new work", with the system's on and off values. The explanation is the
/// hint, and while pending the hint starts with "Saving". The mark is
/// decorative: it is filled when available and a ring when away, and the
/// status word says the same, so nothing depends on colour. The row is at
/// least 44 points high and its text wraps at large sizes.
public struct MonarchAvailabilityToggle: View {
    @Binding private var isAvailable: Bool
    private let isPending: Bool
    private let label: String?
    private let availableLabel: String?
    private let awayLabel: String?
    private let availableHint: String?
    private let awayHint: String?
    private let pendingLabel: String?
    private let showsHint: Bool

    @ScaledMetric(relativeTo: .body) private var markSize = MonarchAvailabilityToggleTokens.markSize
    @Environment(\.monarchTheme) private var theme
    @Environment(\.isEnabled) private var isEnabled

    /// An availability toggle.
    ///
    /// - Parameters:
    ///   - isAvailable: Whether the person is available for new work.
    ///   - isPending: Whether a change is being saved. The switch is disabled meanwhile.
    ///   - label: Replaces the built-in spoken name "Available for new work".
    ///   - availableLabel: Replaces the built-in status word "Available".
    ///   - awayLabel: Replaces the built-in status word "Away".
    ///   - availableHint: Replaces the built-in explanation shown when available.
    ///   - awayHint: Replaces the built-in explanation shown when away.
    ///   - pendingLabel: Replaces the built-in spoken word "Saving".
    ///   - showsHint: Whether the explanation is shown under the status word.
    public init(
        isAvailable: Binding<Bool>,
        isPending: Bool = false,
        label: String? = nil,
        availableLabel: String? = nil,
        awayLabel: String? = nil,
        availableHint: String? = nil,
        awayHint: String? = nil,
        pendingLabel: String? = nil,
        showsHint: Bool = false
    ) {
        _isAvailable = isAvailable
        self.isPending = isPending
        self.label = label
        self.availableLabel = availableLabel
        self.awayLabel = awayLabel
        self.availableHint = availableHint
        self.awayHint = awayHint
        self.pendingLabel = pendingLabel
        self.showsHint = showsHint
    }

    private var status: String {
        isAvailable
            ? (availableLabel ?? MonarchAvailabilityToggleStrings.available)
            : (awayLabel ?? MonarchAvailabilityToggleStrings.away)
    }

    private var hint: String? {
        guard showsHint else { return nil }
        return isAvailable
            ? (availableHint ?? MonarchAvailabilityToggleStrings.availableHint)
            : (awayHint ?? MonarchAvailabilityToggleStrings.awayHint)
    }

    /// The content of the view.
    public var body: some View {
        Toggle(isOn: $isAvailable) {
            HStack(spacing: MonarchAvailabilityToggleTokens.pendingSpacing) {
                text
                if isPending {
                    ProgressView()
                        .controlSize(.small)
                        .accessibilityHidden(true)
                }
            }
        }
        .disabled(isPending)
        .frame(minHeight: MonarchAccessibility.minimumHitTarget)
        .accessibilityLabel(label ?? MonarchAvailabilityToggleStrings.label)
        .accessibilityHint(
            MonarchAvailabilityToggleTokens.accessibilityHint(
                hint: hint,
                pending: isPending ? (pendingLabel ?? MonarchAvailabilityToggleStrings.pending) : nil
            )
        )
    }

    private var text: some View {
        VStack(alignment: .leading, spacing: MonarchAvailabilityToggleTokens.textSpacing) {
            HStack(alignment: .firstTextBaseline, spacing: MonarchAvailabilityToggleTokens.markSpacing) {
                mark
                Text(status)
                    .monarchTextStyle(.label)
                    .foregroundStyle(isEnabled ? theme.colors.textDefault : theme.colors.textDisabled)
            }
            if let hint {
                Text(hint)
                    .monarchTextStyle(.caption)
                    .foregroundStyle(isEnabled ? theme.colors.textMuted : theme.colors.textDisabled)
            }
        }
        .multilineTextAlignment(.leading)
        .fixedSize(horizontal: false, vertical: true)
    }

    /// A filled dot when available, a ring when away.
    @ViewBuilder private var mark: some View {
        let color = theme.colors[
            role: MonarchAvailabilityToggleTokens.mark(isAvailable: isAvailable, isEnabled: isEnabled)
        ]
        if isAvailable {
            Circle().fill(color).frame(width: markSize, height: markSize)
        } else {
            Circle()
                .strokeBorder(color, lineWidth: markSize * MonarchAvailabilityToggleTokens.ringRatio)
                .frame(width: markSize, height: markSize)
        }
    }
}

#if DEBUG
    #Preview("MonarchAvailabilityToggle") {
        MonarchPreviewMatrix {
            VStack(alignment: .leading) {
                MonarchAvailabilityToggle(isAvailable: .constant(true))
                MonarchAvailabilityToggle(isAvailable: .constant(false), showsHint: true)
                MonarchAvailabilityToggle(isAvailable: .constant(true), isPending: true, showsHint: true)
                MonarchAvailabilityToggle(isAvailable: .constant(false)).disabled(true)
            }
        }
    }
#endif
