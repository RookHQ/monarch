/// A way to confirm identity in a ``MonarchStepUpAuthDialog``.
public enum MonarchStepUpMethod: String, Sendable, Hashable, CaseIterable, Identifiable {
    /// The account password.
    case password
    /// A 6-digit code from an authenticator app.
    case code
    /// A passkey. The app runs the system passkey sheet in `onVerify`.
    case passkey
    /// Face ID, Touch ID, Optic ID or the device passcode, checked on this
    /// device with LocalAuthentication. It proves who holds the device, not
    /// the account, so offer it only where that is enough.
    case deviceOwner

    /// The method itself.
    public var id: Self { self }
}
