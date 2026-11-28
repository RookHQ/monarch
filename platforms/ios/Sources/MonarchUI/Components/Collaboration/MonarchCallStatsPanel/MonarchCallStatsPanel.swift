import SwiftUI

/// The data a call has used and each person's round trip, packet loss and
/// connection grade, for a call's Connection panel.
///
/// ```swift
/// .sheet(isPresented: $showsStats) {
///     NavigationStack {
///         ScrollView {
///             MonarchCallStatsPanel(
///                 usage: MonarchCallStatsPanelUsage(sentBytes: sent, receivedBytes: received, upBitsPerSecond: up),
///                 peers: peers,
///                 footnote: routingNote
///             )
///             .padding()
///         }
///         .navigationTitle("Connection")
///     }
///     .presentationDetents([.medium, .large])
/// }
/// ```
///
/// It takes plain numbers; the app reads them from its media statistics
/// and sets new values every second or two. Numbers use the units people
/// know (MB, kbps or Mbps, milliseconds, percent). A number that has not
/// been measured shows a dash, never a made-up zero, and loss shows the
/// worse direction. How media is routed or encrypted is the app's copy,
/// passed as `footnote`.
///
/// Accessibility: the usage title is a header. Each person is one element
/// read as the name, the round trip, the loss and the grade; a number not
/// measured is read as "Not measured yet". The grade is a status badge with
/// an icon and a name, not colour alone.
public struct MonarchCallStatsPanel: View {
    private let usage: MonarchCallStatsPanelUsage
    private let peers: [MonarchCallStatsPanelPeer]
    private let isLowData: Bool
    private let footnote: String?
    private let labels: MonarchCallStatsPanelLabels

    @Environment(\.monarchTheme) private var theme
    @Environment(\.locale) private var locale

    /// A call stats panel.
    ///
    /// - Parameters:
    ///   - usage: The data the call has used and the current rates.
    ///   - peers: One entry per remote person.
    ///   - isLowData: Whether low data mode is on. Adds a note.
    ///   - footnote: A closing note, such as how media is routed or encrypted. Already localised.
    ///   - labels: Replacements for the built-in strings.
    public init(
        usage: MonarchCallStatsPanelUsage,
        peers: [MonarchCallStatsPanelPeer],
        isLowData: Bool = false,
        footnote: String? = nil,
        labels: MonarchCallStatsPanelLabels = MonarchCallStatsPanelLabels()
    ) {
        self.usage = usage
        self.peers = peers
        self.isLowData = isLowData
        self.footnote = footnote
        self.labels = labels
    }

