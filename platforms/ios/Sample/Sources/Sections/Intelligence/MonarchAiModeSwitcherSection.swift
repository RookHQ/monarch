import MonarchUI
import SwiftUI

struct MonarchAiModeSwitcherSection: CataloguePage {
    static let title = "AI mode switcher"

    @State private var mode = "ask"
    @State private var ownMode = "research"
    @State private var isRunning = false

    private let ownModes = [
        MonarchAiMode(id: "research", label: "Research", description: "Reads sources and cites them."),
        MonarchAiMode(id: "write", label: "Write", description: "Drafts text for you to edit."),
        MonarchAiMode(
            id: "automate",
            label: "Automate",
            description: "Not set up in this workspace.",
            isDisabled: true
        ),
    ]

    var body: some View {
        CatalogueCaption("Ask, Edit and Agent, with the consequence underneath")
        MonarchAiModeSwitcher(selection: $mode)

        CatalogueCaption("Own modes, md, one disabled")
        MonarchAiModeSwitcher(selection: $ownMode, modes: ownModes, size: .md)

        CatalogueCaption("Disabled while a response runs")
        Toggle("Response running", isOn: $isRunning)
            .monarchTextStyle(.body)
        MonarchAiModeSwitcher(selection: $mode)
            .disabled(isRunning)
    }
}
