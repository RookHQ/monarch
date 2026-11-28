import Foundation
import MonarchTokens
import SwiftUI
import Testing

@testable import MonarchUI

enum MonarchBurndownChartFixtures: ComponentFixtures {
    static let id = "burndown-chart"

    /// Ten sprint days at noon UTC, weekend left out, fixed so the fixtures never depend on the clock.
    static let days: [Date] = (0..<10).map { index in
        Date(timeIntervalSince1970: 1_772_452_800 + Double(index + (index >= 5 ? 2 : 0)) * 86_400)
    }
    static let remaining: [Double?] = [40, 38, 35, 37, 31, 26, 24, nil, nil, nil]
    static let scope: [Double?] = [40, 40, 40, 45, 45, 43, 43, nil, nil, nil]
    static let utc = Date.FormatStyle(date: .abbreviated, time: .omitted, timeZone: TimeZone(identifier: "UTC") ?? .gmt)

    static var sprint: [MonarchBurndownChartPoint] {
        days.indices.map { MonarchBurndownChartPoint(date: days[$0], remaining: remaining[$0], scope: scope[$0]) }
    }

    static var done: [MonarchBurndownChartPoint] {
        [30, 28, 24, 21, 18, 14, 11, 7, 3, 0].enumerated().map {
            MonarchBurndownChartPoint(label: "Day \($0.offset + 1)", remaining: Double($0.element))
        }
    }

    static let fixtures = [
        Fixture("in progress with scope changes, today and inspection") {
            MonarchBurndownChart("Sprint 14", points: sprint, activeIndex: .constant(3), dateFormat: utc)
        },
        Fixture("finished sprint, labelled days, custom units and description") {
            MonarchBurndownChart(
                "Sprint 13",
                points: done,
                description: "Story points, closed sprint",
                formatValue: { "\(Int($0)) pts" }
            )
        },
        Fixture("not started, ideal hidden, scope off, today given") {
            MonarchBurndownChart(
                "Sprint 15 with a long title that wraps at the largest text size",
                points: days.map { MonarchBurndownChartPoint(date: $0, scope: 30) },
                todayIndex: 0,
                showsScope: false,
                hiddenLines: .constant([.ideal]),
                dateFormat: utc
            )
        },
        Fixture("one day, no today marker, own labels") {
            MonarchBurndownChart(
                "Hotfix",
                points: [MonarchBurndownChartPoint(label: "Mon", remaining: 3, scope: 3)],
                idealStart: 5,
                showsToday: false,
                height: 140,
                labels: MonarchBurndownChartLabels(remaining: "Left", ideal: "Plan", day: "Weekday")
            )
        },
        Fixture("loading, error and empty") {
            VStack {
                MonarchBurndownChart("Sprint 14", points: sprint, status: .loading, height: 80)
                MonarchBurndownChart("Sprint 14", points: sprint, status: .error, height: 80, onRetry: {})
                MonarchBurndownChart("Sprint 14", points: [], emptyMessage: "The burndown starts with the sprint.")
            }
        },
    ]
}

@Suite("MonarchBurndownChart")
struct MonarchBurndownChartTests {
    private let english = Locale(identifier: "en_US")
    private typealias Tokens = MonarchBurndownChartTokens

    private func model(
        _ points: [MonarchBurndownChartPoint],
        idealStart: Double? = nil,
        todayIndex: Int? = nil,
        showsToday: Bool = true,
        showsScope: Bool = true
    ) -> Tokens.Model {
        Tokens.model(
            points,
            idealStart: idealStart,
            todayIndex: todayIndex,
            showsToday: showsToday,
            showsScope: showsScope,
            dayLabel: { $0.label ?? "?" }
        )
    }

    private func labelled(_ remaining: [Double?], scope: [Double?] = []) -> [MonarchBurndownChartPoint] {
        remaining.indices.map { index in
            MonarchBurndownChartPoint(
                label: "D\(index + 1)",
                remaining: remaining[index],
                scope: scope.indices.contains(index) ? scope[index] : nil
            )
        }
    }

    @Test("The ideal falls in equal steps from the start to zero on the last day")
    func ideal() {
        #expect(Tokens.ideal(start: 40, count: 5) == [40, 30, 20, 10, 0])
        #expect(Tokens.ideal(start: 7, count: 1) == [7])
        #expect(Tokens.ideal(start: nil, count: 2) == [nil, nil])
        #expect(Tokens.ideal(start: 1, count: 0).isEmpty)
    }

    @Test("The ideal starts at the given start, else the first scope, else the first remaining")
    func idealStart() {
        #expect(model(labelled([30, nil], scope: [40, 40])).ideal.first == 40)
        #expect(model(labelled([30, nil])).ideal.first == 30)
        #expect(model(labelled([30, nil], scope: [40, 40]), idealStart: 50).ideal.first == 50)
        #expect(model(labelled([nil, nil])).ideal == [nil, nil])
        #expect(model(labelled([.infinity, 3])).remaining == [nil, 3])
    }

