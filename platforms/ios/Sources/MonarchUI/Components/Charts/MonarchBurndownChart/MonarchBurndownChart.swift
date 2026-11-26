import SwiftUI

/// A sprint burndown: work remaining each day against the ideal line, with
/// the scope line and its changes, a today marker and a sentence saying
/// whether the sprint is ahead or behind, in a ``MonarchChartFrame``.
///
/// ```swift
/// MonarchBurndownChart(
///     "Sprint 14",
///     points: [
///         MonarchBurndownChartPoint(date: day1, remaining: 40, scope: 40),
///         MonarchBurndownChartPoint(date: day2, remaining: 38, scope: 40),
///         MonarchBurndownChartPoint(date: day3, remaining: 37, scope: 45),
///         MonarchBurndownChartPoint(date: day4),
///     ]
/// )
/// ```
///
/// Pass one point per sprint day, including days not reached yet (without
/// `remaining`), so the ideal line reaches zero on the last day. Days are
/// evenly spaced. Built on Swift Charts: the remaining and ideal lines are
/// `LineMark`s (the ideal dashed), the scope is a step `LineMark` with a
/// `PointMark` and a "+5" or "−2" label where it changes, today is a dashed
/// `RuleMark`, and touching the plot (`chartXSelection`) shows a crosshair
/// with the values of that day and its scope change.
///
/// The description under the title is, unless you give one, the generated
/// summary: "Mar 6: 24 remaining, ideal 17.8, 6.2 behind ideal. Scope 43, 5
/// added and 2 removed since the start".
///
/// Accessibility: the chart frame names the chart by its title, gives
/// VoiceOver the summary and the data as an accessibility chart descriptor
/// (Audio Graphs and the data table) and offers "View as table", where each
/// row reads the day, remaining, ideal, scope and scope change. The legend
/// toggles each line and keeps one visible. The inspection popover is
/// visual only. The ideal line differs from the others by its dash and
/// name, never by colour alone.
public struct MonarchBurndownChart: View {
    private let title: String
    private let points: [MonarchBurndownChartPoint]
    private let description: String?
    private let idealStart: Double?
    private let todayIndex: Int?
    private let showsToday: Bool
    private let showsScope: Bool
    private let status: MonarchChartFrameStatus
    private let hiddenLines: Binding<Set<MonarchBurndownChartLine>>?
    private let activeIndex: Binding<Int?>?
    private let dateFormat: Date.FormatStyle
    private let emptyMessage: String?
    private let footer: String?
    private let height: CGFloat?
    private let labels: MonarchBurndownChartLabels
    private let formatValue: ((Double) -> String)?
    private let onRetry: (@MainActor () -> Void)?

    @State private var localHidden: Set<MonarchBurndownChartLine> = []
    @State private var localActive: Int?
    @Environment(\.locale) private var locale

    /// A burndown chart.
    ///
    /// - Parameters:
    ///   - title: The name of the chart, already localised.
    ///   - points: One point per sprint day, in order, including days not reached yet.
    ///   - description: One line under the title. Default: the generated summary of where the sprint stands.
    ///   - idealStart: The work at the start of the ideal line. Default: the first day's scope, else its remaining.
    ///   - todayIndex: The day of the today marker. Default: the latest day with data, when later days exist.
    ///   - showsToday: Whether the today marker is drawn.
    ///   - showsScope: Whether the scope line and its changes are drawn, when any day has a scope.
    ///   - status: Whether the body shows the chart, a placeholder, an empty state or an error.
    ///   - hiddenLines: The hidden lines. Kept inside when `nil`. The legend keeps one line visible.
    ///   - activeIndex: The inspected day, to share one crosshair between charts. Kept inside when `nil`.
    ///   - dateFormat: How dated points are written on the axis, in the popover and in the data table.
    ///   - emptyMessage: The empty-state message.
    ///   - footer: Notes under the chart: the source, the last update.
    ///   - height: The height of the plot. 240 points by default.
    ///   - labels: Replacements for the built-in strings, including those of the frame.
    ///   - formatValue: Writes a value, such as "24 pts". The default is a number in the current locale.
    ///   - onRetry: Shows Try again in the error status and runs when it is pressed.
    public init(
        _ title: String,
        points: [MonarchBurndownChartPoint],
        description: String? = nil,
        idealStart: Double? = nil,
        todayIndex: Int? = nil,
        showsToday: Bool = true,
        showsScope: Bool = true,
        status: MonarchChartFrameStatus = .ready,
        hiddenLines: Binding<Set<MonarchBurndownChartLine>>? = nil,
        activeIndex: Binding<Int?>? = nil,
        dateFormat: Date.FormatStyle = .dateTime.month(.abbreviated).day(),
        emptyMessage: String? = nil,
        footer: String? = nil,
        height: CGFloat? = nil,
        labels: MonarchBurndownChartLabels = MonarchBurndownChartLabels(),
        formatValue: ((Double) -> String)? = nil,
        onRetry: (@MainActor () -> Void)? = nil
    ) {
        self.title = title
        self.points = points
        self.description = description
        self.idealStart = idealStart
        self.todayIndex = todayIndex
        self.showsToday = showsToday
        self.showsScope = showsScope
        self.status = status
        self.hiddenLines = hiddenLines
        self.activeIndex = activeIndex
        self.dateFormat = dateFormat
        self.emptyMessage = emptyMessage
        self.footer = footer
        self.height = height
        self.labels = labels
        self.formatValue = formatValue
        self.onRetry = onRetry
    }

