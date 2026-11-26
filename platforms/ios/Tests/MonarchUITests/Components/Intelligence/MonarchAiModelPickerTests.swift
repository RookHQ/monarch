import Foundation
import MonarchTokens
import SwiftUI
import Testing

@testable import MonarchUI

enum MonarchAiModelPickerFixtures: ComponentFixtures {
    static let id = "ai-model-picker"
    static let models = [
        MonarchAiModelOption(
            id: "rook-large",
            provider: "Rook",
            name: "Rook Large",
            description: "Best for long documents",
            capabilities: [.vision, .tools, .reasoning, .web],
            contextTokens: 200_000,
            speed: .balanced,
            costTier: 3
        ),
        MonarchAiModelOption(id: "rook-fast", provider: "Rook", name: "Rook Fast", speed: .fast, costTier: 1),
        MonarchAiModelOption(
            id: "byok-1",
            provider: "Workspace provider",
            name: "Your hosted model",
            capabilities: [.tools],
            contextTokens: 128_000,
            requiresKey: true,
            isKeyConfigured: true
        ),
        MonarchAiModelOption(
            id: "byok-2",
            provider: "Workspace provider",
            name: "Another hosted model",
            contextTokens: 1_000_000,
            requiresKey: true
        ),
        MonarchAiModelOption(id: "legacy", provider: "Rook", name: "Rook Legacy", isDisabled: true),
    ]

    static let fixtures = [
        Fixture("button, selected, both sizes") {
            VStack(alignment: .leading) {
                MonarchAiModelPicker(selection: .constant("rook-large"), models: models)
                MonarchAiModelPicker(selection: .constant("rook-fast"), models: models, size: .sm)
            }
        },
        Fixture("button, placeholder, disabled") {
            MonarchAiModelPicker(selection: .constant(nil), models: models).disabled(true)
        },
        Fixture("list, keys can be added") {
            MonarchAiModelPickerList(
                models: models,
                selection: "rook-large",
                canConfigureKey: true,
                labels: MonarchAiModelPickerLabels(),
                onChoose: { _ in },
                onDone: {}
            )
        },
        Fixture("list, keys cannot be added") {
            MonarchAiModelPickerList(
                models: models,
                selection: nil,
                canConfigureKey: false,
                labels: MonarchAiModelPickerLabels(),
                onChoose: { _ in },
                onDone: {}
            )
        },
    ]
}

@Suite("MonarchAiModelPicker")
struct MonarchAiModelPickerTests {
    private let labels = MonarchAiModelPickerLabels()
    private let locale = Locale(identifier: "en_US")
    private let models = MonarchAiModelPickerFixtures.models

    @Test("A missing key leads to adding it only when the product can; disabled models are blocked")
    func states() {
        #expect(MonarchAiModelPickerTokens.state(models[0], canConfigureKey: false) == .available)
        #expect(MonarchAiModelPickerTokens.state(models[2], canConfigureKey: false) == .available)
        #expect(MonarchAiModelPickerTokens.state(models[3], canConfigureKey: true) == .needsKey)
        #expect(MonarchAiModelPickerTokens.state(models[3], canConfigureKey: false) == .blocked)
        #expect(MonarchAiModelPickerTokens.state(models[4], canConfigureKey: true) == .blocked)
    }

    @Test("Models are grouped by provider in first-seen order")
    func groups() {
        let groups = MonarchAiModelPickerTokens.groups(models)
        #expect(groups.map(\.provider) == ["Rook", "Workspace provider"])
        #expect(groups[0].models.map(\.id) == ["rook-large", "rook-fast", "legacy"])
        #expect(groups[1].models.map(\.id) == ["byok-1", "byok-2"])
        #expect(MonarchAiModelPickerTokens.groups([]).isEmpty)
    }