    @Test("Scope changes are the difference from the day before, 0 next to a gap")
    func changes() {
        #expect(Tokens.changes([40, 40, 45, 43, nil, 50]) == [0, 0, 5, -2, 0, 0])
        #expect(Tokens.changes([]).isEmpty)
    }

    @Test("Today is the given day, or the latest day with data when later days exist")
    func today() {
        #expect(Tokens.today(nil, lastActual: 6, count: 10) == 6)
        #expect(Tokens.today(nil, lastActual: 9, count: 10) == nil)
        #expect(Tokens.today(nil, lastActual: nil, count: 10) == nil)
        #expect(Tokens.today(2, lastActual: 6, count: 10) == 2)
        #expect(Tokens.today(12, lastActual: 6, count: 10) == nil)
        #expect(model(labelled([3, nil]), showsToday: false).today == nil)
        #expect(model(labelled([3, nil])).today == 0)
    }

    @Test("Scope is drawn only when asked and when any day has one")
    func scopeShown() {
        #expect(model(labelled([3], scope: [3])).showsScope)
        #expect(!model(labelled([3], scope: [3]), showsScope: false).showsScope)
        #expect(!model(labelled([3])).showsScope)
        #expect(Tokens.lines(model(labelled([3]))) == [.remaining, .ideal])
        #expect(Tokens.visible(model(labelled([3], scope: [3])), hidden: [.ideal]) == [.remaining, .scope])
    }

