import Foundation
import SwiftUI
import Testing

@testable import MonarchUI

enum MonarchTimezoneSelectFixtures: ComponentFixtures {
    static let id = "timezone-select"

    /// 15 January 2026, 12:00 UTC: winter in the north, summer in the south.
    static let winter = Date(timeIntervalSince1970: 1_768_478_400)

    static let fixtures = [
        Fixture("chosen legacy id with sections") {
            MonarchTimezoneSelect(
                "Time zone",
                selection: .constant("Asia/Calcutta"),
                detectedZone: "Europe/London",
                pinned: ["America/New_York", "Asia/Tokyo"],
                recent: ["Australia/Sydney"],
                now: winter
            )
        },
        Fixture("empty, required, with error") {
            MonarchTimezoneSelect(
                "Meeting time zone",
                selection: .constant(nil),
                prompt: "Choose a time zone",
                helper: "Times in the invitation use this zone.",
                error: "Choose where the meeting happens.",
                zones: ["UTC", "Europe/Paris", "Asia/Tokyo"],
                showsDetected: false,
                now: winter,
                isRequired: true
            )
        },
        Fixture("custom label") {
            MonarchTimezoneSelect(
                "Office",
                selection: .constant("Europe/Paris"),
                zones: ["Europe/Paris", "America/Los_Angeles"],
                detectedZone: "Europe/Paris",
                now: winter,
                formatLabel: { "\($0.city) (\($0.offsetLabel))" }
            )
        },
        Fixture("read only") {
            MonarchTimezoneSelect(
                "Time zone",
                selection: .constant("Asia/Calcutta"),
                zones: ["Asia/Kolkata"],
                now: winter,
                isReadOnly: true
            )
        },
        Fixture("disabled") {
            MonarchTimezoneSelect("Time zone", selection: .constant("UTC"), zones: ["UTC"], now: winter)
                .disabled(true)
        },
    ]
}

@Suite("MonarchTimezoneSelect")
struct MonarchTimezoneSelectTests {
    private let us = Locale(identifier: "en_US")
    private let winter = MonarchTimezoneSelectFixtures.winter
    /// 15 July 2026, 12:00 UTC.
    private let summer = Date(timeIntervalSince1970: 1_784_116_800)

    @Test("Old ids are read as their current names")
    func canonical() {
        #expect(MonarchTimezoneSelectTokens.canonical("Asia/Calcutta") == "Asia/Kolkata")
        #expect(MonarchTimezoneSelectTokens.canonical("Europe/Kiev") == "Europe/Kyiv")
        #expect(MonarchTimezoneSelectTokens.canonical("GMT") == "UTC")
        #expect(MonarchTimezoneSelectTokens.canonical("Europe/Paris") == "Europe/Paris")
        #expect(MonarchTimezoneSelectTokens.timeZone("Asia/Kolkata") != nil)
        #expect(MonarchTimezoneSelectTokens.timeZone("Not/AZone") == nil)
    }

    @Test("The known zones start with UTC, by current name, without repeats")
    func knownZones() {
        let zones = MonarchTimezoneSelectTokens.knownZones()
        #expect(zones.first == "UTC")
        #expect(zones.contains("Asia/Kolkata"))
        #expect(!zones.contains("Asia/Calcutta"))
        #expect(!zones.contains("GMT"))
        #expect(Set(zones).count == zones.count)
        #expect(zones.count > 300)
    }

    @Test("Offsets are written as UTC±hh:mm")
    func offsetLabel() {
        #expect(MonarchTimezoneSelectTokens.offsetLabel(seconds: 19_800) == "UTC+05:30")
        #expect(MonarchTimezoneSelectTokens.offsetLabel(seconds: -28_800) == "UTC-08:00")
        #expect(MonarchTimezoneSelectTokens.offsetLabel(seconds: 0) == "UTC+00:00")
        #expect(MonarchTimezoneSelectTokens.offsetLabel(seconds: 45_900) == "UTC+12:45")
        #expect(MonarchTimezoneSelectTokens.offsetLabel(seconds: -9_000) == "UTC-02:30")
    }

