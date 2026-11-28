import MonarchUI
import SwiftUI

/// Native adoption: `keyboardShortcut`, listed by iPadOS while Command is held.
struct KeyboardShortcutsSheetSection: CataloguePage {
    static let title = "Keyboard shortcuts (keyboardShortcut)"

    @Environment(\.monarchTheme) private var theme
    @State private var last = "None yet"

    var body: some View {
        Text("With a hardware keyboard on an iPad, hold Command to see the system's list of these shortcuts.")
            .monarchTextStyle(.body)
        Text("Last action: \(last)")
            .monarchTextStyle(.bodyStrong)

        CatalogueCaption("Command N, Command Shift D, Command F")
        MonarchButton("New ticket", systemImage: "plus", variant: .primary) { last = "New ticket" }
            .keyboardShortcut("n", modifiers: .command)
        MonarchButton("Duplicate ticket") { last = "Duplicate ticket" }
            .keyboardShortcut("d", modifiers: [.command, .shift])
        MonarchButton("Find", systemImage: "magnifyingglass") { last = "Find" }
            .keyboardShortcut("f", modifiers: .command)

        CatalogueCaption("Return runs the default action")
        MonarchButton("Save", variant: .primary) { last = "Save" }
            .keyboardShortcut(.defaultAction)
    }
}