    @Test("Scope is a step line that holds its value until the day it changes")
    func scopeSteps() {
        let runs = Tokens.scopeRuns([40, 40, 45, nil, 43, 41])
        #expect(
            runs == [
                [.init(x: 0, y: 40), .init(x: 1, y: 40), .init(x: 2, y: 40), .init(x: 2, y: 45)],
                [.init(x: 4, y: 43), .init(x: 5, y: 43), .init(x: 5, y: 41)],
            ]
        )
        #expect(Tokens.lineRuns([1, nil, 3, 4]) == [[.init(x: 0, y: 1)], [.init(x: 2, y: 3), .init(x: 3, y: 4)]])
    }

    @Test("The trend compares the latest remaining with the ideal that day, to two decimals")
    func trend() {
        #expect(Tokens.trend(model(labelled([10, 8, nil]))) == .behind(3))
        #expect(Tokens.trend(model(labelled([10, 2, nil]))) == .ahead(3))
        #expect(Tokens.trend(model(labelled([10, 5, nil]))) == .onTrack)
        #expect(Tokens.trend(model(labelled([nil, nil]))) == .notStarted)
        // 0.1 + 0.2 is 0.30000000000000004: float noise is on track, not behind.
        #expect(Tokens.trend(model(labelled([0.1 + 0.2, nil]), idealStart: 0.3)) == .onTrack)
    }

    @Test("The summary says where the sprint stands and how scope changed")
    func summary() {
        let labels = MonarchBurndownChartLabels()
        let sprint = model(labelled([40, 38, 35, 37, nil], scope: [40, 40, 45, 43, nil]))
        #expect(
            Tokens.summary(sprint, labels: labels, locale: english, formatValue: nil)
                == "D4: 37 remaining, ideal 10, 27 behind ideal. Scope 43, 5 added and 2 removed since the start"
        )
        let ahead = model(labelled([10, 2, nil]))
        #expect(
            Tokens.summary(ahead, labels: labels, locale: english, formatValue: { "\(Int($0)) pts" })
                == "D2: 2 pts remaining, ideal 5 pts, 3 pts ahead of ideal"
        )
        #expect(
            Tokens.summary(model(labelled([10, 5, nil])), labels: labels, locale: english, formatValue: nil)
                == "D2: 5 remaining, ideal 5, on the ideal line"
        )
        #expect(
            Tokens.summary(model(labelled([nil])), labels: labels, locale: english, formatValue: nil) == "Not started"
        )
    }

    @Test("A scope change is written with its sign, and no change has no text")
    func changeText() {
        let labels = MonarchBurndownChartLabels()
        #expect(Tokens.changeText(5, labels: labels, locale: english, formatValue: nil) == "+5")
        #expect(Tokens.changeText(-2, labels: labels, locale: english, formatValue: nil) == "−2")
        #expect(Tokens.changeText(0, labels: labels, locale: english, formatValue: nil) == nil)
        let own = MonarchBurndownChartLabels(change: { "\($1 ? "up" : "down") \($0)" })
        #expect(Tokens.changeText(-2, labels: own, locale: english, formatValue: nil) == "down 2")
    }

    @Test("The data table has day, remaining, ideal and, with scope, scope and its change")
    func table() {
        let labels = MonarchBurndownChartLabels()
        let withScope = Tokens.table(
            model(labelled([40, 38, nil], scope: [40, 45, nil])),
            labels: labels,
            locale: english,
            formatValue: nil
        )
        #expect(withScope.categoryTitle == "Day")
        #expect(withScope.categories == ["D1", "D2", "D3"])
        #expect(withScope.series.map(\.name) == ["Remaining", "Ideal", "Scope", "Scope change"])
        #expect(withScope.series[3].formattedValues == ["0", "+5", ""])
        #expect(
            MonarchChartFrameTokens.rowAccessibilityLabel(withScope, row: 1, noValue: "No value")
                == "D2, Remaining 38, Ideal 20, Scope 45, Scope change +5"
        )
        let plain = Tokens.table(model(labelled([3, nil])), labels: labels, locale: english, formatValue: nil)
        #expect(plain.series.map(\.name) == ["Remaining", "Ideal"])
    }

    @Test("The legend names each line; ideal is neutral and dashed")
    func legend() {
        let legend = Tokens.legend(model(labelled([3], scope: [3])), labels: MonarchBurndownChartLabels())
        #expect(legend.map(\.key) == ["remaining", "ideal", "scope"])
        #expect(legend.map(\.label) == ["Remaining", "Ideal", "Scope"])
        #expect(legend.map(\.color) == [.data1, .neutral, .data2])
        #expect(legend.map(\.isDashed) == [false, true, false])
    }

    @Test("The popover lists the visible lines and the scope change that day")
    func tooltip() {
        let labels = MonarchBurndownChartLabels()
        let sprint = model(labelled([40, nil], scope: [40, 45]))
        let rows = Tokens.tooltipRows(
            sprint,
            at: 1,
            visible: [.remaining, .scope],
            labels: labels,
            noValue: "No value",
            locale: english,
            formatValue: nil
        )
        #expect(rows.map(\.value) == ["No value", "45"])
        #expect(rows.map(\.role) == [\MonarchColorTokens.data1, \MonarchColorTokens.data2])
        #expect(
            Tokens.tooltipFooter(sprint, at: 1, labels: labels, locale: english, formatValue: nil) == "Scope change +5"
        )
        #expect(Tokens.tooltipFooter(sprint, at: 0, labels: labels, locale: english, formatValue: nil) == nil)
        #expect(Tokens.tooltipFooter(sprint, at: 7, labels: labels, locale: english, formatValue: nil) == nil)
    }

    @Test("The x axis spans the days, labels at most five of them and maps touches to days")
    func axis() {
        #expect(Tokens.xDomain(count: 10) == 0...9)
        #expect(Tokens.xDomain(count: 1) == -1...1)
        #expect(Tokens.tickIndices(count: 10) == [0, 3, 6, 9])
        #expect(Tokens.tickIndices(count: 14) == [0, 4, 8, 13])
        #expect(Tokens.tickIndices(count: 5) == [0, 1, 2, 3, 4])
        #expect(Tokens.tickIndices(count: 1) == [0])
        #expect(Tokens.tickIndices(count: 0).isEmpty)
        #expect(Tokens.tickIndices(count: 30).count <= Tokens.maximumTicks)
        #expect(Tokens.index(at: 2.4, count: 5) == 2)
        #expect(Tokens.index(at: 2.6, count: 5) == 3)
        #expect(Tokens.index(at: -3, count: 5) == 0)
        #expect(Tokens.index(at: 99, count: 5) == 4)
        #expect(Tokens.index(at: 1, count: 0) == nil)
    }

    @Test("Dated points keep their date and labelled points their text")
    func points() {
        let date = Date(timeIntervalSince1970: 0)
        let dated = MonarchBurndownChartPoint(date: date, remaining: 3)
        #expect(dated.date == date && dated.label == nil && dated.scope == nil)
        let named = MonarchBurndownChartPoint(label: "Mon", scope: 4)
        #expect(named.label == "Mon" && named.date == nil && named.remaining == nil)
    }

    @Test("The built-in strings come from the component's strings table")
    func strings() {
        typealias Strings = MonarchBurndownChartStrings
        #expect(Strings.remaining == "Remaining")
        #expect(Strings.ideal == "Ideal")
        #expect(Strings.scope == "Scope")
        #expect(Strings.day == "Day")
        #expect(Strings.today == "Today")
        #expect(Strings.scopeChange == "Scope change")
        #expect(Strings.change("5", added: true) == "+5")
        #expect(Strings.status(day: "Mar 6", remaining: "24", ideal: "17.8") == "Mar 6: 24 remaining, ideal 17.8")
        #expect(Strings.ahead("2") == "2 ahead of ideal")
        #expect(Strings.behind("2") == "2 behind ideal")
        #expect(Strings.onTrack == "on the ideal line")
        #expect(
            Strings.scopeSummary(scope: "43", added: "5", removed: "2")
                == "Scope 43, 5 added and 2 removed since the start"
        )
        #expect(Strings.notStarted == "Not started")
    }
}
