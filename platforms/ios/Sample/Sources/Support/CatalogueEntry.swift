import SwiftUI

/// One page in a family, created on demand.
struct CatalogueEntry: Identifiable {
    let title: String
    let make: @MainActor () -> AnyView

    var id: String { title }

    @MainActor
    init<Page: CataloguePage>(_ page: Page.Type) {
        self.title = page.title
        self.make = { AnyView(page.init()) }
    }
}
