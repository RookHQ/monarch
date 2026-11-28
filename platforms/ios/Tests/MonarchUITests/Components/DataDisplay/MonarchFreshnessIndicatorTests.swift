import Foundation
import MonarchTokens
import SwiftUI
import Testing

@testable import MonarchUI

enum MonarchFreshnessIndicatorFixtures: ComponentFixtures {
    static let id = "freshness-indicator"
    static let fixtures = [
        Fixture("fresh with source and refresh") {
            MonarchFreshnessIndicator(updatedAt: .now.addingTimeInterval(-180), source: "Directory") {}
        },
        Fixture("stale banner") {
            MonarchFreshnessIndicator(
                updatedAt: .now.addingTimeInterval(-7200),
                source: "Registry",
                staleAfter: .seconds(3600),
                variant: .banner
            ) {}
        },
        Fixture("expired with details") {
            MonarchFreshnessIndicator(
                updatedAt: .now.addingTimeInterval(-200_000),
                details: ["Events on", "3 failures"],
                staleAfter: .seconds(3600),
                expiredAfter: .seconds(86_400)
            )
        },
        Fixture("never, checking and error") {
            VStack(alignment: .leading) {
                MonarchFreshnessIndicator(updatedAt: nil)
                MonarchFreshnessIndicator(updatedAt: nil, status: .checking, variant: .banner)
                MonarchFreshnessIndicator(updatedAt: nil, error: "The directory could not be reached", status: .error)
            }
        },
        Fixture("refreshing, long text") {
            MonarchFreshnessIndicator(
                updatedAt: .now.addingTimeInterval(-600),
                source: "Customer directory from the identity provider",
                details: ["Automatic sync every hour"],
                error: "The last sync was rejected by the server",
                status: .error,
                variant: .banner,
                isRefreshing: true
            ) {}
        },
    ]
}

@Suite("MonarchFreshnessIndicator")
struct MonarchFreshnessIndicatorTests {
    private let now = Date(timeIntervalSince1970: 1_800_000_000)
    private let english = Locale(identifier: "en_US")

