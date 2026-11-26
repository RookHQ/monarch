import Foundation
import MonarchUI
import SwiftUI

struct MonarchWorkspaceSwitcherSection: CataloguePage {
    static let title = "Workspace switcher"

    private let few = [
        MonarchWorkspaceOption(id: "acme", name: "Acme", handle: "acme"),
        MonarchWorkspaceOption(id: "rook", name: "Rook Labs", handle: "rooklabs"),
        MonarchWorkspaceOption(id: "nord", name: "Café Nord", handle: "cafenord"),
    ]
    private let many = (1...12).map {
        MonarchWorkspaceOption(id: "w\($0)", name: "Client workspace \($0)", handle: "client\($0)")
    }
    @State private var workspace: String? = "acme"
    @State private var client: String? = "w1"
    @State private var created = 0

    var body: some View {
        CatalogueCaption("Three workspaces: a menu")
        MonarchWorkspaceSwitcher(
            selection: $workspace,
            workspaces: few,
            manageURL: URL(string: "https://example.com/workspaces"),
            onCreate: { created += 1 }
        )
        Text("Current: \(workspace ?? "none") · Create pressed \(created) times")
            .monarchTextStyle(.caption)

        CatalogueCaption("Twelve workspaces: a searchable sheet")
        MonarchWorkspaceSwitcher(
            selection: $client,
            workspaces: many,
            manageURL: URL(string: "https://example.com/workspaces"),
            onCreate: { created += 1 }
        )

        CatalogueCaption("Logo only, small")
        MonarchWorkspaceSwitcher(selection: $workspace, workspaces: few, size: .sm, isIconOnly: true)

        CatalogueCaption("Nothing chosen, loading")
        MonarchWorkspaceSwitcher(selection: .constant(nil), workspaces: [], isLoading: true)
    }
}
