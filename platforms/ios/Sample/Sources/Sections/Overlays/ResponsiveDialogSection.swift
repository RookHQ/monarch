import MonarchUI
import SwiftUI

/// Native adoption: the sheet, which is a form sheet at regular width.
struct ResponsiveDialogSection: CataloguePage {
    static let title = "Responsive dialog (sheet)"

    @Environment(\.monarchTheme) private var theme
    @State private var isOpen = false
    @State private var email = ""
    @State private var sent: [String] = []

    var body: some View {
        CatalogueCaption("A sheet from the bottom on iPhone, a centred form sheet on iPad")
        MonarchButton("Invite", systemImage: "person.badge.plus", variant: .primary) {
            email = ""
            isOpen = true
        }
        .sheet(isPresented: $isOpen) {
            NavigationStack {
                VStack(alignment: .leading, spacing: theme.spacing.space4) {
                    MonarchTextField("Email address", text: $email, prompt: "name@example.com")
                        .keyboardType(.emailAddress)
                        .textInputAutocapitalization(.never)
                }
                .padding(theme.spacing.space4)
                .frame(maxHeight: .infinity, alignment: .top)
                .navigationTitle("Invite a person")
                .navigationBarTitleDisplayMode(.inline)
                .toolbar {
                    ToolbarItem(placement: .cancellationAction) {
                        Button("Cancel") { isOpen = false }
                    }
                    ToolbarItem(placement: .confirmationAction) {
                        Button("Send") {
                            sent.append(email)
                            isOpen = false
                        }
                        .disabled(email.isEmpty)
                    }
                }
            }
            .presentationDetents([.medium, .large])
            .presentationDragIndicator(.visible)
            .presentationBackground(theme.colors.bgSurface)
        }
        ForEach(sent, id: \.self) { address in
            Text("Invited \(address)").monarchTextStyle(.body)
        }
    }
}
