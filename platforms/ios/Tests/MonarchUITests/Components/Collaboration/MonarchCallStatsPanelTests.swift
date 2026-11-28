import Foundation
import MonarchTokens
import SwiftUI
import Testing

@testable import MonarchUI

enum MonarchCallStatsPanelFixtures: ComponentFixtures {
    static let id = "call-stats-panel"

    static let usage = MonarchCallStatsPanelUsage(
        sentBytes: 6_200_000,
        receivedBytes: 9_400_000,
        upBitsPerSecond: 420_000,
        downBitsPerSecond: 1_250_000
    )

    static let peers = [
        MonarchCallStatsPanelPeer(
            id: "a",
            name: "Robin Park",
            roundTripMilliseconds: 38,
            inboundLossPercent: 0.2,
            outboundLossPercent: 0.1,
            quality: .good
        ),
        MonarchCallStatsPanelPeer(
            id: "b",
            name: "Alex Chen",
            roundTripMilliseconds: 186,
            inboundLossPercent: 3.4,
            outboundLossPercent: 1.2,
            quality: .fair
        ),
        MonarchCallStatsPanelPeer(id: "c", name: "Jordan Diaz"),
    ]

    static let fixtures = [
        Fixture("group call with every grade, low data and a footnote") {
            MonarchCallStatsPanel(
                usage: usage,
                peers: peers + [
                    MonarchCallStatsPanelPeer(
                        id: "d",
                        name: "A participant with a very long display name that wraps",
                        roundTripMilliseconds: 640,
                        inboundLossPercent: 12.5,
                        quality: .poor
                    )
                ],
                isLowData: true,
                footnote: "Example footnote: say here how your product routes and protects call media."
            )
        },
        Fixture("alone on the call, nothing measured") {
            MonarchCallStatsPanel(usage: MonarchCallStatsPanelUsage(sentBytes: 0, receivedBytes: 0), peers: [])
        },
        Fixture("custom labels") {
            MonarchCallStatsPanel(
                usage: usage,
                peers: Array(peers.prefix(1)),
                labels: MonarchCallStatsPanelLabels(
                    dataUsed: "Datos usados",
                    usageSummary: { total, up, down in "\(total) (\(up) / \(down))" },
                    roundTrip: "Ida y vuelta",
                    qualityNames: [.good: "Buena"]
                )
            )
        },
    ]
}

@Suite("MonarchCallStatsPanel")
struct MonarchCallStatsPanelTests {
    private let english = Locale(identifier: "en_US")

