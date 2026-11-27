import MonarchUI
import SwiftUI

struct MonarchTermsAcceptanceSection: CataloguePage {
    static let title = "Terms acceptance"

    private static func url(_ string: String) -> URL { URL(string: string) ?? URL(filePath: "/") }

    private static let documents = [
        MonarchTermsDocument("Terms of Service", url: url("https://example.com/terms"), version: "2026-11-01"),
        MonarchTermsDocument("Privacy Policy", url: url("https://example.com/privacy"), version: "2026-11-01"),
    ]

    @State private var accepted = false
    @State private var triedToContinue = false
    @State private var status = "Not accepted yet."
    @State private var optional = false

    var body: some View {
        CatalogueCaption("Sign-up: press Create account without ticking")
        MonarchTermsAcceptance(
            isAccepted: $accepted,
            documents: Self.documents,
            showsRequiredError: triedToContinue
        ) { isAccepted, details in
            status =
                isAccepted
                ? "Accepted \(details.versions.count) versions at \(details.date.formatted(date: .omitted, time: .standard))."
                : "Not accepted."
        }
        MonarchButton("Create account", variant: .primary) {
            triedToContinue = true
            if accepted { status = "Account created." }
        }
        Text(status).monarchTextStyle(.caption)

        CatalogueCaption("Optional, disabled")
        MonarchTermsAcceptance(isAccepted: $optional, documents: Self.documents, isRequired: false)
            .disabled(true)
    }
}
