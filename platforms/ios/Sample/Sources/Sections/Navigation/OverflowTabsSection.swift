import MonarchUI
import SwiftUI

/// Recipe: MonarchTabs that scrolls, or MonarchTabs with a More menu.
struct OverflowTabsSection: CataloguePage {
    static let title = "Overflow tabs (MonarchTabs recipe)"

    @State private var section = "overview"

    private let sections = [
        MonarchTabsItem("Overview", value: "overview"),
        MonarchTabsItem("Activity", value: "activity", count: "3"),
        MonarchTabsItem("Members", value: "members"),
        MonarchTabsItem("Billing", value: "billing"),
        MonarchTabsItem("Security", value: "security"),
        MonarchTabsItem("Audit log", value: "audit"),
    ]

    private var overflow: [MonarchTabsItem<String>] { Array(sections.dropFirst(3)) }

    var body: some View {
        Text("Selected: \(section)").monarchTextStyle(.bodyStrong)

        CatalogueCaption("Six tabs: the strip scrolls sideways")
        MonarchTabs("Project sections", selection: $section, items: sections)

        CatalogueCaption("The last three in a More menu")
        HStack(spacing: 0) {
            MonarchTabs("Project sections", selection: $section, items: Array(sections.prefix(3)))
            Menu {
                Picker("More sections", selection: $section) {
                    ForEach(overflow) { item in
                        Text(item.title).tag(item.value)
                    }
                }
                .pickerStyle(.inline)
            } label: {
                Label(overflow.first { $0.value == section }?.title ?? "More", systemImage: "chevron.down")
            }
            .buttonStyle(.monarch(.ghost, size: .sm))
        }
    }
}
