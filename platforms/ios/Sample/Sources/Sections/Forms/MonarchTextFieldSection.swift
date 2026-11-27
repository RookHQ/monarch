import MonarchUI
import SwiftUI

struct MonarchTextFieldSection: CataloguePage {
    static let title = "Text field"

    @State private var email = ""
    @State private var password = ""
    @State private var name = ""
    @State private var notes = ""
    @State private var amount = ""
    @FocusState private var isAmountFocused: Bool

    private var emailError: String? {
        email.isEmpty || email.contains("@") ? nil : "Enter a valid email address."
    }

    var body: some View {
        MonarchTextField(
            "Email",
            text: $email,
            prompt: "name@example.com",
            helper: "Type text without an @ to see the error state.",
            error: emailError
        )
        .textContentType(.emailAddress)
        .keyboardType(.emailAddress)
        .textInputAutocapitalization(.never)
        .autocorrectionDisabled()

        MonarchTextField("Password", text: $password, helper: "At least 12 characters.", isSecure: true)
            .textContentType(.newPassword)

        MonarchTextField("Disabled", text: .constant("Read only"))
            .disabled(true)

        CatalogueCaption("Required, multiline and read-only")
        MonarchTextField("Name", text: $name, error: name.isEmpty ? "Enter your name." : nil, isRequired: true)
            .textContentType(.name)
        MonarchTextField("Notes", text: $notes, prompt: "Grows with the text", axis: .vertical, lineLimit: 2...6)
        MonarchTextField("Account ID", text: .constant("acct_1042"), helper: "Select to copy.", isReadOnly: true)

        CatalogueCaption("Accessories and a focus binding")
        MonarchTextField("Amount", text: $amount, focus: $isAmountFocused) {
            Text(verbatim: "$").accessibilityHidden(true)
        } trailing: {
            MonarchButton("Clear amount", systemImage: "xmark.circle.fill", variant: .ghost, isIconOnly: true) {
                amount = ""
            }
        }
        .keyboardType(.decimalPad)
        MonarchButton("Focus the amount", size: .sm) { isAmountFocused = true }

        CatalogueCaption("In a Form row, the label is hidden")
        MonarchTextField("Search", text: $name, prompt: "Search", labelVisibility: .hidden)
    }
}
