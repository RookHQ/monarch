import Foundation
import MonarchUI
import SwiftUI

struct MonarchPasskeyManagerSection: CataloguePage {
    static let title = "Passkey manager"

    @State private var passkeys = [
        MonarchPasskey(
            id: "phone",
            name: "iPhone",
            created: .now.addingTimeInterval(-86_400 * 60),
            lastUsed: .now.addingTimeInterval(-3_600),
            isSynced: true,
            detail: "iCloud Keychain"
        ),
        MonarchPasskey(id: "key", name: "YubiKey", created: .now.addingTimeInterval(-86_400 * 200)),
    ]
    @State private var addCancels = true
    @State private var isLastSignInMethod = false

    var body: some View {
        CatalogueCaption("The first Add is cancelled, as if the system sheet was closed")
        MonarchPasskeyManager(
            passkeys,
            isLastSignInMethod: isLastSignInMethod,
            onAdd: {
                try await Task.sleep(for: .milliseconds(800))
                if addCancels {
                    addCancels = false
                    throw CancellationError()
                }
                passkeys.append(MonarchPasskey(id: UUID().uuidString, name: "New passkey", created: .now))
            },
            onRename: { id, name in
                try await Task.sleep(for: .milliseconds(500))
                if let index = passkeys.firstIndex(where: { $0.id == id }) {
                    let old = passkeys[index]
                    passkeys[index] = MonarchPasskey(
                        id: old.id,
                        name: name,
                        created: old.created,
                        lastUsed: old.lastUsed,
                        isSynced: old.isSynced,
                        detail: old.detail
                    )
                }
            },
            onRemove: { id in
                try await Task.sleep(for: .milliseconds(500))
                passkeys.removeAll { $0.id == id }
            }
        )
        Toggle("Passkeys are the only sign-in method", isOn: $isLastSignInMethod)

        CatalogueCaption("This device can’t create passkeys")
        MonarchPasskeyManager([], isSupported: false, onAdd: {})
    }
}
