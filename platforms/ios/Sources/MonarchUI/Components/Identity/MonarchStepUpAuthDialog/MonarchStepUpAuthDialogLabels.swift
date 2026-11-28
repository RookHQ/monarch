/// Replacements for the built-in strings of ``MonarchStepUpAuthDialog``.
///
/// Every property is `nil` by default, which keeps the built-in string.
public struct MonarchStepUpAuthDialogLabels: Sendable {
    /// The title. Built in: "Confirm it’s you".
    public var title: String?
    /// The line under the title, also the reason the system shows for Face ID. Built in: "This action needs you to confirm your identity again."
    public var description: String?
    /// The spoken name of the method picker. Built in: "Verification method".
    public var methodPicker: String?
    /// The name of a method in the picker. Return `nil` to keep the built-in name: "Password", "Authenticator app", "Passkey", "This device".
    public var methodName: (@Sendable (MonarchStepUpMethod) -> String?)?
    /// The password field. Built in: "Password".
    public var passwordLabel: String?
    /// Shown when Confirm is pressed with no password. Built in: "Enter your password."
    public var passwordRequired: String?
    /// The code field. Built in: "Authentication code".
    public var codeLabel: String?
    /// Under the code field. Built in: "Enter the 6-digit code from your authenticator app."
    public var codeHelper: String?
    /// Shown when Confirm is pressed with fewer than 6 digits. Built in: "Enter all 6 digits."
    public var codeIncomplete: String?
    /// The text of the passkey method. Built in: "Use your passkey, security key or device screen lock."
    public var passkeyDescription: String?
    /// The passkey button. Built in: "Use passkey".
    public var passkeyButton: String?
    /// The text of the device method. Built in: "Confirm with Face ID, Touch ID or your device passcode."
    public var deviceOwnerDescription: String?
    /// The device method's button. Return `nil` to keep the built-in title: "Use Face ID", "Use Touch ID", "Use Optic ID" or "Use passcode".
    public var deviceOwnerButton: (@Sendable (MonarchStepUpBiometry) -> String?)?
    /// Shown when the device has no passcode set. Built in: "Set a passcode on this device to use it to confirm."
    public var deviceOwnerUnavailable: String?
    /// The confirm button of the password and code methods. Built in: "Confirm".
    public var submit: String?
    /// The cancel button. Built in: "Cancel".
    public var cancel: String?
    /// The forgot-password button. Built in: "Forgot your password?"
    public var forgot: String?
    /// Shown when verification fails without a message. Built in: "That did not work. Try again."
    public var genericError: String?

    /// Replacements for built-in strings. Every parameter is optional.
    public init(
        title: String? = nil,
        description: String? = nil,
        methodPicker: String? = nil,
        methodName: (@Sendable (MonarchStepUpMethod) -> String?)? = nil,
        passwordLabel: String? = nil,
        passwordRequired: String? = nil,
        codeLabel: String? = nil,
        codeHelper: String? = nil,
        codeIncomplete: String? = nil,
        passkeyDescription: String? = nil,
        passkeyButton: String? = nil,
        deviceOwnerDescription: String? = nil,
        deviceOwnerButton: (@Sendable (MonarchStepUpBiometry) -> String?)? = nil,
        deviceOwnerUnavailable: String? = nil,
        submit: String? = nil,
        cancel: String? = nil,
        forgot: String? = nil,
        genericError: String? = nil
    ) {
        self.title = title
        self.description = description
        self.methodPicker = methodPicker
        self.methodName = methodName
        self.passwordLabel = passwordLabel
        self.passwordRequired = passwordRequired
        self.codeLabel = codeLabel
        self.codeHelper = codeHelper
        self.codeIncomplete = codeIncomplete
        self.passkeyDescription = passkeyDescription
        self.passkeyButton = passkeyButton
        self.deviceOwnerDescription = deviceOwnerDescription
        self.deviceOwnerButton = deviceOwnerButton
        self.deviceOwnerUnavailable = deviceOwnerUnavailable
        self.submit = submit
        self.cancel = cancel
        self.forgot = forgot
        self.genericError = genericError
    }
}
