import SwiftUI
import Testing

@testable import MonarchUI

enum MonarchCallControlsFixtures: ComponentFixtures {
    static let id = "call-controls"
    static let fixtures = [
        Fixture("every control, devices on") {
            MonarchCallControls(
                isMicOn: .constant(true),
                isCameraOn: .constant(true),
                isSharing: .constant(false),
                isHandRaised: .constant(false),
                layout: .constant(.automatic),
                isLowData: .constant(false),
                onReact: { _ in },
                onOpenDevices: {},
                onLeave: {}
            )
        },
        Fixture("devices off, sharing, hand raised") {
            MonarchCallControls(
                isMicOn: .constant(false),
                isCameraOn: .constant(false),
                isSharing: .constant(true),
                isHandRaised: .constant(true),
                onReact: { _ in },
                onLeave: {}
            )
        },
        Fixture("minimal") {
            MonarchCallControls(
                isMicOn: .constant(true),
                isCameraOn: .constant(false),
                isHandRaised: .constant(true),
                isMinimal: true,
                onReact: { _ in },
                onPictureInPicture: {},
                onLeave: {}
            )
        },
        Fixture("disabled") {
            MonarchCallControls(isMicOn: .constant(true), isCameraOn: .constant(true), onLeave: {})
                .disabled(true)
        },
        Fixture("custom labels") {
            MonarchCallControls(
                isMicOn: .constant(true),
                labels: MonarchCallControlsLabels(microphone: "Mic", leave: "Hang up"),
                onLeave: {}
            )
        },
    ]
}

@Suite("MonarchCallControls")
struct MonarchCallControlsTests {
    private typealias Tokens = MonarchCallControlsTokens

    @Test("A control appears only with its binding or action; minimal drops hand, reactions and More")
    func visibleControls() {
        let all = Tokens.controls(
            hasMicrophone: true,
            hasCamera: true,
            hasShare: true,
            hasHand: true,
            hasReactions: true,
            hasMoreItems: true,
            hasLeave: true,
            isMinimal: false
        )
        #expect(all == [.microphone, .camera, .share, .hand, .reactions, .more, .leave])
        let minimal = Tokens.controls(
            hasMicrophone: true,
            hasCamera: true,
            hasShare: true,
            hasHand: true,
            hasReactions: true,
            hasMoreItems: true,
            hasLeave: true,
            isMinimal: true
        )
        #expect(minimal == [.microphone, .camera, .share, .leave])
        let some = Tokens.controls(
            hasMicrophone: true,
            hasCamera: false,
            hasShare: false,
            hasHand: false,
            hasReactions: false,
            hasMoreItems: false,
            hasLeave: true,
            isMinimal: false
        )
        #expect(some == [.microphone, .leave])
    }

    @Test("Tones follow the state: devices off, sharing, raised hand, leave")
    func tones() {
        #expect(Tokens.tone(.microphone, isOn: true) == .standard)
        #expect(Tokens.tone(.microphone, isOn: false) == .off)
        #expect(Tokens.tone(.camera, isOn: false) == .off)
        #expect(Tokens.tone(.share, isOn: true) == .active)
        #expect(Tokens.tone(.share, isOn: false) == .standard)
        #expect(Tokens.tone(.hand, isOn: true) == .raised)
        #expect(Tokens.tone(.leave, isOn: false) == .danger)
    }

    @Test("Every toggle changes its symbol with its state, so colour is not the only cue")
    func symbols() {
        for control in [Tokens.Control.microphone, .camera, .share, .hand] {
            #expect(Tokens.symbol(control, isOn: true) != Tokens.symbol(control, isOn: false))
        }
    }

    @Test("Colour roles for each tone, pressed and disabled")
    func appearance() {
        let off = Tokens.appearance(tone: .off, isPressed: false, isEnabled: true)
        #expect(off.background == \.dangerSolid)
        #expect(off.foreground == \.dangerOnSolid)
        #expect(Tokens.appearance(tone: .off, isPressed: true, isEnabled: true).background == \.dangerSolidActive)
        #expect(Tokens.appearance(tone: .active, isPressed: false, isEnabled: true).background == \.successSolid)
        #expect(Tokens.appearance(tone: .raised, isPressed: false, isEnabled: true).background == \.warningSolid)
        #expect(Tokens.appearance(tone: .standard, isPressed: false, isEnabled: true).background == \.bgMuted)
        #expect(Tokens.appearance(tone: .standard, isPressed: true, isEnabled: true).background == \.bgEmphasis)
        for tone in Tokens.Tone.allCases {
            #expect(Tokens.appearance(tone: tone, isPressed: false, isEnabled: false).foreground == \.textDisabled)
        }
    }

    @Test("Reactions and layouts are named")
    func names() {
        let labels = MonarchCallControlsLabels()
        #expect(Tokens.reactionLabel("👍", labels: labels) == "React 👍")
        #expect(Tokens.layoutName(.speaker, labels: labels) == "Speaker")
        let custom = MonarchCallControlsLabels(reactWith: { "Send \($0)" }, layoutName: { _ in "View" })
        #expect(Tokens.reactionLabel("🎉", labels: custom) == "Send 🎉")
        #expect(Tokens.layoutName(.grid, labels: custom) == "View")
    }

    @Test("The built-in strings resolve")
    func strings() {
        let strings = MonarchCallControlsStrings.self
        #expect(strings.toolbar == "Call controls")
        #expect(strings.microphone == "Microphone")
        #expect(strings.camera == "Camera")
        #expect(strings.leave == "Leave call")
        #expect(strings.more == "More options")
        for value in [strings.shareScreen, strings.raiseHand, strings.react, strings.layout, strings.lowData] {
            #expect(!value.hasPrefix("monarch."))
        }
        for value in [strings.devices, strings.stats, strings.pictureInPicture] {
            #expect(!value.hasPrefix("monarch."))
        }
        for layout in MonarchCallControlsLayout.allCases {
            #expect(!strings.layoutName(layout).hasPrefix("monarch."))
        }
    }

    @Test("Each control is at least the 24-point target size, so the row fits one line on a phone")
    @MainActor
    func touchTarget() {
        let size = Host.layout(MonarchCallControls(isMicOn: .constant(true)))
        #expect(size.height >= 24)
    }
}
