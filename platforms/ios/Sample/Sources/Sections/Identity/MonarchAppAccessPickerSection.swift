@_spi(Experimental) import MonarchUI
import SwiftUI

struct MonarchAppAccessPickerSection: CataloguePage {
    static let title = "App access picker"

    private static let apps = [
        MonarchAppAccessOption(id: "mail", name: "Mail", description: "Email and calendars for the team."),
        MonarchAppAccessOption(id: "docs", name: "Docs", description: "Shared documents.", badge: "Beta"),
        MonarchAppAccessOption(id: "sheets", name: "Sheets", description: "Spreadsheets and charts."),
        MonarchAppAccessOption(id: "reports", name: "Reports", badge: "Business plan"),
        MonarchAppAccessOption(id: "home", name: "Home", description: "Everyone has it.", isDisabled: true),
    ]

    @State private var granted: Set<String> = ["mail", "home"]
    @State private var isLoading = false
    @State private var isSaving = false

    var body: some View {
        Toggle("Loading", isOn: $isLoading)
        Toggle("Saving (disables the picker)", isOn: $isSaving)
        MonarchAppAccessPicker(
            Self.apps,
            selection: $granted,
            helper: "Changes apply the next time they sign in.",
            isLoading: isLoading
        )
        .disabled(isSaving)

        CatalogueCaption("Summary, as in a member list")
        MonarchAppAccessSummary(Self.apps, granted: granted)

        CatalogueCaption("No apps")
        MonarchAppAccessPicker([], selection: .constant([]))
    }
}
