import Foundation

/// Pure logic of ``MonarchTimezoneSelect``: zone names, offsets, options and search.
enum MonarchTimezoneSelectTokens {
    /// Old ids that systems still report, mapped to the current IANA names.
    static let legacyZones: [String: String] = [
        "Africa/Asmera": "Africa/Asmara",
        "America/Buenos_Aires": "America/Argentina/Buenos_Aires",
        "America/Catamarca": "America/Argentina/Catamarca",
        "America/Cordoba": "America/Argentina/Cordoba",
        "America/Jujuy": "America/Argentina/Jujuy",
        "America/Mendoza": "America/Argentina/Mendoza",
        "America/Coral_Harbour": "America/Atikokan",
        "America/Godthab": "America/Nuuk",
        "America/Indianapolis": "America/Indiana/Indianapolis",
        "America/Louisville": "America/Kentucky/Louisville",
        "Asia/Calcutta": "Asia/Kolkata",
        "Asia/Katmandu": "Asia/Kathmandu",
        "Asia/Rangoon": "Asia/Yangon",
        "Asia/Saigon": "Asia/Ho_Chi_Minh",
        "Asia/Dacca": "Asia/Dhaka",
        "Asia/Thimbu": "Asia/Thimphu",
        "Asia/Ulan_Bator": "Asia/Ulaanbaatar",
        "Asia/Macao": "Asia/Macau",
        "Atlantic/Faeroe": "Atlantic/Faroe",
        "Europe/Kiev": "Europe/Kyiv",
        "Pacific/Enderbury": "Pacific/Kanton",
        "Pacific/Ponape": "Pacific/Pohnpei",
        "Pacific/Truk": "Pacific/Chuuk",
        "Etc/UTC": "UTC",
        "Etc/GMT": "UTC",
        "Etc/Universal": "UTC",
        "Etc/Zulu": "UTC",
        "GMT": "UTC",
        "UCT": "UTC",
        "Universal": "UTC",
        "Zulu": "UTC",
    ]

    /// Country names, old names and abbreviations, so people find zones the way they think of them.
    static let aliases: [String: String] = [
        "UTC": "gmt zulu universal coordinated",
        "Asia/Kolkata": "india ist calcutta mumbai delhi bengaluru bangalore chennai hyderabad",
        "Asia/Dubai": "uae emirates gulf abu dhabi",
        "Asia/Riyadh": "saudi arabia",
        "Asia/Karachi": "pakistan",
        "Asia/Dhaka": "bangladesh",
        "Asia/Kathmandu": "nepal katmandu",
        "Asia/Colombo": "sri lanka",
        "Asia/Singapore": "sgt",
        "Asia/Tokyo": "japan jst",
        "Asia/Seoul": "korea kst",
        "Asia/Shanghai": "china beijing cst",
        "Asia/Hong_Kong": "hkt",
        "Asia/Jakarta": "indonesia wib",
        "Asia/Manila": "philippines",
        "Asia/Ho_Chi_Minh": "vietnam saigon",
        "Asia/Bangkok": "thailand",
        "Asia/Jerusalem": "israel tel aviv",
        "Europe/London": "uk united kingdom britain england gmt bst",
        "Europe/Dublin": "ireland",
        "Europe/Paris": "france cet",
        "Europe/Berlin": "germany cet",
        "Europe/Madrid": "spain",
        "Europe/Rome": "italy",
        "Europe/Amsterdam": "netherlands holland",
        "Europe/Kyiv": "ukraine kiev",
        "Europe/Istanbul": "turkey türkiye",
        "Europe/Moscow": "russia msk",
        "Africa/Lagos": "nigeria wat",
        "Africa/Nairobi": "kenya eat",
        "Africa/Johannesburg": "south africa sast",
        "Africa/Cairo": "egypt",
        "America/New_York": "us usa united states eastern est edt",
        "America/Chicago": "us usa united states central cst cdt",
        "America/Denver": "us usa united states mountain mst mdt",
        "America/Phoenix": "us usa arizona mst",
        "America/Los_Angeles": "us usa united states pacific pst pdt california san francisco",
        "America/Anchorage": "alaska",
        "Pacific/Honolulu": "hawaii hst",
        "America/Toronto": "canada eastern",
        "America/Vancouver": "canada pacific",
        "America/Mexico_City": "mexico",
        "America/Sao_Paulo": "brazil brasil",
        "America/Argentina/Buenos_Aires": "argentina",
        "America/Bogota": "colombia",
        "Australia/Sydney": "australia aest aedt",
        "Australia/Melbourne": "australia aest aedt",
        "Australia/Perth": "australia awst",
        "Pacific/Auckland": "new zealand nzst nzdt",
    ]

