import SwiftUI
import Testing

extension InventoryExample: CustomTestStringConvertible {
    var testDescription: String { "\(id): \(title)" }

    /// Every compiled example that is part of this run.
    static let selected: [InventoryExample] = InventoryExamples.all.filter { ComponentFilter.includes($0.id) }
}

/// Renders every `examples[].code` of every inventory file. The examples are
/// compiled into `Generated/Examples` by `Scripts/generate.py`, so the
/// documentation cannot show code that does not compile.
///
/// Serialized for the same reason as the component matrix: each case needs
/// the main actor, and its time limit must not include the wait.
@Suite("Inventory examples", .serialized)
@MainActor
struct InventoryExampleTests {
    @Test("Every example renders at the largest text size", arguments: InventoryExample.selected, ThemeCase.all)
    func exampleRenders(example: InventoryExample, theme: ThemeCase) {
        let size = Host.layout(example.make().rendered(in: theme, .largestText))
        #expect(size.width > 0 && size.height > 0, "\(example) has no size in \(theme)")
    }
}
