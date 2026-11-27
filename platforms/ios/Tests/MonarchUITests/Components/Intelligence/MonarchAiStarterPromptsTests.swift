import MonarchTokens
import SwiftUI
import Testing

@testable import MonarchUI

enum MonarchAiStarterPromptsFixtures: ComponentFixtures {
    static let id = "ai-starter-prompts"

    static let starters = [
        MonarchAiStarterPrompt(
            id: "summarise",
            title: "Summarise a document",
            prompt: "Summarise the attached report in five bullet points.",
            systemImage: "doc.text"
        ),
        MonarchAiStarterPrompt(id: "draft", title: "Draft a reply", prompt: "Write a polite reply to this email."),
        MonarchAiStarterPrompt(
            id: "plan",
            title: "Plan my week",
            prompt: "Turn my open tasks into a plan for the week, with the most urgent ones first.",
            systemImage: "calendar"
        ),
        MonarchAiStarterPrompt(
            id: "long",
            title: "A starter with a title that is long enough to wrap onto a second line in a narrow column",
            prompt: String(repeating: "A long prompt that is cut after two lines. ", count: 6),
            systemImage: "text.alignleft"
        ),
    ]

    static let fixtures = [
        Fixture("grid") {
            MonarchAiStarterPrompts(starters) { _ in }
        },
        Fixture("list") {
            MonarchAiStarterPrompts(starters, layout: .list) { _ in }
        },
        Fixture("prompt hidden") {
            MonarchAiStarterPrompts(starters, layout: .grid, hidesPrompt: true, label: "Ideas") { _ in }
        },
        Fixture("disabled") {
            MonarchAiStarterPrompts(starters, layout: .list) { _ in }.disabled(true)
        },
        Fixture("one starter") {
            MonarchAiStarterPrompts([starters[0]]) { _ in }
        },
    ]
}

@Suite("MonarchAiStarterPrompts")
struct MonarchAiStarterPromptsTests {
    @Test("A starter takes the AI tint while pressed and is muted when disabled")
    func appearance() {
        let resting = MonarchAiStarterPromptsTokens.appearance(isPressed: false, isEnabled: true)
        #expect(resting.background == \MonarchColorTokens.bgSurface)
        #expect(resting.border == \MonarchColorTokens.borderDefault)
        #expect(resting.title == \MonarchColorTokens.textDefault)
        #expect(resting.icon == \MonarchColorTokens.aiStrong)
        #expect(resting.prompt == \MonarchColorTokens.textMuted)

        let pressed = MonarchAiStarterPromptsTokens.appearance(isPressed: true, isEnabled: true)
        #expect(pressed.background == \MonarchColorTokens.aiSubtle)
        #expect(pressed.border == \MonarchColorTokens.aiBorder)
        #expect(pressed.title == resting.title)

        for isPressed in [false, true] {
            let disabled = MonarchAiStarterPromptsTokens.appearance(isPressed: isPressed, isEnabled: false)
            #expect(disabled.background == \MonarchColorTokens.bgSurface)
            #expect(disabled.border == \MonarchColorTokens.borderSubtle)
            #expect(disabled.title == \MonarchColorTokens.textDisabled)
            #expect(disabled.icon == \MonarchColorTokens.textDisabled)
            #expect(disabled.prompt == \MonarchColorTokens.textDisabled)
        }
    }

    @Test("The list is one column, and so is the grid at the accessibility text sizes")
    func columns() {
        #expect(!MonarchAiStarterPromptsTokens.isSingleColumn(layout: .grid, isAccessibilitySize: false))
        #expect(MonarchAiStarterPromptsTokens.isSingleColumn(layout: .grid, isAccessibilitySize: true))
        #expect(MonarchAiStarterPromptsTokens.isSingleColumn(layout: .list, isAccessibilitySize: false))
        #expect(MonarchAiStarterPromptsTokens.isSingleColumn(layout: .list, isAccessibilitySize: true))
    }

    @Test("The list is denser than the grid")
    func density() {
        #expect(MonarchAiStarterPromptsTokens.spacing(.list) < MonarchAiStarterPromptsTokens.spacing(.grid))
        #expect(
            MonarchAiStarterPromptsTokens.verticalPadding(.list) < MonarchAiStarterPromptsTokens.verticalPadding(.grid)
        )
        #expect(MonarchAiStarterPromptsTokens.columnMinWidth == 208)
    }

    @Test("VoiceOver reads the prompt as the hint unless it is hidden")
    func accessibilityHint() {
        #expect(
            MonarchAiStarterPromptsTokens.accessibilityHint(prompt: "Write a reply.", hidesPrompt: false)
                == "Write a reply."
        )
        #expect(MonarchAiStarterPromptsTokens.accessibilityHint(prompt: "Write a reply.", hidesPrompt: true).isEmpty)
    }

    @Test("The group name comes from the component's strings table")
    func strings() {
        #expect(MonarchAiStarterPromptsStrings.label == "Suggested prompts")
    }

    @Test("A starter is identified by its id")
    func identity() {
        let starter = MonarchAiStarterPrompt(id: "a", title: "Title", prompt: "Prompt")
        #expect(starter.id == "a")
        #expect(starter.systemImage == nil)
        #expect(starter == MonarchAiStarterPrompt(id: "a", title: "Title", prompt: "Prompt", systemImage: nil))
    }

    @Test("Nothing is laid out without starters, and two columns are shorter than one")
    @MainActor
    func layout() {
        let starters = Array(MonarchAiStarterPromptsFixtures.starters.prefix(2))
        let empty = Host.layout(MonarchAiStarterPrompts([]) { _ in })
        let list = Host.layout(MonarchAiStarterPrompts(starters, layout: .list) { _ in })
        #expect(empty.height < list.height)
        #expect(list.height >= 2 * MonarchAccessibility.minimumHitTarget)
    }
}
