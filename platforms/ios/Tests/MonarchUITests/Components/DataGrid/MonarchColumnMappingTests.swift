import Foundation
import MonarchTokens
import SwiftUI
import Testing

@testable import MonarchUI

private let mappingHeight = MonarchTokens.Space.space24 * 4

private let mappingSources = [
    MonarchColumnMappingSource("E-mail Address", samples: ["ada@example.com", "", "bea@example.com"]),
    MonarchColumnMappingSource("Full name", samples: ["Ada Moss", "Bea Lund", "Cal Ortiz", "Dee Park"]),
    MonarchColumnMappingSource("Phone number"),
    MonarchColumnMappingSource("Notes", samples: ["Met at the fair"]),
]

private let mappingFields = [
    MonarchColumnMappingField(id: "email", label: "Email", isRequired: true, aliases: ["E-mail", "Mail"]),
    MonarchColumnMappingField(id: "name", label: "Name", isRequired: true, aliases: ["Full name"]),
    MonarchColumnMappingField(id: "phone", label: "Mobile phone", description: "International format, +44 7700 900123"),
    MonarchColumnMappingField(id: "company", label: "Company", isRequired: true),
]

enum MonarchColumnMappingFixtures: ComponentFixtures {
    static let id = "column-mapping"
    static let fixtures = [
        Fixture("suggested, with help and a missing required field") {
            MonarchColumnMapping(
                mappingSources,
                mapping: .constant(["E-mail Address": "email", "Full name": "name", "Phone number": "phone"]),
                fields: mappingFields
            )
            .frame(height: mappingHeight)
        },
        Fixture("a field chosen twice") {
            MonarchColumnMapping(
                mappingSources,
                mapping: .constant(["E-mail Address": "email", "Notes": "email", "Full name": "name"]),
                fields: mappingFields
            )
            .frame(height: mappingHeight)
        },
        Fixture("nothing mapped, issues hidden, no bulk actions") {
            MonarchColumnMapping(
                mappingSources,
                mapping: .constant([:]),
                fields: mappingFields,
                showsIssues: false,
                showsBulkActions: false
            )
            .frame(height: mappingHeight)
        },
        Fixture("blank and long headers, disabled") {
            MonarchColumnMapping(
                [
                    MonarchColumnMappingSource(" "),
                    MonarchColumnMappingSource(
                        "A very long column header from a spreadsheet export that has to wrap",
                        samples: ["A sample value that is also rather long"]
                    ),
                ],
                mapping: .constant([:]),
                fields: mappingFields
            )
            .disabled(true)
            .frame(height: mappingHeight)
        },
    ]
}

@Suite("MonarchColumnMapping")
@MainActor
struct MonarchColumnMappingTests {
    typealias Tokens = MonarchColumnMappingTokens
    let labels = MonarchColumnMappingLabels()

    @Test("Names are normalised to letters and digits, and split into words")
    func names() {
        #expect(Tokens.normalize("E-mail Address") == "emailaddress")
        #expect(Tokens.normalize("Prénom") == "prenom")
        #expect(Tokens.words("firstName") == ["first", "name"])
        #expect(Tokens.words("Phone_number (mobile)") == ["phone", "number", "mobile"])
    }

    @Test("Similarity takes the best of word overlap, containment and edit distance")
    func similarity() {
        #expect(Tokens.similarity("E-mail", "email") == 1)
        #expect(Tokens.similarity("", "email") == 0)
        #expect(abs(Tokens.similarity("Phone number", "Mobile phone") - 0.5) < 0.001)
        #expect(abs(Tokens.similarity("email", "work email") - (0.6 + 0.3 * 5 / 9)) < 0.001)
        #expect(abs(Tokens.similarity("adress", "address") - (1 - 1.0 / 7)) < 0.001)
        #expect(Tokens.distance(Array("kitten".unicodeScalars), Array("sitting".unicodeScalars)) == 3)
        #expect(Tokens.distance([], Array("abc".unicodeScalars)) == 3)
    }

    @Test("Suggestions put exact matches first and give each field to one column")
    func suggest() {
        let suggestions = MonarchColumnMapping.suggest(mappingSources, fields: mappingFields)
        #expect(suggestions["E-mail Address"]?.fieldID == "email")
        #expect(suggestions["E-mail Address"]?.confidence == .similar)
        #expect(suggestions["Full name"]?.fieldID == "name")
        #expect(suggestions["Full name"]?.confidence == .exact)
        #expect(suggestions["Full name"]?.score == 1)
        #expect(suggestions["Notes"] == nil)
        let twice = MonarchColumnMapping.suggest(
            [MonarchColumnMappingSource("Email"), MonarchColumnMappingSource("Mail")],
            fields: mappingFields
        )
        #expect(twice["Email"]?.fieldID == "email")
        #expect(twice["Mail"] == nil)
        #expect(MonarchColumnMapping.suggest(mappingSources, fields: mappingFields, threshold: 1.01).isEmpty)
    }

    @Test("The suggestions become a mapping")
    func mappingFromSuggestions() {
        let mapping = MonarchColumnMapping.mapping(
            from: ["A": MonarchColumnMappingSuggestion(fieldID: "x", confidence: .exact, score: 1)]
        )
        #expect(mapping == ["A": "x"])
    }

