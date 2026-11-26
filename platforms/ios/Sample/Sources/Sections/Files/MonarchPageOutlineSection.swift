import MonarchUI
import SwiftUI

struct MonarchPageOutlineSection: CataloguePage {
    static let title = "Page outline"

    private static let headings = [
        MonarchPageOutlineHeading("Overview", level: 1, id: "overview"),
        MonarchPageOutlineHeading("Goals", level: 2, id: "goals"),
        MonarchPageOutlineHeading("Measures", level: 3, id: "measures"),
        MonarchPageOutlineHeading("Risks", level: 2, id: "risks"),
        MonarchPageOutlineHeading("Timeline", level: 2, id: "timeline"),
    ]
    private static let sectionText =
        "This paragraph stands in for a section of a longer document. Scroll or pick a heading in the outline."

    @Environment(\.monarchTheme) private var theme
    @State private var current: String? = "overview"
    @State private var showsOutline = false

    var body: some View {
        CatalogueCaption("Beside the content")
        MonarchPageOutline(
            Self.headings,
            currentID: current,
            stats: MonarchPageOutlineStats(text: Array(repeating: Self.sectionText, count: 5).joined(separator: " "))
        ) { heading in
            current = heading.id
        }

        CatalogueCaption("In a sheet, scrolling a document")
        ScrollViewReader { proxy in
            VStack(alignment: .leading, spacing: theme.spacing.space3) {
                MonarchButton("On this page", systemImage: "list.bullet.indent") { showsOutline = true }
                ScrollView {
                    VStack(alignment: .leading, spacing: theme.spacing.space4) {
                        ForEach(Self.headings) { heading in
                            Text(heading.text)
                                .monarchTextStyle(heading.level == 1 ? .title : .heading)
                                .accessibilityAddTraits(.isHeader)
                                .id(heading.id)
                            Text(Self.sectionText).monarchTextStyle(.body)
                        }
                    }
                }
                .frame(height: theme.spacing.space24 * 3)
            }
            .sheet(isPresented: $showsOutline) {
                MonarchPageOutline(Self.headings, currentID: current) { heading in
                    current = heading.id
                    showsOutline = false
                    withAnimation { proxy.scrollTo(heading.id, anchor: .top) }
                }
                .padding()
                .presentationDetents([.medium, .large])
            }
        }

        CatalogueCaption("Empty")
        MonarchPageOutline([]) { _ in }
    }
}
