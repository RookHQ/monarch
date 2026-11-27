import Foundation
import MonarchTokens
import SwiftUI
import Testing

@testable import MonarchUI

enum MonarchUptimeBarFixtures: ComponentFixtures {
    static let id = "uptime-bar"

    /// Thirty days with every state, an incident note and details.
    static let days: [MonarchUptimeBarSegment] = (0..<30).map { day in
        switch day {
        case 3: MonarchUptimeBarSegment(id: "d\(day)", label: "Mar \(day + 1)", status: .maintenance, uptime: 100)
        case 8:
            MonarchUptimeBarSegment(
                id: "d\(day)",
                label: "Mar \(day + 1)",
                uptime: 99.4,
                details: [
                    MonarchUptimeBarDetail("Checks", value: "1,440"), MonarchUptimeBarDetail("p95", value: "410 ms"),
                ],
                note: "Slow responses from the search API"
            )
        case 12: MonarchUptimeBarSegment(id: "d\(day)", label: "Mar \(day + 1)", status: .partial, uptime: 98.2)
        case 15: MonarchUptimeBarSegment(id: "d\(day)", label: "Mar \(day + 1)", uptime: 96.5)
        case 20: MonarchUptimeBarSegment(id: "d\(day)", label: "Mar \(day + 1)")
        default: MonarchUptimeBarSegment(id: "d\(day)", label: "Mar \(day + 1)", uptime: 100)
        }
    }

    static let fixtures = [
        Fixture("status page bar with header, ends and legend") {
            MonarchUptimeBar(
                "Search API",
                segments: days,
                description: "Last 30 days",
                startLabel: "30 days ago",
                endLabel: "Today",
                showsLegend: true
            )
        },
        Fixture("small, inspected, without overall uptime") {
            MonarchUptimeBar(segments: days, showsOverallUptime: false, size: .sm, activeIndex: .constant(8))
        },
        Fixture("given overall, custom labels, digits and thresholds") {
            MonarchUptimeBar(
                "A very long service name that has to wrap onto a second line at large text sizes",
                segments: days,
                overallUptime: 99.123,
                thresholds: MonarchUptimeBarThresholds(operational: 99.99, degraded: 95),
                uptimeDigits: 1,
                summary: "Thirty days of the search API",
                labels: MonarchUptimeBarLabels(
                    statusNames: [.operational: "Up"],
                    overall: { "Overall \($0)" }
                ),
                onSegmentSelect: { _ in }
            )
        },
        Fixture("no data and no segments") {
            VStack(alignment: .leading) {
                MonarchUptimeBar(
                    "New monitor",
                    segments: (0..<7).map { MonarchUptimeBarSegment(id: "\($0)", label: "Day \($0)") }
                )
                MonarchUptimeBar("Empty", segments: [])
            }
        },
        Fixture("details popover content") {
            VStack(alignment: .leading) {
                MonarchUptimeBarCard(segment: days[8], status: .degraded, digits: 2, labels: MonarchUptimeBarLabels())
                MonarchUptimeBarCard(segment: days[20], status: .noData, digits: 2, labels: MonarchUptimeBarLabels())
            }
        },
    ]
}

@Suite("MonarchUptimeBar")
struct MonarchUptimeBarTests {
    private let english = Locale(identifier: "en_US")
    private let thresholds = MonarchUptimeBarThresholds()

