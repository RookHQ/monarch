/// One requirement of a password, with its own test, shown in the list of
/// ``MonarchPasswordStrengthMeter``.
public struct MonarchPasswordRule: Identifiable, Sendable {
    /// A stable key, unique within the list.
    public let id: String
    /// What the password needs, already localised, such as "At least 8 characters".
    public let label: String
    /// Returns `true` when a password meets the rule.
    public let test: @Sendable (String) -> Bool

    /// A requirement.
    ///
    /// - Parameters:
    ///   - id: A stable key, unique within the list.
    ///   - label: What the password needs, already localised.
    ///   - test: Returns `true` when a password meets the rule.
    public init(id: String, label: String, test: @escaping @Sendable (String) -> Bool) {
        self.id = id
        self.label = label
        self.test = test
    }

    /// The default requirements: at least 8 characters, upper and lower case
    /// letters, a number, and a symbol or space. The labels come from the
    /// component's strings table.
    public static var defaults: [MonarchPasswordRule] {
        [
            MonarchPasswordRule(id: "length", label: MonarchPasswordStrengthMeterStrings.ruleLength) {
                $0.unicodeScalars.count >= MonarchPasswordStrengthMeterTokens.minimumLength
            },
            MonarchPasswordRule(id: "case", label: MonarchPasswordStrengthMeterStrings.ruleCase) {
                let categories = $0.unicodeScalars.map(\.properties.generalCategory)
                return categories.contains(.uppercaseLetter) && categories.contains(.lowercaseLetter)
            },
            MonarchPasswordRule(id: "number", label: MonarchPasswordStrengthMeterStrings.ruleNumber) {
                $0.unicodeScalars.contains { $0.properties.generalCategory == .decimalNumber }
            },
            MonarchPasswordRule(id: "symbol", label: MonarchPasswordStrengthMeterStrings.ruleSymbol) {
                $0.unicodeScalars.contains { !MonarchPasswordStrengthMeterTokens.isLetterOrDigit($0) }
            },
        ]
    }
}
