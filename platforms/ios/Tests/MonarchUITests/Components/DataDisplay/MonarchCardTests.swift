import SwiftUI
import Testing

@testable import MonarchUI

enum MonarchCardFixtures: ComponentFixtures {
    static let id = "card"
    static let fixtures = [
        Fixture("every elevation") {
            VStack {
                ForEach(MonarchElevationLevel.allCases, id: \.self) { level in
                    MonarchCard(elevation: level) {
                        Text("Title").monarchTextStyle(.subheading)
                        Text("Body").monarchTextStyle(.body)
                    }
                }
            }
        },
        Fixture("flush with a footer") {
            MonarchCard(padding: .flush) {
                Text("Edge to edge").monarchTextStyle(.body)
            } footer: {
                MonarchButton("View all", variant: .ghost, size: .sm) {}
            }
        },
    ]
}

@Suite("MonarchCard")
struct MonarchCardTests {
    @Test("The card is a bordered surface with the container radius")
    func tokens() {
        #expect(MonarchCardTokens.background == \.bgSurface)
        #expect(MonarchCardTokens.border == \.borderSubtle)
        #expect(MonarchCardTokens.cornerRadius == 12)
        #expect(MonarchCardTokens.padding == 16)
    }
}

@Suite("MonarchCard padding and footer")
@MainActor
struct MonarchCardPaddingTests {
    @Test("Flush content has no padding")
    func padding() {
        #expect(MonarchCardTokens.contentPadding(.standard) == MonarchCardTokens.padding)
        #expect(MonarchCardTokens.contentPadding(.flush) == 0)
        let standard = Host.layout(MonarchCard { Text("A") })
        let flush = Host.layout(MonarchCard(padding: .flush) { Text("A") })
        #expect(flush.height < standard.height)
    }

    @Test("A footer adds a row under the content")
    func footer() {
        let plain = Host.layout(MonarchCard { Text("A") })
        let footed = Host.layout(
            MonarchCard {
                Text("A")
            } footer: {
                Text("Note")
            }
        )
        #expect(footed.height > plain.height)
    }
}