    @Test("Each grade has a tone, an icon and a name, and unknown is neutral")
    func grades() {
        #expect(
            MonarchCallStatsPanelQuality.allCases.map(MonarchCallStatsPanelTokens.tone) == [
                .success, .warning, .danger, .neutral,
            ]
        )
        #expect(Set(MonarchCallStatsPanelQuality.allCases.map(MonarchCallStatsPanelTokens.symbol)).count == 4)
        #expect(MonarchCallStatsPanelTokens.name(.fair, labels: MonarchCallStatsPanelLabels()) == "Unstable connection")
        #expect(
            MonarchCallStatsPanelTokens.name(.fair, labels: MonarchCallStatsPanelLabels(qualityNames: [.fair: "Shaky"]))
                == "Shaky"
        )
    }

    @Test("Rates use bps, kbps and Mbps")
    func rates() {
        #expect(MonarchCallStatsPanelTokens.rate(800, locale: english) == "800 bps")
        #expect(MonarchCallStatsPanelTokens.rate(64_000, locale: english) == "64 kbps")
        #expect(MonarchCallStatsPanelTokens.rate(1_250_000, locale: english) == "1.3 Mbps")
        #expect(MonarchCallStatsPanelTokens.rate(12_000_000, locale: english) == "12 Mbps")
        #expect(MonarchCallStatsPanelTokens.rate(nil, locale: english) == "0 bps")
        #expect(MonarchCallStatsPanelTokens.rate(-5, locale: english) == "0 bps")
        #expect(MonarchCallStatsPanelTokens.rate(.infinity, locale: english) == "0 bps")
        #expect(MonarchCallStatsPanelTokens.rate(1_500_000, locale: Locale(identifier: "de_DE")) == "1,5 Mbps")
    }

    @Test("The usage line has the total, then the rates")
    func usage() {
        let text = MonarchCallStatsPanelTokens.usageText(
            MonarchCallStatsPanelFixtures.usage,
            labels: MonarchCallStatsPanelLabels(),
            locale: english
        )
        #expect(text.hasSuffix(" total · up 420 kbps · down 1.3 Mbps"))
        #expect(text.hasPrefix(MonarchCallStatsPanelTokens.bytes(15_600_000, locale: english)))
        #expect(MonarchCallStatsPanelTokens.bytes(15_600_000, locale: english).contains("MB"))
        let custom = MonarchCallStatsPanelTokens.usageText(
            MonarchCallStatsPanelFixtures.usage,
            labels: MonarchCallStatsPanelLabels(usageSummary: { _, up, down in "\(up)|\(down)" }),
            locale: english
        )
        #expect(custom == "420 kbps|1.3 Mbps")
        #expect(
            MonarchCallStatsPanelTokens.bytes(-1, locale: english)
                == MonarchCallStatsPanelTokens.bytes(0, locale: english)
        )
    }

    @Test("Round trip is whole milliseconds; loss is the worse direction with one decimal")
    func numbers() {
        #expect(MonarchCallStatsPanelTokens.roundTrip(38.4, locale: english) == "38 ms")
        #expect(MonarchCallStatsPanelTokens.roundTrip(nil, locale: english) == nil)
        #expect(MonarchCallStatsPanelTokens.roundTrip(.nan, locale: english) == nil)
        let peers = MonarchCallStatsPanelFixtures.peers
        #expect(MonarchCallStatsPanelTokens.loss(peers[1]) == 3.4)
        #expect(MonarchCallStatsPanelTokens.loss(peers[2]) == nil)
        #expect(
            MonarchCallStatsPanelTokens.loss(
                MonarchCallStatsPanelPeer(id: "x", name: "x", inboundLossPercent: .nan, outboundLossPercent: 2)
            ) == 2
        )
        #expect(MonarchCallStatsPanelTokens.lossText(3.4, locale: english) == "3.4%")
        #expect(MonarchCallStatsPanelTokens.lossText(0, locale: english) == "0.0%")
        #expect(MonarchCallStatsPanelTokens.lossText(nil, locale: english) == nil)
    }

    @Test("VoiceOver reads each person with every number, saying what was not measured")
    func accessibility() {
        let peers = MonarchCallStatsPanelFixtures.peers
        let labels = MonarchCallStatsPanelLabels()
        #expect(
            MonarchCallStatsPanelTokens.accessibilityLabel(peers[1], labels: labels, locale: english)
                == "Alex Chen, Round trip 186 ms, Loss 3.4%, Unstable connection"
        )
        #expect(
            MonarchCallStatsPanelTokens.accessibilityLabel(peers[2], labels: labels, locale: english)
                == "Jordan Diaz, Round trip Not measured yet, Loss Not measured yet, Not measured yet"
        )
    }

    @Test("The built-in strings come from the component's strings table")
    func strings() {
        #expect(MonarchCallStatsPanelStrings.dataUsed == "Data used this call")
        #expect(MonarchCallStatsPanelStrings.lowData == "Low data mode is on: audio only.")
        #expect(MonarchCallStatsPanelStrings.connections == "Connection to each person")
        #expect(MonarchCallStatsPanelStrings.roundTrip == "Round trip")
        #expect(MonarchCallStatsPanelStrings.loss == "Loss")
        #expect(MonarchCallStatsPanelStrings.noPeers == "No one else is connected yet.")
        #expect(
            MonarchCallStatsPanelQuality.allCases.map(MonarchCallStatsPanelStrings.quality) == [
                "Good connection", "Unstable connection", "Poor connection", "Not measured yet",
            ]
        )
        #expect(
            MonarchCallStatsPanelStrings.usageSummary(total: "1 MB", up: "2 kbps", down: "3 kbps")
                == "1 MB total · up 2 kbps · down 3 kbps"
        )
        #expect(MonarchCallStatsPanelStrings.bitsPerSecond("1") == "1 bps")
        #expect(MonarchCallStatsPanelStrings.kilobitsPerSecond("1") == "1 kbps")
        #expect(MonarchCallStatsPanelStrings.megabitsPerSecond("1") == "1 Mbps")
    }
}
