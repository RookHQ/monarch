import SwiftUI

/// A row of per-period status segments, usually one per day, for uptime and
/// status pages.
///
/// ```swift
/// MonarchUptimeBar(
///     "API",
///     segments: days,
///     description: "Last 30 days",
///     startLabel: "30 days ago",
///     endLabel: "Today"
/// )
/// ```
///
/// Each segment is coloured by its state, given or derived from its uptime
/// with `thresholds`. Touch replaces hover: tapping a segment rings it and
/// opens a popover with the period, its state, its uptime, the details and
/// the note, and calls `onSegmentSelect`. The overall uptime, the mean of
/// the measured segments unless given, is written under the bar.
///
/// Accessibility: the bar is one adjustable element named by `label` and
/// the summary ("Periods: 30. Operational 28, Down 2. Uptime 99.95%.").
/// Swiping up or down inspects the next or previous period and reads its
/// details as the value, without opening the popover; double-tapping
/// selects the inspected period when `onSegmentSelect` is given. VoiceOver
/// also gets the periods as an accessibility chart descriptor (Audio Graphs
/// and the chart's data table). Every state has a name, so colour never
/// carries it alone.
public struct MonarchUptimeBar: View {
    private let label: String?
    private let segments: [MonarchUptimeBarSegment]
    private let description: String?
    private let overallUptime: Double?
    private let showsOverallUptime: Bool
    private let startLabel: String?
    private let endLabel: String?
    private let thresholds: MonarchUptimeBarThresholds
    private let size: MonarchUptimeBarSize
    private let showsLegend: Bool
    private let uptimeDigits: Int
    private let activeIndex: Binding<Int?>?
    private let summary: String?
    private let labels: MonarchUptimeBarLabels
    private let onSegmentSelect: (@MainActor (MonarchUptimeBarSegment) -> Void)?

    @State private var localActive: Int?
    @State private var presented: Int?
    @Environment(\.monarchTheme) private var theme
    @Environment(\.locale) private var locale
    @Environment(\.layoutDirection) private var layoutDirection

    /// An uptime bar.
    ///
    /// - Parameters:
    ///   - label: What is monitored, such as a service name, shown above the bar. Already localised.
    ///   - segments: The periods, oldest first.
    ///   - description: A short line under the label.
    ///   - overallUptime: The overall uptime in percent. The mean of the measured segments when `nil`.
    ///   - showsOverallUptime: Whether the overall uptime is written under the bar.
    ///   - startLabel: Text under the oldest segment, such as "30 days ago".
    ///   - endLabel: Text under the newest segment, such as "Today".
    ///   - thresholds: The cut points for segments with an uptime but no status.
    ///   - size: The height of the segments.
    ///   - showsLegend: Whether the names of the colours are listed under the bar.
    ///   - uptimeDigits: The decimal places of uptime percentages.
    ///   - activeIndex: The inspected segment, to share inspection between bars. Kept inside when `nil`.
    ///   - summary: Replaces the generated summary VoiceOver reads.
    ///   - labels: Replacements for the built-in strings.
    ///   - onSegmentSelect: Runs when a segment is tapped, or double-tapped with VoiceOver, such as to open its incidents.
    public init(
        _ label: String? = nil,
        segments: [MonarchUptimeBarSegment],
        description: String? = nil,
        overallUptime: Double? = nil,
        showsOverallUptime: Bool = true,
        startLabel: String? = nil,
        endLabel: String? = nil,
        thresholds: MonarchUptimeBarThresholds = MonarchUptimeBarThresholds(),
        size: MonarchUptimeBarSize = .md,
        showsLegend: Bool = false,
        uptimeDigits: Int = 2,
        activeIndex: Binding<Int?>? = nil,
        summary: String? = nil,
        labels: MonarchUptimeBarLabels = MonarchUptimeBarLabels(),
        onSegmentSelect: (@MainActor (MonarchUptimeBarSegment) -> Void)? = nil
    ) {
        self.label = label
        self.segments = segments
        self.description = description
        self.overallUptime = overallUptime
        self.showsOverallUptime = showsOverallUptime
        self.startLabel = startLabel
        self.endLabel = endLabel
        self.thresholds = thresholds
        self.size = size
        self.showsLegend = showsLegend
        self.uptimeDigits = uptimeDigits
        self.activeIndex = activeIndex
        self.summary = summary
        self.labels = labels
        self.onSegmentSelect = onSegmentSelect
    }

