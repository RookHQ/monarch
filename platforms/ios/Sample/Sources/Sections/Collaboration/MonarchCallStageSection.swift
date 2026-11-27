@_spi(Experimental) import MonarchUI
import SwiftUI

struct MonarchCallStageSection: CataloguePage {
    static let title = "CallStage"

    @State private var isMicOn = true
    @State private var mode = MonarchCallControlsLayout.automatic
    @State private var pinnedID: String?
    @State private var surface = MonarchCallStageSurface.stage
    @State private var isReconnecting = false
    @State private var startedAt = Date.now.addingTimeInterval(-4 * 60)
    @State private var action = "none"

    private var team: [MonarchCallStageParticipant] {
        [
            MonarchCallStageParticipant("me", name: "Sam Lee", isSelf: true, isMicOff: !isMicOn),
            MonarchCallStageParticipant("robin", name: "Robin Park", isSpeaking: true, quality: .good),
            MonarchCallStageParticipant("alex", name: "Alex Chen", isMicOff: true, quality: .fair),
            MonarchCallStageParticipant("jordan", name: "Jordan Diaz", isHandRaised: true, quality: .good),
        ]
    }

    var body: some View {
        Picker("Surface", selection: $surface) {
            Text("Stage").tag(MonarchCallStageSurface.stage)
            Text("Docked").tag(MonarchCallStageSurface.docked)
            Text("PiP").tag(MonarchCallStageSurface.pip)
        }
        .pickerStyle(.segmented)
        Toggle("Reconnecting", isOn: $isReconnecting)
        CatalogueCaption("Group call. Last action: \(action)")
        MonarchCallStage(
            "Weekly planning",
            participants: team,
            startedAt: startedAt,
            mode: mode,
            pinnedID: $pinnedID,
            activeSpeakerID: "robin",
            surface: surface,
            isReconnecting: isReconnecting,
            onSurfaceChange: { surface = $0 },
            onMinimize: { action = "minimize" },
            onPictureInPicture: { action = "picture in picture" },
            onFullScreen: { action = "full screen \($0)" },
            controls: {
                MonarchCallControls(
                    isMicOn: $isMicOn,
                    layout: $mode,
                    isMinimal: surface != .stage,
                    onLeave: { action = "leave" }
                )
            }
        )
        .frame(maxWidth: surface == .stage ? .infinity : MonarchCallStageSectionMetrics.dockedWidth)

        CatalogueCaption("One-to-one: the other person is large")
        MonarchCallStage("Robin Park", participants: Array(team.prefix(2)), startedAt: startedAt) {
            MonarchCallControls(isMicOn: $isMicOn, onLeave: { action = "leave" })
        }

        CatalogueCaption("Connection problems")
        MonarchCallStage(
            "Support call",
            participants: [team[0], MonarchCallStageParticipant("x", name: "Alex Chen", status: "Connecting…")],
            layout: .grid(ids: ["me", "x"], columns: 2),
            isReconnecting: true,
            error: "A connection couldn't be established. Check your network, then leave and rejoin.",
            isSpeakingWhileMuted: true
        )
    }
}

/// The catalogue's docked width, as a product's corner panel.
private enum MonarchCallStageSectionMetrics {
    static let dockedWidth: CGFloat = 360
}
