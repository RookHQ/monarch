import MonarchUI
import SwiftUI

struct MonarchBusinessHoursEditorSection: CataloguePage {
    static let title = "Business hours editor"

    @Environment(\.monarchTheme) private var theme
    @State private var hours = MonarchBusinessHoursValue(
        timeZone: "Europe/London",
        hours: (1...5).map { MonarchWeeklyHoursInterval(day: $0, start: 540, end: 1050) },
        exceptions: [
            MonarchBusinessHoursException(date: Date(timeIntervalSince1970: 1_798_156_800), name: "Christmas Day"),
            MonarchBusinessHoursException(
                date: Date(timeIntervalSince1970: 1_798_675_200),
                name: "New Year’s Eve",
                intervals: [MonarchBusinessHoursInterval(start: 540, end: 780)]
            ),
        ],
        name: "Support hours"
    )
    @State private var isReadOnly = false
    @State private var saveTried = false
    @State private var status = "Not saved"

    var body: some View {
        CatalogueCaption("A calendar in a Form: swipe a date to remove it, or use its button")
        Toggle("Show as text", isOn: $isReadOnly)
            .monarchTextStyle(.body)
        Form {
            MonarchBusinessHoursEditor(
                value: $hours,
                showsDefault: true,
                showsEffectiveDates: true,
                isNameRequired: true,
                isOpenRequired: true,
                showsErrors: saveTried,
                pinnedTimeZones: ["Europe/London", "America/New_York", "Asia/Kolkata"],
                isReadOnly: isReadOnly
            ) {
                Section {
                    Button("Save") {
                        saveTried = true
                        let isValid = hours.validate(isNameRequired: true, isOpenRequired: true).isValid
                        status = isValid ? "Saved \(hours.exceptions.count) dates" : "Fix the highlighted fields"
                    }
                    .buttonStyle(.monarch(.primary))
                    Text(status)
                        .monarchTextStyle(.caption)
                        .foregroundStyle(theme.colors.textMuted)
                }
            }
        }
        .frame(minHeight: 1_800)
    }
}
