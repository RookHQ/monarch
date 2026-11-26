import MonarchUI
import SwiftUI

struct MonarchWeeklyHoursEditorSection: CataloguePage {
    static let title = "Weekly hours editor"

    @Environment(\.monarchTheme) private var theme
    @State private var hours = (1...5).map { MonarchWeeklyHoursInterval(day: $0, start: 540, end: 1020) }
    @State private var isReadOnly = false
    @State private var saved = "Not saved"

    private var problems: [Int: MonarchWeeklyHoursError] { MonarchWeeklyHoursInterval.validate(hours) }

    var body: some View {
        Toggle("Show as text", isOn: $isReadOnly)
            .monarchTextStyle(.body)
        MonarchWeeklyHoursEditor(value: $hours, isReadOnly: isReadOnly) {
            Text("Times in Europe/London")
        }
        MonarchButton("Save", variant: .primary) {
            saved = "Saved \(hours.count) intervals"
        }
        .disabled(!problems.isEmpty)
        Text(saved)
            .monarchTextStyle(.caption)
            .foregroundStyle(theme.colors.textMuted)
        CatalogueCaption("Disabled")
        MonarchWeeklyHoursEditor(value: .constant([MonarchWeeklyHoursInterval(day: 1, start: 540, end: 720)]))
            .disabled(true)
    }
}
