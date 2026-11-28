/// The characters a one-time code accepts.
public enum MonarchOtpPinInputMode: Sendable, Hashable, CaseIterable {
    /// Digits only, typed on the number pad. The default.
    case numeric
    /// The letters A to Z and digits. Letters are stored in upper case.
    case alphanumeric
}
