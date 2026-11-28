import Foundation
import MonarchTokens
import SwiftUI
import Testing

@testable import MonarchUI

enum MonarchMyStatusFixtures: ComponentFixtures {
    static let id = "my-status"
    /// A fixed clock, so the fixtures never depend on the time they run.
    static let now = Date(timeIntervalSinceReferenceDate: 800_000_000)

    static let fixtures = [
        Fixture("custom status with emoji") {
            MonarchMyStatus(
                "Ana Silva",
                image: nil,
                presence: .online,
                customStatus: MonarchMyStatusCustomStatus("On holiday", emoji: "🌴"),
                now: now
            ) {}
        },
        Fixture("every presence, no status") {
            VStack(alignment: .leading) {
                ForEach(MonarchPresenceStatus.allCases, id: \.self) { presence in
                    MonarchMyStatus("Ben Okafor", image: nil, presence: presence, now: now) {}
                }
            }
        },
        Fixture("expired status shows the invitation") {
            MonarchMyStatus(
                "Chen Wei",
                url: nil,
                presence: .online,
                customStatus: MonarchMyStatusCustomStatus("In a meeting", until: now.addingTimeInterval(-60)),
                now: now
            ) {}
        },
        Fixture("long name and status") {
            MonarchMyStatus(
                "Maria Fernanda Gonçalves de Albuquerque",
                image: nil,
                presence: .busy,
                customStatus: MonarchMyStatusCustomStatus(
                    "Presenting the quarterly results to the board until the end of the afternoon"
                ),
                now: now
            ) {}
        },
        Fixture("live clock") {
            MonarchMyStatus(
                "Dara Kim",
                image: Image(systemName: "person.crop.square"),
                presence: .away,
                customStatus: MonarchMyStatusCustomStatus("Lunch", until: now)
            ) {}
        },
    ]
}

@Suite("MonarchMyStatus")
struct MonarchMyStatusTests {
    private let labels = MonarchMyStatusLabels()
    private let now = MonarchMyStatusFixtures.now

    @Test("A status ends at its until time, not before")
    func expiry() {
        let status = MonarchMyStatusCustomStatus("Lunch", until: now)
        #expect(MonarchMyStatusTokens.isExpired(status, now: now))
        #expect(!MonarchMyStatusTokens.isExpired(status, now: now.addingTimeInterval(-1)))
        #expect(!MonarchMyStatusTokens.isExpired(MonarchMyStatusCustomStatus("Away"), now: now))
    }

    @Test("Blank and ended statuses are not active")
    func active() {
        #expect(MonarchMyStatusTokens.activeStatus(MonarchMyStatusCustomStatus("  "), now: now) == nil)
        #expect(MonarchMyStatusTokens.activeStatus(MonarchMyStatusCustomStatus(emoji: "🌴"), now: now) != nil)
        #expect(MonarchMyStatusTokens.activeStatus(nil, now: now) == nil)
        let ended = MonarchMyStatusCustomStatus("Lunch", until: now.addingTimeInterval(-1))
        #expect(MonarchMyStatusTokens.activeStatus(ended, now: now) == nil)
    }

    @Test("The line is the status, else a telling presence, else the invitation")
    func line() {
        let status = MonarchMyStatusCustomStatus("On holiday", emoji: "🌴")
        #expect(
            MonarchMyStatusTokens.line(presence: .online, status: status, now: now, labels: labels)
                == .init(text: "🌴 On holiday", isPlaceholder: false)
        )
        #expect(
            MonarchMyStatusTokens.line(presence: .away, status: nil, now: now, labels: labels)
                == .init(text: "Away", isPlaceholder: false)
        )
        #expect(
            MonarchMyStatusTokens.line(presence: .online, status: nil, now: now, labels: labels)
                == .init(text: "Set a status", isPlaceholder: true)
        )
        #expect(
            MonarchMyStatusTokens.line(presence: .offline, status: nil, now: now, labels: labels).isPlaceholder
        )
        let custom = MonarchMyStatusLabels(setStatus: "Définir un statut")
        #expect(
            MonarchMyStatusTokens.line(presence: .online, status: nil, now: now, labels: custom).text
                == "Définir un statut"
        )
    }

    @Test("VoiceOver reads the presence, then the line when it differs")
    func value() {
        let status = MonarchMyStatusCustomStatus("On holiday")
        #expect(
            MonarchMyStatusTokens.accessibilityValue(presence: .online, status: status, now: now, labels: labels)
                == "Online, On holiday"
        )
        #expect(
            MonarchMyStatusTokens.accessibilityValue(presence: .busy, status: nil, now: now, labels: labels) == "Busy"
        )
        #expect(
            MonarchMyStatusTokens.accessibilityValue(presence: .offline, status: nil, now: now, labels: labels)
                == "Offline, Set a status"
        )
        let custom = MonarchMyStatusLabels(presence: { _ in "En ligne" })
        #expect(
            MonarchMyStatusTokens.accessibilityValue(presence: .online, status: status, now: now, labels: custom)
                == "En ligne, On holiday"
        )
    }

    @Test("The row is drawn again when a status ends, from an injected start")
    func redraw() {
        let later = now.addingTimeInterval(3600)
        #expect(MonarchMyStatusTokens.redrawDates(from: now, until: later) == [now, later])
        #expect(MonarchMyStatusTokens.redrawDates(from: now, until: now) == [now])
        #expect(MonarchMyStatusTokens.redrawDates(from: now, until: nil) == [now])
        let schedule = MonarchMyStatusSchedule(until: later)
        #expect(schedule.entries(from: now, mode: .normal) == [now, later])
    }

    @Test("Pressed rows get a background; the invitation is quieter than a status")
    func colours() {
        #expect(MonarchMyStatusTokens.background(isPressed: true) == \.bgMuted)
        #expect(MonarchMyStatusTokens.background(isPressed: false) == nil)
        #expect(MonarchMyStatusTokens.lineRole(isPlaceholder: true) == \.textSubtle)
        #expect(MonarchMyStatusTokens.lineRole(isPlaceholder: false) == \.textMuted)
    }

    @Test("Every built-in string resolves")
    func strings() {
        #expect(MonarchMyStatusStrings.hint == "Opens your status and settings")
        #expect(MonarchMyStatusStrings.setStatus == "Set a status")
    }

    #if os(iOS)
        @Test("The row is at least 44 points high")
        @MainActor func hitTarget() {
            let size = Host.layout(
                MonarchMyStatus("A", image: nil, presence: .online, now: MonarchMyStatusFixtures.now) {}
            )
            #expect(size.height >= MonarchAccessibility.minimumHitTarget)
        }
    #endif
}
