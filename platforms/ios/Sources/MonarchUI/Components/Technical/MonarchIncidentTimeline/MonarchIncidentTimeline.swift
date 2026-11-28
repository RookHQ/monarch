import SwiftUI

/// The story of one incident: each update with its stage (investigating,
/// identified, monitoring, resolved), time and text, joined by a line.
/// System-posted updates are tagged so readers can tell them from people.
///
/// ```swift
/// MonarchIncidentTimeline([
///     MonarchIncidentUpdate(id: "1", stage: .investigating, body: "Checkout is failing for some users.", time: start),
///     MonarchIncidentUpdate(id: "2", stage: .resolved, body: "Payments are back to normal.", time: end),
/// ])
/// ```
///
/// Updates are shown in the order given; status pages usually show the
/// oldest first so the incident reads as a story. Each stage is a
/// ``MonarchStatusBadge`` (a word and a tone) and a dot of the same tone on
/// the line. Times are shown in one time zone, UTC by default, with the
/// zone name, so every reader quotes the same time. Product stages are added
/// by key with `stages`, which also overrides the standard labels and tones.
///
/// Accessibility: the updates are a container named "Incident updates".
/// Each update is one element read as the stage, the time, the author and
/// "Automated", then the text. With `isLive`, an update added while the
/// view is shown is announced.
public struct MonarchIncidentTimeline: View {
    private let updates: [MonarchIncidentUpdate]
    private let stages: [String: MonarchStatusDefinition]
    private let density: MonarchIncidentTimelineDensity
    private let isLive: Bool
    private let timeZone: TimeZone?
    private let labels: MonarchIncidentTimelineLabels

    @Environment(\.monarchTheme) private var theme
    @Environment(\.locale) private var locale
    @ScaledMetric(relativeTo: .body) private var column = MonarchIncidentTimelineTokens.markerColumn
    @ScaledMetric(relativeTo: .body) private var dot = MonarchIncidentTimelineTokens.dotSide
    @State private var announcedCount: Int?

    /// An incident timeline.
    ///
    /// - Parameters:
    ///   - updates: The updates, in the order to show them.
    ///   - stages: Product stages by key, or overrides of the standard ones.
    ///   - density: `regular`, or `compact` for cards and side panels.
    ///   - isLive: Whether updates added while the view is shown are announced.
    ///   - timeZone: The time zone of the times. UTC by default; `nil` is the device's zone
    ///     (`TimeZone.autoupdatingCurrent`), read each time.
    ///   - labels: Replacements for the built-in strings.
    public init(
        _ updates: [MonarchIncidentUpdate],
        stages: [String: MonarchStatusDefinition] = [:],
        density: MonarchIncidentTimelineDensity = .regular,
        isLive: Bool = false,
        timeZone: TimeZone? = TimeZone(identifier: "UTC"),
        labels: MonarchIncidentTimelineLabels = MonarchIncidentTimelineLabels()
    ) {
        self.updates = updates
        self.stages = stages
        self.density = density
        self.isLive = isLive
        self.timeZone = timeZone
        self.labels = labels
    }

    /// The zone the times are shown in: `timeZone`, else the device's, read each time.
    var resolvedTimeZone: TimeZone { timeZone ?? .autoupdatingCurrent }

    /// The content of the view.
    public var body: some View {
        Group {
            if updates.isEmpty {
                Text(labels.resolvedEmpty)
                    .monarchTextStyle(.body)
                    .foregroundStyle(theme.colors.textMuted)
                    .frame(maxWidth: .infinity, alignment: .leading)
            } else {
                VStack(alignment: .leading, spacing: 0) {
                    ForEach(Array(updates.enumerated()), id: \.element.id) { index, update in
                        row(update, isLast: index == updates.count - 1)
                    }
                }
                .accessibilityElement(children: .contain)
                .accessibilityLabel(labels.resolvedList)
            }
        }
        .onAppear { announcedCount = updates.count }
        .onChange(of: updates.count) { _, count in
            defer { announcedCount = count }
            guard isLive,
                let update = MonarchIncidentTimelineTokens.newUpdate(updates, previousCount: announcedCount ?? count)
            else { return }
            AccessibilityNotification.Announcement(spokenLabel(update)).post()
        }
    }

