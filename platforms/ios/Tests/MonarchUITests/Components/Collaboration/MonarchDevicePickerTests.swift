import AVFoundation
import SwiftUI
import Testing

@testable import MonarchUI

private let microphones = [
    MonarchDevicePickerDevice("desk", name: "Desk microphone"),
    MonarchDevicePickerDevice("headset", name: "Headset microphone"),
]
private let cameras = [
    MonarchDevicePickerDevice("front", name: "Front Camera"),
    MonarchDevicePickerDevice("usb", name: ""),
]

enum MonarchDevicePickerFixtures: ComponentFixtures {
    static let id = "device-picker"
    static let fixtures = [
        Fixture("every field, a choice, an unnamed camera, speaker row") {
            MonarchDevicePicker(
                microphone: .constant("headset"),
                camera: .constant(nil),
                microphones: microphones,
                cameras: cameras,
                showsAudioOutput: true,
                access: .granted
            )
        },
        Fixture("small, not asked yet, a device that was removed") {
            MonarchDevicePicker(
                microphone: .constant("gone"),
                microphones: microphones,
                access: .notDetermined,
                size: .sm,
                onRequestAccess: {}
            )
        },
        Fixture("refused, unavailable, disabled, custom labels") {
            VStack {
                MonarchDevicePicker(camera: .constant(nil), cameras: cameras, access: .denied)
                MonarchDevicePicker(microphone: .constant(nil), access: .granted)
                MonarchDevicePicker(microphone: .constant(nil), microphones: microphones, access: .unavailable)
                MonarchDevicePicker(
                    microphone: .constant("desk"),
                    microphones: microphones,
                    access: .granted,
                    labels: MonarchDevicePickerLabels(microphone: "Mikrofon", systemDefault: "Systemstandard")
                )
                .disabled(true)
            }
        },
    ]
}

@Suite("MonarchDevicePicker")
struct MonarchDevicePickerTests {
    private let labels = MonarchDevicePickerLabels()

    @Test("The system default comes first, then the devices, unnamed ones numbered by position")
    func options() {
        let options = MonarchDevicePickerTokens.options(cameras, selection: nil, kind: .camera, labels: labels)
        #expect(options.map(\.id) == [nil, "front", "usb"])
        #expect(options.map(\.title) == ["System default", "Front Camera", "Camera 2"])
    }

    @Test("A selected device that is no longer listed stays, unnamed")
    func missingSelection() {
        let options = MonarchDevicePickerTokens.options(
            microphones,
            selection: "gone",
            kind: .microphone,
            labels: labels
        )
        #expect(options.map(\.id) == [nil, "gone", "desk", "headset"])
        #expect(options[1].title == "Microphone 1")
        let listed = MonarchDevicePickerTokens.options(
            microphones,
            selection: "desk",
            kind: .microphone,
            labels: labels
        )
        #expect(listed.count == 3)
    }

    @Test("Devices without an id are left out; an empty selection is the system default")
    func emptyIDs() {
        let devices = [MonarchDevicePickerDevice("", name: "Ghost"), MonarchDevicePickerDevice("a", name: "A")]
        let options = MonarchDevicePickerTokens.options(devices, selection: "", kind: .microphone, labels: labels)
        #expect(options.map(\.id) == [nil, "a"])
        #expect(MonarchDevicePickerTokens.normalized("") == nil)
        #expect(MonarchDevicePickerTokens.normalized(nil) == nil)
        #expect(MonarchDevicePickerTokens.normalized("a") == "a")
    }

    @Test("Labels replace the built-in names")
    func customLabels() {
        let custom = MonarchDevicePickerLabels(systemDefault: "Standard", unnamedCamera: { "Kamera \($0)" })
        let options = MonarchDevicePickerTokens.options(cameras, selection: nil, kind: .camera, labels: custom)
        #expect(options.map(\.title) == ["Standard", "Front Camera", "Kamera 2"])
    }

    @Test("Access: system statuses map, and the shown kinds combine")
    func access() {
        typealias T = MonarchDevicePickerTokens
        #expect(T.access(.authorized) == .granted)
        #expect(T.access(.notDetermined) == .notDetermined)
        #expect(T.access(.denied) == .denied)
        #expect(T.access(.restricted) == .denied)
        #expect(T.combined([]) == .granted)
        #expect(T.combined([.granted, .notDetermined]) == .notDetermined)
        #expect(T.combined([.notDetermined, .denied]) == .denied)
        #expect(T.combined([.granted, .granted]) == .granted)
        #expect(T.combined([.granted, .unavailable]) == .unavailable)
    }

    @Test("Notices: none when allowed with devices; unavailable without; tones and actions")
    func notices() {
        typealias T = MonarchDevicePickerTokens
        #expect(T.notice(access: .granted, hasDevices: true) == nil)
        #expect(T.notice(access: .granted, hasDevices: false) == .unavailable)
        #expect(T.notice(access: .denied, hasDevices: true) == .denied)
        #expect(T.notice(access: .notDetermined, hasDevices: false) == .notDetermined)
        #expect(T.tone(.notDetermined) == .info)
        #expect(T.tone(.denied) == .warning)
        #expect(T.tone(.unavailable) == .neutral)
        #expect(T.actionTitle(.notDetermined, canOpenSettings: false, labels: labels) == "Allow access")
        #expect(T.actionTitle(.denied, canOpenSettings: true, labels: labels) == "Open Settings")
        #expect(T.actionTitle(.denied, canOpenSettings: false, labels: labels) == nil)
        #expect(T.actionTitle(.unavailable, canOpenSettings: true, labels: labels) == nil)
        #expect(T.message(.denied, labels: MonarchDevicePickerLabels(denied: "Aus")) == "Aus")
        for notice in MonarchDevicePickerAccess.allCases {
            #expect(!T.message(notice, labels: labels).hasPrefix("monarch."))
        }
    }

    @Test("The current output's name, or the system default text")
    func outputName() {
        #expect(MonarchDevicePickerTokens.outputName(["AirPods"], fallback: "System default") == "AirPods")
        #expect(MonarchDevicePickerTokens.outputName(["A", "B"], fallback: "x") == "A, B")
        #expect(MonarchDevicePickerTokens.outputName([], fallback: "System default") == "System default")
        #expect(MonarchDevicePickerTokens.outputName([""], fallback: "System default") == "System default")
    }

    @Test("The small size is tighter and uses small controls")
    func sizes() {
        #expect(MonarchDevicePickerTokens.spacing(.sm) < MonarchDevicePickerTokens.spacing(.md))
        #expect(MonarchDevicePickerTokens.controlSize(.sm) == .small)
        #expect(MonarchDevicePickerTokens.controlSize(.md) == .regular)
    }

    @Test("Built-in strings resolve")
    func strings() {
        let all = [
            MonarchDevicePickerStrings.microphone, MonarchDevicePickerStrings.camera,
            MonarchDevicePickerStrings.speaker, MonarchDevicePickerStrings.systemDefault,
            MonarchDevicePickerStrings.notDetermined, MonarchDevicePickerStrings.allowAccess,
            MonarchDevicePickerStrings.denied, MonarchDevicePickerStrings.openSettings,
            MonarchDevicePickerStrings.unavailable, MonarchDevicePickerStrings.unnamedMicrophone("2"),
            MonarchDevicePickerStrings.unnamedCamera("2"),
        ]
        for string in all {
            #expect(!string.hasPrefix("monarch."))
        }
        #expect(MonarchDevicePickerStrings.unnamedMicrophone("2") == "Microphone 2")
    }
}
