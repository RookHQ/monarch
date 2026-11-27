import Foundation
import MonarchTokens
import SwiftUI

/// Component tokens and pure logic for ``MonarchAiPromptTemplatePicker``.
enum MonarchAiPromptTemplatePickerTokens {
    /// A `{{name}}` blank; spaces inside the braces are allowed.
    static var blank: Regex<(Substring, Substring)> { /\{\{\s*([\w.\-]+)\s*\}\}/ }

    /// The gap between the sections of the detail page.
    static let sectionSpacing: CGFloat = MonarchTokens.Space.space5
    /// The gap between a heading and its content, and between fields.
    static let spacing: CGFloat = MonarchTokens.Space.space2
    /// Padding of the detail page.
    static let pagePadding: CGFloat = MonarchTokens.Space.space4
    /// Padding inside the body and preview blocks.
    static let blockPadding: CGFloat = MonarchTokens.Space.space3
    /// The corner radius of the body and preview blocks.
    static let blockRadius: CGFloat = MonarchTokens.Radius.md
    /// The gap between a row's title and description.
    static let rowSpacing: CGFloat = MonarchTokens.Space.space0_5
    /// The least number of lines of a multi-line field.
    static let multilineMinLines = 3
    /// The most lines a multi-line field grows to before it scrolls.
    static let multilineMaxLines = 8
    /// The height of the picker in the preview, which sits in a scroll view.
    static let previewHeight: CGFloat = MonarchTokens.Space.space24 * 5
    /// The SF Symbol of a template row.
    static let rowSymbol = "text.bubble"

    /// Makes a label from a blank's name: `customer_name` and
    /// `customerName` become "Customer name".
    static func humanize(_ name: String) -> String {
        var words = ""
        var previous: Character?
        for character in name {
            if character == "_" || character == "." || character == "-" {
                words.append(" ")
            } else {
                if character.isUppercase, let previous, previous.isLowercase { words.append(" ") }
                words.append(character)
            }
            previous = character
        }
        let trimmed = words.split(separator: " ").joined(separator: " ").lowercased()
        guard let first = trimmed.first else { return name }
        return first.uppercased() + trimmed.dropFirst()
    }

    /// Whether a template matches a search: the title, the description, the
    /// body or a keyword contains the trimmed query, ignoring case and
    /// diacritics. An empty query matches everything.
    static func matches(_ template: MonarchAiPromptTemplate, query: String) -> Bool {
        let query = query.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !query.isEmpty else { return true }
        let options: String.CompareOptions = [.caseInsensitive, .diacriticInsensitive]
        let fields = [template.title, template.description ?? "", template.body] + template.keywords
        return fields.contains { $0.range(of: query, options: options) != nil }
    }

    /// The templates in a category (all when `nil`) that match the query.
    static func filtered(
        _ templates: [MonarchAiPromptTemplate],
        query: String,
        category: String?
    ) -> [MonarchAiPromptTemplate] {
        templates.filter { template in
            (category == nil || template.category == category) && matches(template, query: query)
        }
    }

    /// The values a form starts with: each blank's default, or empty.
    static func initialValues(_ variables: [MonarchAiPromptTemplateVariable]) -> [String: String] {
        Dictionary(uniqueKeysWithValues: variables.map { ($0.name, $0.defaultValue ?? "") })
    }

    /// The names of required blanks that are still empty, in form order.
    static func missing(_ variables: [MonarchAiPromptTemplateVariable], values: [String: String]) -> [String] {
        variables.filter { variable in
            variable.isRequired
                && (values[variable.name] ?? "").trimmingCharacters(in: .whitespacesAndNewlines).isEmpty
        }
        .map(\.name)
    }

    /// The values handed to the app: each trimmed, empty optional blanks
    /// included as empty strings so they are removed from the prompt.
    static func submitted(_ variables: [MonarchAiPromptTemplateVariable], values: [String: String]) -> [String: String]
    {
        Dictionary(
            uniqueKeysWithValues: variables.map {
                ($0.name, (values[$0.name] ?? "").trimmingCharacters(in: .whitespacesAndNewlines))
            }
        )
    }

    /// The live preview: filled blanks show their value, empty ones show
    /// their label in square brackets.
    static func preview(
        _ body: String,
        variables: [MonarchAiPromptTemplateVariable],
        values: [String: String]
    ) -> String {
        let labels = Dictionary(variables.map { ($0.name, $0.label) }, uniquingKeysWith: { first, _ in first })
        return body.replacing(blank) { match in
            let name = String(match.output.1)
            let value = (values[name] ?? "").trimmingCharacters(in: .whitespacesAndNewlines)
            return value.isEmpty ? "[" + (labels[name] ?? name) + "]" : value
        }
    }

    /// What VoiceOver reads for a field's label: the label, and "Optional"
    /// when it may be left empty.
    static func fieldLabel(_ variable: MonarchAiPromptTemplateVariable, optional: String) -> String {
        variable.isRequired ? variable.label : variable.label + ", " + optional
    }

    /// What VoiceOver reads for a template row: the title, then the description.
    static func rowLabel(_ template: MonarchAiPromptTemplate) -> String {
        guard let description = template.description, !description.isEmpty else { return template.title }
        return template.title + ", " + description
    }
}
