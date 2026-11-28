import MonarchUI
import SwiftUI

struct MonarchAiLauncherSection: CataloguePage {
    static let title = "AI launcher"

    @Environment(\.monarchTheme) private var theme
    @State private var isOpen = false

    var body: some View {
        Text(isOpen ? "The assistant is open." : "The assistant is closed.")
            .monarchTextStyle(.body)

        CatalogueCaption("Solid, for floating over content (Command-J on a hardware keyboard)")
        HStack(spacing: theme.spacing.space4) {
            MonarchAiLauncher(isOpen: $isOpen)
            MonarchAiLauncher(isOpen: $isOpen, badge: .unread, shortcut: nil)
            MonarchAiLauncher(isOpen: $isOpen, badge: .working, shortcut: nil)
            MonarchAiLauncher(isOpen: $isOpen, badge: .count(128), shortcut: nil)
        }

        CatalogueCaption("Subtle, for a toolbar")
        MonarchAiLauncher(isOpen: $isOpen, variant: .subtle, shortcut: nil)
        MonarchAiLauncher(isOpen: $isOpen, variant: .subtle, badge: .count(3), shortcut: nil)

        CatalogueCaption("Disabled")
        MonarchAiLauncher(isOpen: $isOpen, shortcut: nil)
            .disabled(true)
    }
}
