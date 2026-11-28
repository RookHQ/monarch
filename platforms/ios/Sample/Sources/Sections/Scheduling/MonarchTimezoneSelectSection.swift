import MonarchUI
import SwiftUI

struct MonarchTimezoneSelectSection: CataloguePage {
    static let title = "Time zone select"

    @State private var zone: String? = "Asia/Calcutta"
    @State private var meetingZone: String?

    var body: some View {
        MonarchTimezoneSelect(
            "Time zone",
            selection: $zone,
            pinned: ["Europe/London", "America/New_York"],
            recent: ["Asia/Tokyo"]
        )
        Text("Chosen: \(zone ?? "none")").monarchTextStyle(.caption)

        CatalogueCaption("A curated list, required")
        MonarchTimezoneSelect(
            "Meeting time zone",
            selection: $meetingZone,
            prompt: "Choose a time zone",
            helper: "Times in the invitation use this zone.",
            zones: ["UTC", "Europe/London", "Europe/Paris", "Asia/Kolkata", "Asia/Tokyo", "America/Los_Angeles"],
            showsDetected: false,
            isRequired: true
        )
    }
}