    @Test("Zone information follows daylight saving at the instant")
    func info() {
        let kolkata = MonarchTimezoneSelectTokens.info("Asia/Kolkata", at: winter, locale: us)
        #expect(kolkata.city == "Kolkata")
        #expect(kolkata.region == "Asia")
        #expect(kolkata.offsetSeconds == 19_800)
        #expect(kolkata.longName == "India Standard Time")
        let york = MonarchTimezoneSelectTokens.info("America/New_York", at: winter, locale: us)
        #expect(york.city == "New York")
        #expect(york.offsetLabel == "UTC-05:00")
        #expect(MonarchTimezoneSelectTokens.info("America/New_York", at: summer, locale: us).offsetLabel == "UTC-04:00")
        let utc = MonarchTimezoneSelectTokens.info("UTC", at: winter, locale: us)
        #expect(utc.city == "UTC")
        #expect(utc.region.isEmpty)
        #expect(MonarchTimezoneSelectTokens.defaultLabel(kolkata) == "(UTC+05:30) Kolkata")
    }

    @Test("Keywords hold the offset spellings and the aliases")
    func keywords() {
        let kolkata = MonarchTimezoneSelectTokens.info("Asia/Kolkata", at: winter, locale: us)
        let words = MonarchTimezoneSelectTokens.keywords(kolkata)
        for word in ["Asia/Kolkata", "UTC+05:30", "utc+5:30", "gmt+5:30", "+530", "india"] {
            #expect(words.contains(word))
        }
        let tokyo = MonarchTimezoneSelectTokens.info("Asia/Tokyo", at: winter, locale: us)
        #expect(MonarchTimezoneSelectTokens.keywords(tokyo).contains("utc+9:00"))
    }

    /// Fixed long names, so the options do not depend on the time zone data
    /// of the OS that runs the tests (the iOS Simulator calls London
    /// "Greenwich Mean Time" in en_US where macOS says "GMT+00:00").
    private static let names: MonarchTimezoneSelectTokens.NameLookup = { zone, style, _ in
        let table: [String: String] = [
            "Europe/London": "GMT+00:00",
            "Europe/Paris": "Central European Standard Time",
            "Asia/Tokyo": "Japan Standard Time",
            "America/New_York": "Eastern Standard Time",
            "UTC": "Coordinated Universal Time",
        ]
        return style == .standard ? table[zone.identifier] : nil
    }

    @Test("Offset-only long names are left out")
    func displayName() {
        typealias T = MonarchTimezoneSelectTokens
        for name in ["GMT+00:00", "GMT-5", "GMT", "UTC", "UTC+01:00", "GMT\u{2212}03:00", " GMT+1 ", "", nil] {
            #expect(T.displayName(name) == "", "\(name ?? "nil") is offset-only")
        }
        #expect(T.displayName("Greenwich Mean Time") == "Greenwich Mean Time")
        #expect(T.displayName("Coordinated Universal Time") == "Coordinated Universal Time")
        #expect(T.displayName("GMT Standard Time") == "GMT Standard Time")
    }

    @Test("The system's long names never repeat only the offset")
    func systemNames() {
        // The words differ between OS releases; the rule must hold on all of them.
        for zone in ["Europe/London", "UTC", "Europe/Paris", "Asia/Kolkata", "America/New_York"] {
            for date in [winter, summer] {
                let name = MonarchTimezoneSelectTokens.info(zone, at: date, locale: us).longName
                #expect(MonarchTimezoneSelectTokens.displayName(name) == name, "\(zone) shows \(name)")
            }
        }
    }

