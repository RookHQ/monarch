import AVFoundation
import SwiftUI

/// Microphone, camera and speaker choices for a lobby or a call's settings.
///
/// ```swift
/// MonarchDevicePicker(
///     microphone: $call.microphoneID,
///     camera: $call.cameraID,
///     microphones: AVAudioSession.sharedInstance().availableInputs?.map {
///         MonarchDevicePickerDevice(input: $0)
///     } ?? [],
///     cameras: cameraDiscovery.devices.map { MonarchDevicePickerDevice(camera: $0) },
///     showsAudioOutput: true
/// )
/// ```
///
/// The microphone and camera are menu `Picker`s in ``MonarchFormField``s,
/// each shown when its binding is given; `nil` is "System default". A
/// selected device that is no longer listed stays as an unnamed choice. The
/// app lists the devices (with `AVAudioSession` and
/// `AVCaptureDevice.DiscoverySession`), applies the choice (for example
/// `setPreferredInput`) and refreshes the lists when hardware changes. The
/// audio output is the system route picker (`AVRoutePickerView`), as iOS
/// requires, beside the name of the current output.
///
/// Without `access`, the picker reads the camera and microphone
/// authorization of the fields it shows and explains it: before asking, an
/// info notice with Allow access (the system prompt, or `onRequestAccess`);
/// after a refusal, a warning with Open Settings. Disable it with
/// `disabled(_:)`.
///
/// Accessibility: each menu is the standard picker named by its field;
/// notices are ``MonarchInlineNotice``s, read with their tone, and a new
/// notice is announced. The route picker is a 44-point button named
/// "Speaker" with the current output as its value.
public struct MonarchDevicePicker: View {
    private typealias Tokens = MonarchDevicePickerTokens

    private let microphone: Binding<String?>?
    private let camera: Binding<String?>?
    private let microphones: [MonarchDevicePickerDevice]
    private let cameras: [MonarchDevicePickerDevice]
    private let showsAudioOutput: Bool
    private let access: MonarchDevicePickerAccess?
    private let size: MonarchDevicePickerSize
    private let labels: MonarchDevicePickerLabels
    private let onRequestAccess: (@MainActor @Sendable () async -> Void)?

    @State private var systemAccess: MonarchDevicePickerAccess = .granted
    @Environment(\.openURL) private var openURL

    /// A device picker.
    ///
    /// - Parameters:
    ///   - microphone: The chosen microphone's id; `nil` is the system default. Shows the microphone field.
    ///   - camera: The chosen camera's id; `nil` is the system default. Shows the camera field.
    ///   - microphones: The microphones to offer.
    ///   - cameras: The cameras to offer.
    ///   - showsAudioOutput: Shows the speaker row with the system route picker (iOS).
    ///   - access: The camera and microphone access to explain. Read from the system when `nil`.
    ///   - size: `sm` for a popover during a call.
    ///   - labels: Replacements for the built-in strings.
    ///   - onRequestAccess: Replaces the system access request of Allow access.
    public init(
        microphone: Binding<String?>? = nil,
        camera: Binding<String?>? = nil,
        microphones: [MonarchDevicePickerDevice] = [],
        cameras: [MonarchDevicePickerDevice] = [],
        showsAudioOutput: Bool = false,
        access: MonarchDevicePickerAccess? = nil,
        size: MonarchDevicePickerSize = .md,
        labels: MonarchDevicePickerLabels = MonarchDevicePickerLabels(),
        onRequestAccess: (@MainActor @Sendable () async -> Void)? = nil
    ) {
        self.microphone = microphone
        self.camera = camera
        self.microphones = microphones
        self.cameras = cameras
        self.showsAudioOutput = showsAudioOutput
        self.access = access
        self.size = size
        self.labels = labels
        self.onRequestAccess = onRequestAccess
    }

    private var hasDevices: Bool {
        (microphone != nil && !microphones.isEmpty) || (camera != nil && !cameras.isEmpty)
            || (microphone == nil && camera == nil)
    }

    private var settingsURL: URL? {
        MonarchSettingsURL.appSettings
    }

