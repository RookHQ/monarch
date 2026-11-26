import MonarchTokens
import SwiftUI
import Testing

@testable import MonarchUI

enum MonarchAiCodeBlockFixtures: ComponentFixtures {
    static let id = "ai-code-block"
    static let diff = "@@ -1,2 +1,2 @@\n-const port = 3000;\n+const port = 8080;\n export default port;"
    static let swift = "struct Greeting {\n    let name: String\n    var text: String { \"Hello, \\(name)\" }\n}"

    static let fixtures = [
        Fixture("diff with Apply and Insert") {
            MonarchAiCodeBlock(diff, language: "diff", title: "server.ts", onInsert: { _ in }, onApply: { _ in })
        },
        Fixture("incomplete, line numbers") {
            MonarchAiCodeBlock(swift, language: "swift", showsLineNumbers: true, isIncomplete: true, onApply: { _ in })
        },
        Fixture("no actions, no header controls") {
            MonarchAiCodeBlock("echo hello", language: "bash", showsWrapToggle: false, isCopyable: false)
        },
        Fixture("custom action, wrapped long line") {
            MonarchAiCodeBlock(
                "SELECT id, name, email, created_at, updated_at FROM customers WHERE active = true ORDER BY created_at",
                language: "sql",
                isWrapped: .constant(true),
                onApply: { _ in },
                actions: { MonarchButton("Run", systemImage: "play", variant: .ghost, size: .sm) {} }
            )
        },
    ]
}

@Suite("MonarchAiCodeBlock")
struct MonarchAiCodeBlockTests {
    private let labels = MonarchAiCodeBlockLabels()

    @Test("Actions are named with the code, so blocks can be told apart")
    func actionLabels() {
        let byTitle = MonarchAiCodeBlockTokens.name(title: "server.ts", language: "ts", labels: labels)
        #expect(byTitle == "server.ts")
        #expect(MonarchAiCodeBlockTokens.actionLabel("Apply", target: byTitle, labels: labels) == "Apply server.ts")
        let byLanguage = MonarchAiCodeBlockTokens.name(title: nil, language: "ts", labels: labels)
        #expect(byLanguage == "TypeScript code")
        #expect(
            MonarchAiCodeBlockTokens.actionLabel("Insert", target: byLanguage, labels: labels)
                == "Insert TypeScript code"
        )
        #expect(MonarchAiCodeBlockTokens.name(title: nil, language: nil, labels: labels) == "Code")
        let custom = MonarchAiCodeBlockLabels(actionLabel: { title, target in "\(target) : \(title)" })
        #expect(MonarchAiCodeBlockTokens.actionLabel("Appliquer", target: "a.ts", labels: custom) == "a.ts : Appliquer")
    }

    @Test("Apply and Insert wait for complete code, and say why")
    func incomplete() {
        #expect(!MonarchAiCodeBlockTokens.actionsEnabled(isIncomplete: true))
        #expect(MonarchAiCodeBlockTokens.actionsEnabled(isIncomplete: false))
        #expect(
            MonarchAiCodeBlockTokens.actionHint(isIncomplete: true, labels: labels)
                == "Available when the code is complete"
        )
        #expect(MonarchAiCodeBlockTokens.actionHint(isIncomplete: false, labels: labels).isEmpty)
    }

    @Test("The action row appears only with something in it")
    func actionRow() {
        #expect(!MonarchAiCodeBlockTokens.showsActionRow(hasApply: false, hasInsert: false, hasCustomActions: false))
        #expect(MonarchAiCodeBlockTokens.showsActionRow(hasApply: true, hasInsert: false, hasCustomActions: false))
        #expect(MonarchAiCodeBlockTokens.showsActionRow(hasApply: false, hasInsert: true, hasCustomActions: false))
        #expect(MonarchAiCodeBlockTokens.showsActionRow(hasApply: false, hasInsert: false, hasCustomActions: true))
    }

    @Test("Built-in strings resolve and can be replaced")
    func strings() {
        #expect(MonarchAiCodeBlockTokens.applyTitle(labels) == "Apply")
        #expect(MonarchAiCodeBlockTokens.insertTitle(labels) == "Insert")
        #expect(MonarchAiCodeBlockTokens.applyTitle(MonarchAiCodeBlockLabels(apply: "Appliquer")) == "Appliquer")
        #expect(MonarchAiCodeBlockStrings.incompleteHint != "monarch.aiCodeBlock.incompleteHint")
    }

    #if os(iOS)
        @Test("The action row has 44-point buttons")
        @MainActor func hitTarget() {
            let withActions = Host.layout(
                MonarchAiCodeBlock("x", showsWrapToggle: false, isCopyable: false, onApply: { _ in })
            )
            let without = Host.layout(MonarchAiCodeBlock("x", showsWrapToggle: false, isCopyable: false))
            #expect(withActions.height - without.height >= MonarchAccessibility.minimumHitTarget)
        }
    #endif
}
