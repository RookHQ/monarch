import MonarchTokens
import SwiftUI
import Testing

@testable import MonarchUI

enum MonarchMarketingSectionFixtures: ComponentFixtures {
    static let id = "marketing-section-blocks"
    static let fixtures = [
        Fixture("section with features") {
            MonarchMarketingSection(
                "Every idea gets the right shape.",
                eyebrow: "The editor",
                description: "Type a slash and pick a block."
            ) {
                MonarchFeatureGrid(items: [
                    MonarchFeatureGridItem(
                        "Slash commands",
                        description: "Tables, lists and callouts, one keystroke away."
                    ),
                    MonarchFeatureGridItem("Version history", description: "Named snapshots you can restore safely."),
                ])
            }
        },
        Fixture("centred header, three features with symbols") {
            MonarchMarketingSection("Built for teams", alignment: .center) {
                MonarchFeatureGrid(
                    items: [
                        MonarchFeatureGridItem("Search", description: "Find any page.", systemImage: "magnifyingglass"),
                        MonarchFeatureGridItem("Share", systemImage: "square.and.arrow.up"),
                        MonarchFeatureGridItem("Export", description: "HTML, Markdown and PDF.", systemImage: "doc"),
                    ],
                    columns: 3
                )
            }
        },
        Fixture("product frame, stats, FAQ and CTA") {
            VStack(alignment: .leading, spacing: MonarchTokens.Space.space8) {
                MonarchProductFrame(
                    "Share dialog with a copyable link",
                    chrome: .browser,
                    title: "example.rookhq.com/share"
                ) {
                    Text("Anyone with the link can view").padding()
                }
                MonarchStatStrip(items: [
                    MonarchStatStripItem("3", label: "Export formats"),
                    MonarchStatStripItem("13", label: "Interface languages"),
                ])
                MonarchFaq(
                    items: [
                        MonarchFaqItem("Which formats can I export?", answer: "HTML, Markdown and PDF."),
                        MonarchFaqItem("Can I import pages?", answer: "Yes, from Markdown files."),
                    ],
                    expandedIndices: [0]
                )
                MonarchCtaBand("Start with a blank page.", description: "No card needed.", alignment: .center) {
                    MonarchButton("Start writing", variant: .primary) {}
                    MonarchButton("See pricing") {}
                }
            }
        },
        Fixture("long text") {
            MonarchMarketingSection(
                "A title long enough to wrap onto a second and a third line on a phone",
                eyebrow: "An eyebrow that is longer than usual",
                description: "A description that explains the section in two full sentences. It wraps at every size."
            ) {
                MonarchProductFrame("A plain frame") { Text("Content").padding() }
            }
        },
    ]
}

@Suite("MonarchMarketingSection")
struct MonarchMarketingSectionTests {
    @Test("Alignment maps to the stack, text and frame alignments")
    func alignment() {
        #expect(MonarchMarketingSectionTokens.horizontal(.leading) == .leading)
        #expect(MonarchMarketingSectionTokens.horizontal(.center) == .center)
        #expect(MonarchMarketingSectionTokens.text(.leading) == .leading)
        #expect(MonarchMarketingSectionTokens.text(.center) == .center)
        #expect(MonarchMarketingSectionTokens.frame(.center) == .center)
    }

    @Test("A feature grid keeps its columns, and has one at the accessibility sizes")
    func columns() {
        #expect(MonarchMarketingSectionTokens.columns(requested: 2, dynamicTypeSize: .large) == 2)
        #expect(MonarchMarketingSectionTokens.columns(requested: 3, dynamicTypeSize: .xxxLarge) == 3)
        #expect(MonarchMarketingSectionTokens.columns(requested: 0, dynamicTypeSize: .large) == 1)
        #expect(MonarchMarketingSectionTokens.columns(requested: 3, dynamicTypeSize: .accessibility1) == 1)
    }

    @Test("Colours come from theme roles")
    func colours() {
        #expect(MonarchMarketingSectionTokens.eyebrowColor == \MonarchColorTokens.accentStrong)
        #expect(MonarchMarketingSectionTokens.titleColor == \MonarchColorTokens.textDefault)
        #expect(MonarchMarketingSectionTokens.supportingColor == \MonarchColorTokens.textMuted)
        #expect(MonarchMarketingSectionTokens.bandFill == \MonarchColorTokens.bgSubtle)
    }

    @Test("The section title is larger than a feature title")
    func typeScale() {
        #expect(MonarchMarketingSectionTokens.titleStyle.spec.size > MonarchTextStyle.bodyStrong.spec.size)
        #expect(
            MonarchMarketingSectionTokens.eyebrowStyle.spec.size < MonarchMarketingSectionTokens.titleStyle.spec.size
        )
    }

    @Test("Items keep the values they are given")
    func items() {
        let feature = MonarchFeatureGridItem("Search", description: "Find any page.", systemImage: "magnifyingglass")
        #expect(feature.title == "Search")
        #expect(feature.systemImage == "magnifyingglass")
        #expect(MonarchStatStripItem("13", label: "Interface languages").value == "13")
        #expect(MonarchFaqItem("Q", answer: "A").answer == "A")
    }
}
