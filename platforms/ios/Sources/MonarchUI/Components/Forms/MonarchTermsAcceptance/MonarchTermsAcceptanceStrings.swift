/// Built-in strings of the terms acceptance, from the `MonarchTermsAcceptance` strings table.
enum MonarchTermsAcceptanceStrings {
    private static let table = "MonarchTermsAcceptance"

    /// The checkbox label around the joined document titles.
    static func label(_ documents: String) -> String {
        MonarchStrings.string("monarch.termsAcceptance.label", table: table, documents)
    }

    /// The error while the box is required and not ticked.
    static var requiredMessage: String {
        MonarchStrings.string("monarch.termsAcceptance.requiredMessage", table: table)
    }

    /// The hint on a required box.
    static var requiredHint: String {
        MonarchStrings.string("monarch.termsAcceptance.requiredHint", table: table)
    }
}