    @Test("A segment's own status wins; otherwise the uptime and thresholds decide")
    func status() {
        func status(_ uptime: Double?, _ given: MonarchUptimeBarStatus? = nil) -> MonarchUptimeBarStatus {
            MonarchUptimeBarTokens.status(
                MonarchUptimeBarSegment(id: "a", label: "a", status: given, uptime: uptime),
                thresholds: thresholds
            )
        }
        #expect(status(100) == .operational)
        #expect(status(99.9) == .operational)
        #expect(status(99.5) == .degraded)
        #expect(status(99) == .degraded)
        #expect(status(98.99) == .down)
        #expect(status(nil) == .noData)
        #expect(status(.nan) == .noData)
        #expect(status(10, .maintenance) == .maintenance)
        let strict = MonarchUptimeBarThresholds(operational: 100, degraded: 50)
        #expect(
            MonarchUptimeBarTokens.status(
                MonarchUptimeBarSegment(id: "a", label: "a", uptime: 99.99),
                thresholds: strict
            )
                == .degraded
        )
    }

    @Test("Every state has its own colour role, and no data is muted")
    func roles() {
        let roles = MonarchUptimeBarStatus.allCases.map(MonarchUptimeBarTokens.role)
        #expect(
            roles == [
                \MonarchColorTokens.successSolid, \MonarchColorTokens.warningSolid, \MonarchColorTokens.warningStrong,
                \MonarchColorTokens.dangerSolid, \MonarchColorTokens.infoSolid, \MonarchColorTokens.bgMuted,
            ]
        )
        #expect(Set(roles.map { "\($0)" }).count == roles.count)
        #expect(MonarchUptimeBarTokens.height(.sm) < MonarchUptimeBarTokens.height(.md))
    }

    @Test("The overall uptime is the mean of the measured segments, or the given value")
    func overall() {
        let segments = [
            MonarchUptimeBarSegment(id: "a", label: "a", uptime: 100),
            MonarchUptimeBarSegment(id: "b", label: "b", uptime: 99),
            MonarchUptimeBarSegment(id: "c", label: "c"),
            MonarchUptimeBarSegment(id: "d", label: "d", uptime: .infinity),
        ]
        #expect(MonarchUptimeBarTokens.overall(segments, given: nil) == 99.5)
        #expect(MonarchUptimeBarTokens.overall(segments, given: 98) == 98)
        #expect(MonarchUptimeBarTokens.overall([segments[2]], given: nil) == nil)
    }

    @Test("Percentages keep their decimals in the locale")
    func percent() {
        #expect(MonarchUptimeBarTokens.percent(99.95, digits: 2, locale: english) == "99.95%")
        #expect(MonarchUptimeBarTokens.percent(100, digits: 2, locale: english) == "100.00%")
        #expect(MonarchUptimeBarTokens.percent(99.4, digits: 0, locale: english) == "99%")
        #expect(MonarchUptimeBarTokens.percent(99.95, digits: 2, locale: Locale(identifier: "de_DE")).contains("99,95"))
    }

    @Test("The summary counts each state present and ends with the overall uptime")
    func summary() {
        let counts = MonarchUptimeBarTokens.counts([.operational, .operational, .down, .noData])
        #expect(counts == [.operational: 2, .down: 1, .noData: 1])
        let text = MonarchUptimeBarTokens.summary(
            count: 4,
            counts: counts,
            overall: "99.50%",
            labels: MonarchUptimeBarLabels(),
            locale: english
        )
        #expect(text == "Periods: 4. Operational 2, Down 1, No data 1. Uptime 99.50%.")
        let empty = MonarchUptimeBarTokens.summary(
            count: 0,
            counts: [:],
            overall: nil,
            labels: MonarchUptimeBarLabels(),
            locale: english
        )
        #expect(empty == "Periods: 0.")
        let custom = MonarchUptimeBarTokens.summary(
            count: 4,
            counts: counts,
            overall: nil,
            labels: MonarchUptimeBarLabels(summary: { count, counts, _ in "\(count) days, \(counts[.down] ?? 0) down" }
            ),
            locale: english
        )
        #expect(custom == "4 days, 1 down")
    }

    @Test("A segment is read with its state, uptime, details and note")
    func segmentText() {
        let segment = MonarchUptimeBarFixtures.days[8]
        #expect(
            MonarchUptimeBarTokens.segmentText(
                segment,
                status: .degraded,
                digits: 2,
                labels: MonarchUptimeBarLabels(),
                locale: english
            ) == "Mar 9: Degraded, Uptime 99.40%, Checks 1,440, p95 410 ms, Slow responses from the search API"
        )
        let renamed = MonarchUptimeBarLabels(statusNames: [.noData: "Not checked"], uptime: "Up")
        #expect(
            MonarchUptimeBarTokens.segmentText(
                MonarchUptimeBarSegment(id: "x", label: "Mar 1"),
                status: .noData,
                digits: 2,
                labels: renamed,
                locale: english
            ) == "Mar 1: Not checked"
        )
    }

    @Test("A tap finds its segment from the leading edge, in both directions")
    func index() {
        #expect(MonarchUptimeBarTokens.index(at: 0, width: 300, count: 30, isRightToLeft: false) == 0)
        #expect(MonarchUptimeBarTokens.index(at: 15, width: 300, count: 30, isRightToLeft: false) == 1)
        #expect(MonarchUptimeBarTokens.index(at: 300, width: 300, count: 30, isRightToLeft: false) == 29)
        #expect(MonarchUptimeBarTokens.index(at: 0, width: 300, count: 30, isRightToLeft: true) == 29)
        #expect(MonarchUptimeBarTokens.index(at: 295, width: 300, count: 30, isRightToLeft: true) == 0)
        #expect(MonarchUptimeBarTokens.index(at: -1, width: 300, count: 30, isRightToLeft: false) == nil)
        #expect(MonarchUptimeBarTokens.index(at: 10, width: 300, count: 0, isRightToLeft: false) == nil)
    }

    @Test("VoiceOver swipes move through the periods and stop at the ends")
    func adjust() {
        #expect(MonarchUptimeBarTokens.adjusted(nil, increment: true, count: 5) == 0)
        #expect(MonarchUptimeBarTokens.adjusted(nil, increment: false, count: 5) == 4)
        #expect(MonarchUptimeBarTokens.adjusted(2, increment: true, count: 5) == 3)
        #expect(MonarchUptimeBarTokens.adjusted(4, increment: true, count: 5) == 4)
        #expect(MonarchUptimeBarTokens.adjusted(0, increment: false, count: 5) == 0)
        #expect(MonarchUptimeBarTokens.adjusted(nil, increment: true, count: 0) == nil)
    }

    @Test("The chart descriptor has one point per period with its state and details")
    func descriptor() {
        let descriptor = MonarchUptimeBarDescriptor(
            title: "API",
            summary: "Periods: 2.",
            periods: ["Mar 1", "Mar 2"],
            uptimes: [100, nil],
            statusNames: ["Operational", "No data"],
            allStatusNames: MonarchUptimeBarStatus.allCases.map(MonarchUptimeBarStrings.status),
            texts: ["Mar 1: Operational", "Mar 2: No data"],
            digits: 2,
            labels: MonarchUptimeBarLabels()
        )
        let points = descriptor.points()
        #expect(points.count == 2)
        #expect(points[0].yValue != nil)
        #expect(points[1].yValue == nil)
        #expect(points.map(\.label) == ["Mar 1: Operational", "Mar 2: No data"])
        let chart = descriptor.makeChartDescriptor()
        #expect(chart.title == "API")
        #expect(chart.series.count == 1)
        #expect(chart.additionalAxes.count == 1)
    }

    @Test("The built-in strings come from the component's strings table")
    func strings() {
        #expect(
            MonarchUptimeBarStatus.allCases.map(MonarchUptimeBarStrings.status) == [
                "Operational", "Degraded", "Partial outage", "Down", "Maintenance", "No data",
            ]
        )
        #expect(MonarchUptimeBarStrings.uptime == "Uptime")
        #expect(MonarchUptimeBarStrings.overall("99.95%") == "99.95% uptime")
        #expect(MonarchUptimeBarStrings.periods("30") == "Periods: 30")
        #expect(MonarchUptimeBarStrings.period == "Period")
        #expect(MonarchUptimeBarStrings.status == "Status")
        #expect(MonarchUptimeBarStrings.legend == "Legend")
    }
}
