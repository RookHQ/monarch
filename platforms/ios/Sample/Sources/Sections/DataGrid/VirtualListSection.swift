import MonarchUI
import SwiftUI

/// Native adoption: `List` and `LazyVStack` create rows only when they are
/// near the screen.
struct VirtualListSection: CataloguePage {
    static let title = "VirtualList (List, LazyVStack)"

    private struct Contact: Identifiable {
        let id: Int
        var name: String { "Contact \(id.formatted())" }
    }

    @Environment(\.monarchTheme) private var theme
    @State private var created = 0

    private let contacts = (1...5000).map { Contact(id: $0) }

    var body: some View {
        CatalogueCaption("LazyVStack: 5,000 rows, \(created.formatted()) created so far")
        LazyVStack(alignment: .leading, spacing: 0) {
            ForEach(contacts) { contact in
                VStack(alignment: .leading, spacing: 0) {
                    Text(contact.name)
                        .monarchTextStyle(.body)
                        .foregroundStyle(theme.colors.textDefault)
                        .padding(.vertical, theme.spacing.space3)
                    MonarchDivider()
                }
                .onAppear { created = max(created, contact.id) }
            }
        }
    }
}