    private var active: Int? {
        let value = activeIndex?.wrappedValue ?? localActive
        guard let value, segments.indices.contains(value) else { return nil }
        return value
    }

    private func setActive(_ value: Int?) {
        if let activeIndex {
            activeIndex.wrappedValue = value
        } else {
            localActive = value
        }
    }

    /// The content of the view.
    public var body: some View {
        let statuses = segments.map { MonarchUptimeBarTokens.status($0, thresholds: thresholds) }
        let overall = MonarchUptimeBarTokens.overall(segments, given: overallUptime).map {
            MonarchUptimeBarTokens.percent($0, digits: uptimeDigits, locale: locale)
        }
        let spoken =
            summary
            ?? MonarchUptimeBarTokens.summary(
                count: segments.count,
                counts: MonarchUptimeBarTokens.counts(statuses),
                overall: overall,
                labels: labels,
                locale: locale
            )
        VStack(alignment: .leading, spacing: MonarchUptimeBarTokens.spacing) {
            if label != nil || description != nil {
                header
            }
            plot(statuses: statuses, summary: spoken)
            footer(overall: showsOverallUptime ? overall : nil)
            if showsLegend {
                MonarchUptimeBarLegend(labels: labels)
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
    }

    private var header: some View {
        VStack(alignment: .leading, spacing: MonarchUptimeBarTokens.headingSpacing) {
            if let label {
                Text(label)
                    .monarchTextStyle(.label)
                    .foregroundStyle(theme.colors.textDefault)
            }
            if let description {
                Text(description)
                    .monarchTextStyle(.caption)
                    .foregroundStyle(theme.colors.textMuted)
            }
        }
    }

    private func plot(statuses: [MonarchUptimeBarStatus], summary: String) -> some View {
        let current = active
        let segmentTexts = segments.indices.map {
            MonarchUptimeBarTokens.segmentText(
                segments[$0],
                status: statuses[$0],
                digits: uptimeDigits,
                labels: labels,
                locale: locale
            )
        }
        return GeometryReader { proxy in
            HStack(spacing: MonarchUptimeBarTokens.segmentGap) {
                ForEach(Array(segments.enumerated()), id: \.element.id) { index, segment in
                    segmentView(index: index, segment: segment, status: statuses[index], isActive: index == current)
                }
            }
            .frame(height: MonarchUptimeBarTokens.height(size))
            .frame(width: proxy.size.width, height: proxy.size.height)
            .contentShape(Rectangle())
            .gesture(
                SpatialTapGesture().onEnded { value in
                    tap(at: value.location.x, width: proxy.size.width)
                }
            )
        }
        .frame(height: max(MonarchUptimeBarTokens.height(size), MonarchAccessibility.minimumHitTarget))
        .accessibilityElement(children: .ignore)
        .accessibilityLabel([label, summary].compactMap { $0 }.joined(separator: ", "))
        .accessibilityValue(current.map { segmentTexts[$0] } ?? "")
        .accessibilityAdjustableAction { direction in
            switch direction {
            case .increment:
                setActive(MonarchUptimeBarTokens.adjusted(active, increment: true, count: segments.count))
            case .decrement:
                setActive(MonarchUptimeBarTokens.adjusted(active, increment: false, count: segments.count))
            @unknown default:
                break
            }
        }
        .accessibilityAction {
            if let current, let onSegmentSelect { onSegmentSelect(segments[current]) }
        }
        .accessibilityChartDescriptor(
            MonarchUptimeBarDescriptor(
                title: label ?? summary,
                summary: summary,
                periods: segments.map(\.label),
                uptimes: segments.map { $0.uptime.flatMap { $0.isFinite ? $0 : nil } },
                statusNames: statuses.map { MonarchUptimeBarTokens.name($0, labels: labels) },
                allStatusNames: MonarchUptimeBarTokens.order.map { MonarchUptimeBarTokens.name($0, labels: labels) },
                texts: segmentTexts,
                digits: uptimeDigits,
                labels: labels
            )
        )
    }

    private func segmentView(
        index: Int,
        segment: MonarchUptimeBarSegment,
        status: MonarchUptimeBarStatus,
        isActive: Bool
    ) -> some View {
        let shape = RoundedRectangle(cornerRadius: MonarchUptimeBarTokens.segmentRadius, style: .continuous)
        return
            shape
            .fill(MonarchUptimeBarTokens.fill(status, colors: theme.colors))
            .overlay {
                if status == .noData {
                    shape.strokeBorder(
                        theme.colors[role: MonarchUptimeBarTokens.noDataBorderRole],
                        lineWidth: MonarchShapes.borderWidth
                    )
                }
            }
            .overlay {
                if isActive {
                    shape.strokeBorder(
                        theme.colors[role: MonarchUptimeBarTokens.ringRole],
                        lineWidth: MonarchUptimeBarTokens.ringWidth
                    )
                }
            }
            .frame(minWidth: MonarchShapes.borderWidth, maxWidth: .infinity)
            .popover(
                isPresented: Binding(
                    get: { presented == index },
                    set: { isShown in
                        if !isShown, presented == index {
                            presented = nil
                            if active == index { setActive(nil) }
                        }
                    }
                )
            ) {
                MonarchUptimeBarCard(
                    segment: segment,
                    status: status,
                    digits: uptimeDigits,
                    labels: labels
                )
                .presentationCompactAdaptation(.popover)
            }
    }

    private func tap(at x: CGFloat, width: CGFloat) {
        guard
            let index = MonarchUptimeBarTokens.index(
                at: x,
                width: width,
                count: segments.count,
                isRightToLeft: layoutDirection == .rightToLeft
            )
        else { return }
        setActive(index)
        presented = index
        onSegmentSelect?(segments[index])
    }

    private func footer(overall: String?) -> some View {
        HStack(alignment: .firstTextBaseline, spacing: MonarchUptimeBarTokens.spacing) {
            Text(startLabel ?? "")
            Spacer(minLength: 0)
            if let overall {
                Text(labels.overall?(overall) ?? MonarchUptimeBarStrings.overall(overall))
                    .monospacedDigit()
                    .foregroundStyle(theme.colors.textDefault)
            }
            Spacer(minLength: 0)
            Text(endLabel ?? "")
        }
        .monarchTextStyle(.caption)
        .foregroundStyle(theme.colors.textMuted)
        // The summary already says the overall uptime; the period ends are visual only, as on the web.
        .accessibilityHidden(true)
    }
}

#if DEBUG
    #Preview("MonarchUptimeBar") {
        let days = (0..<30).map { day in
            MonarchUptimeBarSegment(
                id: "d\(day)",
                label: "Day \(day + 1)",
                status: day == 3 ? .maintenance : day == 12 ? .partial : day == 20 ? .noData : nil,
                uptime: day == 8 ? 99.4 : day == 15 ? 97 : 100
            )
        }
        MonarchPreviewMatrix {
            VStack(alignment: .leading) {
                MonarchUptimeBar(
                    "API",
                    segments: days,
                    description: "Last 30 days",
                    startLabel: "30 days ago",
                    endLabel: "Today",
                    showsLegend: true
                )
                MonarchUptimeBar(segments: days, size: .sm, activeIndex: .constant(8))
            }
        }
    }
#endif
