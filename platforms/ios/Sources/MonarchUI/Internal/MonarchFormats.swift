import Foundation

/// Date, time and duration formats shared by components, so one kind of
/// value is written the same way everywhere. Every function takes the
/// locale (and the calendar or time zone where it matters) so output is
/// deterministic in tests.
enum MonarchFormats {
    /// A compact number with its suffix in capitals as the web writes it: "128K", "£2.4M" (the system writes
    /// "128k" and "£2.4m" in British English).
    static func capitalisedCompact(_ text: String) -> String {
        text
            .replacingOccurrences(of: "(?<=[0-9])k(?![A-Za-z])", with: "K", options: .regularExpression)
            .replacingOccurrences(of: "(?<=[0-9])m(?![A-Za-z])", with: "M", options: .regularExpression)
    }

    /// "Nov 4, 2026": the medium date of the locale.
    static func mediumDate(_ date: Date, locale: Locale, timeZone: TimeZone = .autoupdatingCurrent) -> String {
        var calendar = Calendar(identifier: .gregorian)
        calendar.locale = locale
        calendar.timeZone = timeZone
        return date.formatted(
            Date.FormatStyle(date: .abbreviated, time: .omitted, locale: locale, calendar: calendar, timeZone: timeZone)
        )
    }

    /// "2h 05m", "45m", "3d 4h": a duration in minutes, with the minutes
    /// padded to two digits after hours, as the web writes durations.
    /// Negative durations are treated as zero.
    static func duration(minutes: Int) -> String {
        let total = max(0, minutes)
        let days = total / 1440
        let hours = (total % 1440) / 60
        let rest = total % 60
        if days > 0 {
            return hours > 0
                ? MonarchStrings.string("monarch.internal.duration.daysHours", table: "InternalFormats", days, hours)
                : MonarchStrings.string("monarch.internal.duration.days", table: "InternalFormats", days)
        }
        if hours > 0 {
            return MonarchStrings.string(
                "monarch.internal.duration.hoursMinutes",
                table: "InternalFormats",
                hours,
                String(format: "%02d", rest)
            )
        }
        return MonarchStrings.string("monarch.internal.duration.minutes", table: "InternalFormats", rest)
    }

    /// A UTC calendar: a time of day given as minutes from midnight is
    /// formatted on a fixed UTC day, so a daylight-saving change never moves it.
    private static let utc: Calendar = {
        var calendar = Calendar(identifier: .gregorian)
        calendar.timeZone = TimeZone(identifier: "UTC") ?? TimeZone(secondsFromGMT: 0) ?? .gmt
        return calendar
    }()

    /// "6:00 AM" or "06:00" for a time of day in minutes from midnight.
    /// Values outside one day wrap around.
    ///
    /// - Parameters:
    ///   - minutes: Minutes from midnight.
    ///   - locale: The locale of the digits and the day period.
    ///   - uses24HourClock: `true` for a 24-hour clock, `false` for 12 hours, `nil` to follow the locale.
    /// - Returns: The time, such as "06:00".
    static func timeOfDay(_ minutes: Int, locale: Locale, uses24HourClock: Bool? = nil) -> String {
        let wrapped = ((minutes % 1440) + 1440) % 1440
        let date = Date(timeIntervalSinceReferenceDate: TimeInterval(wrapped * 60))
        let utc = Self.utc
        switch uses24HourClock {
        case .some(true):
            let style = Date.VerbatimFormatStyle(
                format: "\(hour: .twoDigits(clock: .twentyFourHour, hourCycle: .zeroBased)):\(minute: .twoDigits)",
                locale: locale,
                timeZone: utc.timeZone,
                calendar: utc
            )
            return date.formatted(style)
        case .some(false):
            let style = Date.VerbatimFormatStyle(
                format:
                    "\(hour: .defaultDigits(clock: .twelveHour, hourCycle: .oneBased)):\(minute: .twoDigits) \(dayPeriod: .standard(.abbreviated))",
                locale: locale,
                timeZone: utc.timeZone,
                calendar: utc
            )
            return date.formatted(style)
        case .none:
            let style = Date.FormatStyle(
                date: .omitted,
                time: .omitted,
                locale: locale,
                calendar: utc,
                timeZone: utc.timeZone
            ).monarchTime()
            return date.formatted(style)
        }
    }

    /// An elapsed time as the AI run views write it: milliseconds under a
    /// second ("450 ms"), seconds with one decimal under ten seconds
    /// ("2.5 s"), whole seconds under a minute, then minutes and seconds
    /// ("1 min 5 s").
    static func elapsed(_ seconds: Double, locale: Locale) -> String {
        let milliseconds = max(0, (seconds * 1000).rounded())
        if milliseconds < 1000 {
            return MonarchStrings.string(
                "monarch.internal.elapsed.milliseconds",
                table: "InternalFormats",
                Int(milliseconds).formatted(.number.locale(locale))
            )
        }
        if milliseconds < 60_000 {
            let digits = milliseconds < 10_000 ? 1 : 0
            return MonarchStrings.string(
                "monarch.internal.elapsed.seconds",
                table: "InternalFormats",
                (milliseconds / 1000).formatted(.number.precision(.fractionLength(digits)).locale(locale))
            )
        }
        let minutes = Int(milliseconds / 60_000)
        let rest = Int(((milliseconds.truncatingRemainder(dividingBy: 60_000)) / 1000).rounded())
        let minuteText = minutes.formatted(.number.locale(locale))
        guard rest > 0 else {
            return MonarchStrings.string("monarch.internal.elapsed.minutes", table: "InternalFormats", minuteText)
        }
        return MonarchStrings.string(
            "monarch.internal.elapsed.minutesSeconds",
            table: "InternalFormats",
            minuteText,
            rest.formatted(.number.locale(locale))
        )
    }

    /// A size in bytes as the web writes it: 1024-based, one decimal at most, "KB", "MB", "GB" (410.2 KB, 4 MB).
    static func fileSize(_ bytes: Int64, locale: Locale) -> String {
        let units = ["bytes", "KB", "MB", "GB", "TB"]
        var value = Double(max(bytes, 0))
        var unit = 0
        while value >= 1024, unit < units.count - 1 {
            value /= 1024
            unit += 1
        }
        let digits = unit == 0 ? 0 : 1
        let text = value.formatted(.number.precision(.fractionLength(0...digits)).locale(locale))
        return text + " " + units[unit]
    }

    /// A cost given in millionths of the currency unit: two decimals, and
    /// up to four below one unit, so a cost of a fraction of a cent shows.
    static func cost(micros: Int, currency: String, locale: Locale) -> String {
        let value = Decimal(micros) / Decimal(1_000_000)
        let digits = micros != 0 && abs(micros) < 1_000_000 ? 4 : 2
        return value.formatted(.currency(code: currency).precision(.fractionLength(2...digits)).locale(locale))
    }
}

extension Date.FormatStyle {
    /// The same style with the time of day added as the web writes it: hours and minutes with two digits on a
    /// 24-hour clock ("09:30"), the locale's 12-hour form otherwise. The date and the time are joined by the
    /// style's own pattern, so "12 Mar 2026, 10:05" and never "12 Mar 2026 at 10:05".
    func monarchTime() -> Date.FormatStyle {
        let pattern = DateFormatter.dateFormat(fromTemplate: "j", options: 0, locale: locale) ?? ""
        if pattern.contains("H") || pattern.contains("k") {
            return hour(.twoDigits(amPM: .omitted)).minute(.twoDigits)
        }
        return hour(.defaultDigits(amPM: .abbreviated)).minute(.twoDigits)
    }
}
