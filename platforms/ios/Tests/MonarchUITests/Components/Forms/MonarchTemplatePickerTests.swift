import Foundation
import MonarchTokens
import SwiftUI
import Testing

@testable import MonarchUI

enum MonarchTemplatePickerFixtures: ComponentFixtures {
    static let id = "template-picker"
    static let templates = [
        MonarchTemplatePickerItem(
            id: "notes",
            name: "Meeting notes",
            description: "Agenda, decisions and actions.",
            category: "meetings",
            systemImage: "person.3",
            keywords: ["minutes"]
        ),
        MonarchTemplatePickerItem(
            id: "brief",
            name: "Project brief",
            description: "Goals, scope, owners and the dates everyone agreed on before work starts.",
            category: "planning",
            badge: "New",
            systemImage: "doc.richtext"
        ),
        MonarchTemplatePickerItem(
            id: "retro",
            name: "Retrospective",
            category: "meetings",
            image: Image(systemName: "arrow.triangle.2.circlepath")
        ),
        MonarchTemplatePickerItem(id: "okr", name: "Quarterly goals", category: "planning", isDisabled: true),
    ]
    static let categories = [
        MonarchTemplatePickerCategory(id: "meetings", label: "Meetings"),
        MonarchTemplatePickerCategory(id: "planning", label: "Planning"),
    ]

    static let fixtures = [
        Fixture("filters, blank and a selection") {
            MonarchTemplatePicker(
                "Templates",
                selection: .constant("notes"),
                templates: templates,
                categories: categories,
                blank: .blank()
            )
        },
        Fixture("no selection, no filters") {
            MonarchTemplatePicker("Templates", selection: .constant(nil), templates: templates)
        },
        Fixture("blank selected, one category") {
            MonarchTemplatePicker(
                "Templates",
                selection: .constant("blank"),
                templates: templates,
                categories: categories,
                blank: .blank(description: "Start from an empty page."),
                initialCategory: "planning"
            )
        },
        Fixture("search with no results") {
            MonarchTemplatePicker(
                "Templates",
                selection: .constant(nil),
                templates: templates,
                categories: categories,
                query: "invoice",
                blank: .blank()
            )
        },
        Fixture("disabled") {
            MonarchTemplatePicker("Templates", selection: .constant("brief"), templates: templates).disabled(true)
        },
    ]
}

@Suite("MonarchTemplatePicker")
struct MonarchTemplatePickerTests {
    private let templates = MonarchTemplatePickerFixtures.templates

    @Test("Search matches every word in the name, description or keywords, ignoring case and accents")
    func matches() {
        let notes = templates[0]
        #expect(MonarchTemplatePickerTokens.matches(notes, query: ""))
        #expect(MonarchTemplatePickerTokens.matches(notes, query: "  "))
        #expect(MonarchTemplatePickerTokens.matches(notes, query: "MEETING"))
        #expect(MonarchTemplatePickerTokens.matches(notes, query: "actions meeting"))
        #expect(MonarchTemplatePickerTokens.matches(notes, query: "minutes"))
        #expect(MonarchTemplatePickerTokens.matches(notes, query: "décisions"))
        #expect(!MonarchTemplatePickerTokens.matches(notes, query: "meeting invoice"))
    }

    @Test("Filtering by category and query keeps the order")
    func filtered() {
        #expect(
            MonarchTemplatePickerTokens.filtered(templates, category: nil, query: "").map(\.id) == [
                "notes", "brief", "retro", "okr",
            ]
        )
        #expect(
            MonarchTemplatePickerTokens.filtered(templates, category: "meetings", query: "").map(\.id) == [
                "notes", "retro",
            ]
        )
        #expect(
            MonarchTemplatePickerTokens.filtered(templates, category: "planning", query: "goals").map(\.id) == [
                "brief", "okr",
            ]
        )
        #expect(MonarchTemplatePickerTokens.filtered(templates, category: "meetings", query: "goals").isEmpty)
    }

    @Test("Filtering is on with a category or a non-blank query")
    func isFiltering() {
        #expect(!MonarchTemplatePickerTokens.isFiltering(category: nil, query: " "))
        #expect(MonarchTemplatePickerTokens.isFiltering(category: "planning", query: ""))
        #expect(MonarchTemplatePickerTokens.isFiltering(category: nil, query: "x"))
    }

    @Test("The blank card has the built-in name, a plus and no category")
    func blank() {
        let blank = MonarchTemplatePickerItem.blank()
        #expect(blank.id == "blank")
        #expect(blank.name == "Blank")
        #expect(blank.category == nil)
        #expect(blank.systemImage == MonarchTemplatePickerTokens.blankSymbol)
        #expect(MonarchTemplatePickerItem.blank(name: "Empty", id: "none").name == "Empty")
    }

    @Test("VoiceOver reads the badge, then the description")
    func value() {
        #expect(MonarchTemplatePickerTokens.accessibilityValue(badge: "New", description: "Goals.") == "New, Goals.")
        #expect(MonarchTemplatePickerTokens.accessibilityValue(badge: nil, description: "Goals.") == "Goals.")
        #expect(MonarchTemplatePickerTokens.accessibilityValue(badge: "", description: nil) == "")
    }

    @Test("Cards look like radio cards: accent and heavier when selected, muted when disabled")
    func appearance() {
        let selected = MonarchTemplatePickerTokens.appearance(isSelected: true, isPressed: false, isEnabled: true)
        let rest = MonarchTemplatePickerTokens.appearance(isSelected: false, isPressed: false, isEnabled: true)
        let pressed = MonarchTemplatePickerTokens.appearance(isSelected: false, isPressed: true, isEnabled: true)
        let disabled = MonarchTemplatePickerTokens.appearance(isSelected: true, isPressed: false, isEnabled: false)
        #expect(selected.border == \MonarchColorTokens.accentStrong)
        #expect(selected.mark == \MonarchColorTokens.accentStrong)
        #expect(rest.background == \MonarchColorTokens.bgSurface)
        #expect(pressed.background != rest.background)
        #expect(disabled.title == \MonarchColorTokens.textDisabled)
        #expect(
            MonarchTemplatePickerTokens.borderWidth(isSelected: true)
                > MonarchTemplatePickerTokens.borderWidth(isSelected: false)
        )
    }

    @Test("Built-in strings resolve")
    func strings() {
        #expect(MonarchTemplatePickerStrings.blank == "Blank")
        #expect(MonarchTemplatePickerStrings.categories == "Categories")
        #expect(MonarchTemplatePickerStrings.allCategories == "All")
        #expect(MonarchTemplatePickerStrings.results(1) == "1 template")
        #expect(MonarchTemplatePickerStrings.results(3) == "3 templates")
        #expect(MonarchTemplatePickerStrings.results(0) == "0 templates")
        #expect(MonarchTemplatePickerStrings.noResults("tax") == "No templates match “tax”.")
    }
}