    private var hidden: Binding<Set<MonarchBurndownChartLine>> { hiddenLines ?? $localHidden }

    /// The hidden lines as the legend's keys.
    private var hiddenKeys: Binding<Set<String>> {
        let hidden = hidden
        return Binding(
            get: { Set(hidden.wrappedValue.map(\.rawValue)) },
            set: { keys in hidden.wrappedValue = Set(keys.compactMap(MonarchBurndownChartLine.init(rawValue:))) }
        )
    }

    private var active: Int? {
        let value = activeIndex?.wrappedValue ?? localActive
        guard let value, value >= 0, value < points.count else { return nil }
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
        let format = dateFormat.locale(locale)
        let model = MonarchBurndownChartTokens.model(
            points,
            idealStart: idealStart,
            todayIndex: todayIndex,
            showsToday: showsToday,
            showsScope: showsScope,
            dayLabel: { point in point.label ?? point.date?.formatted(format) ?? "" }
        )
        MonarchChartFrame(
            title,
            description: description
                ?? (model.count == 0
                    ? nil
                    : MonarchBurndownChartTokens.summary(
                        model,
                        labels: labels,
                        locale: locale,
                        formatValue: formatValue
                    )),
            status: MonarchLineChartTokens.status(status, count: model.count),
            table: MonarchBurndownChartTokens.table(model, labels: labels, locale: locale, formatValue: formatValue),
            legend: MonarchBurndownChartTokens.legend(model, labels: labels),
            hiddenSeries: hiddenKeys,
            legendMarker: .line,
            emptyMessage: emptyMessage,
            footer: footer,
            height: height,
            labels: labels.frame,
            onRetry: onRetry
        ) {
            MonarchBurndownChartPlot(
                model: model,
                visible: MonarchBurndownChartTokens.visible(model, hidden: hidden.wrappedValue),
                labels: labels,
                noValue: labels.frame.noValue ?? MonarchChartFrameStrings.noValue,
                active: active,
                formatValue: formatValue,
                setActive: setActive
            )
            .frame(height: height ?? MonarchChartFrameTokens.defaultHeight)
        }
    }
}

#if DEBUG
    #Preview("MonarchBurndownChart") {
        let remaining: [Double?] = [40, 38, 35, 37, 31, 26, 24, nil, nil, nil]
        let scope: [Double?] = [40, 40, 40, 45, 45, 43, 43, nil, nil, nil]
        MonarchPreviewMatrix {
            VStack {
                MonarchBurndownChart(
                    "Sprint 14",
                    points: remaining.indices.map {
                        MonarchBurndownChartPoint(label: "Day \($0 + 1)", remaining: remaining[$0], scope: scope[$0])
                    },
                    activeIndex: .constant(3)
                )
                MonarchBurndownChart(
                    "Sprint 13",
                    points: [30, 28, 24, 21, 18, 14, 11, 7, 3, 0].enumerated().map {
                        MonarchBurndownChartPoint(label: "Day \($0.offset + 1)", remaining: $0.element)
                    },
                    formatValue: { "\(Int($0)) pts" }
                )
            }
        }
    }
#endif
