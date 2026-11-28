import MonarchUI
import SwiftUI

struct MonarchMethodBadgeSection: CataloguePage {
    static let title = "Method badge"

    @Environment(\.monarchTheme) private var theme

    private let endpoints = [
        ("GET", "/v1/invoices"),
        ("POST", "/v1/invoices"),
        ("PUT", "/v1/invoices/{id}"),
        ("PATCH", "/v1/invoices/{id}"),
        ("DELETE", "/v1/invoices/{id}"),
        ("HEAD", "/v1/invoices/{id}"),
        ("OPTIONS", "/v1/invoices"),
    ]

    var body: some View {
        CatalogueCaption("An endpoint list")
        ForEach(endpoints, id: \.0) { method, path in
            HStack(alignment: .firstTextBaseline, spacing: theme.spacing.space2) {
                MonarchMethodBadge(method)
                Text(path)
                    .monarchTextStyle(.code)
                    .foregroundStyle(theme.colors.textDefault)
            }
        }
        CatalogueCaption("A method outside the common set is neutral")
        MonarchMethodBadge("purge")
        CatalogueCaption("A tone chosen by the app")
        MonarchMethodBadge("purge", tone: .danger)
    }
}
