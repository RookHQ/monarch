import Foundation

/// The value logic of ``MonarchRangeSlider``: snapping, limits, the thumb a
/// touch picks and the mapping between values and positions. Pure, so it is
/// tested without a view.
enum MonarchRangeSliderModel {
    /// One of the two thumbs.
    enum Thumb: Sendable, Hashable, CaseIterable {
        /// The thumb of the lower value.
        case lower
        /// The thumb of the upper value.
        case upper
    }

    /// The rules of one slider.
    struct Rules: Sendable, Equatable {
        /// The lowest and highest values.
        let bounds: ClosedRange<Double>
        /// The distance between allowed values; 0 or less for any value.
        let step: Double
        /// The smallest distance between the two values.
        let gap: Double

        init(bounds: ClosedRange<Double>, step: Double, minimumStepsBetween: Int) {
            self.bounds = bounds
            self.step = step
            let gap = step > 0 ? Double(max(0, minimumStepsBetween)) * step : 0
            self.gap = min(gap, bounds.upperBound - bounds.lowerBound)
        }
    }

    /// A value clamped to the bounds and rounded to the nearest step.
    static func snap(_ value: Double, rules: Rules) -> Double {
        let low = rules.bounds.lowerBound
        let high = rules.bounds.upperBound
        let clamped = min(max(value, low), high)
        guard rules.step > 0 else { return clamped }
        let steps = ((clamped - low) / rules.step).rounded()
        let snapped = low + steps * rules.step
        // Rounds to the decimal places of the step and the lower bound, which
        // removes the error that repeated multiplication adds (0.30000000000000004).
        let scale = pow(10, Double(max(decimalPlaces(rules.step), decimalPlaces(low))))
        return min(max((snapped * scale).rounded() / scale, low), high)
    }

    /// The number of decimal places of a value, at most 10.
    static func decimalPlaces(_ value: Double) -> Int {
        var places = 0
        var scaled = abs(value)
        while places < 10, abs(scaled.rounded() - scaled) > 1e-9 * max(1, scaled) {
            scaled *= 10
            places += 1
        }
        return places
    }

    /// A range with both ends snapped, low first, and at least the gap apart
    /// where the bounds allow it.
    static func normalized(_ range: ClosedRange<Double>, rules: Rules) -> ClosedRange<Double> {
        let low = snap(range.lowerBound, rules: rules)
        var high = snap(range.upperBound, rules: rules)
        if high - low < rules.gap {
            high = min(rules.bounds.upperBound, low + rules.gap)
        }
        return min(low, high)...max(low, high)
    }

    /// Where one thumb may go, given the other thumb.
    static func limits(of thumb: Thumb, in value: ClosedRange<Double>, rules: Rules) -> ClosedRange<Double> {
        switch thumb {
        case .lower:
            let upper = max(rules.bounds.lowerBound, value.upperBound - rules.gap)
            return rules.bounds.lowerBound...upper
        case .upper:
            let lower = min(rules.bounds.upperBound, value.lowerBound + rules.gap)
            return lower...rules.bounds.upperBound
        }
    }

    /// The range after one thumb moves towards a raw value. The thumb stops
    /// at the other thumb (plus the gap), so the thumbs never cross.
    static func moving(
        _ thumb: Thumb,
        to raw: Double,
        in value: ClosedRange<Double>,
        rules: Rules
    ) -> ClosedRange<Double> {
        let limit = limits(of: thumb, in: value, rules: rules)
        let target = min(max(snap(raw, rules: rules), limit.lowerBound), limit.upperBound)
        switch thumb {
        case .lower: return target...value.upperBound
        case .upper: return value.lowerBound...target
        }
    }

    /// The range after VoiceOver's increment or decrement on one thumb: one
    /// step, or a hundredth of the bounds when there is no step.
    static func adjusting(
        _ thumb: Thumb,
        increment: Bool,
        in value: ClosedRange<Double>,
        rules: Rules
    ) -> ClosedRange<Double> {
        let span = rules.bounds.upperBound - rules.bounds.lowerBound
        let delta = rules.step > 0 ? rules.step : span / 100
        let current = thumb == .lower ? value.lowerBound : value.upperBound
        return moving(thumb, to: current + (increment ? delta : -delta), in: value, rules: rules)
    }

    /// The thumb a touch at `raw` moves: the nearer one. Below both it is the
    /// lower thumb and above both the upper, except that when both thumbs
    /// sit at the top the lower one is picked, so it can be pulled back.
    static func nearestThumb(to raw: Double, in value: ClosedRange<Double>, rules: Rules) -> Thumb {
        let low = value.lowerBound
        let high = value.upperBound
        if raw <= low { return .lower }
        if raw >= high { return low == high && high == rules.bounds.upperBound ? .lower : .upper }
        return raw - low <= high - raw ? .lower : .upper
    }

    /// The position of a value along the bounds, from 0 to 1.
    static func fraction(of value: Double, rules: Rules) -> Double {
        let span = rules.bounds.upperBound - rules.bounds.lowerBound
        guard span > 0 else { return 0 }
        return min(max((value - rules.bounds.lowerBound) / span, 0), 1)
    }

    /// The raw value at a fraction of the bounds.
    static func value(atFraction fraction: Double, rules: Rules) -> Double {
        let span = rules.bounds.upperBound - rules.bounds.lowerBound
        return rules.bounds.lowerBound + min(max(fraction, 0), 1) * span
    }

    /// The horizontal centre of a thumb, from the leading or trailing edge
    /// in reading order. The thumbs travel between half a thumb from each end.
    static func position(
        ofFraction fraction: Double,
        width: Double,
        thumbDiameter: Double,
        isRightToLeft: Bool
    ) -> Double {
        let travel = max(0, width - thumbDiameter)
        let fromStart = thumbDiameter / 2 + fraction * travel
        return isRightToLeft ? width - fromStart : fromStart
    }

    /// The fraction under a touch at `x`, the inverse of
    /// ``position(ofFraction:width:thumbDiameter:isRightToLeft:)``.
    static func fraction(atX x: Double, width: Double, thumbDiameter: Double, isRightToLeft: Bool) -> Double {
        let travel = width - thumbDiameter
        guard travel > 0 else { return 0 }
        let fromStart = isRightToLeft ? width - x : x
        return min(max((fromStart - thumbDiameter / 2) / travel, 0), 1)
    }
}
