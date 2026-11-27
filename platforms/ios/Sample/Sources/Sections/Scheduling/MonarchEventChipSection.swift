import MonarchTokens
import MonarchUI
import SwiftUI

struct MonarchEventChipSection: CataloguePage {
    static let title = "Event chip"

    @Environment(\.monarchTheme) private var theme
    @State private var opened = "Nothing opened yet"

    var body: some View {
        Text(opened)
            .monarchTextStyle(.body)
            .foregroundStyle(theme.colors.textMuted)

        CatalogueCaption("Dot: one line, for a month cell")
        ForEach(MonarchEventChipStatus.allCases, id: \.self) { status in
            MonarchEventChip("Stand-up", time: "09:30", status: status) {
                opened = "Opened Stand-up (\(status.rawValue))"
            }
        }

        CatalogueCaption("Bar: for agenda rows")
        ForEach(MonarchEventChipStatus.allCases, id: \.self) { status in
            MonarchEventChip(
                "Design review",
                time: "14:00 – 15:00",
                timeLabel: "2 to 3 in the afternoon",
                detail: "Room 4",
                status: status,
                variant: .bar
            ) {
                opened = "Opened Design review (\(status.rawValue))"
            }
        }

        CatalogueCaption("Block: fills the frame of a time grid")
        HStack(alignment: .top, spacing: theme.spacing.space2) {
            MonarchEventChip("Workshop", time: "10:00 – 12:00", detail: "Lab", variant: .block) {
                opened = "Opened Workshop"
            }
            .frame(height: MonarchTokens.Space.space24)
            MonarchEventChip("Interview", time: "10:30 – 11:30", status: .tentative, variant: .block) {
                opened = "Opened Interview"
            }
            .frame(height: MonarchTokens.Space.space20)
        }
        MonarchEventChip("Short call", time: "16:00", variant: .block, isCompact: true) {
            opened = "Opened Short call"
        }
        .frame(height: MonarchAccessibility.minimumHitTarget)

        CatalogueCaption("An event colour from the app, and a past event")
        MonarchEventChip(
            "Dentist",
            time: "11:00",
            detail: "Personal calendar",
            color: theme.colors.data3,
            variant: .bar
        )
        MonarchEventChip("Retrospective", time: "08:00", variant: .bar, isPast: true)

        CatalogueCaption("Without an action it is a label")
        MonarchEventChip("Company holiday")
    }
}
