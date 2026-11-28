/// The kind of device check available for ``MonarchStepUpMethod/deviceOwner``.
/// It names the button.
public enum MonarchStepUpBiometry: Sendable, Hashable, CaseIterable {
    /// Face ID.
    case faceID
    /// Touch ID.
    case touchID
    /// Optic ID.
    case opticID
    /// No biometrics: the device passcode.
    case passcode
}
