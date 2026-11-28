import MonarchTokens
import SwiftUI
import Testing

@testable import MonarchUI

enum MonarchBadgeFixtures: ComponentFixtures {
    static let id = "badge"
    static let fixtures = [
        Fixture("tones and variants") {
            VStack {
                ForEach(MonarchBadgeVariant.allCases, id: \.self) { variant in
                    ForEach(MonarchTone.allCases, id: \.self) { tone in
                        MonarchBadge("Status", tone: tone, variant: variant)
                    }
                }
            }
        },
        Fixture("with icon") {
            MonarchBadge("Synced", tone: .success, systemImage: "checkmark")
        },
    ]
}

@Suite("MonarchBadge")
struct MonarchBadgeTests {
    @Test("Colours follow tone and variant")
    func appearance() {
        let subtle = MonarchBadgeTokens.appearance(tone: .success, variant: .subtle)
        #expect(subtle.background == \MonarchColorTokens.successSubtle)
        #expect(subtle.foreground == \MonarchColorTokens.successStrong)
        #expect(subtle.border == nil)

        let solid = MonarchBadgeTokens.appearance(tone: .danger, variant: .solid)
        #expect(solid.background == \MonarchColorTokens.dangerSolid)
        #expect(solid.foreground == \MonarchColorTokens.dangerOnSolid)

        let outline = MonarchBadgeTokens.appearance(tone: .warning, variant: .outline)
        #expect(outline.background == nil)
        #expect(outline.foreground == \MonarchColorTokens.warningStrong)
        #expect(outline.border == \MonarchColorTokens.warningBorder)

        let neutral = MonarchBadgeTokens.appearance(tone: .neutral, variant: .solid)
        #expect(neutral.background == \MonarchColorTokens.bgInverse)
        #expect(neutral.foreground == \MonarchColorTokens.textInverse)
    }

    @Test("Every tone has its own colours in every variant", arguments: MonarchBadgeVariant.allCases)
    func tonesAreDistinct(variant: MonarchBadgeVariant) {
        let foregrounds = MonarchTone.allCases.map {
            MonarchBadgeTokens.appearance(tone: $0, variant: variant).foreground
        }
        #expect(Set(foregrounds).count == MonarchTone.allCases.count)
    }
}
