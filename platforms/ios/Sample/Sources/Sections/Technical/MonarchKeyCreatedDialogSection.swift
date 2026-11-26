import MonarchUI
import SwiftUI

struct MonarchKeyCreatedDialogSection: CataloguePage {
    static let title = "Key created dialog"

    private struct CreatedKey: Identifiable {
        let id = UUID()
        let secret: String
        let kind: MonarchKeyCreatedDialogKind
    }

    @State private var created: CreatedKey?
    @State private var result = "No key created yet."

    var body: some View {
        MonarchButton("Create secret key", systemImage: "key", variant: .primary) {
            created = CreatedKey(secret: "sk_live_51HxQ2JkL0mNpQr8sT", kind: .secret)
        }
        MonarchButton("Create publishable key", variant: .secondary) {
            created = CreatedKey(secret: "pk_live_9d2f7a61c0b8", kind: .public)
        }
        Text(result).monarchTextStyle(.caption)
            .sheet(item: $created) { key in
                MonarchKeyCreatedDialog(
                    key.secret,
                    label: key.kind == .secret ? "API key" : "Publishable key",
                    name: "Production server",
                    kind: key.kind,
                    onCopy: { _ in result = "Copied the key." },
                    onDone: { result = "Closed after saving." }
                )
            }

        CatalogueCaption("The sheet content, masked")
        MonarchKeyCreatedDialog("whsec_9d2f7a61c0b84e3f", label: "Signing secret", isMasked: true)
    }
}
