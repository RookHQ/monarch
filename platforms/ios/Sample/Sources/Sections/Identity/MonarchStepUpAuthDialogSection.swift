import Foundation
import MonarchUI
import SwiftUI

/// A stand-in for the server rejecting a password or code.
private struct WrongEntry: LocalizedError {
    var errorDescription: String? { "That isn’t right. Try again." }
}

struct MonarchStepUpAuthDialogSection: CataloguePage {
    static let title = "Step-up auth dialog"

    @State private var isConfirming = false
    @State private var isOnlyPassword = false
    @State private var result = "Not confirmed yet"

    var body: some View {
        CatalogueCaption("The password “monarch” or the code 123456 is accepted")
        Toggle("Password only", isOn: $isOnlyPassword)
        MonarchButton("Delete workspace", variant: .danger) { isConfirming = true }
            .sheet(isPresented: $isConfirming) {
                MonarchStepUpAuthDialog(
                    description: "Deleting a workspace needs you to confirm it’s you.",
                    methods: isOnlyPassword ? [.password] : [.password, .code, .deviceOwner],
                    onForgot: { result = "Forgot password tapped" },
                    onCancel: { result = "Cancelled" },
                    onVerified: { result = "Confirmed" },
                    onVerify: { credential in
                        try await Task.sleep(for: .milliseconds(800))
                        switch credential {
                        case .password(let password) where password == "monarch": return
                        case .code(let code) where code == "123456": return
                        case .deviceOwner: return
                        default: throw WrongEntry()
                        }
                    }
                )
            }
        Text(result).monarchTextStyle(.caption)
    }
}