    /// The content of the view.
    public var body: some View {
        VStack(alignment: .leading, spacing: MonarchCallStatsPanelTokens.sectionSpacing) {
            VStack(alignment: .leading, spacing: MonarchCallStatsPanelTokens.lineSpacing) {
                Text(labels.dataUsed ?? MonarchCallStatsPanelStrings.dataUsed)
                    .monarchTextStyle(.subheading)
                    .foregroundStyle(theme.colors.textDefault)
                    .accessibilityAddTraits(.isHeader)
                muted(MonarchCallStatsPanelTokens.usageText(usage, labels: labels, locale: locale))
                if isLowData {
                    muted(labels.lowData ?? MonarchCallStatsPanelStrings.lowData)
                }
            }
            if peers.isEmpty {
                muted(labels.noPeers ?? MonarchCallStatsPanelStrings.noPeers)
            } else {
                connectionsTable
                    .accessibilityElement(children: .contain)
                    .accessibilityLabel(labels.connections ?? MonarchCallStatsPanelStrings.connections)
            }
            if let footnote, !footnote.isEmpty {
                muted(footnote)
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
    }

    private func muted(_ text: String) -> some View {
        Text(text)
            .monarchTextStyle(.caption)
            .foregroundStyle(theme.colors.textMuted)
            .multilineTextAlignment(.leading)
    }

    /// Person, Round trip, Loss and Link as a table, as on the web; it scrolls sideways at the largest text sizes.
    private var connectionsTable: some View {
        let widths: [CGFloat] = [92, 68, 44, 110]
        let heads = [
            MonarchCallStatsPanelStrings.person,
            labels.roundTrip ?? MonarchCallStatsPanelStrings.roundTrip,
            labels.loss ?? MonarchCallStatsPanelStrings.loss,
            MonarchCallStatsPanelStrings.link,
        ]
        return ScrollView(.horizontal, showsIndicators: false) {
            VStack(alignment: .leading, spacing: 0) {
                HStack(spacing: 0) {
                    ForEach(Array(heads.enumerated()), id: \.offset) { index, head in
                        Text(head)
                            .monarchTextStyle(.caption)
                            .foregroundStyle(theme.colors.textMuted)
                            .frame(width: widths[index], alignment: .leading)
                    }
                }
                .padding(.vertical, MonarchCallStatsPanelTokens.itemSpacing)
                ForEach(peers) { peer in
                    MonarchDivider()
                    peerRow(peer, widths: widths)
                }
            }
        }
    }

    private func peerRow(_ peer: MonarchCallStatsPanelPeer, widths: [CGFloat]) -> some View {
        let missing = MonarchCallStatsPanelTokens.noValueSymbol
        return HStack(spacing: 0) {
            Text(peer.name)
                .monarchTextStyle(.body)
                .foregroundStyle(theme.colors.textDefault)
                .lineLimit(2)
                .frame(width: widths[0], alignment: .leading)
            Text(MonarchCallStatsPanelTokens.roundTrip(peer.roundTripMilliseconds, locale: locale) ?? missing)
                .monarchTextStyle(.body)
                .foregroundStyle(theme.colors.textDefault)
                .monospacedDigit()
                .frame(width: widths[1], alignment: .leading)
            Text(
                MonarchCallStatsPanelTokens.lossText(MonarchCallStatsPanelTokens.loss(peer), locale: locale)
                    ?? missing
            )
            .monarchTextStyle(.body)
            .foregroundStyle(theme.colors.textDefault)
            .monospacedDigit()
            .frame(width: widths[2], alignment: .leading)
            link(peer.quality)
                .frame(width: widths[3], alignment: .leading)
        }
        .padding(.vertical, MonarchCallStatsPanelTokens.peerSpacing)
        .accessibilityElement(children: .ignore)
        .accessibilityLabel(MonarchCallStatsPanelTokens.accessibilityLabel(peer, labels: labels, locale: locale))
    }

    /// Signal bars in the quality's colour, or the plain words for a connection not measured yet.
    @ViewBuilder private func link(_ quality: MonarchCallStatsPanelQuality) -> some View {
        if quality == .unknown {
            Text(MonarchCallStatsPanelTokens.name(.unknown, labels: labels))
                .monarchTextStyle(.body)
                .foregroundStyle(theme.colors.textMuted)
                .fixedSize(horizontal: false, vertical: true)
        } else {
            let lit = quality == .good ? 3 : (quality == .fair ? 2 : 1)
            let role = MonarchTonePalette.palette(for: MonarchCallStatsPanelTokens.tone(quality)).strong
            HStack(alignment: .bottom, spacing: 2) {
                ForEach(0..<3, id: \.self) { index in
                    RoundedRectangle(cornerRadius: 1)
                        .fill(index < lit ? theme.colors[role: role] : theme.colors.borderDefault)
                        .frame(width: 4, height: CGFloat(6 + index * 4))
                }
            }
            .accessibilityHidden(true)
        }
    }
}

#if DEBUG
    #Preview("MonarchCallStatsPanel") {
        MonarchPreviewMatrix {
            VStack(alignment: .leading) {
                MonarchCallStatsPanel(
                    usage: MonarchCallStatsPanelUsage(
                        sentBytes: 6_200_000,
                        receivedBytes: 9_400_000,
                        upBitsPerSecond: 420_000,
                        downBitsPerSecond: 1_250_000
                    ),
                    peers: [
                        MonarchCallStatsPanelPeer(
                            id: "1",
                            name: "Ana Ruiz",
                            roundTripMilliseconds: 48,
                            inboundLossPercent: 0.2,
                            outboundLossPercent: 0.4,
                            quality: .good
                        ),
                        MonarchCallStatsPanelPeer(id: "2", name: "Ben Okafor"),
                    ],
                    isLowData: true,
                    footnote: "Example footnote: say here how your product routes and protects call media."
                )
                MonarchCallStatsPanel(usage: MonarchCallStatsPanelUsage(sentBytes: 0, receivedBytes: 0), peers: [])
            }
        }
    }
#endif
