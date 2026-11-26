import MonarchUI
import SwiftUI
import Testing

enum MonarchDividerFixtures: ComponentFixtures {
    static let id = "separator"
    static let fixtures = [
        Fixture("in a vertical stack") {
            VStack {
                Text("Above")
                MonarchDivider()
                Text("Below")
            }
        },
        Fixture("in a horizontal stack") {
            HStack {
                Text("Before")
                MonarchDivider()
                Text("After")
            }
            .fixedSize(horizontal: false, vertical: true)
        },
    ]
}

@Suite("MonarchDivider")
@MainActor
struct MonarchDividerTests {
    @Test("A divider in a vertical stack is a hairline")
    func isThin() {
        let block = Text("Row").frame(height: 40)
        let without = Host.layout(
            VStack(spacing: 0) {
                block
                block
            }
        )
        let with = Host.layout(
            VStack(spacing: 0) {
                block
                MonarchDivider()
                block
            }
        )
        let thickness = with.height - without.height
        #expect(thickness > 0 && thickness <= 2)
    }
}
