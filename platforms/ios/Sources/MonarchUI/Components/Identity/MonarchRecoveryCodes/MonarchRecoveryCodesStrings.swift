/// Built-in strings of the recovery codes, from the `MonarchRecoveryCodes` strings table.
enum MonarchRecoveryCodesStrings {
    private static let table = "MonarchRecoveryCodes"

    private static func string(_ key: String) -> String {
        MonarchStrings.string("monarch.recoveryCodes.\(key)", table: table)
    }

    /// The spoken name of the code list.
    static var listLabel: String { string("listLabel") }
    /// Read after a used code.
    static var used: String { string("used") }
    /// The copy button.
    static var copy: String { string("copy") }
    /// The copy button after copying.
    static var copied: String { string("copied") }
    /// The share button.
    static var save: String { string("save") }
    /// The button that saves the codes with the file exporter.
    static var saveToFiles: String { string("saveToFiles") }
    /// Announced after the file is saved.
    static var saved: String { string("saved") }
    /// Announced when the file could not be saved.
    static var saveFailed: String { string("saveFailed") }
    /// The button that shows the codes.
    static var reveal: String { string("reveal") }
    /// The button that hides the codes.
    static var conceal: String { string("conceal") }
    /// Shown instead of the codes while they are hidden.
    static var concealedHint: String { string("concealedHint") }
    /// The regenerate button.
    static var regenerate: String { string("regenerate") }
    /// The title of the regenerate confirmation.
    static var regenerateTitle: String { string("regenerateTitle") }
    /// The message of the regenerate confirmation.
    static var regenerateDescription: String { string("regenerateDescription") }
    /// The confirming button of the regenerate confirmation.
    static var regenerateConfirm: String { string("regenerateConfirm") }
    /// The cancel button of the regenerate confirmation.
    static var cancel: String { string("cancel") }
    /// Announced after new codes arrive.
    static var regenerated: String { string("regenerated") }
    /// The acknowledgement checkbox.
    static var acknowledge: String { string("acknowledge") }
    /// Shown when Continue is pressed before the checkbox is ticked.
    static var acknowledgeMissing: String { string("acknowledgeMissing") }
    /// The button that finishes the step.
    static var continueTitle: String { string("continue") }

    /// The count line.
    static func remaining(_ left: Int, of total: Int) -> String {
        MonarchStrings.string("monarch.recoveryCodes.remaining", table: table, left, total)
    }
}
