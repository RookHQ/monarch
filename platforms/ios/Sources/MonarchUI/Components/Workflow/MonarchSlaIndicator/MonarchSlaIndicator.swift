import SwiftUI

/// A service-level target at a glance: on track, at risk, breached, paused
/// or met, with the time left or overdue.
///
/// ```swift
/// MonarchSlaIndicator(.onTrack, dueAt: ticket.firstResponseDue, warnBefore: 3600, target: "First response")
/// MonarchSlaIndicator(.met, variant: .text)
/// ```
///
/// Pass the state the server reported. For a running target with a due
/// time the indicator follows the clock between refreshes: it counts down
/// in whole minutes, turns at risk `warnBefore` seconds before the due time
/// and breached when the due time passes. `onStateChange` reports that.
///
/// Accessibility: the indicator is one element. Its label is the name of
/// the target and the time left in words; its value is the state, the due
/// date and a note about an earlier breach. The state is an icon and text,
/// never colour alone. With `announcesChanges` a change of state is
/// announced; turn it on for one prominent indicator, not for every row.
public struct MonarchSlaIndicator: View {
    private let state: MonarchSlaIndicatorState
    private let dueAt: Date?
    private let warnBefore: TimeInterval?
    private let target: String?
    private let variant: MonarchSlaIndicatorVariant
    private let hasEarlierBreach: Bool
    private let showsNotApplicable: Bool
    private let isLive: Bool
    private let announcesChanges: Bool
    private let now: Date?
    private let labels: MonarchSlaIndicatorLabels
    private let onStateChange: (@MainActor (MonarchSlaIndicatorState) -> Void)?

    /// An SLA indicator.
    ///
    /// - Parameters:
    ///   - state: The state the server reported.
    ///   - dueAt: When the target is due. It drives the countdown and the
    ///     overdue time.
    ///   - warnBefore: Turns an on-track target at risk once it is due
    ///     within this many seconds. By default the state is trusted.
    ///   - target: The name of the target, such as "First response". It is
    ///     read by VoiceOver and not shown.
    ///   - variant: `chip` for lists and headers, `text` for rows of a panel.
    ///   - hasEarlierBreach: Whether an earlier target of the same item was
    ///     breached. VoiceOver says so.
    ///   - showsNotApplicable: Whether a missing target is written as "No
    ///     SLA" and not as a dash.
    ///   - isLive: Whether a running target follows the clock by itself.
    ///   - announcesChanges: Whether a change of state is announced.
    ///   - now: A fixed reference time, which also stops the clock. Use it
    ///     for a snapshot, for example in a preview or an export.
    ///   - labels: Replacements for the built-in strings.
    ///   - onStateChange: Called when the shown state changes after the
    ///     indicator appeared, such as when a countdown runs out.
    public init(
        _ state: MonarchSlaIndicatorState,
        dueAt: Date? = nil,
        warnBefore: TimeInterval? = nil,
        target: String? = nil,
        variant: MonarchSlaIndicatorVariant = .chip,
        hasEarlierBreach: Bool = false,
        showsNotApplicable: Bool = false,
        isLive: Bool = true,
        announcesChanges: Bool = false,
        now: Date? = nil,
        labels: MonarchSlaIndicatorLabels = MonarchSlaIndicatorLabels(),
        onStateChange: (@MainActor (MonarchSlaIndicatorState) -> Void)? = nil
    ) {
        self.state = state
        self.dueAt = dueAt
        self.warnBefore = warnBefore
        self.target = target
        self.variant = variant
        self.hasEarlierBreach = hasEarlierBreach
        self.showsNotApplicable = showsNotApplicable
        self.isLive = isLive
        self.announcesChanges = announcesChanges
        self.now = now
        self.labels = labels
        self.onStateChange = onStateChange
    }

    /// The content of the view.
    public var body: some View {
        let isTicking = MonarchSlaIndicatorTokens.isTicking(state: state, dueAt: dueAt, isLive: isLive, now: now)
        // One timeline in every case, so that the content keeps its identity
        // and a change of state is always noticed. A target that does not
        // follow the clock has a timeline of one entry.
        TimelineView(MonarchSlaIndicatorSchedule(dueAt: isTicking ? dueAt : nil, warnBefore: warnBefore)) { context in
            // The schedule gives the planned moment; the view may be drawn
            // a little later, and the later of the two is right.
            content(now: now ?? max(context.date, .now))
        }
    }

