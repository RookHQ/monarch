import MonarchTokens
import SwiftUI
import Testing

@testable import MonarchUI

enum MonarchLogoCloudFixtures: ComponentFixtures {
    static let id = "logo-cloud"
    static let names = ["Company A", "Company B", "Company C", "Company D", "Company E", "Company F"]
    static let fixtures = [
        Fixture("wordmarks in a row") {
            MonarchLogoCloud(title: "Placeholder organisations", logos: names.map { MonarchLogoCloudLogo($0) })
        },
        Fixture("grid with links, small") {
            MonarchLogoCloud(
                title: "Integrations",
                logos: names.prefix(4).enumerated().map { index, name in
                    MonarchLogoCloudLogo(name, destination: URL(string: "https://example.com/integration-\(index + 1)"))
                },
                layout: .grid,
                size: .sm
            )
        },
        Fixture("images, every size, monochrome and in colour") {
            VStack {
                ForEach(MonarchLogoCloudSize.allCases, id: \.self) { size in
                    MonarchLogoCloud(
                        logos: [
                            MonarchLogoCloudLogo("Company A", image: Image(systemName: "building.2")),
                            MonarchLogoCloudLogo("Company B"),
                        ],
                        size: size,
                        isMonochrome: size != .lg
                    )
                }
            }
        },
        Fixture("no title, one logo") {
            MonarchLogoCloud(logos: [MonarchLogoCloudLogo("Company A")])
        },
    ]
}

@Suite("MonarchLogoCloud")
struct MonarchLogoCloudTests {
    @Test("Logo heights are 24, 32 and 44 points")
    func heights() {
        #expect(MonarchLogoCloudTokens.logoHeight(.sm) == 24)
        #expect(MonarchLogoCloudTokens.logoHeight(.md) == 32)
        #expect(MonarchLogoCloudTokens.logoHeight(.lg) == 44)
    }

    @Test("Wordmarks grow with the size")
    func wordmarks() {
        let sizes = MonarchLogoCloudSize.allCases.map { MonarchLogoCloudTokens.wordmarkStyle($0).spec.size }
        #expect(sizes == sizes.sorted())
        #expect(Set(sizes).count == sizes.count)
    }

    @Test("The list is named by its title, else by the built-in string or the override")
    func listLabel() {
        #expect(MonarchLogoCloudTokens.listLabel(title: "Integrations", override: nil) == "Integrations")
        #expect(MonarchLogoCloudTokens.listLabel(title: nil, override: nil) == "Organisations")
        #expect(MonarchLogoCloudTokens.listLabel(title: "", override: "Partners") == "Partners")
    }

    @Test("The built-in string resolves")
    func strings() {
        #expect(MonarchLogoCloudStrings.listLabel == "Organisations")
    }

    @Test("Colours are muted theme roles")
    func colours() {
        #expect(MonarchLogoCloudTokens.wordmarkColor == \MonarchColorTokens.textMuted)
        #expect(MonarchLogoCloudTokens.titleColor == \MonarchColorTokens.textMuted)
        #expect(MonarchLogoCloudTokens.cellBorder == \MonarchColorTokens.borderSubtle)
    }
}