    @Test("The status follows the age and the limits")
    func status() {
        let hour: TimeInterval = 3600
        #expect(
            MonarchFreshnessIndicatorTokens.status(updatedAt: nil, now: now, staleAfter: hour, expiredAfter: nil)
                == .never
        )
        #expect(
            MonarchFreshnessIndicatorTokens.status(
                updatedAt: now.addingTimeInterval(-60),
                now: now,
                staleAfter: hour,
                expiredAfter: 24 * hour
            ) == .fresh
        )
        #expect(
            MonarchFreshnessIndicatorTokens.status(
                updatedAt: now.addingTimeInterval(-2 * hour),
                now: now,
                staleAfter: hour,
                expiredAfter: 24 * hour
            ) == .stale
        )
        #expect(
            MonarchFreshnessIndicatorTokens.status(
                updatedAt: now.addingTimeInterval(-25 * hour),
                now: now,
                staleAfter: hour,
                expiredAfter: 24 * hour
            ) == .expired
        )
        #expect(
            MonarchFreshnessIndicatorTokens.status(
                updatedAt: now.addingTimeInterval(-100 * hour),
                now: now,
                staleAfter: nil,
                expiredAfter: nil
            ) == .fresh
        )
        // Exactly at the limit is still fresh.
        #expect(
            MonarchFreshnessIndicatorTokens.status(
                updatedAt: now.addingTimeInterval(-hour),
                now: now,
                staleAfter: hour,
                expiredAfter: nil
            ) == .fresh
        )
    }

    @Test("Each status has its tone, dot and word colour")
    func colours() {
        #expect(MonarchFreshnessIndicatorTokens.tone(.fresh) == .success)
        #expect(MonarchFreshnessIndicatorTokens.tone(.stale) == .warning)
        #expect(MonarchFreshnessIndicatorTokens.tone(.expired) == .danger)
        #expect(MonarchFreshnessIndicatorTokens.tone(.error) == .danger)
        #expect(MonarchFreshnessIndicatorTokens.tone(.checking) == .info)
        #expect(MonarchFreshnessIndicatorTokens.tone(.never) == .neutral)
        #expect(MonarchFreshnessIndicatorTokens.dot(.fresh) == \MonarchColorTokens.successSolid)
        #expect(MonarchFreshnessIndicatorTokens.dot(.never) == \MonarchColorTokens.textSubtle)
        #expect(MonarchFreshnessIndicatorTokens.flag(.stale) == \MonarchColorTokens.warningStrong)
        #expect(MonarchFreshnessIndicatorTokens.flag(.expired) == \MonarchColorTokens.dangerStrong)
    }

    @Test("The parts say the age and add the status words")
    func parts() {
        let labels = MonarchFreshnessIndicatorLabels()
        let stale = MonarchFreshnessIndicatorTokens.parts(
            status: .stale,
            updatedAt: now.addingTimeInterval(-7200),
            now: now,
            error: nil,
            details: ["Events on", ""],
            labels: labels,
            locale: english
        )
        #expect(stale.map(\.text) == ["Updated 2 hours ago", "Out of date", "Events on"])
        #expect(stale[0].emphasis == nil)
        #expect(stale[1].emphasis == \MonarchColorTokens.warningStrong)

        let never = MonarchFreshnessIndicatorTokens.parts(
            status: .never,
            updatedAt: nil,
            now: now,
            error: nil,
            details: [],
            labels: labels,
            locale: english
        )
        #expect(never.map(\.text) == ["Never updated"])

        let checking = MonarchFreshnessIndicatorTokens.parts(
            status: .checking,
            updatedAt: now,
            now: now,
            error: nil,
            details: [],
            labels: labels,
            locale: english
        )
        #expect(checking.map(\.text) == ["Checking…"])

        let errorWithoutTime = MonarchFreshnessIndicatorTokens.parts(
            status: .error,
            updatedAt: nil,
            now: now,
            error: nil,
            details: [],
            labels: labels,
            locale: english
        )
        #expect(errorWithoutTime.map(\.text) == ["Status unavailable"])
        #expect(errorWithoutTime[0].emphasis == \MonarchColorTokens.dangerStrong)

        let errorWithTime = MonarchFreshnessIndicatorTokens.parts(
            status: .error,
            updatedAt: now.addingTimeInterval(-180),
            now: now,
            error: "Sync failed",
            details: [],
            labels: labels,
            locale: english
        )
        #expect(errorWithTime.map(\.text) == ["Updated 3 minutes ago", "Sync failed"])
    }

    @Test("Labels replace every built-in string")
    func labels() {
        let labels = MonarchFreshnessIndicatorLabels(
            updated: { "Synced \($0)" },
            never: "Not synced",
            stale: "Old"
        )
        let parts = MonarchFreshnessIndicatorTokens.parts(
            status: .stale,
            updatedAt: now.addingTimeInterval(-120),
            now: now,
            error: nil,
            details: [],
            labels: labels,
            locale: english
        )
        #expect(parts.map(\.text) == ["Synced 2 minutes ago", "Old"])
        let never = MonarchFreshnessIndicatorTokens.parts(
            status: .never,
            updatedAt: nil,
            now: now,
            error: nil,
            details: [],
            labels: labels,
            locale: english
        )
        #expect(never.map(\.text) == ["Not synced"])
    }

    @Test("VoiceOver reads the source and the parts with pauses")
    func accessibilityLabel() {
        let parts = [
            MonarchFreshnessIndicatorTokens.Part(text: "Updated 2 hours ago", emphasis: nil),
            MonarchFreshnessIndicatorTokens.Part(text: "Out of date", emphasis: \.warningStrong),
        ]
        #expect(
            MonarchFreshnessIndicatorTokens.accessibilityLabel(source: "Directory", parts: parts)
                == "Directory, Updated 2 hours ago, Out of date"
        )
        #expect(
            MonarchFreshnessIndicatorTokens.accessibilityLabel(source: "", parts: parts)
                == "Updated 2 hours ago, Out of date"
        )
    }

    @Test("Only a change to stale, expired or error is announced")
    func announcements() {
        let labels = MonarchFreshnessIndicatorLabels()
        #expect(
            MonarchFreshnessIndicatorTokens.announcement(from: .fresh, to: .stale, error: nil, labels: labels)
                == "Out of date"
        )
        #expect(
            MonarchFreshnessIndicatorTokens.announcement(from: .stale, to: .expired, error: nil, labels: labels)
                == "Very out of date"
        )
        #expect(
            MonarchFreshnessIndicatorTokens.announcement(from: .fresh, to: .error, error: "Offline", labels: labels)
                == "Offline"
        )
        #expect(
            MonarchFreshnessIndicatorTokens.announcement(from: .stale, to: .fresh, error: nil, labels: labels) == nil
        )
        #expect(
            MonarchFreshnessIndicatorTokens.announcement(from: .stale, to: .stale, error: nil, labels: labels) == nil
        )
    }

    @Test("The line is redrawn every minute and just after each limit")
    func schedule() {
        let updatedAt = now.addingTimeInterval(-3570)
        let next = MonarchFreshnessIndicatorTokens.nextUpdate(after: now, updatedAt: updatedAt, limits: [3600])
        #expect(next == updatedAt.addingTimeInterval(3601))
        let later = MonarchFreshnessIndicatorTokens.nextUpdate(after: now, updatedAt: updatedAt, limits: [])
        #expect(later == now.addingTimeInterval(60))
        var entries = MonarchFreshnessSchedule(updatedAt: nil, limits: [])
            .entries(from: now, mode: .normal)
        #expect(entries.next() == now)
        #expect(entries.next() == now.addingTimeInterval(60))
        #expect(MonarchFreshnessIndicatorTokens.seconds(.milliseconds(1500)) == 1.5)
        #expect(MonarchFreshnessIndicatorTokens.seconds(nil) == nil)
    }

    @Test("The built-in strings resolve")
    func strings() {
        #expect(MonarchFreshnessIndicatorStrings.updated("now") == "Updated now")
        #expect(MonarchFreshnessIndicatorStrings.never == "Never updated")
        #expect(MonarchFreshnessIndicatorStrings.checking == "Checking…")
        #expect(MonarchFreshnessIndicatorStrings.stale == "Out of date")
        #expect(MonarchFreshnessIndicatorStrings.expired == "Very out of date")
        #expect(MonarchFreshnessIndicatorStrings.error == "Status unavailable")
        #expect(MonarchFreshnessIndicatorStrings.refresh == "Refresh")
        #expect(MonarchFreshnessIndicatorStrings.refreshing == "Refreshing…")
        #expect(MonarchFreshnessIndicatorStrings.refreshed == "Refreshed.")
        #expect(MonarchFreshnessIndicatorStrings.refreshFailed == "Refresh failed.")
    }
}
