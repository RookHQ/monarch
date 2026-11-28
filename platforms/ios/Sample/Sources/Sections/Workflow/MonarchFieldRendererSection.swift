import MonarchUI
import SwiftUI
import UniformTypeIdentifiers

struct MonarchFieldRendererSection: CataloguePage {
    static let title = "Field renderer"

    private static let form = [
        MonarchFormFieldDefinition(
            id: "about",
            type: .section,
            label: "About you",
            description: "We only use this to reply."
        ),
        MonarchFormFieldDefinition(id: "name", type: .shortText, label: "Full name", isRequired: true),
        MonarchFormFieldDefinition(id: "email", type: .email, label: "Email", isRequired: true),
        MonarchFormFieldDefinition(
            id: "where",
            type: .radio,
            label: "Where will you join from?",
            options: [
                MonarchFormFieldOption(value: "remote", label: "Remote"),
                MonarchFormFieldOption(value: "office", label: "Office"),
            ]
        ),
        MonarchFormFieldDefinition(
            id: "office",
            type: .select,
            label: "Which office?",
            options: [
                MonarchFormFieldOption(value: "london", label: "London"),
                MonarchFormFieldOption(value: "lisbon", label: "Lisbon"),
            ],
            conditions: [MonarchCondition(id: "c1", field: "where", operator: .equals, value: .text("office"))]
        ),
        MonarchFormFieldDefinition(id: "relocate", type: .yesNo, label: "Can you relocate?"),
        MonarchFormFieldDefinition(id: "start", type: .date, label: "Earliest start date"),
        MonarchFormFieldDefinition(id: "call", type: .time, label: "Best time to call"),
        MonarchFormFieldDefinition(id: "rating", type: .rating, label: "How was the process?"),
        MonarchFormFieldDefinition(id: "cv", type: .file, label: "CV", allowedContentTypes: [.pdf]),
        MonarchFormFieldDefinition(
            id: "consent",
            type: .consent,
            label: "Consent",
            isRequired: true,
            consentText: "I agree to be contacted about this application."
        ),
    ]

    @State private var answers: [String: MonarchFormFieldValue] = ["name": .text("Alex Morgan")]
    @State private var submitted = false

    var body: some View {
        CatalogueCaption("A form from data, checked on submit")
        ForEach(Self.form.filter { $0.isVisible(in: answers) }) { question in
            MonarchFieldRenderer(
                question,
                value: $answers[question.id],
                problem: submitted ? question.problem(for: answers[question.id]) : nil,
                confidence: question.id == "name" ? 0.92 : nil
            )
        }
        MonarchButton("Submit", variant: .primary) { submitted = true }

        CatalogueCaption("Filled in automatically, needs a check")
        MonarchFieldRenderer(
            MonarchFormFieldDefinition(id: "phone", type: .phone, label: "Phone"),
            value: .constant(.text("+44 20 7946 0000")),
            confidence: 0.4
        )

        CatalogueCaption("Read-only")
        MonarchFieldRenderer(
            MonarchFormFieldDefinition(id: "verified", type: .email, label: "Verified email", isReadOnly: true),
            value: .constant(.text("alex@example.com"))
        )
    }
}
