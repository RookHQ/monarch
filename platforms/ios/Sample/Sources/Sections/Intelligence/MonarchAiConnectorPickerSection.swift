import MonarchUI
import SwiftUI

struct MonarchAiConnectorPickerSection: CataloguePage {
    static let title = "AI connector picker"

    @State private var isPicking = false
    @State private var connectors = [
        MonarchAiConnector(
            id: "calendar",
            name: "Calendar",
            description: "Read and create events.",
            status: .connected,
            account: "ada@example.com",
            isEnabled: true,
            tools: [
                MonarchAiConnectorTool(id: "list", name: "List events", risk: .read, permission: .allow),
                MonarchAiConnectorTool(id: "create", name: "Create event", risk: .write, permission: .ask),
                MonarchAiConnectorTool(id: "delete", name: "Delete event", risk: .destructive, permission: .ask),
            ]
        ),
        MonarchAiConnector(
            id: "drive",
            name: "Drive",
            description: "Search your files.",
            status: .needsSignIn,
            isEnabled: false
        ),
        MonarchAiConnector(id: "crm", name: "CRM", status: .error, isEnabled: false),
    ]

    var body: some View {
        let on = connectors.filter { $0.isEnabled && $0.status == .connected }.map(\.name)
        CatalogueCaption("Sign in turns the connector on")
        MonarchButton("Connectors", systemImage: "puzzlepiece.extension") { isPicking = true }
            .sheet(isPresented: $isPicking) {
                MonarchAiConnectorPicker(
                    connectors: $connectors,
                    onConnect: { id in
                        if let index = connectors.firstIndex(where: { $0.id == id }) {
                            let old = connectors[index]
                            connectors[index] = MonarchAiConnector(
                                id: old.id,
                                name: old.name,
                                description: old.description,
                                status: .connected,
                                account: "ada@example.com",
                                isEnabled: true,
                                tools: old.tools
                            )
                        }
                    },
                    onManage: { _ in }
                )
            }
        Text(on.isEmpty ? "No connectors on" : "On: \(on.joined(separator: ", "))")
            .monarchTextStyle(.caption)
    }
}
