import SwiftUI
import Testing

@_spi(Experimental) @testable import MonarchUI

private let people = [
    MonarchPresencePerson(id: "1", name: "Ana Lopez"),
    MonarchPresencePerson(id: "2", name: "Ben Okafor"),
    MonarchPresencePerson(id: "3", name: "Chloé Martin"),
]

enum MonarchPreJoinLobbyFixtures: ComponentFixtures {
    static let id = "pre-join-lobby"
    static let fixtures = [
        Fixture("camera on with the app's preview, devices and participants") {
            MonarchPreJoinLobby(
                isMicOn: .constant(true),
                isCameraOn: .constant(true),
                audioLevel: 0.7,
                participants: people,
                onCancel: {},
                preview: { Rectangle().fill(.tint) },
                devices: {
                    MonarchDevicePicker(
                        microphone: .constant(nil),
                        microphones: [MonarchDevicePickerDevice("mic", name: "Built-in microphone")],
                        access: .granted,
                        size: .sm
                    )
                },
                onJoin: {}
            )
        },
        Fixture("camera off, microphone muted") {
            MonarchPreJoinLobby(isMicOn: .constant(false), isCameraOn: .constant(false), preview: { EmptyView() }) {}
        },
        Fixture("camera starting") {
            MonarchPreJoinLobby(
                isMicOn: .constant(true),
                isCameraOn: .constant(true),
                isCameraStarting: true,
                preview: { EmptyView() },
                onJoin: {}
            )
        },
        Fixture("error, joining") {
            MonarchPreJoinLobby(
                isMicOn: .constant(true),
                isCameraOn: .constant(true),
                error: "Camera access is blocked. Allow it in Settings.",
                isJoining: true,
                onCancel: {},
                preview: { EmptyView() },
                onJoin: {}
            )
        },
        Fixture("audio call, custom title and join title, long text") {
            MonarchPreJoinLobby(
                "Weekly planning with the design and engineering teams",
                isMicOn: .constant(true),
                audioLevel: 2,
                participants: people,
                joinTitle: "Start the call now",
                onCancel: {},
                onJoin: {}
            )
        },
        Fixture("title hidden") {
            MonarchPreJoinLobby(isMicOn: .constant(true), isTitleHidden: true) {}
        },
    ]
}

@Suite("MonarchPreJoinLobby")
struct MonarchPreJoinLobbyTests {
    private typealias Tokens = MonarchPreJoinLobbyTokens

    @Test("A problem wins, then the camera state decides the preview")
    func preview() {
        #expect(Tokens.preview(hasError: true, isCameraOn: true, isCameraStarting: false) == .noPreview)
        #expect(Tokens.preview(hasError: false, isCameraOn: true, isCameraStarting: false) == .video)
        #expect(Tokens.preview(hasError: false, isCameraOn: true, isCameraStarting: true) == .starting)
        #expect(Tokens.preview(hasError: false, isCameraOn: false, isCameraStarting: true) == .cameraOff)
        #expect(Tokens.preview(hasError: false, isCameraOn: nil, isCameraStarting: false) == .cameraOff)
    }

    @Test("Each placeholder has its own text and symbol; the video has none")
    func placeholder() {
        let labels = MonarchPreJoinLobbyLabels()
        #expect(Tokens.placeholderText(.video, labels: labels) == nil)
        #expect(Tokens.placeholderText(.cameraOff, labels: labels) == "Camera is off")
        #expect(Tokens.placeholderText(.starting, labels: labels) == "Starting camera…")
        #expect(Tokens.placeholderText(.noPreview, labels: labels) == "No preview")
        #expect(Tokens.placeholderText(.cameraOff, labels: MonarchPreJoinLobbyLabels(cameraOff: "Off")) == "Off")
        let symbols = Set([Tokens.Preview.cameraOff, .starting, .noPreview].map(Tokens.placeholderSymbol))
        #expect(symbols.count == 3)
    }

    @Test("The meter shows only with the microphone on, and its level stays in range")
    func meter() {
        #expect(Tokens.showsMeter(isMicOn: true))
        #expect(!Tokens.showsMeter(isMicOn: false))
        #expect(Tokens.clampedLevel(0.4) == 0.4)
        #expect(Tokens.clampedLevel(-1) == 0)
        #expect(Tokens.clampedLevel(3) == 1)
        #expect(Tokens.clampedLevel(.nan) == 0)
    }

    @Test("Participants and join texts")
    func texts() {
        let labels = MonarchPreJoinLobbyLabels()
        #expect(Tokens.alreadyInText(0, labels: labels) == nil)
        #expect(Tokens.alreadyInText(1, labels: labels) == "1 already in the call")
        #expect(Tokens.alreadyInText(4, labels: labels) == "4 already in the call")
        #expect(Tokens.joinTitle(isJoining: false, joinTitle: nil, labels: labels) == "Join now")
        #expect(Tokens.joinTitle(isJoining: false, joinTitle: "Start call", labels: labels) == "Start call")
        #expect(Tokens.joinTitle(isJoining: true, joinTitle: "Start call", labels: labels) == "Joining…")
        let custom = MonarchPreJoinLobbyLabels(alreadyIn: { "\($0) inside" }, join: "Enter")
        #expect(Tokens.alreadyInText(2, labels: custom) == "2 inside")
        #expect(Tokens.joinTitle(isJoining: false, joinTitle: nil, labels: custom) == "Enter")
    }

    @Test("Built-in strings resolve")
    func strings() {
        let strings = [
            MonarchPreJoinLobbyStrings.title, MonarchPreJoinLobbyStrings.preview,
            MonarchPreJoinLobbyStrings.cameraOff, MonarchPreJoinLobbyStrings.startingCamera,
            MonarchPreJoinLobbyStrings.noPreview, MonarchPreJoinLobbyStrings.controls,
            MonarchPreJoinLobbyStrings.micLevel, MonarchPreJoinLobbyStrings.cancel,
            MonarchPreJoinLobbyStrings.join, MonarchPreJoinLobbyStrings.joining,
            MonarchPreJoinLobbyStrings.alreadyIn(2),
        ]
        for string in strings {
            #expect(!string.hasPrefix("monarch."))
            #expect(!string.isEmpty)
        }
    }

    #if os(iOS)
        @Test("Join and Cancel keep their touch targets")
        @MainActor
        func touchTargets() {
            let size = Host.layout(
                MonarchPreJoinLobby(isMicOn: .constant(true), isTitleHidden: true, onCancel: {}) {}
            )
            #expect(size.height >= MonarchAccessibility.minimumHitTarget * 2)
        }
    #endif
}
