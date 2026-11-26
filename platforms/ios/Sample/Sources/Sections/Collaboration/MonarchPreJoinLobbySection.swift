@_spi(Experimental) import MonarchUI
import SwiftUI

struct MonarchPreJoinLobbySection: CataloguePage {
    static let title = "PreJoinLobby"

    @Environment(\.monarchTheme) private var theme
    @State private var isMicOn = true
    @State private var isCameraOn = true
    @State private var level = 0.4
    @State private var microphone: String?
    @State private var isJoining = false
    @State private var result = "Not joined"

    var body: some View {
        Text(result).monarchTextStyle(.bodyStrong)
        CatalogueCaption("Video call; the slot stands in for the app's camera preview")
        Slider(value: $level, in: 0...1) { Text("Simulated microphone level") }
        MonarchPreJoinLobby(
            isMicOn: $isMicOn,
            isCameraOn: $isCameraOn,
            audioLevel: level,
            participants: [
                MonarchPresencePerson(id: "1", name: "Ana Lopez"),
                MonarchPresencePerson(id: "2", name: "Ben Okafor"),
            ],
            isJoining: isJoining,
            onCancel: { result = "Cancelled" },
            preview: {
                theme.colors.bgEmphasis
                    .overlay { Text("Camera preview").foregroundStyle(theme.colors.textInverse) }
            },
            devices: {
                MonarchDevicePicker(
                    microphone: $microphone,
                    microphones: [
                        MonarchDevicePickerDevice("built-in", name: "Built-in microphone"),
                        MonarchDevicePickerDevice("headset", name: "Headset"),
                    ],
                    access: .granted,
                    size: .sm
                )
            },
            onJoin: {
                isJoining = true
                Task { @MainActor in
                    try? await Task.sleep(for: .seconds(1))
                    isJoining = false
                    result = "Joined with microphone \(isMicOn ? "on" : "off"), camera \(isCameraOn ? "on" : "off")"
                }
            }
        )

        CatalogueCaption("Audio call with a problem")
        MonarchPreJoinLobby(
            "Support call",
            isMicOn: $isMicOn,
            error: "Microphone access is blocked. Allow it in Settings.",
            joinTitle: "Start call"
        ) {
            result = "Started the audio call"
        }
    }
}
