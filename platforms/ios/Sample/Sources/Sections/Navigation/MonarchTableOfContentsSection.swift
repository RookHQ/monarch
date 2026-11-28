import MonarchUI
import SwiftUI

struct MonarchTableOfContentsSection: CataloguePage {
    static let title = "Table of contents"

    @Environment(\.monarchTheme) private var theme
    @State private var current: String?
    @State private var showsContents = false

    private let sections = [
        MonarchTableOfContentsItem("Overview", id: "overview"),
        MonarchTableOfContentsItem("Billing", id: "billing"),
        MonarchTableOfContentsItem("Invoices", id: "invoices", level: 3),
        MonarchTableOfContentsItem("Payment methods", id: "methods", level: 3),
        MonarchTableOfContentsItem("Members", id: "members"),
    ]

    var body: some View {
        CatalogueCaption("The list on its own. Tap an entry to make it current.")
        MonarchTableOfContents(sections, selection: $current) { _ in }

        CatalogueCaption("A document whose sections are tracked while it scrolls")
        ScrollViewReader { proxy in
            ScrollView {
                VStack(alignment: .leading, spacing: theme.spacing.space4) {
                    ForEach(sections) { section in
                        Text(section.title)
                            .monarchTextStyle(section.level == 2 ? .heading : .subheading)
                            .accessibilityAddTraits(.isHeader)
                            .monarchTableOfContentsTarget(section.id)
                        Text(String(repeating: "Text of the \(section.title.lowercased()) section. ", count: 12))
                            .monarchTextStyle(.body)
                    }
                }
                .foregroundStyle(theme.colors.textDefault)
                .padding(theme.spacing.space4)
            }
            .monarchTableOfContentsSpy(selection: $current)
            .frame(height: theme.spacing.space24 * 3)
            .background(theme.colors.bgSurface)
            .sheet(isPresented: $showsContents) {
                MonarchTableOfContents(sections, selection: $current) { id in
                    showsContents = false
                    proxy.scrollTo(id, anchor: .top)
                }
                .padding(theme.spacing.space4)
                .presentationDetents([.medium])
            }
        }
        MonarchButton("Contents", systemImage: "list.bullet.indent") { showsContents = true }
    }
}
