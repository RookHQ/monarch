/// What the person gave in a ``MonarchStepUpAuthDialog``, passed to `onVerify`.
public enum MonarchStepUpCredential: Sendable, Hashable {
    /// The password, as typed.
    case password(String)
    /// The 6-digit authenticator code.
    case code(String)
    /// The person chose their passkey. Run the system passkey sheet
    /// (`ASAuthorizationController`) in `onVerify`.
    case passkey
    /// The device owner was confirmed on this device with Face ID, Touch ID,
    /// Optic ID or the passcode before `onVerify` was called.
    case deviceOwner

    /// The method that produced this credential.
    public var method: MonarchStepUpMethod {
        switch self {
        case .password: .password
        case .code: .code
        case .passkey: .passkey
        case .deviceOwner: .deviceOwner
        }
    }
}