    @Test("Options list the sections first, then every zone by offset")
    func options() {
        let options = MonarchTimezoneSelectTokens.options(
            zones: ["Asia/Tokyo", "UTC", "America/New_York", "Asia/Calcutta"],
            selection: "Europe/Paris",
            detected: "Europe/London",
            pinned: ["Asia/Tokyo", "Europe/London"],
            recent: [],
            at: winter,
            locale: us,
            labels: MonarchTimezoneSelectLabels(),
            formatLabel: nil,
            names: Self.names
        )
        #expect(
            options.map(\.id) == [
                "Europe/London", "Asia/Tokyo", "Europe/Paris", "America/New_York", "UTC", "Asia/Kolkata", "Asia/Tokyo",
            ]
        )
        #expect(
            options.map(\.group) == [
                "Detected", "Pinned", "All time zones", "All time zones", "All time zones", "All time zones",
                "All time zones",
            ]
        )
        // An offset-only long name ("GMT+00:00") is left out; a missing one too.
        #expect(options[0].description == "Europe/London")
        #expect(options[1].description == "Asia/Tokyo · Japan Standard Time")
        #expect(options[2].description == "Europe/Paris · Central European Standard Time")
        #expect(options[5].description == "Asia/Kolkata")
        let plain = MonarchTimezoneSelectTokens.options(
            zones: ["UTC", "Asia/Tokyo"],
            selection: nil,
            detected: nil,
            pinned: [],
            recent: [],
            at: winter,
            locale: us,
            labels: MonarchTimezoneSelectLabels(),
            formatLabel: { $0.city }
        )
        #expect(plain.map(\.group) == [nil, nil])
        #expect(plain.map(\.label) == ["UTC", "Tokyo"])
    }

    @Test("Every word must match, and searching skips the sections")
    func matches() {
        let tokyo = MonarchTimezoneSelectTokens.option(
            MonarchTimezoneSelectTokens.info("Asia/Tokyo", at: winter, locale: us),
            group: "All time zones",
            formatLabel: nil
        )
        let sections: Set<String> = ["Detected", "Pinned", "Recent"]
        #expect(MonarchTimezoneSelectTokens.matches(tokyo, query: "", sectionTitles: sections))
        #expect(MonarchTimezoneSelectTokens.matches(tokyo, query: "japan", sectionTitles: sections))
        #expect(MonarchTimezoneSelectTokens.matches(tokyo, query: "Asia  TOKYO", sectionTitles: sections))
        #expect(MonarchTimezoneSelectTokens.matches(tokyo, query: "+9", sectionTitles: sections))
        #expect(!MonarchTimezoneSelectTokens.matches(tokyo, query: "japan paris", sectionTitles: sections))
        let pinned = MonarchComboboxOption(
            id: "Asia/Tokyo",
            label: tokyo.label,
            group: "Pinned",
            keywords: tokyo.keywords
        )
        #expect(MonarchTimezoneSelectTokens.matches(pinned, query: "", sectionTitles: sections))
        #expect(!MonarchTimezoneSelectTokens.matches(pinned, query: "japan", sectionTitles: sections))
    }

    @Test("The built-in strings resolve")
    func strings() {
        #expect(MonarchTimezoneSelectStrings.detected == "Detected")
        #expect(MonarchTimezoneSelectStrings.pinned == "Pinned")
        #expect(MonarchTimezoneSelectStrings.recent == "Recent")
        #expect(MonarchTimezoneSelectStrings.all == "All time zones")
        #expect(MonarchTimezoneSelectStrings.empty == "No time zones match")
        #expect(MonarchTimezoneSelectStrings.label("UTC+01:00", "Paris") == "(UTC+01:00) Paris")
    }

    @Test("The field is at least a touch target high")
    @MainActor
    func layout() {
        let size = Host.layout(
            MonarchTimezoneSelect("Time zone", selection: .constant("UTC"), zones: ["UTC"], now: winter)
        )
        #expect(size.height >= MonarchAccessibility.minimumHitTarget)
    }

    @Test("A read-only field shows the zone as text, is a touch target high and says Read only")
    @MainActor
    func readOnly() {
        let size = Host.layout(
            MonarchTimezoneSelect(
                "Time zone",
                selection: .constant("Asia/Calcutta"),
                zones: ["Asia/Kolkata"],
                now: winter,
                isReadOnly: true
            )
        )
        #expect(size.height >= MonarchAccessibility.minimumHitTarget)
        let hint = MonarchFieldChrome.accessibilityHint(
            helper: nil,
            error: nil,
            isRequired: true,
            isReadOnly: true
        )
        #expect(hint == "Read only, Required")
    }
}
