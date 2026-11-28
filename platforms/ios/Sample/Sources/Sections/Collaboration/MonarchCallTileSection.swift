import MonarchUI
import SwiftUI

struct MonarchCallTileSection: CataloguePage {
    static let title = "Call tile"

    @State private var isPinned = false
    @State private var isMicOff = false
    @State private var isSpeaking = true
    @State private var isHandRaised = false
    @State private var reaction: MonarchCallReaction?
    @State private var fullScreen = ""

    var body: some View {
        MonarchCallTile(
            "Ana Silva",
            isPinned: $isPinned,
            isMicOff: isMicOff,
            isSpeaking: isSpeaking,
            isHandRaised: isHandRaised,
            quality: .fair,
            reaction: reaction
        ) {
            fullScreen = "Ana's tile asked for full screen"
        }
        if !fullScreen.isEmpty {
            Text(fullScreen).monarchTextStyle(.caption)
        }
        Toggle("Muted", isOn: $isMicOff)
        Toggle("Speaking", isOn: $isSpeaking)
        Toggle("Hand raised", isOn: $isHandRaised)
        Button("Send a reaction") { reaction = MonarchCallReaction("👏", sentAt: Date()) }
            .buttonStyle(.monarch(.secondary))

        CatalogueCaption("A video slot, your own camera, and the filmstrip size")
        HStack {
            MonarchCallTile("Ben Okafor", isSelf: true, quality: .good, size: .sm) {
                Rectangle().fill(.tint)
            }
            MonarchCallTile("Chen Wei", status: "Reconnecting…", statusTone: .warning, size: .sm)
        }
    }
}
