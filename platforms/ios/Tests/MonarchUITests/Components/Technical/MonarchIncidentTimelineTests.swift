import Foundation
import MonarchTokens
import SwiftUI
import Testing

@testable import MonarchUI

private let start = Date(timeIntervalSinceReferenceDate: 800_000_000)

private let story = [
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
        body: "A database migration locked the orders table.\nWe are rolling it back.",
        time: start.addingTimeInterval(900),
        author: "Ada Lovelace"
    ),
    MonarchIncidentUpdate(
        id: "3",
        stage: .monitoring,
        body: "The rollback is done.",
        time: start.addingTimeInterval(1_800)
    ),
    MonarchIncidentUpdate(
        id: "4",
        stage: .resolved,
        body: "Payments are back to normal.",
        time: start.addingTimeInterval(3_600)
    ),
]

enum MonarchIncidentTimelineFixtures: ComponentFixtures {
    static let id = "incident-timeline"
    static let fixtures = [
        Fixture("every standard stage") {
            MonarchIncidentTimeline(story)
        },
        Fixture("compact, custom stage, other time zone") {
            MonarchIncidentTimeline(
                [
                    MonarchIncidentUpdate(id: "a", stage: "partial_outage", body: "Some uploads fail.", time: start),
                    MonarchIncidentUpdate(id: "b", stage: "maintenance", body: "Planned work.", time: start),
                ],
                stages: ["maintenance": MonarchStatusDefinition("Scheduled maintenance", tone: .info)],
                density: .compact,
                timeZone: TimeZone(identifier: "America/New_York") ?? .gmt
            )
        },
        Fixture("live, long text") {
            MonarchIncidentTimeline(
                [
                    MonarchIncidentUpdate(
                        id: "x",
                        stage: .investigating,
                        body: String(repeating: "We are looking into elevated error rates on the API. ", count: 8),
                        time: start,
                        isAutomated: true,
                        author: "Status bot for the European region"
                    )
                ],
                isLive: true
            )
        },
        Fixture("empty") {
            MonarchIncidentTimeline([], labels: MonarchIncidentTimelineLabels(empty: "Nothing yet"))
        },
    ]
}

@Suite("MonarchIncidentTimeline")
struct MonarchIncidentTimelineTests {
    typealias Tokens = MonarchIncidentTimelineTokens

    @Test("Standard stages have their own label and tone, matched ignoring case")
    func standardStages() {
        let labels = MonarchIncidentTimelineLabels()
        #expect(
            Tokens.resolve("investigating", stages: [:], labels: labels)
                == MonarchStatusDefinition("Investigating", tone: .danger)
        )
        #expect(
            Tokens.resolve("Identified", stages: [:], labels: labels)
                == MonarchStatusDefinition("Identified", tone: .warning)
        )
        #expect(
            Tokens.resolve("MONITORING", stages: [:], labels: labels)
                == MonarchStatusDefinition("Monitoring", tone: .info)
        )
        #expect(
            Tokens.resolve("resolved", stages: [:], labels: labels)
                == MonarchStatusDefinition("Resolved", tone: .success)
        )
        let tones = Set(MonarchIncidentStage.allCases.map(Tokens.tone))
        #expect(tones.count == MonarchIncidentStage.allCases.count)
    }

    @Test("Product stages come first; unknown keys are readable and neutral")
    func customStages() {
        let labels = MonarchIncidentTimelineLabels()
        let custom = MonarchStatusDefinition("Fixed", tone: .success)
        #expect(Tokens.resolve("resolved", stages: ["resolved": custom], labels: labels) == custom)
        #expect(
            Tokens.resolve("partial_outage", stages: [:], labels: labels)
                == MonarchStatusDefinition("Partial outage", tone: .neutral)
        )
        #expect(Tokens.readable("data-loss_risk") == "Data loss risk")
        #expect(Tokens.readable("__") == "__")
        #expect(Tokens.readable("") == "")
    }

    @Test("Stage labels can be replaced")
    func labels() {
        let labels = MonarchIncidentTimelineLabels(
            list: "Updates",
            automated: "Bot",
            empty: "None",
            investigating: "Untersuchung",
            identified: "Erkannt",
            monitoring: "Beobachtung",
            resolved: "Behoben"
        )
        #expect(labels.resolvedList == "Updates")
        #expect(labels.resolvedAutomated == "Bot")
        #expect(labels.resolvedEmpty == "None")
        #expect(
            MonarchIncidentStage.allCases.map(labels.stageLabel) == [
                "Untersuchung", "Erkannt", "Beobachtung", "Behoben",
            ]
        )
        let builtIn = MonarchIncidentTimelineLabels()
        #expect(builtIn.resolvedList == "Incident updates")
        #expect(builtIn.resolvedAutomated == "Automated")
        #expect(builtIn.resolvedEmpty == "No updates posted yet")
    }

    @Test("Times use the given time zone and name it")
    func time() {
        let locale = Locale(identifier: "en_US")
        let utc = Tokens.time(start, timeZone: .gmt, locale: locale)
        #expect(utc.contains("May"))
        #expect(utc.contains("06:13"))
        #expect(utc.contains("GMT") || utc.contains("UTC"))
        let tokyo = Tokens.time(start, timeZone: TimeZone(identifier: "Asia/Tokyo") ?? .gmt, locale: locale)
        #expect(tokyo.contains("15:13"))
    }

    @Test("The zone defaults to UTC, a given zone wins, and nil is the device zone (D3)")
    @MainActor
    func zoneParameter() {
        #expect(MonarchIncidentTimeline([]).resolvedTimeZone == TimeZone(identifier: "UTC"))
        #expect(MonarchIncidentTimeline([]).resolvedTimeZone.secondsFromGMT() == 0)
        let tokyo = TimeZone(identifier: "Asia/Tokyo")
        #expect(MonarchIncidentTimeline([], timeZone: tokyo).resolvedTimeZone.identifier == "Asia/Tokyo")
        #expect(MonarchIncidentTimeline([], timeZone: nil).resolvedTimeZone == TimeZone.autoupdatingCurrent)
    }

    @Test("Each update is read as stage, time, author, tag, then body")
    func accessibilityLabel() {
        #expect(
            Tokens.accessibilityLabel(
                stage: "Investigating",
                time: "Nov 14, 09:30 GMT",
                author: nil,
                automated: "Automated",
                body: "Down."
            )
                == "Investigating, Nov 14, 09:30 GMT, Automated. Down."
        )
        #expect(
            Tokens.accessibilityLabel(stage: "Resolved", time: "T", author: "Ada", automated: nil, body: "")
                == "Resolved, T, Ada"
        )
    }

    @Test("Only an added update is announced")
    func newUpdate() {
        #expect(Tokens.newUpdate(story, previousCount: 3)?.id == "4")
        #expect(Tokens.newUpdate(story, previousCount: 4) == nil)
        #expect(Tokens.newUpdate(Array(story.prefix(2)), previousCount: 4) == nil)
        #expect(Tokens.newUpdate([], previousCount: 0) == nil)
    }

    @Test("Markers take the solid tone, neutral takes the strong border")
    func markers() {
        #expect(Tokens.markerRole(.neutral) == \.borderStrong)
        #expect(Tokens.markerRole(.danger) == \.dangerSolid)
        #expect(Tokens.markerRole(.success) == \.successSolid)
        #expect(Tokens.rowSpacing(.compact) < Tokens.rowSpacing(.regular))
    }

    @Test("A standard stage update stores its raw key")
    func update() {
        #expect(story[0].stage == "investigating")
        #expect(!story[1].isAutomated)
    }
}