    @Test("Issues list missing required fields and fields chosen twice")
    func issues() {
        let result = MonarchColumnMapping.issues(["E-mail Address": "email", "Notes": "email"], fields: mappingFields)
        #expect(result.missing.map(\.id) == ["name", "company"])
        #expect(result.duplicates["email"]?.sorted() == ["E-mail Address", "Notes"])
        let ordered = Tokens.issues(
            ["Notes": "email", "E-mail Address": "email"],
            fields: mappingFields,
            sourceOrder: mappingSources.map(\.name)
        )
        #expect(ordered.duplicates["email"] == ["E-mail Address", "Notes"])
        #expect(MonarchColumnMapping.issues([:], fields: []).missing.isEmpty)
    }

    @Test("Counts only columns mapped to a known field")
    func mappedCount() {
        #expect(
            Tokens.mappedCount(
                ["Notes": "unknown", "Full name": "name"],
                sources: mappingSources,
                fields: mappingFields
            ) == 1
        )
        #expect(Tokens.summary(mapped: 3, total: 4, labels: labels) == "3 of 4 columns mapped")
    }

    @Test("Bulk actions show only when they change something")
    func bulkActions() {
        let suggestions = MonarchColumnMapping.suggest(mappingSources, fields: mappingFields)
        let suggested = MonarchColumnMapping.mapping(from: suggestions)
        #expect(!Tokens.offersSuggestions(suggested, suggestions: suggestions, sources: mappingSources))
        #expect(Tokens.offersSuggestions([:], suggestions: suggestions, sources: mappingSources))
        #expect(!Tokens.offersSuggestions([:], suggestions: [:], sources: mappingSources))
        #expect(Tokens.offersSkipAll(suggested, sources: mappingSources))
        #expect(!Tokens.offersSkipAll([:], sources: mappingSources))
    }

    @Test("Rows show the header or its position, and up to three example values")
    func rowText() {
        #expect(Tokens.displayName(MonarchColumnMappingSource(" "), index: 2) == "#3")
        #expect(Tokens.samplesText(mappingSources[0], labels: labels) == "ada@example.com, bea@example.com")
        #expect(Tokens.samplesText(mappingSources[1], labels: labels) == "Ada Moss, Bea Lund, Cal Ortiz")
        #expect(Tokens.samplesText(mappingSources[2], labels: labels) == "No values")
        #expect(Tokens.optionTitle(mappingFields[0], labels: labels) == "Email (required)")
        #expect(Tokens.optionTitle(mappingFields[2], labels: labels) == "Mobile phone")
    }

    @Test("Errors name the missing fields and the other columns of a duplicate")
    func errors() {
        #expect(Tokens.missingText([], labels: labels) == nil)
        #expect(Tokens.missingText(Array(mappingFields.prefix(2)), labels: labels) == "Map a column to: Email, Name.")
        let duplicates = ["email": ["E-mail Address", "Notes"]]
        #expect(
            Tokens.duplicateText(source: "Notes", field: mappingFields[0], duplicates: duplicates, labels: labels)
                == "Email is also chosen for E-mail Address."
        )
        #expect(
            Tokens.duplicateText(source: "Notes", field: mappingFields[1], duplicates: duplicates, labels: labels)
                == nil
        )
        #expect(Tokens.duplicateText(source: "Notes", field: nil, duplicates: duplicates, labels: labels) == nil)
    }

    @Test("Pickers are named by their column and hint the samples, badge, error and help")
    func accessibility() {
        #expect(Tokens.pickerLabel("E-mail", labels: labels) == "Field for E-mail")
        #expect(
            Tokens.pickerHint(samples: "ada@example.com", badge: "Matched", error: nil, help: "Work address")
                == "ada@example.com. Matched. Work address"
        )
        #expect(Tokens.pickerHint(samples: "No values", badge: nil, error: nil, help: nil) == "No values")
    }

    @Test("Badges are a tone and a word: success for exact, info for close")
    func badges() {
        #expect(Tokens.badgeTone(.exact) == .success)
        #expect(Tokens.badgeTone(.similar) == .info)
        #expect(Tokens.badgeText(.exact, labels: labels) == "Matched")
        #expect(Tokens.badgeText(.similar, labels: labels) == "Suggested")
        #expect(Tokens.errorColor == \.dangerStrong)
    }

    @Test("Overrides replace the built-in strings")
    func overrides() {
        let custom = MonarchColumnMappingLabels(
            fieldFor: { "Feld für \($0)" },
            required: "Pflicht",
            exactMatch: "Erkannt",
            noSamples: "Leer",
            summary: { "\($0)/\($1)" },
            missing: { "Fehlt: \($0)" },
            duplicate: { "\($0) doppelt: \($1)" }
        )
        #expect(Tokens.pickerLabel("A", labels: custom) == "Feld für A")
        #expect(Tokens.optionTitle(mappingFields[0], labels: custom) == "Email (Pflicht)")
        #expect(Tokens.badgeText(.exact, labels: custom) == "Erkannt")
        #expect(Tokens.samplesText(mappingSources[2], labels: custom) == "Leer")
        #expect(Tokens.summary(mapped: 1, total: 2, labels: custom) == "1/2")
        #expect(Tokens.missingText([mappingFields[0]], labels: custom) == "Fehlt: Email")
        #expect(
            Tokens.duplicateText(
                source: "B",
                field: mappingFields[0],
                duplicates: ["email": ["A", "B"]],
                labels: custom
            )
                == "Email doppelt: A"
        )
    }

    @Test("The built-in strings come from the component's strings table")
    func strings() {
        #expect(MonarchColumnMappingStrings.label == "Column mapping")
        #expect(MonarchColumnMappingStrings.skip == "Don’t import")
        #expect(MonarchColumnMappingStrings.useSuggestions == "Use suggestions")
        #expect(MonarchColumnMappingStrings.skipAll == "Don’t import any")
        #expect(MonarchColumnMappingStrings.suggested == "Suggested")
    }
}
