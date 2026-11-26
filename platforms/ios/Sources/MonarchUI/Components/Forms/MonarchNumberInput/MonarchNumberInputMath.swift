import Foundation

/// Decimal arithmetic for ``MonarchNumberInput``, so steps never drift:
/// from 0.3 with a step of 0.1, one step up is exactly 0.4.
enum MonarchNumberInputMath {
    /// The exact decimal value of the shortest text that reads back as `value`.
    static func decimal(_ value: Double) -> Decimal? {
        guard value.isFinite else { return nil }
        return Decimal(string: "\(value)", locale: Locale(identifier: "en_US_POSIX"))
    }

    /// The double nearest to a decimal.
    static func double(_ value: Decimal) -> Double? {
        Double(NSDecimalNumber(decimal: value).description(withLocale: Locale(identifier: "en_US_POSIX")))
    }

    /// The number of digits after the decimal point: 0.1 has 1, 1e-7 has 7, 10 has 0.
    static func decimalPlaces(_ value: Double) -> Int {
        guard let decimal = decimal(value) else { return 0 }
        return max(0, -decimal.exponent)
    }

    /// The largest whole number not above `value`.
    static func floor(_ value: Decimal) -> Decimal {
        var input = value
        var result = Decimal()
        NSDecimalRound(&result, &input, 0, .down)
        return result > value ? result - 1 : result
    }

    /// The smallest whole number not below `value`.
    static func ceiling(_ value: Decimal) -> Decimal {
        var input = value
        var result = Decimal()
        NSDecimalRound(&result, &input, 0, .up)
        return result < value ? result + 1 : result
    }

    /// Moves `value` by one step up (`direction` 1) or down (-1) on the grid
    /// `base + k × step`. A value between grid points moves to the next grid
    /// point in that direction: 5 goes to 10 with a step of 10 going up, and
    /// to 0 going down. A step that is not positive leaves the value alone.
    static func stepped(_ value: Double, by step: Double, direction: Int, base: Double = 0) -> Double {
        guard step > 0, let exact = decimal(value), let size = decimal(step), let origin = decimal(base) else {
            return value
        }
        let quotient = (exact - origin) / size
        let below = floor(quotient)
        let delta = Decimal(direction)
        let index: Decimal
        if below == quotient {
            index = quotient + delta
        } else if direction > 0 {
            index = below + delta
        } else {
            index = ceiling(quotient) + delta
        }
        return double(origin + index * size) ?? value
    }

    /// `value` inside the bounds that are given.
    static func clamped(_ value: Double, minimum: Double?, maximum: Double?) -> Double {
        var result = value
        if let maximum { result = min(result, maximum) }
        if let minimum { result = max(result, minimum) }
        return result
    }

    /// The value after the stepper moves it. An empty value starts from the
    /// minimum, or from 0 brought inside the bounds. Steps stop at the bounds.
    static func stepping(
        _ value: Double?,
        by step: Double,
        direction: Int,
        minimum: Double?,
        maximum: Double?
    ) -> Double {
        guard let value else { return clamped(minimum ?? 0, minimum: minimum, maximum: maximum) }
        let next = stepped(value, by: step, direction: direction, base: minimum ?? 0)
        return clamped(next, minimum: minimum, maximum: maximum)
    }

    /// Whether a step up changes the value.
    static func canIncrement(_ value: Double?, step: Double, maximum: Double?) -> Bool {
        guard step > 0 else { return false }
        guard let value, let maximum else { return true }
        return value < maximum
    }

    /// Whether a step down changes the value.
    static func canDecrement(_ value: Double?, step: Double, minimum: Double?) -> Bool {
        guard step > 0 else { return false }
        guard let value, let minimum else { return true }
        return value > minimum
    }

    /// Whether a value is outside the bounds that are given.
    static func isOutOfRange(_ value: Double?, minimum: Double?, maximum: Double?) -> Bool {
        guard let value else { return false }
        if let minimum, value < minimum { return true }
        if let maximum, value > maximum { return true }
        return false
    }
}
