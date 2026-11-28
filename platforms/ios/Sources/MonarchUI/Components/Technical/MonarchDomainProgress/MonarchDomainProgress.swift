import SwiftUI

/// Stage-by-stage progress for connecting a custom domain: DNS record found,
/// HTTPS certificate issued, live. Shows the problems the product found,
/// what public DNS shows right now and that checking continues on its own.
///
/// ```swift
/// MonarchDomainProgress(
///     "book.example.com",
///     stage: .certificate,
///     dnsFound: MonarchDomainDnsSnapshot(cnames: ["domains.example.net"]),
///     checkedAt: lastCheck
/// )
/// ```
///
/// The three steps are a vertical stepper. A finished step shows a check, the
/// step in progress shows a spinner (a still dot with Reduce Motion), and a
/// step with a blocking problem shows an exclamation mark; each state is
/// also spoken. Blocking problems are shown as warnings under the steps; an
/// informational one ("certificate pending") is shown quietly. Until the
/// domain is live, the view says what public DNS shows, that the product
/// keeps checking, and when it last checked. The view does not check
/// anything itself: poll with ``recheckInterval(for:suggested:isStopped:)``
/// and pass the new stage.
///
/// Accessibility: the steps are a container named "Progress for <host>".
/// Each step is one element: its title as the label, its state and detail
/// as the value ("in progress, Being issued now…"). When the active step or
/// its state changes, "Step 2 of 3: HTTPS certificate, in progress." is
/// announced.
public struct MonarchDomainProgress: View {
    private let host: String
    private let stage: MonarchDomainStage
    private let problems: [MonarchDomainProblem]
    private let dnsFound: MonarchDomainDnsSnapshot?
    private let checkedAt: Date?
    private let now: Date?
    private let isRechecking: Bool
    private let notify: MonarchDomainNotify?
    private let labels: MonarchDomainProgressLabels

    @Environment(\.monarchTheme) private var theme
    @Environment(\.locale) private var locale
    @Environment(\.accessibilityReduceMotion) private var reduceMotion
    @ScaledMetric(relativeTo: .body) private var markerSide = MonarchDomainProgressTokens.markerSide
    @ScaledMetric(relativeTo: .body) private var stillDotSide = MonarchDomainProgressTokens.stillDotSide

    /// A domain progress.
    ///
    /// - Parameters:
    ///   - host: The custom host name, such as `book.example.com`.
    ///   - stage: Where the domain is in its connection.
    ///   - problems: What the last check found. Blocking problems mark their
    ///     step and are shown as warnings.
    ///   - dnsFound: What public DNS shows for the host right now. `nil`
    ///     hides the line.
    ///   - checkedAt: When the product last checked.
    ///   - now: The reference time for `checkedAt`. The current time,
    ///     updated every minute, when `nil`.
    ///   - isRechecking: Whether the product keeps checking on its own.
    ///   - notify: How the product tells the user it went live.
    ///   - labels: Replacements for the built-in strings.
    public init(
        _ host: String,
        stage: MonarchDomainStage,
        problems: [MonarchDomainProblem] = [],
        dnsFound: MonarchDomainDnsSnapshot? = nil,
        checkedAt: Date? = nil,
        now: Date? = nil,
        isRechecking: Bool = true,
        notify: MonarchDomainNotify? = nil,
        labels: MonarchDomainProgressLabels = MonarchDomainProgressLabels()
    ) {
        self.host = host
        self.stage = stage
        self.problems = problems
        self.dnsFound = dnsFound
        self.checkedAt = checkedAt
        self.now = now
        self.isRechecking = isRechecking
        self.notify = notify
        self.labels = labels
    }

    /// How long to wait before checking a domain again: the product's
    /// suggested delay kept between 15 seconds and 5 minutes (30 seconds
    /// when it suggests none), 15 seconds before the first stage is known,
    /// and `nil` once live or when checking has stopped.
    ///
    /// - Parameters:
    ///   - stage: The current stage, or `nil` before the first check.
    ///   - suggested: The product's suggested delay.
    ///   - isStopped: Whether the product stopped checking.
    /// - Returns: The time to wait, or `nil` when there is nothing more to check.
    nonisolated public static func recheckInterval(
        for stage: MonarchDomainStage?,
        suggested: Duration? = nil,
        isStopped: Bool = false
    ) -> Duration? {
        MonarchDomainProgressTokens.recheckInterval(for: stage, suggested: suggested, isStopped: isStopped)
    }

    private var items: [MonarchDomainProgressItem] {
        MonarchDomainProgressTokens.items(host: host, stage: stage, problems: problems, labels: labels)
    }

