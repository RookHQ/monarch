import MonarchUI
import SwiftUI

struct MonarchEventPopoverSection: CataloguePage {
    static let title = "Event popover"

    @Environment(\.monarchTheme) private var theme
    @State private var isPopoverShown = false
    @State private var isSheetShown = false
    private let start = Calendar.current.date(bySettingHour: 14, minute: 0, second: 0, of: .now) ?? .now

    var body: some View {
        CatalogueCaption("From an event chip, as a popover")
        MonarchEventChip("Design review", time: "14:00 – 14:45", detail: "Room 4", variant: .bar) {
            isPopoverShown = true
        }
        .popover(isPresented: $isPopoverShown) {
            popover
                .presentationCompactAdaptation(.popover)
                .presentationBackground(theme.colors.bgRaised)
        }
        CatalogueCaption("As a sheet with a medium detent")
        MonarchEventChip("Offsite", time: "All day", status: .tentative, variant: .bar) {
            isSheetShown = true
        }
        .sheet(isPresented: $isSheetShown) {
            MonarchEventPopover(
                "Offsite",
                start: start,
                end: start.addingTimeInterval(2 * 86_400),
                isAllDay: true,
                status: .tentative
            ) {
                MonarchButton("Accept", variant: .primary, size: .sm) { isSheetShown = false }
                MonarchButton("Decline", size: .sm) { isSheetShown = false }
            }
            .presentationDetents([.medium])
        }
        CatalogueCaption("Inline, cancelled")
        MonarchEventPopover(
            "Customer call",
            start: start,
            end: start.addingTimeInterval(1_800),
            status: .cancelled,
            note: "Cancelled by the customer."
        )
        .background(theme.colors.bgRaised, in: RoundedRectangle(cornerRadius: theme.radius.lg))
    }

    private var popover: some View {
        MonarchEventPopover(
            "Design review",
            subtitle: "Product calendar",
            start: start,
            end: start.addingTimeInterval(2_700),
            status: .confirmed,
            details: [
                MonarchEventPopoverDetail("Location", value: "Room 4", systemImage: "mappin.and.ellipse"),
                MonarchEventPopoverDetail("Hosts", value: "Ana Silva, Jonas Weber"),
            ],
            note: "Bring the Q3 numbers."
        ) {
            MonarchButton("Open", variant: .primary, size: .sm) { isPopoverShown = false }
            MonarchButton("Reschedule", size: .sm) { isPopoverShown = false }
        }
    }
}
