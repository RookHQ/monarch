import MonarchUI
import SwiftUI

struct MonarchLogoCloudSection: CataloguePage {
    static let title = "Logo cloud"

    private let names = ["Company A", "Company B", "Company C", "Company D", "Company E", "Company F"]

    var body: some View {
        CatalogueCaption("Wordmarks in a row")
        MonarchLogoCloud(title: "Placeholder organisations", logos: names.map { MonarchLogoCloudLogo($0) })

        CatalogueCaption("Grid with links")
        MonarchLogoCloud(
            title: "Integrations",
            logos: names.prefix(4).map { MonarchLogoCloudLogo($0, destination: URL(string: "https://example.com")) },
            layout: .grid,
            size: .sm
        )
    }
}
