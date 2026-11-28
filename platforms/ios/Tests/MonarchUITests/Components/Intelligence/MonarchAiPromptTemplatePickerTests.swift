import SwiftUI
import Testing

@testable import MonarchUI

private let reply = MonarchAiPromptTemplate(
    id: "reply",
    title: "Reply to a customer",
    body: "Write a friendly reply to {{customer_name}} about {{ topic }}. Mention {{customer_name}} once.",
    description: "A short, warm reply to a support message.",
    category: "support",
    keywords: ["email"]
)

private let brief = MonarchAiPromptTemplate(
    id: "brief",
    title: "Project brief",
    body: "Write a brief for {{project}}.{{notes}}",
    category: "writing",
    variables: [
        MonarchAiPromptTemplateVariable("project", label: "Project", isRequired: true, placeholder: "Northwind"),
        MonarchAiPromptTemplateVariable(
            "notes",
            label: "Notes",
            helper: "Anything the brief must cover.",
            isMultiline: true,
            defaultValue: " Keep it short."
        ),
    ]
)

private let summary = MonarchAiPromptTemplate(id: "summary", title: "Summarise this page", body: "Summarise this page.")

enum MonarchAiPromptTemplatePickerFixtures: ComponentFixtures {
    static let id = "ai-prompt-template-picker"
    static let fixtures = [
        Fixture("gallery with categories and cancel") {
            MonarchAiPromptTemplatePicker(
                [reply, brief, summary],
                categories: [
                    MonarchAiPromptTemplateCategory(id: "support", label: "Support"),
                    MonarchAiPromptTemplateCategory(id: "writing", label: "Writing"),
                ],
                onCancel: {}
            ) { _, _, _ in }
            .frame(height: 560)
        },
        Fixture("no templates") {
            MonarchAiPromptTemplatePicker([]) { _, _, _ in }
                .frame(height: 400)
        },
        Fixture("form: required, optional multi-line, preview") {
            NavigationStack {
                MonarchAiPromptTemplateForm(template: brief, labels: MonarchAiPromptTemplatePickerLabels()) { _, _, _ in
                }
            }
            .frame(height: 720)
        },
        Fixture("form: blanks read from the body; no blanks") {
            VStack {
                MonarchAiPromptTemplateForm(template: reply, labels: MonarchAiPromptTemplatePickerLabels()) { _, _, _ in
                }
                MonarchAiPromptTemplateForm(template: summary, labels: MonarchAiPromptTemplatePickerLabels()) {
                    _,
                    _,
                    _ in
                }
            }
            .frame(height: 900)
        },
    ]
}

@Suite("MonarchAiPromptTemplatePicker")
struct MonarchAiPromptTemplatePickerTests {
    typealias Tokens = MonarchAiPromptTemplatePickerTokens

    @Test("Blanks are read once each, in order, with spaces inside the braces")
    func variableNames() {
        #expect(MonarchAiPromptTemplate.variableNames(in: reply.body) == ["customer_name", "topic"])
        #expect(MonarchAiPromptTemplate.variableNames(in: "No blanks {here}").isEmpty)
    }

    @Test("Filling replaces every use, keeps blanks without a value, and removes empty ones")
    func fill() {
        let filled = MonarchAiPromptTemplate.fill(reply.body, values: ["customer_name": "Ada"])
        #expect(filled == "Write a friendly reply to Ada about {{ topic }}. Mention Ada once.")
        #expect(MonarchAiPromptTemplate.fill("A{{x}}B", values: ["x": ""]) == "AB")
    }

    @Test("Without variables, blanks are required and labelled from their names")
    func resolvedVariables() {
        let variables = reply.resolvedVariables
        #expect(variables.map(\.label) == ["Customer name", "Topic"])
        #expect(variables.allSatisfy { $0.isRequired })
        #expect(brief.resolvedVariables.map(\.name) == ["project", "notes"])
        #expect(Tokens.humanize("customerName") == "Customer name")
        #expect(Tokens.humanize("due.date") == "Due date")
        #expect(Tokens.humanize("") == "")
    }

    @Test("Search matches title, description, body and keywords; categories filter")
    func search() {
        #expect(Tokens.matches(reply, query: "EMAIL"))
        #expect(Tokens.matches(reply, query: "warm"))
        #expect(Tokens.matches(reply, query: "friendly"))
        #expect(!Tokens.matches(reply, query: "invoice"))
        let all = [reply, brief, summary]
        #expect(Tokens.filtered(all, query: "", category: nil).count == 3)
        #expect(Tokens.filtered(all, query: "", category: "writing").map(\.id) == ["brief"])
        #expect(Tokens.filtered(all, query: "write", category: "support").map(\.id) == ["reply"])
    }

    @Test("Required blanks must be filled; values are trimmed when used")
    func validation() {
        let variables = brief.resolvedVariables
        let initial = Tokens.initialValues(variables)
        #expect(initial == ["project": "", "notes": " Keep it short."])
        #expect(Tokens.missing(variables, values: initial) == ["project"])
        #expect(Tokens.missing(variables, values: ["project": "  "]) == ["project"])
        #expect(Tokens.missing(variables, values: ["project": "Atlas"]).isEmpty)
        let submitted = Tokens.submitted(variables, values: ["project": " Atlas "])
        #expect(submitted == ["project": "Atlas", "notes": ""])
        #expect(MonarchAiPromptTemplate.fill(brief.body, values: submitted) == "Write a brief for Atlas.")
    }

    @Test("The preview shows values, and labels in brackets for empty blanks")
    func preview() {
        let text = Tokens.preview(reply.body, variables: reply.resolvedVariables, values: ["topic": "refunds"])
        #expect(text == "Write a friendly reply to [Customer name] about refunds. Mention [Customer name] once.")
    }

    @Test("Spoken labels mark optional fields and read rows with their description")
    func accessibility() {
        let variables = brief.resolvedVariables
        #expect(Tokens.fieldLabel(variables[0], optional: "Optional") == "Project")
        #expect(Tokens.fieldLabel(variables[1], optional: "Optional") == "Notes, Optional")
        #expect(Tokens.rowLabel(reply) == "Reply to a customer, A short, warm reply to a support message.")
        #expect(Tokens.rowLabel(summary) == "Summarise this page")
    }

    @Test("Own labels replace the built-in strings")
    func overrides() {
        let labels = MonarchAiPromptTemplatePickerLabels(
            title: "T",
            use: "Insert",
            missing: { "Need " + $0 },
            noResults: { "None for " + $0 }
        )
        #expect(labels.title == "T")
        #expect(labels.use == "Insert")
        #expect(labels.missing("Topic") == "Need Topic")
        #expect(labels.noResults("x") == "None for x")
    }

    @Test("The built-in strings come from the strings table")
    func strings() {
        typealias Strings = MonarchAiPromptTemplatePickerStrings
        #expect(Strings.title == "Prompt templates")
        #expect(Strings.search == "Search templates")
        #expect(Strings.use == "Use template")
        #expect(Strings.missing("Customer name") == "Enter customer name.")
        #expect(Strings.noResults("tax") == "No templates match “tax”")
        for value in [
            Strings.category, Strings.allCategories, Strings.template, Strings.preview, Strings.cancel,
            Strings.optional, Strings.empty,
        ] {
            #expect(!value.hasPrefix("monarch."))
        }
    }
}
