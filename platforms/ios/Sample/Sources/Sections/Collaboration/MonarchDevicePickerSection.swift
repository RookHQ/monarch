import AVFoundation
import MonarchUI
import SwiftUI

struct MonarchDevicePickerSection: CataloguePage {
    static let title = "DevicePicker"

    @State private var microphone: String?
    @State private var camera: String?
    @State private var sampleMicrophone: String? = "headset"
    @State private var sampleCamera: String?
    @State private var access = MonarchDevicePickerAccess.granted
    @State private var size = MonarchDevicePickerSize.md
    @State private var note = ""

    private let sampleMicrophones = [
        MonarchDevicePickerDevice("desk", name: "Desk microphone"),
        MonarchDevicePickerDevice("headset", name: "Headset microphone"),
    ]
    private let sampleCameras = [
        MonarchDevicePickerDevice("built-in", name: "Built-in camera"),
        MonarchDevicePickerDevice("usb", name: ""),
    ]

    /// This device's microphones and cameras, as a product lists them.
    private var deviceMicrophones: [MonarchDevicePickerDevice] {
        AVAudioSession.sharedInstance().availableInputs?.map { MonarchDevicePickerDevice(input: $0) } ?? []
    }

    private var deviceCameras: [MonarchDevicePickerDevice] {
        AVCaptureDevice.DiscoverySession(
            deviceTypes: [.builtInWideAngleCamera, .external],
            mediaType: .video,
            position: .unspecified
        )
        .devices
        .map { MonarchDevicePickerDevice(camera: $0) }
    }

    var body: some View {
        CatalogueCaption("This device: real devices and the real access state")
        MonarchDevicePicker(
            microphone: $microphone,
            camera: $camera,
            microphones: deviceMicrophones,
            cameras: deviceCameras,
            showsAudioOutput: true,
            onRequestAccess: {
                // A product declares NSCameraUsageDescription and NSMicrophoneUsageDescription and
                // leaves this out, so Allow access shows the system prompt. The catalogue declares
                // neither, and asking without them would end the app.
                note = "The catalogue declares no camera or microphone usage description, so it does not ask."
            }
        )
        if !note.isEmpty {
            CatalogueCaption(note)
        }

        CatalogueCaption("Given devices and access")
        Picker("Access", selection: $access) {
            Text("Granted").tag(MonarchDevicePickerAccess.granted)
            Text("Not asked").tag(MonarchDevicePickerAccess.notDetermined)
            Text("Denied").tag(MonarchDevicePickerAccess.denied)
            Text("None").tag(MonarchDevicePickerAccess.unavailable)
        }
        .pickerStyle(.segmented)
        Picker("Size", selection: $size) {
            Text("Medium").tag(MonarchDevicePickerSize.md)
            Text("Small").tag(MonarchDevicePickerSize.sm)
        }
        .pickerStyle(.segmented)
        MonarchDevicePicker(
            microphone: $sampleMicrophone,
            camera: $sampleCamera,
            microphones: sampleMicrophones,
            cameras: sampleCameras,
            showsAudioOutput: true,
            access: access,
            size: size,
            onRequestAccess: { access = .granted }
        )
        CatalogueCaption(
            "Microphone: \(sampleMicrophone ?? "system default"), camera: \(sampleCamera ?? "system default")"
        )
    }
}
