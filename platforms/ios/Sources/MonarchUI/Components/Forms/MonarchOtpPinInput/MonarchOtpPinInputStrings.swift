/// Built-in strings of the one-time code input, from the `MonarchOtpPinInput` strings table.
enum MonarchOtpPinInputStrings {
    /// Spoken as part of the hint: how many characters the code has.
    static func lengthHint(_ length: Int) -> String {
        MonarchStrings.string("monarch.otpPinInput.lengthHint", table: "MonarchOtpPinInput", String(length))
    }

    /// Spoken as part of the hint, and announced, when the input has an error.
    static func error(_ message: String) -> String {
        MonarchStrings.string("monarch.otpPinInput.error", table: "MonarchOtpPinInput", message)
    }
}