    private func definition(_ update: MonarchIncidentUpdate) -> MonarchStatusDefinition {
        MonarchIncidentTimelineTokens.resolve(update.stage, stages: stages, labels: labels)
    }

    private func timeText(_ update: MonarchIncidentUpdate) -> String {
        MonarchIncidentTimelineTokens.time(update.time, timeZone: resolvedTimeZone, locale: locale)
    }

    private func spokenLabel(_ update: MonarchIncidentUpdate) -> String {
        MonarchIncidentTimelineTokens.accessibilityLabel(
            stage: definition(update).label,
            time: timeText(update),
            author: update.author,
            automated: update.isAutomated ? labels.resolvedAutomated : nil,
            body: update.body
        )
    }

    private func row(_ update: MonarchIncidentUpdate, isLast: Bool) -> some View {
        let definition = definition(update)
        return HStack(alignment: .top, spacing: MonarchIncidentTimelineTokens.columnSpacing) {
            Circle()
                .fill(theme.colors[role: MonarchIncidentTimelineTokens.markerRole(definition.tone)])
                .frame(width: dot, height: dot)
                .frame(width: column, height: column)
                .accessibilityHidden(true)
            VStack(alignment: .leading, spacing: MonarchIncidentTimelineTokens.lineSpacing) {
                MonarchFlowLayout(spacing: MonarchIncidentTimelineTokens.metaSpacing) {
                    // The web draws the stage as semibold text in the strong tone colour, not as a pill.
                    Text(definition.label)
                        .monarchTextStyle(.caption, weight: .semibold)
                        .foregroundStyle(
                            theme.colors[
                                role: definition.tone == .neutral
                                    ? \.textDefault : MonarchTonePalette.palette(for: definition.tone).strong
                            ]
                        )
                    Text(timeText(update))
                        .monarchTextStyle(.caption)
                        .monospacedDigit()
                        .foregroundStyle(theme.colors.textSubtle)
                    if let author = update.author, !author.isEmpty {
                        Text(author)
                            .monarchTextStyle(.caption)
                            .monospacedDigit()
                            .foregroundStyle(theme.colors.textSubtle)
                    }
                    if update.isAutomated {
                        MonarchTag(labels.resolvedAutomated, size: .sm)
                    }
                }
                if !update.body.isEmpty {
                    Text(update.body)
                        .monarchTextStyle(.body)
                        .foregroundStyle(theme.colors.textMuted)
                        .multilineTextAlignment(.leading)
                        .fixedSize(horizontal: false, vertical: true)
                        .textSelection(.enabled)
                }
            }
            .frame(maxWidth: .infinity, alignment: .leading)
        }
        .padding(.bottom, isLast ? 0 : MonarchIncidentTimelineTokens.rowSpacing(density))
        .background(alignment: .leading) {
            if !isLast {
                // Leading padding keeps the line under the markers in right-to-left layouts.
                Rectangle()
                    .fill(theme.colors[role: MonarchIncidentTimelineTokens.lineRole])
                    .frame(width: MonarchIncidentTimelineTokens.lineWidth)
                    .padding(.top, (column + dot) / 2)
                    .padding(.leading, (column - MonarchIncidentTimelineTokens.lineWidth) / 2)
            }
        }
        .accessibilityElement(children: .ignore)
        .accessibilityLabel(spokenLabel(update))
    }
}

#if DEBUG
    #Preview("MonarchIncidentTimeline") {
        let start = Date(timeIntervalSinceReferenceDate: 800_000_000)
        MonarchPreviewMatrix {
            VStack(alignment: .leading) {
                MonarchIncidentTimeline([
                    MonarchIncidentUpdate(
                        id: "1",
                        stage: .investigating,
                        body: "Checkout is failing for some users.",
                        time: start,
                        isAutomated: true
                    ),
                    MonarchIncidentUpdate(
                        id: "2",
                        stage: .identified,
                        body: "A database migration locked the orders table.",
                        time: start.addingTimeInterval(900),
                        author: "Ada"
                    ),
                    MonarchIncidentUpdate(
                        id: "3",
                        stage: .resolved,
                        body: "Payments are back to normal.",
                        time: start.addingTimeInterval(3_600)
                    ),
                ])
                MonarchIncidentTimeline([], density: .compact)
            }
        }
    }
#endif
