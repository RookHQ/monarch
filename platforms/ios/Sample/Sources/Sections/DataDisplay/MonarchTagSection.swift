import MonarchUI
import SwiftUI

struct MonarchTagSection: CataloguePage {
    static let title = "Tag"

    @Environment(\.monarchTheme) private var theme
    @State private var labels = ["Design", "Research", "Priority: high"]
    @State private var showsUnread = true
    @State private var showsMine = false

    var body: some View {
        CatalogueCaption("Tones")
        ForEach(MonarchTone.allCases, id: \.self) { tone in
            HStack(spacing: theme.spacing.space2) {
                MonarchTag(String(describing: tone), tone: tone)
                MonarchTag(String(describing: tone), tone: tone, size: .sm)
            }
        }
        CatalogueCaption("Removable")
        ForEach(labels, id: \.self) { label in
            MonarchTag(label, systemImage: "tag") {
                labels.removeAll { $0 == label }
            }
        }
        MonarchButton("Reset", variant: .ghost, size: .sm) {
            labels = ["Design", "Research", "Priority: high"]
        }
        CatalogueCaption("Selectable")
        MonarchTag("Unread", isSelected: $showsUnread)
        MonarchTag("Assigned to me", isSelected: $showsMine, tone: .info)
        CatalogueCaption("Disabled")
        MonarchTag("Archived", isSelected: .constant(true)) {}
            .disabled(true)
    }
}