    /// The current IANA name of an id: `Asia/Calcutta` is `Asia/Kolkata`, `GMT` is `UTC`.
    static func canonical(_ zone: String) -> String {
        legacyZones[zone] ?? zone
    }

    /// The system's time zone for an id, trying its old name when the current one is unknown.
    static func timeZone(_ zone: String) -> TimeZone? {
        if let found = TimeZone(identifier: zone) { return found }
        let legacy = legacyZones.first { $0.value == zone }?.key
        return legacy.flatMap { TimeZone(identifier: $0) }
    }

    /// Every zone the system knows, by current name, with UTC first.
    static func knownZones() -> [String] {
        var seen: Set<String> = ["UTC"]
        var out = ["UTC"]
        for zone in TimeZone.knownTimeZoneIdentifiers.map(canonical) where seen.insert(zone).inserted {
            out.append(zone)
        }
        return out
    }

    /// An offset as text: `UTC+05:30`, `UTC-08:00`, `UTC+00:00`.
    static func offsetLabel(seconds: Int) -> String {
        let minutes = abs(seconds) / 60
        return String(format: "UTC%@%02d:%02d", seconds < 0 ? "-" : "+", minutes / 60, minutes % 60)
    }

    /// Looks up the long name of a zone ("India Standard Time") in a name
    /// style and a locale. The names come from the system's time zone data,
    /// which differs between OS releases (an iOS Simulator and macOS can
    /// disagree), so tests pass their own table.
    typealias NameLookup = @Sendable (TimeZone, NSTimeZone.NameStyle, Locale) -> String?

    /// The system's long names.
    static let systemNames: NameLookup = { zone, style, locale in
        zone.localizedName(for: style, locale: locale)
    }

    /// A long name worth showing, or an empty string. A name that only
    /// repeats the offset ("GMT+00:00", "GMT-5", "UTC+01:00") adds nothing
    /// to the offset label and is left out.
    static func displayName(_ name: String?) -> String {
        guard let name = name?.trimmingCharacters(in: .whitespaces), !name.isEmpty else { return "" }
        for prefix in ["GMT", "UTC"] where name.hasPrefix(prefix) {
            let rest = name.dropFirst(prefix.count)
            if rest.isEmpty || rest.first == "+" || rest.first == "-" || rest.first == "\u{2212}" { return "" }
        }
        return name
    }

    /// The offset, city and long name of a zone at an instant.
    static func info(
        _ zone: String,
        at now: Date,
        locale: Locale,
        names: NameLookup = systemNames
    ) -> MonarchTimezoneInfo {
        let parts = zone.split(separator: "/").map(String.init)
        let city = zone == "UTC" ? "UTC" : (parts.last ?? zone).replacingOccurrences(of: "_", with: " ")
        let region = parts.count > 1 ? parts[0] : ""
        let system = timeZone(zone)
        let offset = system?.secondsFromGMT(for: now) ?? 0
        var longName = ""
        if let system {
            let style: NSTimeZone.NameStyle = system.isDaylightSavingTime(for: now) ? .daylightSaving : .standard
            longName = displayName(names(system, style, locale))
        }
        return MonarchTimezoneInfo(
            id: zone,
            city: city,
            region: region,
            offsetSeconds: offset,
            offsetLabel: offsetLabel(seconds: offset),
            longName: longName
        )
    }

    /// The default option label: `(UTC+05:30) Kolkata`.
    static func defaultLabel(_ info: MonarchTimezoneInfo) -> String {
        MonarchTimezoneSelectStrings.label(info.offsetLabel, info.city)
    }

