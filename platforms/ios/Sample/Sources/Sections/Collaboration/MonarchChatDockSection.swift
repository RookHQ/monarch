import MonarchUI
import SwiftUI

struct MonarchChatDockSection: CataloguePage {
    static let title = "Chat dock"

    @State private var isOpen = false
    @Environment(\.monarchTheme) private var theme

    var body: some View {
        CatalogueCaption("Tap the launcher")
        Text("The page of the host app.")
            .monarchTextStyle(.body)
            .foregroundStyle(theme.colors.textMuted)
            .frame(maxWidth: .infinity, minHeight: theme.spacing.space24 * 2, alignment: .topLeading)
            .overlay(alignment: .bottomTrailing) {
                MonarchChatDock(isPresented: $isOpen, unread: 3) {
                    NavigationStack {
                        List {
                            MonarchListRow("Design review", subtitle: "Notes are in the doc.")
                            MonarchListRow("Launch planning", subtitle: "Moving the date to Friday.")
                            MonarchListRow("Ada Lovelace", subtitle: "Thanks!")
                        }
                        .navigationTitle("Chat")
                    }
                }
            }
    }
}