    /// The content of the view.
    public var body: some View {
        let notice = Tokens.notice(access: access ?? systemAccess, hasDevices: hasDevices)
        VStack(alignment: .leading, spacing: Tokens.spacing(size)) {
            if let notice {
                noticeView(notice)
            }
            if let microphone {
                field(.microphone, selection: microphone, devices: microphones)
            }
            if let camera {
                field(.camera, selection: camera, devices: cameras)
            }
            #if os(iOS)
                if showsAudioOutput {
                    MonarchDevicePickerSpeakerRow(
                        label: labels.speaker ?? MonarchDevicePickerStrings.speaker,
                        fallback: labels.systemDefault ?? MonarchDevicePickerStrings.systemDefault,
                        size: size
                    )
                }
            #endif
        }
        .controlSize(Tokens.controlSize(size))
        .onAppear(perform: readSystemAccess)
        .onChange(of: notice) { _, new in
            if let new {
                AccessibilityNotification.Announcement(Tokens.message(new, labels: labels)).post()
            }
        }
    }

    private func noticeView(_ notice: MonarchDevicePickerAccess) -> some View {
        let actionTitle = Tokens.actionTitle(notice, canOpenSettings: settingsURL != nil, labels: labels)
        return MonarchInlineNotice(
            Tokens.message(notice, labels: labels),
            tone: Tokens.tone(notice),
            actionTitle: actionTitle,
            action: actionTitle == nil ? nil : action(for: notice)
        )
    }

    private func field(_ kind: Tokens.Kind, selection: Binding<String?>, devices: [MonarchDevicePickerDevice])
        -> some View
    {
        let title =
            kind == .microphone
            ? labels.microphone ?? MonarchDevicePickerStrings.microphone
            : labels.camera ?? MonarchDevicePickerStrings.camera
        let current = Binding<String?>(
            get: { Tokens.normalized(selection.wrappedValue) },
            set: { selection.wrappedValue = $0 }
        )
        let options = Tokens.options(devices, selection: current.wrappedValue, kind: kind, labels: labels)
        return MonarchSelectField(
            title,
            selection: current,
            valueText: options.first { $0.id == current.wrappedValue }?.title
        ) {
            ForEach(options, id: \.self) { option in
                Text(option.title).tag(option.id)
            }
        }
    }

    /// The action of a notice. It captures only sendable values, so the
    /// notice can run it on the main actor.
    private func action(for notice: MonarchDevicePickerAccess) -> @MainActor @Sendable () -> Void {
        let open = openURL
        let url = settingsURL
        let request = onRequestAccess
        let audio = microphone != nil
        let video = camera != nil
        let reads = access == nil
        let state = $systemAccess
        return {
            switch notice {
            case .notDetermined:
                Task { @MainActor in
                    if let request {
                        await request()
                    } else {
                        if audio { _ = await AVCaptureDevice.requestAccess(for: .audio) }
                        if video { _ = await AVCaptureDevice.requestAccess(for: .video) }
                    }
                    if reads { state.wrappedValue = Self.systemStatus(audio: audio, video: video) }
                }
            case .denied:
                if let url { open(url) }
            case .granted, .unavailable:
                break
            }
        }
    }

    /// Reads the authorization of the shown kinds, when the app did not give `access`.
    private func readSystemAccess() {
        guard access == nil else { return }
        systemAccess = Self.systemStatus(audio: microphone != nil, video: camera != nil)
    }

    /// The system authorization of the given kinds together.
    private static func systemStatus(audio: Bool, video: Bool) -> MonarchDevicePickerAccess {
        var statuses: [MonarchDevicePickerAccess] = []
        if audio { statuses.append(Tokens.access(AVCaptureDevice.authorizationStatus(for: .audio))) }
        if video { statuses.append(Tokens.access(AVCaptureDevice.authorizationStatus(for: .video))) }
        return Tokens.combined(statuses)
    }
}

#if DEBUG
    #Preview("MonarchDevicePicker") {
        let microphones = [
            MonarchDevicePickerDevice("desk", name: "Desk microphone"),
            MonarchDevicePickerDevice("headset", name: "Headset microphone"),
        ]
        MonarchPreviewMatrix {
            VStack {
                MonarchDevicePicker(
                    microphone: .constant("headset"),
                    camera: .constant(nil),
                    microphones: microphones,
                    cameras: [MonarchDevicePickerDevice("front", name: "Front Camera")],
                    showsAudioOutput: true,
                    access: .granted
                )
                MonarchDevicePicker(
                    microphone: .constant(nil),
                    microphones: microphones,
                    access: .notDetermined,
                    size: .sm
                )
                MonarchDevicePicker(camera: .constant(nil), access: .denied)
            }
        }
    }
#endif
