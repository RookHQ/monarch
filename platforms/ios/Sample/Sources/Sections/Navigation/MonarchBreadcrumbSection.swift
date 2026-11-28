import MonarchUI
import SwiftUI

struct MonarchBreadcrumbSection: CataloguePage {
    static let title = "Breadcrumb"

    @State private var path = ["Drive", "Projects", "Monarch", "Specs", "Navigation", "Breadcrumb"]

    var body: some View {
        CatalogueCaption("Tap a level to go back to it. The middle levels collapse into a menu.")
        MonarchBreadcrumb(path.map { MonarchBreadcrumbItem($0) }) { item in
            if let index = path.firstIndex(of: item.id) {
                path = Array(path.prefix(through: index))
            }
        }
        MonarchButton("Open a subfolder", systemImage: "folder") {
            path.append("Folder \(path.count + 1)")
        }

        CatalogueCaption("A level without a screen is text")
        MonarchBreadcrumb(
            [
                MonarchBreadcrumbItem("Drive"),
                MonarchBreadcrumbItem("Shared with me", isLinked: false),
                MonarchBreadcrumbItem("Quarterly report"),
            ]
        ) { _ in }
    }
}
