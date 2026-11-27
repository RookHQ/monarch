import MonarchUI
import SwiftUI

struct MonarchCallStatsPanelSection: CataloguePage {
    static let title = "CallStatsPanel"

    @State private var showsSheet = false
    @State private var isLowData = false

    private let usage = MonarchCallStatsPanelUsage(
        sentBytes: 6_200_000,
        receivedBytes: 9_400_000,
        upBitsPerSecond: 420_000,
        downBitsPerSecond: 1_250_000
    )

    private let peers = [
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

    var body: some View {
        CatalogueCaption("During a group call")
        Toggle("Low data mode", isOn: $isLowData)
        MonarchCallStatsPanel(
            usage: usage,
            peers: peers,
            isLowData: isLowData,
            footnote: "Example footnote: say here how your product routes and protects call media."
        )

        CatalogueCaption("Alone on the call")
        MonarchCallStatsPanel(usage: MonarchCallStatsPanelUsage(sentBytes: 120_000, receivedBytes: 0), peers: [])

        CatalogueCaption("In a sheet, as a call's Connection panel")
        MonarchButton("Show connection", variant: .secondary) { showsSheet = true }
            .sheet(isPresented: $showsSheet) {
                NavigationStack {
                    ScrollView {
                        MonarchCallStatsPanel(usage: usage, peers: peers).padding()
                    }
                    .navigationTitle("Connection")
                }
                .presentationDetents([.medium, .large])
            }
    }
}
