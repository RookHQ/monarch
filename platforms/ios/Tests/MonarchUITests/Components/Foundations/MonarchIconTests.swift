import SwiftUI
import Testing

@testable import MonarchUI

enum MonarchIconFixtures: ComponentFixtures {
    static let id = "icon"
    static let fixtures = [
        Fixture("sizes, decorative and labelled") {
            HStack {
                ForEach(MonarchIconSize.allCases, id: \.self) { size in
                    MonarchIcon(systemName: "tray", size: size)
                }
                MonarchIcon(systemName: "tray.full", label: "Inbox")
            }
        }
    ]
}

@Suite("MonarchIcon")
struct MonarchIconTests {
    @Test("Sizes come from the spacing tokens")
    func sizes() {
        #expect(MonarchIconTokens.pointSize(.sm) == 16)
        #expect(MonarchIconTokens.pointSize(.md) == 20)
        #expect(MonarchIconTokens.pointSize(.lg) == 24)
    }
}
