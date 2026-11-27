import MonarchUI
import SwiftUI

struct MonarchSavedViewsSection: CataloguePage {
    static let title = "Saved views"

    @State private var views = [
        MonarchSavedView("All deals", id: "all", isReadOnly: true, group: "Built-in"),
        MonarchSavedView("Open deals", id: "open", count: 12, isDefault: true),
        MonarchSavedView("Team pipeline", id: "team", count: 48, isShared: true, group: "Shared with me"),
    ]
    @State private var selection: String? = "open"
    @State private var onlyMine = false
    @State private var saved: [String: Bool] = ["all": false, "open": false, "team": false]

    private var isModified: Bool { onlyMine != (saved[selection ?? ""] ?? false) }

    var body: some View {
        MonarchSavedViews(
            views,
            selection: $selection,
            isModified: isModified,
            onSaveNew: { name in
                let id = UUID().uuidString
                views.append(MonarchSavedView(name, id: id))
                saved[id] = onlyMine
                selection = id
            },
            onSaveChanges: { id in saved[id] = onlyMine },
            onDiscardChanges: { onlyMine = saved[selection ?? ""] ?? false },
            onRename: { id, name in
                if let index = views.firstIndex(where: { $0.id == id }) { views[index].name = name }
            },
            onDelete: { id in
                views.removeAll { $0.id == id }
                selection = views.first?.id
            },
            onSetDefault: { id in
                for index in views.indices { views[index].isDefault = views[index].id == id }
            }
        )
        .onChange(of: selection) { _, new in onlyMine = saved[new ?? ""] ?? false }

        CatalogueCaption("Change the filter to mark the view as edited")
        Toggle("Only my deals", isOn: $onlyMine).monarchTextStyle(.body)
    }
}