    /// The content of the view.
    public var body: some View {
        let items = items
        VStack(alignment: .leading, spacing: MonarchDomainProgressTokens.sectionSpacing) {
            VStack(alignment: .leading, spacing: 0) {
                ForEach(Array(items.enumerated()), id: \.element.id) { index, item in
                    row(item, isLast: index == items.count - 1)
                }
            }
            .accessibilityElement(children: .contain)
            .accessibilityLabel(labels.listLabelText(host))
            problemNotices
            if stage != .live {
                meta
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .onChange(of: MonarchDomainProgressTokens.announcement(items, labels: labels)) { _, text in
            AccessibilityNotification.Announcement(text).post()
        }
    }

    private func row(_ item: MonarchDomainProgressItem, isLast: Bool) -> some View {
        HStack(alignment: .top, spacing: MonarchDomainProgressTokens.markerSpacing) {
            marker(item.state)
                .frame(width: markerSide, height: markerSide)
                .accessibilityHidden(true)
            VStack(alignment: .leading, spacing: MonarchDomainProgressTokens.textSpacing) {
                Text(item.title)
                    .monarchTextStyle(.bodyStrong)
                    .foregroundStyle(theme.colors[role: MonarchDomainProgressTokens.titleRole(item.state)])
                Text(item.detail)
                    .monarchTextStyle(.caption)
                    .foregroundStyle(theme.colors.textMuted)
            }
            .multilineTextAlignment(.leading)
            .fixedSize(horizontal: false, vertical: true)
            .frame(maxWidth: .infinity, alignment: .leading)
        }
        .padding(.bottom, isLast ? 0 : MonarchDomainProgressTokens.stepSpacing)
        .background(alignment: .leading) {
            if !isLast {
                // Leading padding keeps the line under the markers in right-to-left layouts.
                Rectangle()
                    .fill(theme.colors[role: MonarchDomainProgressTokens.connectorRole(after: item.state)])
                    .frame(width: MonarchDomainProgressTokens.lineWidth)
                    .padding(.top, markerSide)
                    .padding(.leading, (markerSide - MonarchDomainProgressTokens.lineWidth) / 2)
            }
        }
        .accessibilityElement(children: .ignore)
        .accessibilityLabel(item.title)
        .accessibilityValue(MonarchDomainProgressTokens.stepValue(item, labels: labels))
    }

    private func marker(_ state: MonarchDomainStepState) -> some View {
        let roles = MonarchDomainProgressTokens.marker(state)
        return ZStack {
            Circle().fill(theme.colors[role: roles.background ?? \.bgSurface])
            Circle().strokeBorder(
                theme.colors[role: roles.border ?? \.borderDefault],
                lineWidth: MonarchDomainProgressTokens.lineWidth
            )
            if let symbol = MonarchDomainProgressTokens.symbol(state) {
                MonarchIcon(systemName: symbol, size: .sm)
                    .foregroundStyle(theme.colors[role: roles.foreground])
            } else if state == .current {
                MonarchArcSpinner(side: stillDotSide * 2, color: theme.colors[role: roles.foreground])
            }
        }
    }

    @ViewBuilder private var problemNotices: some View {
        let shown = MonarchDomainProgressTokens.shownProblems(problems)
        if !shown.isEmpty {
            VStack(alignment: .leading, spacing: MonarchDomainProgressTokens.textSpacing) {
                ForEach(Array(shown.enumerated()), id: \.offset) { _, problem in
                    MonarchInlineNotice(problem.message, tone: MonarchDomainProgressTokens.tone(problem))
                }
            }
        }
    }

    @ViewBuilder private var meta: some View {
        let lines = metaText
        if !lines.isEmpty || checkedAt != nil {
            VStack(alignment: .leading, spacing: MonarchDomainProgressTokens.textSpacing) {
                if !lines.isEmpty {
                    Text(lines)
                        .monarchTextStyle(.caption)
                        .foregroundStyle(theme.colors.textMuted)
                }
                if let checkedAt {
                    if let now {
                        checkedLine(checkedAt, now: now)
                    } else {
                        MonarchRelativeTimeClock(dates: [checkedAt]) { current in
                            checkedLine(checkedAt, now: current)
                        }
                    }
                }
            }
            .multilineTextAlignment(.leading)
            .fixedSize(horizontal: false, vertical: true)
        }
    }

    private var metaText: String {
        var parts: [String] = []
        if let dnsFound {
            let seen = MonarchDomainProgressTokens.snapshotText(dnsFound, empty: labels.resolvedNoRecord)
            parts.append(labels.dnsSeenText(host: host, seen: seen))
        }
        if isRechecking {
            parts.append(labels.recheckingText(notify))
        }
        return parts.joined(separator: " ")
    }

    private func checkedLine(_ date: Date, now: Date) -> some View {
        Text(labels.checkedText(MonarchDomainProgressTokens.relativeTime(date, now: now, locale: locale)))
            .monarchTextStyle(.caption)
            .foregroundStyle(theme.colors.textMuted)
    }
}

#if DEBUG
    #Preview("MonarchDomainProgress") {
        let now = Date(timeIntervalSinceReferenceDate: 800_000_000)
        MonarchPreviewMatrix {
            VStack(alignment: .leading) {
                MonarchDomainProgress(
                    "book.example.com",
                    stage: .dns,
                    dnsFound: MonarchDomainDnsSnapshot(),
                    checkedAt: now.addingTimeInterval(-120),
                    now: now
                )
                MonarchDomainProgress(
                    "book.example.com",
                    stage: .certificate,
                    problems: [MonarchDomainProblem("Certificate pending.", severity: .info)],
                    notify: .email
                )
                MonarchDomainProgress(
                    "book.example.com",
                    stage: .dns,
                    problems: [MonarchDomainProblem("Your domain points somewhere else. Remove the old A record.")],
                    dnsFound: MonarchDomainDnsSnapshot(a: ["203.0.113.10"])
                )
                MonarchDomainProgress("book.example.com", stage: .live)
            }
        }
    }
#endif
