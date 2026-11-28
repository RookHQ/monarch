import Foundation

/// Exact conversions for ``MonarchCurrencyInput``: rounding to the
/// currency's decimals and moving between major and minor units through
/// `Decimal`, so 19.99 dollars is always 1999 cents.
enum MonarchCurrencyInputMath {
    /// The number of decimals of an ISO 4217 currency: USD 2, JPY 0, KWD 3.
    static func fractionDigits(currencyCode: String) -> Int {
        MonarchMoneyTokens.fractionDigits(currencyCode: currencyCode)
    }

    /// `value` rounded half away from zero to `digits` decimals, computed on
    /// its shortest decimal text so 1.005 rounds to 1.01.
    static func rounded(_ value: Double, digits: Int) -> Double {
        guard var exact = MonarchNumberInputMath.decimal(value) else { return value }
        var result = Decimal()
        NSDecimalRound(&result, &exact, digits, .plain)
        return MonarchNumberInputMath.double(result) ?? value
    }

    /// A major-unit amount as whole minor units: 19.99 USD is 1999.
    static func minorUnits(_ major: Double, digits: Int) -> Int? {
        guard var exact = MonarchNumberInputMath.decimal(major) else { return nil }
        var scaled = Decimal()
        NSDecimalMultiplyByPowerOf10(&scaled, &exact, Int16(digits), .plain)
        var whole = Decimal()
        NSDecimalRound(&whole, &scaled, 0, .plain)
        return Int(exactly: NSDecimalNumber(decimal: whole).int64Value)
    }

    /// Whole minor units as a major-unit amount: 1999 USD is 19.99.
    static func majorUnits(_ minor: Int, digits: Int) -> Double {
        let amount = Decimal(sign: minor < 0 ? .minus : .plus, exponent: -digits, significand: Decimal(minor.magnitude))
        return MonarchNumberInputMath.double(amount) ?? Double(minor)
    }

    /// The lowest amount allowed: the given minimum, or zero when negative
    /// amounts are not allowed and no minimum is given.
    static func lowest(minimum: Double?, allowsNegative: Bool) -> Double? {
        minimum ?? (allowsNegative ? nil : 0)
    }
}
