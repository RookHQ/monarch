import SwiftUI
import Testing

/// The iOS side of the cross-platform scenario fixtures (CROSS-PLATFORM.md section 8). Each fixture is the
/// scenario of a web primary example; the first inventory example of the same id is written from it by
/// `scripts/native/fixtures.mjs`. This checks that the bound example is the primary one (the docs page and the
/// preview show the first example) and that it renders with the fixture's content.
@Suite("Parity fixtures", .serialized)
@MainActor
struct ParityFixtureTests {
    /// Components whose web primary example draws no text of its own: shapes, bars, icons, icon-only buttons and
    /// fields with an empty value. Their fixtures list tones, actions or symbols instead (or only data).
    private static let drawsNoText: Set<String> = [
        "sparkline", "call-controls", "progress-bar", "editable-cell", "skeleton", "spinner", "file-type-icon",
        "text-field", "ai-feedback", "ai-message-actions", "drag-handle", "tooltip",
    ]

    @Test("The fixture is the primary example", arguments: ParityFixtures.all.map(\.id))
    func fixtureIsPrimary(id: String) throws {
        let fixture = try #require(ParityFixtures.all.first { $0.id == id })
        let primary = try #require(InventoryExamples.all.first { $0.id == id }, "\(id) has no compiled example")
        #expect(primary.title == fixture.exampleTitle)
        #expect(!fixture.texts.isEmpty || Self.drawsNoText.contains(id), "\(id) lists no visible text")
        #expect(fixture.symbols.allSatisfy { !$0.isEmpty }, "every web glyph of \(id) has an SF Symbol")
        let size = Host.layout(primary.make().rendered(in: ThemeCase.all[0], .standard))
        #expect(size.width > 0 && size.height > 0)
    }
}
