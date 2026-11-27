/// Built-in strings of the password strength meter, from the `MonarchPasswordStrengthMeter` strings table.
enum MonarchPasswordStrengthMeterStrings {
    private static let table = "MonarchPasswordStrengthMeter"

    /// The name of a strength.
    static func level(_ strength: MonarchPasswordStrength) -> String {
        switch strength {
        case .veryWeak: MonarchStrings.string("monarch.passwordStrengthMeter.level.veryWeak", table: table)
        case .weak: MonarchStrings.string("monarch.passwordStrengthMeter.level.weak", table: table)
        case .fair: MonarchStrings.string("monarch.passwordStrengthMeter.level.fair", table: table)
        case .good: MonarchStrings.string("monarch.passwordStrengthMeter.level.good", table: table)
        case .strong: MonarchStrings.string("monarch.passwordStrengthMeter.level.strong", table: table)
        }
    }

    /// Shown in place of a strength while the password is empty.
    static var empty: String { MonarchStrings.string("monarch.passwordStrengthMeter.empty", table: table) }

    /// The strength line.
    static func strength(_ level: String) -> String {
        MonarchStrings.string("monarch.passwordStrengthMeter.strength", table: table, level)
    }

    /// The spoken name of the list of rules.
    static var rulesLabel: String { MonarchStrings.string("monarch.passwordStrengthMeter.rulesLabel", table: table) }

    /// A rule that is met, as spoken.
    static func met(_ rule: String) -> String {
        MonarchStrings.string("monarch.passwordStrengthMeter.met", table: table, rule)
    }

    /// A rule that is not met, as spoken.
    static func unmet(_ rule: String) -> String {
        MonarchStrings.string("monarch.passwordStrengthMeter.unmet", table: table, rule)
    }

    /// The label of the default length rule.
    static var ruleLength: String { MonarchStrings.string("monarch.passwordStrengthMeter.rule.length", table: table) }
    /// The label of the default letter case rule.
    static var ruleCase: String { MonarchStrings.string("monarch.passwordStrengthMeter.rule.case", table: table) }
    /// The label of the default number rule.
    static var ruleNumber: String { MonarchStrings.string("monarch.passwordStrengthMeter.rule.number", table: table) }
    /// The label of the default symbol rule.
    static var ruleSymbol: String { MonarchStrings.string("monarch.passwordStrengthMeter.rule.symbol", table: table) }
}
