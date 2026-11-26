import MonarchUI
import SwiftUI

struct MonarchAiContextDividerSection: CataloguePage {
    static let title = "AI context divider"

    @State private var showsEarlier = false

    var body: some View {
        CatalogueCaption("Day")
        MonarchAiContextDivider(.day, date: Date())
        MonarchAiContextDivider(.day, date: Date().addingTimeInterval(-86_400))
        MonarchAiContextDivider(.day, date: Date().addingTimeInterval(-86_400 * 400))

        CatalogueCaption("Context reset")
        MonarchAiContextDivider(.contextReset)

        CatalogueCaption("Summarized")
        MonarchAiContextDivider(.summarized)
        MonarchAiContextDivider(
            .summarized,
            summary: "The team agreed to ship the report on Friday and to review the numbers on Monday."
        ) {
            showsEarlier.toggle()
        }
        if showsEarlier {
            Text("The earlier messages would be shown here.")
                .monarchTextStyle(.caption)
        }
    }
}
