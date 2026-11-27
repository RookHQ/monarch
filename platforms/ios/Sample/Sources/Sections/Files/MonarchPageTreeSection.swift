import MonarchUI
import SwiftUI

struct MonarchPageTreeSection: CataloguePage {
    static let title = "Page tree"

    @State private var groups = [
        MonarchPageTreeGroup(
            id: "basics",
            label: "Basics",
            pages: [
                MonarchPageTreePage(id: "welcome", title: "Welcome"),
                MonarchPageTreePage(id: "setup", title: "Set up your workspace"),
                MonarchPageTreePage(id: "invite", title: "Invite your team"),
            ]
        ),
        MonarchPageTreeGroup(
            id: "guides",
            label: "Guides",
            pages: [
                MonarchPageTreePage(id: "billing", title: "Billing"),
                MonarchPageTreePage(id: "export", title: "Export data"),
            ]
        ),
    ]
    @State private var selection: String? = "welcome"
    @State private var editMode: EditMode = .inactive
    @State private var nextID = 1

    var body: some View {
        CatalogueCaption("Organiser: drag in edit mode, long-press a page, or use a group's menu")
        EditButton()
        MonarchPageTree(
            groups,
            selection: $selection,
            onReorder: { newGroups, _ in groups = newGroups },
            onAddPage: { groupID in
                guard let index = groups.firstIndex(where: { $0.id == groupID }) else { return }
                groups[index].pages.append(MonarchPageTreePage(id: newID("page"), title: ""))
            },
            onAddGroup: { name in
                groups.append(MonarchPageTreeGroup(id: newID("group"), label: name, pages: []))
            },
            onRenameGroup: { groupID, name in
                guard let index = groups.firstIndex(where: { $0.id == groupID }) else { return }
                groups[index].label = name
            },
            onDeleteGroup: { groupID in groups.removeAll { $0.id == groupID } },
            onDeletePage: { pageID in
                for index in groups.indices { groups[index].pages.removeAll { $0.id == pageID } }
            }
        )
        .environment(\.editMode, $editMode)
        .frame(minHeight: 480)
        Text("Open: \(selection ?? "none")").monarchTextStyle(.caption)

        CatalogueCaption("Navigation only, without numbers")
        MonarchPageTree(groups, selection: $selection, isNumbered: false)
            .frame(minHeight: 320)
    }

    private func newID(_ prefix: String) -> String {
        nextID += 1
        return "\(prefix)-\(nextID)"
    }
}
