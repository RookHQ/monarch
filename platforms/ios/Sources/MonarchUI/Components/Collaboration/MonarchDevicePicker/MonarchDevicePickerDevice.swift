import AVFoundation

/// A microphone or camera offered by ``MonarchDevicePicker``.
public struct MonarchDevicePickerDevice: Sendable, Hashable, Identifiable {
    /// The system's stable id: `AVAudioSessionPortDescription.uid` or `AVCaptureDevice.uniqueID`.
    public let id: String
    /// The name shown, already localised by the system. Empty names are
    /// shown as "Microphone 2" or "Camera 2".
    public let name: String

    /// A device.
    ///
    /// - Parameters:
    ///   - id: The system's stable id.
    ///   - name: The name shown.
    public init(_ id: String, name: String) {
        self.id = id
        self.name = name
    }

    /// A camera from `AVCaptureDevice.DiscoverySession`.
    ///
    /// - Parameter camera: The capture device.
    public init(camera: AVCaptureDevice) {
        self.init(camera.uniqueID, name: camera.localizedName)
    }

    #if os(iOS)
        /// A microphone from `AVAudioSession.availableInputs`.
        ///
        /// - Parameter input: The audio session port.
        public init(input: AVAudioSessionPortDescription) {
            self.init(input.uid, name: input.portName)
        }
    #endif
}