    private func content(now: Date) -> some View {
        MonarchSlaIndicatorContent(
            state: MonarchSlaIndicatorTokens.resolve(state, dueAt: dueAt, now: now, warnBefore: warnBefore),
            dueAt: dueAt,
            now: now,
            target: target,
            variant: variant,
            hasEarlierBreach: hasEarlierBreach,
            showsNotApplicable: showsNotApplicable,
            announcesChanges: announcesChanges,
            labels: labels,
            onStateChange: onStateChange
        )
    }
}

/// The indicator for a resolved state at one moment.
private struct MonarchSlaIndicatorContent: View {
    let state: MonarchSlaIndicatorState
    let dueAt: Date?
    let now: Date
    let target: String?
    let variant: MonarchSlaIndicatorVariant
    let hasEarlierBreach: Bool
    let showsNotApplicable: Bool
    let announcesChanges: Bool
    let labels: MonarchSlaIndicatorLabels
    let onStateChange: (@MainActor (MonarchSlaIndicatorState) -> Void)?

    @Environment(\.monarchTheme) private var theme
    @Environment(\.locale) private var locale
    @Environment(\.timeZone) private var timeZone

    var body: some View {
        let appearance = MonarchSlaIndicatorTokens.appearance(
            state: state,
            variant: variant,
            isHighContrast: theme.isHighContrast
        )
        label
            .padding(.horizontal, variant == .chip ? MonarchSlaIndicatorTokens.horizontalPadding : 0)
            .padding(.vertical, variant == .chip ? MonarchSlaIndicatorTokens.verticalPadding : 0)
            .monarchSurface(appearance, in: MonarchSlaIndicatorTokens.chipShape)
            .accessibilityElement(children: .ignore)
            .accessibilityLabel(
                MonarchSlaIndicatorTokens.accessibilityLabel(
                    state: state,
                    dueAt: dueAt,
                    now: now,
                    target: target,
                    labels: labels,
                    locale: locale
                )
            )
            .accessibilityValue(
                MonarchSlaIndicatorTokens.accessibilityValue(
                    state: state,
                    dueAt: dueAt,
                    now: now,
                    dueText: dueAt.map {
                        MonarchSlaIndicatorTokens.dueText($0, locale: locale, timeZone: timeZone)
                    },
                    hasEarlierBreach: hasEarlierBreach,
                    labels: labels
                )
            )
            .onChange(of: state) { _, new in
                onStateChange?(new)
                if announcesChanges {
                    let text = MonarchSlaIndicatorTokens.changeAnnouncement(to: new, target: target, labels: labels)
                    AccessibilityNotification.Announcement(text).post()
                }
            }
    }

    private var label: some View {
        HStack(spacing: MonarchSlaIndicatorTokens.iconSpacing) {
            if let symbol = MonarchSlaIndicatorTokens.symbol(for: state) {
                MonarchIcon(systemName: symbol, size: .sm)
            }
            Text(
                MonarchSlaIndicatorTokens.visibleText(
                    state: state,
                    dueAt: dueAt,
                    now: now,
                    showsNotApplicable: showsNotApplicable,
                    labels: labels,
                    locale: locale
                )
            )
            .monarchTextStyle(
                MonarchSlaIndicatorTokens.textStyle,
                weight: MonarchSlaIndicatorTokens.fontWeight(for: state)
            )
            .monospacedDigit()
        }
    }
}

#if DEBUG
    #Preview("MonarchSlaIndicator") {
        MonarchPreviewMatrix {
            let now = Date(timeIntervalSinceReferenceDate: 0)
            VStack(alignment: .leading) {
                ForEach(MonarchSlaIndicatorVariant.allCases, id: \.self) { variant in
                    HStack {
                        MonarchSlaIndicator(.onTrack, dueAt: now.addingTimeInterval(8100), variant: variant, now: now)
                        MonarchSlaIndicator(.atRisk, dueAt: now.addingTimeInterval(1500), variant: variant, now: now)
                        MonarchSlaIndicator(.breached, dueAt: now.addingTimeInterval(-2400), variant: variant, now: now)
                    }
                    HStack {
                        MonarchSlaIndicator(.paused, variant: variant)
                        MonarchSlaIndicator(.met, variant: variant)
                        MonarchSlaIndicator(.notApplicable, variant: variant)
                        MonarchSlaIndicator(.notApplicable, variant: variant, showsNotApplicable: true)
                    }
                }
                MonarchSlaIndicator(.onTrack, dueAt: .now.addingTimeInterval(125), warnBefore: 60)
            }
        }
    }
#endif
