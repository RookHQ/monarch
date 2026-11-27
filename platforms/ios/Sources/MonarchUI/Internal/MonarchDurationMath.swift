import Foundation

/// Parsing, formatting and stepping of durations in minutes ("1h 30m"), for
/// ``MonarchDurationInput``, the escalation policy editor and the timesheet
/// grid. Pure, so it is tested without a view.
enum MonarchDurationMath {
    /// What a text reads as.
    enum Parsed: Equatable {
        /// The text is empty or only spaces.
        case empty
        /// A duration in whole minutes.
        case minutes(Int)
        /// Text that cannot be read as a duration.
        case invalid
    }

    /// The units a duration can be written in.
    private enum DurationUnit {
        case days, hours, minutes
    }

    /// Unit words, in English, as the web component reads them.
    private static let unitWords: [String: DurationUnit] = [
        "d": .days, "day": .days, "days": .days,
        "h": .hours, "hr": .hours, "hrs": .hours, "hour": .hours, "hours": .hours,
        "m": .minutes, "min": .minutes, "mins": .minutes, "minute": .minutes, "minutes": .minutes,
    ]

    /// Minutes in an hour.
    static let minutesPerHour = 60
    /// Minutes in a calendar day, the default length of a "d".
    static let minutesPerCalendarDay = 1440

    /// Reads a duration as whole minutes. Accepts `90`, `90m`, `1h 30m`,
    /// `1h30`, `1.5h`, `1,5h`, `1:30`, `2 hours 15 minutes` and `1d 4h`.
    static func parse(_ text: String, minutesPerDay: Int = minutesPerCalendarDay) -> Parsed {
        let source = text.trimmingCharacters(in: .whitespacesAndNewlines).lowercased()
            .replacingOccurrences(of: ",", with: ".")
        if source.isEmpty { return .empty }
        let characters = Array(source)
        var index = 0

        // A bare number is minutes.
        if let number = readNumber(characters, &index), index == characters.count {
            return .minutes(rounded(number))
        }
        // A clock, hours and two-digit minutes below 60.
        if let clock = readClock(characters) { return .minutes(clock) }

        index = 0
        var total = 0.0
        var consumed = 0
        var lastUnit: DurationUnit?
        while index < characters.count {
            var cursor = index
            guard let number = readNumber(characters, &cursor) else { break }
            skipSpaces(characters, &cursor)
            let wordStart = cursor
            while cursor < characters.count, characters[cursor].isASCII, characters[cursor].isLetter { cursor += 1 }
            guard cursor > wordStart else { break }
            guard let unit = unitWords[String(characters[wordStart..<cursor])] else { return .invalid }
            skipSpaces(characters, &cursor)
            switch unit {
            case .days: total += number * Double(minutesPerDay)
            case .hours: total += number * Double(minutesPerHour)
            case .minutes: total += number
            }
            index = cursor
            consumed = cursor
            lastUnit = unit
        }
        if consumed == 0 { return .invalid }
        // "1h30": a bare number after hours is minutes.
        let rest = String(characters[consumed...]).trimmingCharacters(in: .whitespaces)
        if !rest.isEmpty {
            guard lastUnit == .hours, rest.allSatisfy({ $0.isASCII && $0.isNumber }), let extra = Double(rest) else {
                return .invalid
            }
            total += extra
        }
        return .minutes(rounded(total))
    }

    /// Writes minutes as `0m`, `45m`, `1h 30m`, `26h`, or `1d 2h` when days are the largest unit.
    static func format(
        _ minutes: Int,
        largestUnit: MonarchDurationInputUnit = .hours,
        minutesPerDay: Int = minutesPerCalendarDay
    ) -> String {
        let total = max(0, minutes)
        if total == 0 { return "0m" }
        let days = largestUnit == .days && minutesPerDay > 0 ? total / minutesPerDay : 0
        let afterDays = total - days * minutesPerDay
        let hours = afterDays / minutesPerHour
        let rest = afterDays % minutesPerHour
        var parts: [String] = []
        if days > 0 { parts.append("\(days)d") }
        if hours > 0 { parts.append("\(hours)h") }
        if rest > 0 { parts.append("\(rest)m") }
        return parts.joined(separator: " ")
    }

    /// Moves `base` one step up or down, snapped to the step grid
    /// (50 + 15 gives 60, 50 − 15 gives 45), then kept inside the bounds.
    static func stepped(_ base: Int, by step: Int, direction: Int, minimum: Int, maximum: Int?) -> Int {
        let size = max(1, abs(step))
        let next: Int
        if direction > 0 {
            next = floorDiv(base, size) * size + size
        } else {
            next = ceilDiv(base, size) * size - size
        }
        return clamped(next, minimum: minimum, maximum: maximum)
    }

    /// `value` inside the bounds.
    static func clamped(_ value: Int, minimum: Int, maximum: Int?) -> Int {
        var result = max(minimum, value)
        if let maximum { result = min(result, max(minimum, maximum)) }
        return result
    }

    /// Whether a value is outside the bounds.
    static func isOutOfRange(_ value: Int, minimum: Int, maximum: Int?) -> Bool {
        if value < minimum { return true }
        if let maximum, value > maximum { return true }
        return false
    }

    /// The value a step starts from: the text if it reads, else the saved value, else the minimum.
    static func stepBase(parsed: Parsed, saved: Int?, minimum: Int) -> Int {
        if case .minutes(let minutes) = parsed { return minutes }
        return saved ?? minimum
    }

    private static func rounded(_ value: Double) -> Int {
        Int(value.rounded(.toNearestOrAwayFromZero))
    }

    private static func floorDiv(_ value: Int, _ size: Int) -> Int {
        let quotient = value / size
        return value % size != 0 && value < 0 ? quotient - 1 : quotient
    }

    private static func ceilDiv(_ value: Int, _ size: Int) -> Int {
        let quotient = value / size
        return value % size != 0 && value > 0 ? quotient + 1 : quotient
    }

    private static func skipSpaces(_ characters: [Character], _ index: inout Int) {
        while index < characters.count, characters[index].isWhitespace { index += 1 }
    }

    /// Reads digits with an optional fraction ("1", "1.5") at `index`.
    private static func readNumber(_ characters: [Character], _ index: inout Int) -> Double? {
        let start = index
        var cursor = index
        while cursor < characters.count, characters[cursor].isASCII, characters[cursor].isNumber { cursor += 1 }
        guard cursor > start else { return nil }
        if cursor + 1 < characters.count, characters[cursor] == ".", characters[cursor + 1].isASCII,
            characters[cursor + 1].isNumber
        {
            cursor += 1
            while cursor < characters.count, characters[cursor].isASCII, characters[cursor].isNumber { cursor += 1 }
        }
        guard let number = Double(String(characters[start..<cursor])) else { return nil }
        index = cursor
        return number
    }

    /// Reads `h:mm` with minutes from 00 to 59.
    private static func readClock(_ characters: [Character]) -> Int? {
        guard let colon = characters.firstIndex(of: ":") else { return nil }
        let hours = characters[..<colon]
        let minutes = characters[(colon + 1)...]
        guard !hours.isEmpty, minutes.count == 2,
            hours.allSatisfy({ $0.isASCII && $0.isNumber }),
            minutes.allSatisfy({ $0.isASCII && $0.isNumber }),
            let hourValue = Int(String(hours)), let minuteValue = Int(String(minutes)), minuteValue < minutesPerHour
        else { return nil }
        return hourValue * minutesPerHour + minuteValue
    }
}
