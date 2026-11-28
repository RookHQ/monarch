import SwiftUI

/// A searchable IANA time zone picker, labelled by UTC offset and city and
/// sorted by offset, with Detected, Pinned and Recent sections.
///
/// ```swift
/// MonarchTimezoneSelect(
///     "Time zone",
///     selection: $zone,
///     pinned: ["Europe/London", "America/New_York"],
///     recent: person.recentZones
/// )
/// ```
///
/// It is ``MonarchCombobox`` over `TimeZone.knownTimeZoneIdentifiers`.
/// Options read "(UTC+05:30) Kolkata" with the id and the long name ("Asia/
/// Kolkata · India Standard Time") under it. Offsets include daylight saving
/// at `now`. Search matches every word typed against the city, the id, the
/// offset (`+5:30`, `utc-8`, `0530`), the long name and common country names
/// and abbreviations ("india", "pst"). Old ids select their current name:
/// `Asia/Calcutta` is shown as Kolkata. The detected zone is the
/// environment's time zone unless `detectedZone` is given.
///
/// Accessibility: as ``MonarchCombobox``: the field is a button labelled with
/// `label` whose value is the chosen zone; in the sheet each zone is a button
/// read with its label and description, and the chosen one has the selected
/// trait and a check mark. Section headings name Detected, Pinned and Recent.
/// With `isReadOnly` the field shows the zone as selectable text, its hint
/// says "Read only", and it does not open.
public struct MonarchTimezoneSelect: View {
    private let label: String
    @Binding private var selection: String?
    private let prompt: String?
    private let helper: String?
    private let error: String?
    private let zones: [String]?
    private let detectedZone: String?
    private let showsDetected: Bool
    private let pinned: [String]
    private let recent: [String]
    private let now: Date?
    private let isRequired: Bool
    private let isClearable: Bool
    private let isReadOnly: Bool
    private let labels: MonarchTimezoneSelectLabels
    private let formatLabel: (@Sendable (MonarchTimezoneInfo) -> String)?

    @Environment(\.locale) private var locale
    @Environment(\.timeZone) private var environmentTimeZone

    /// A time zone picker.
    ///
    /// - Parameters:
    ///   - label: The visible label, already localised.
    ///   - selection: The IANA id of the chosen zone, or `nil`. An old id selects its current name.
    ///   - prompt: Shown in the field while nothing is chosen.
    ///   - helper: Help text under the field.
    ///   - error: An error message, shown with an icon and announced.
    ///   - zones: Limits the list to these zones. Every zone the system knows when `nil`.
    ///   - detectedZone: The zone under Detected. The environment's time zone when `nil`.
    ///   - showsDetected: Whether the Detected section is shown.
    ///   - pinned: Zones listed first under Pinned, such as the workspace's offices.
    ///   - recent: Zones listed under Recent, such as the person's last choices.
    ///   - now: The instant offsets are computed for. The current time when `nil`.
    ///   - isRequired: Shows a mark after the label and adds "Required" to the hint.
    ///   - isClearable: Shows a clear button in the field while a zone is chosen.
    ///   - isReadOnly: Shows the chosen zone as text in the field box, without opening the list or a clear button.
    ///   - labels: Replacements for the built-in strings.
    ///   - formatLabel: Builds an option's label. Built in: "(UTC+05:30) Kolkata".
    public init(
        _ label: String,
        selection: Binding<String?>,
        prompt: String? = nil,
        helper: String? = nil,
        error: String? = nil,
        zones: [String]? = nil,
        detectedZone: String? = nil,
        showsDetected: Bool = true,
        pinned: [String] = [],
        recent: [String] = [],
        now: Date? = nil,
        isRequired: Bool = false,
        isClearable: Bool = true,
        isReadOnly: Bool = false,
        labels: MonarchTimezoneSelectLabels = MonarchTimezoneSelectLabels(),
        formatLabel: (@Sendable (MonarchTimezoneInfo) -> String)? = nil
    ) {
        self.label = label
        _selection = selection
        self.prompt = prompt
        self.helper = helper
        self.error = error
        self.zones = zones
        self.detectedZone = detectedZone
        self.showsDetected = showsDetected
        self.pinned = pinned
        self.recent = recent
        self.now = now
        self.isRequired = isRequired
        self.isClearable = isClearable
        self.isReadOnly = isReadOnly
        self.labels = labels
        self.formatLabel = formatLabel
    }

    /// The selection, with an old id read as its current name.
    private var canonicalSelection: Binding<String?> {
        Binding(
            get: { selection.map(MonarchTimezoneSelectTokens.canonical) },
            set: { selection = $0 }
        )
    }

    /// The content of the view.
    public var body: some View {
        let detected = showsDetected ? detectedZone ?? environmentTimeZone.identifier : nil
        let options = MonarchTimezoneSelectTokens.options(
            zones: zones ?? MonarchTimezoneSelectTokens.knownZones(),
            selection: selection,
            detected: detected,
            pinned: pinned,
            recent: recent,
            at: now ?? Date(),
            locale: locale,
            labels: labels,
            formatLabel: formatLabel
        )
        let sections: Set<String> = [
            labels.detected ?? MonarchTimezoneSelectStrings.detected,
            labels.pinned ?? MonarchTimezoneSelectStrings.pinned,
            labels.recent ?? MonarchTimezoneSelectStrings.recent,
        ]
        var comboboxLabels = labels.combobox
        if comboboxLabels.empty == nil {
            comboboxLabels.empty = labels.empty ?? MonarchTimezoneSelectStrings.empty
        }
        return MonarchCombobox(
            label,
            selection: canonicalSelection,
            options: options,
            prompt: prompt,
            helper: helper,
            error: error,
            isRequired: isRequired,
            isClearable: isClearable,
            isReadOnly: isReadOnly,
            labels: comboboxLabels,
            filter: { option, query in
                MonarchTimezoneSelectTokens.matches(option, query: query, sectionTitles: sections)
            }
        )
    }
}

#if DEBUG
    #Preview("MonarchTimezoneSelect") {
        MonarchPreviewMatrix {
            VStack {
                MonarchTimezoneSelect(
                    "Time zone",
                    selection: .constant("Asia/Calcutta"),
                    detectedZone: "Europe/London",
                    pinned: ["America/New_York"]
                )
                MonarchTimezoneSelect(
                    "Meeting time zone",
                    selection: .constant(nil),
                    prompt: "Choose a time zone",
                    error: "Choose where the meeting happens.",
                    zones: ["UTC", "Europe/Paris", "Asia/Tokyo"],
                    showsDetected: false,
                    isRequired: true
                )
            }
        }
    }
#endif
