import Foundation
import MonarchUI
import SwiftUI

struct MonarchSpaceSwitcherSection: CataloguePage {
    static let title = "Space switcher"

    private let spaces = [
        MonarchSpaceOption(id: "eng", name: "Engineering", description: "18 members", systemImage: "hammer"),
        MonarchSpaceOption(id: "mkt", name: "Marketing", description: "9 members", systemImage: "megaphone"),
        MonarchSpaceOption(id: "ops", name: "Operations", description: "6 members"),
        MonarchSpaceOption(id: "sales", name: "Sales", description: "12 members"),
    ]
    @State private var space: String?
    @State private var fixedSpace: String? = "ops"
    @State private var created = 0

    var body: some View {
        CatalogueCaption("All spaces or one Space, with New Space and Manage Spaces")
        MonarchSpaceSwitcher(
            selection: $space,
            spaces: spaces,
            manageURL: URL(string: "https://example.com/spaces"),
            onCreate: { created += 1 }
        )
        Text("Chosen: \(space ?? "all") · New Space pressed \(created) times")
            .monarchTextStyle(.caption)

        CatalogueCaption("A Space must stay chosen")
        MonarchSpaceSwitcher(selection: $fixedSpace, spaces: spaces, allowsAll: false)

        CatalogueCaption("Icon only, small")
        MonarchSpaceSwitcher(selection: $space, spaces: spaces, size: .sm, isIconOnly: true)

        CatalogueCaption("Loading")
        MonarchSpaceSwitcher(selection: .constant(nil), spaces: [], isLoading: true)
    }
}
