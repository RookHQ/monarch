import MonarchUI
import SwiftUI

/// Recipe: a settings row that opens one switch for sale or sharing.
struct PrivacyChoicesLinkSection: CataloguePage {
    static let title = "Privacy choices link (recipe)"

    @State private var isOpen = false
    @State private var allowsSale = true
    @State private var draft = true
    @State private var isLockedBySignal = false

    var body: some View {
        Toggle("Global Privacy Control on the account", isOn: $isLockedBySignal)
        Button {
            draft = allowsSale && !isLockedBySignal
            isOpen = true
        } label: {
            Label("Your privacy choices", systemImage: "hand.raised")
        }
        .buttonStyle(.monarchLink(.standalone))
        .monarchTextStyle(.label)
        .sheet(isPresented: $isOpen) {
            NavigationStack {
                Form {
                    Section {
                        Text(
                            "We may share information about how you use the app, such as screens viewed and device identifiers, with advertising partners. You can turn this off at any time."
                        )
                        .monarchTextStyle(.body)
                    }
                    Section {
                        Toggle("Allow sale or sharing", isOn: $draft)
                            .disabled(isLockedBySignal)
                    } footer: {
                        Text(
                            isLockedBySignal
                                ? "A Global Privacy Control signal is on for your account, so sale and sharing stay off."
                                : "When off, we stop selling or sharing your personal information."
                        )
                    }
                    if let policy = URL(string: "https://example.com/privacy") {
                        Link("Privacy policy", destination: policy)
                            .buttonStyle(.monarchLink(.standalone, isExternal: true))
                    }
                }
                .navigationTitle("Your privacy choices")
                .navigationBarTitleDisplayMode(.inline)
                .toolbar {
                    ToolbarItem(placement: .cancellationAction) {
                        Button("Cancel") { isOpen = false }
                    }
                    ToolbarItem(placement: .confirmationAction) {
                        Button("Save") {
                            allowsSale = draft
                            isOpen = false
                        }
                    }
                }
            }
            .presentationDetents([.medium, .large])
        }
        Text(allowsSale && !isLockedBySignal ? "Sale and sharing allowed" : "Sale and sharing off")
            .monarchTextStyle(.caption)
    }
}