    @Test("Context sizes are compact and the details line joins context, speed and cost")
    func meta() {
        #expect(MonarchAiModelPickerTokens.compactTokens(200_000, locale: locale) == "200K")
        #expect(MonarchAiModelPickerTokens.compactTokens(1_000_000, locale: locale) == "1M")
        #expect(MonarchAiModelPickerTokens.compactTokens(32_768, locale: locale) == "32.8K")
        #expect(
            MonarchAiModelPickerTokens.metaText(models[0], locale: locale, labels: labels)
                == "200K context · Balanced · $$$"
        )
        #expect(MonarchAiModelPickerTokens.metaText(models[1], locale: locale, labels: labels) == "Fast · $")
        #expect(MonarchAiModelPickerTokens.metaText(models[4], locale: locale, labels: labels) == nil)
        #expect(MonarchAiModelPickerTokens.clampedCost(7) == 3)
        #expect(MonarchAiModelPickerTokens.clampedCost(0) == 1)
        #expect(MonarchAiModelPickerTokens.clampedCost(nil) == nil)
    }

    @Test("Rows read the name and key, then the details with the cost in words")
    func rowAccessibility() {
        #expect(MonarchAiModelPickerTokens.rowLabel(models[0], labels: labels) == "Rook Large")
        #expect(MonarchAiModelPickerTokens.rowLabel(models[2], labels: labels) == "Your hosted model, Your key")
        #expect(MonarchAiModelPickerTokens.rowLabel(models[3], labels: labels) == "Another hosted model, Key needed")
        #expect(
            MonarchAiModelPickerTokens.rowValue(models[0], state: .available, locale: locale, labels: labels)
                == "Best for long documents, Vision, Tools, Reasoning, Web, 200K context, Balanced, Cost: high"
        )
        #expect(
            MonarchAiModelPickerTokens.rowValue(models[4], state: .blocked, locale: locale, labels: labels)
                == "Unavailable"
        )
        #expect(MonarchAiModelPickerTokens.rowHint(state: .needsKey, labels: labels) == "Select to add an API key.")
        #expect(MonarchAiModelPickerTokens.rowHint(state: .available, labels: labels) == nil)
    }

    @Test("Blocked rows use the disabled text colour")
    func colours() {
        #expect(MonarchAiModelPickerTokens.textRoles(state: .available).name == \MonarchColorTokens.textDefault)
        #expect(MonarchAiModelPickerTokens.textRoles(state: .needsKey).detail == \MonarchColorTokens.textMuted)
        #expect(MonarchAiModelPickerTokens.textRoles(state: .blocked).name == \MonarchColorTokens.textDisabled)
        #expect(MonarchAiModelPickerTokens.textRoles(state: .blocked).detail == \MonarchColorTokens.textDisabled)
    }

    @Test("The button reads the selected model, or the placeholder")
    func trigger() {
        #expect(MonarchAiModelPickerTokens.triggerValue(models[0], labels: labels) == "Rook Large, Rook")
        #expect(MonarchAiModelPickerTokens.triggerValue(nil, labels: labels) == "Choose a model")
        #expect(
            MonarchAiModelPickerTokens.triggerValue(nil, labels: MonarchAiModelPickerLabels(placeholder: "Modelo"))
                == "Modelo"
        )
    }

    @Test("Every built-in string can be replaced")
    func overrides() {
        let custom = MonarchAiModelPickerLabels(
            capability: { $0.rawValue.uppercased() },
            context: { "ctx \($0)" },
            speed: { _ in "Speedy" },
            cost: { "tier \($0)" },
            ownKey: "Own",
            unavailable: "No"
        )
        #expect(
            MonarchAiModelPickerTokens.rowValue(models[0], state: .blocked, locale: locale, labels: custom)
                == "Best for long documents, VISION, TOOLS, REASONING, WEB, ctx 200K, Speedy, tier 3, No"
        )
        #expect(MonarchAiModelPickerTokens.keyText(models[2], labels: custom) == "Own")
    }

    @Test("The built-in strings come from the component's strings table")
    func strings() {
        #expect(MonarchAiModelPickerStrings.label == "Model")
        #expect(MonarchAiModelPickerStrings.placeholder == "Choose a model")
        #expect(MonarchAiModelPickerStrings.done == "Done")
        #expect(MonarchAiModelPickerStrings.speed(.slow) == "Slower")
        #expect(MonarchAiModelPickerStrings.cost(1) == "Cost: low")
        #expect(MonarchAiModelPickerStrings.cost(2) == "Cost: medium")
        #expect(MonarchAiModelPickerStrings.context("8K") == "8K context")
    }
}
