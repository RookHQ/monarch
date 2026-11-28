import MonarchUI
import SwiftUI

struct MonarchAiAutofillFieldSection: CataloguePage {
    static let title = "AI autofill field"

    @State private var company = "Rook Labs"
    @State private var companyState: MonarchAiAutofillState = .suggested
    @State private var total = "1 240.00"
    @State private var totalState: MonarchAiAutofillState = .suggested

    var body: some View {
        CatalogueCaption("Accept, clear or type to edit")
        MonarchAiAutofillField(
            state: companyState,
            suggestion: "Rook Labs",
            fieldName: "Company",
            source: "invoice.pdf",
            confidence: 0.92,
            model: "Rook 2",
            onAccept: { companyState = .accepted },
            onClear: {
                company = ""
                companyState = .cleared
            },
            control: { MonarchTextField("Company", text: $company) }
        )
        .onChange(of: company) {
            if companyState == .suggested || companyState == .accepted { companyState = .edited }
        }

        CatalogueCaption("Low confidence")
        MonarchAiAutofillField(
            state: totalState,
            suggestion: "1 240.00",
            fieldName: "Total",
            source: "receipt-0412.jpg",
            confidence: 0.41,
            confidenceExplanation: "The total is partly hidden by a fold.",
            onAccept: { totalState = .accepted },
            onClear: {
                total = ""
                totalState = .cleared
            },
            control: { MonarchTextField("Total", text: $total) }
        )
        .onChange(of: total) {
            if totalState == .suggested || totalState == .accepted { totalState = .edited }
        }

        MonarchButton("Reset", variant: .secondary, size: .sm) {
            company = "Rook Labs"
            total = "1 240.00"
            companyState = .suggested
            totalState = .suggested
        }
    }
}
