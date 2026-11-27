/// Built-in strings of the step-up dialog, from the `MonarchStepUpAuthDialog` strings table.
enum MonarchStepUpAuthDialogStrings {
    private static let table = "MonarchStepUpAuthDialog"

    private static func string(_ key: String) -> String {
        MonarchStrings.string("monarch.stepUpAuthDialog.\(key)", table: table)
    }

    /// The title.
    static var title: String { string("title") }
    /// The line under the title.
    static var description: String { string("description") }
    /// The spoken name of the method picker.
    static var methodPicker: String { string("methodPicker") }
    /// The name of a method.
    static func methodName(_ method: MonarchStepUpMethod) -> String { string("method.\(method.rawValue)") }
    /// The password field.
    static var passwordLabel: String { string("passwordLabel") }
    /// The empty password error.
    static var passwordRequired: String { string("passwordRequired") }
    /// The code field.
    static var codeLabel: String { string("codeLabel") }
    /// The help under the code field.
    static var codeHelper: String { string("codeHelper") }
    /// The incomplete code error.
    static var codeIncomplete: String { string("codeIncomplete") }
    /// The passkey text.
    static var passkeyDescription: String { string("passkeyDescription") }
    /// The passkey button.
    static var passkeyButton: String { string("passkeyButton") }
    /// The device text.
    static var deviceOwnerDescription: String { string("deviceOwnerDescription") }
    /// The device button for a kind of check.
    static func deviceOwnerButton(_ biometry: MonarchStepUpBiometry) -> String {
        switch biometry {
        case .faceID: string("deviceOwnerButton.faceID")
        case .touchID: string("deviceOwnerButton.touchID")
        case .opticID: string("deviceOwnerButton.opticID")
        case .passcode: string("deviceOwnerButton.passcode")
        }
    }
    /// The device check is unavailable.
    static var deviceOwnerUnavailable: String { string("deviceOwnerUnavailable") }
    /// The confirm button.
    static var submit: String { string("submit") }
    /// The cancel button.
    static var cancel: String { string("cancel") }
    /// The forgot-password button.
    static var forgot: String { string("forgot") }
    /// The generic failure.
    static var genericError: String { string("genericError") }
}
