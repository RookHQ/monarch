/// How strong a password is, from very weak to strong.
public enum MonarchPasswordStrength: Int, Sendable, Hashable, CaseIterable, Comparable {
    /// Fewer than half of the rules are met.
    case veryWeak = 0
    /// At least half of the rules are met.
    case weak
    /// At least three quarters of the rules are met.
    case fair
    /// Every rule is met.
    case good
    /// Every rule is met and the password has 14 or more characters.
    case strong

    /// Orders strengths from very weak to strong.
    public static func < (lhs: MonarchPasswordStrength, rhs: MonarchPasswordStrength) -> Bool {
        lhs.rawValue < rhs.rawValue
    }

    /// The default score: the share of rules met, plus one step for 14 or
    /// more characters once every rule is met. An empty password is very weak.
    ///
    /// Replace it with a stronger estimator through the `score` parameter of
    /// ``MonarchPasswordStrengthMeter``.
    ///
    /// - Parameters:
    ///   - password: The password to rate.
    ///   - rules: The requirements to count.
    /// - Returns: The strength.
    public static func score(
        _ password: String,
        rules: [MonarchPasswordRule] = MonarchPasswordRule.defaults
    ) -> MonarchPasswordStrength {
        guard !password.isEmpty else { return .veryWeak }
        let met = rules.filter { $0.test(password) }.count
        if met == rules.count {
            return password.unicodeScalars.count >= MonarchPasswordStrengthMeterTokens.strongLength ? .strong : .good
        }
        let ratio = Double(met) / Double(rules.count)
        if ratio >= MonarchPasswordStrengthMeterTokens.fairRatio { return .fair }
        if ratio >= MonarchPasswordStrengthMeterTokens.weakRatio { return .weak }
        return .veryWeak
    }
}