    /// The searched words of a zone: its id, offset in several spellings and aliases.
    static func keywords(_ info: MonarchTimezoneInfo) -> String {
        let short = String(info.offsetLabel.dropFirst("UTC".count))
        let trimmed =
            short.count > 1 && short.dropFirst().hasPrefix("0") ? "\(short.prefix(1))\(short.dropFirst(2))" : short
        return [
            info.id.replacingOccurrences(of: "_", with: " "),
            info.offsetLabel,
            "gmt\(trimmed)",
            "utc\(trimmed)",
            trimmed,
            trimmed.replacingOccurrences(of: ":", with: ""),
            aliases[info.id] ?? "",
        ]
        .joined(separator: " ")
    }

    /// The option of a zone.
    static func option(
        _ info: MonarchTimezoneInfo,
        group: String?,
        formatLabel: (@Sendable (MonarchTimezoneInfo) -> String)?
    ) -> MonarchComboboxOption {
        MonarchComboboxOption(
            id: info.id,
            label: formatLabel?(info) ?? defaultLabel(info),
            description: info.longName.isEmpty ? info.id : "\(info.id) · \(info.longName)",
            group: group,
            keywords: keywords(info)
        )
    }

    /// The zones of the full list, by current name, sorted by offset then id.
    static func sortedZones(_ zones: [String], at now: Date) -> [String] {
        var seen: Set<String> = []
        var offsets: [(zone: String, offset: Int)] = []
        for zone in zones.map(canonical) where seen.insert(zone).inserted {
            let offset: Int = timeZone(zone)?.secondsFromGMT(for: now) ?? 0
            offsets.append((zone, offset))
        }
        offsets.sort { lhs, rhs in
            lhs.offset == rhs.offset ? lhs.zone < rhs.zone : lhs.offset < rhs.offset
        }
        return offsets.map { $0.zone }
    }

    /// The options: the detected, pinned and recent zones in their sections,
    /// then every zone (also a selected zone missing from the list), under
    /// "All time zones" when other sections are shown.
    static func options(
        zones: [String],
        selection: String?,
        detected: String?,
        pinned: [String],
        recent: [String],
        at now: Date,
        locale: Locale,
        labels: MonarchTimezoneSelectLabels,
        formatLabel: (@Sendable (MonarchTimezoneInfo) -> String)?,
        names: NameLookup = systemNames
    ) -> [MonarchComboboxOption] {
        let all = sortedZones(zones, at: now)
        var cache: [String: MonarchTimezoneInfo] = [:]
        func info(_ zone: String) -> MonarchTimezoneInfo {
            if let known = cache[zone] { return known }
            let made = self.info(zone, at: now, locale: locale, names: names)
            cache[zone] = made
            return made
        }
        var out: [MonarchComboboxOption] = []
        var seen: Set<String> = []
        func section(_ list: [String], _ title: String) {
            for zone in list.map(canonical) where seen.insert(zone).inserted {
                out.append(option(info(zone), group: title, formatLabel: formatLabel))
            }
        }
        section(detected.map { [$0] } ?? [], labels.detected ?? MonarchTimezoneSelectStrings.detected)
        section(pinned, labels.pinned ?? MonarchTimezoneSelectStrings.pinned)
        section(recent, labels.recent ?? MonarchTimezoneSelectStrings.recent)
        let group = out.isEmpty ? nil : labels.all ?? MonarchTimezoneSelectStrings.all
        let selected = selection.map(canonical)
        let extra = selected.flatMap { all.contains($0) ? nil : $0 }.map { [$0] } ?? []
        for zone in extra + all {
            out.append(option(info(zone), group: group, formatLabel: formatLabel))
        }
        return out
    }

    /// Whether an option matches the search: every word typed is in its
    /// label, description or keywords. While searching, only the full list is
    /// searched, so a zone is not listed twice.
    static func matches(_ option: MonarchComboboxOption, query: String, sectionTitles: Set<String>) -> Bool {
        let words = query.lowercased().split(whereSeparator: \.isWhitespace)
        guard !words.isEmpty else { return true }
        if let group = option.group, sectionTitles.contains(group) { return false }
        let haystack = [option.label, option.description ?? "", option.keywords ?? ""].joined(separator: " ")
            .lowercased()
        return words.allSatisfy { haystack.contains($0) }
    }
}
