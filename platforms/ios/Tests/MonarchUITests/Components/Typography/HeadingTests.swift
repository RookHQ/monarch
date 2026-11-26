import MonarchTokens
import SwiftUI
import Testing

@testable import MonarchUI

enum HeadingFixtures: ComponentFixtures {
    static let id = "heading"
    static let fixtures = [
        Fixture("every heading role, marked as a header") {
            VStack(alignment: .leading) {
                ForEach(HeadingRoles.all, id: \.self) { role in
                    Text("Invoices").monarchTextStyle(role).accessibilityAddTraits(.isHeader)
                }
            }
        },
        Fixture("with an outline level") {
            VStack(alignment: .leading) {
                Text("Billing").monarchTextStyle(.title).accessibilityHeading(.h1)
                Text("Payment methods").monarchTextStyle(.heading).accessibilityHeading(.h2)
                Text("Cards").monarchTextStyle(.subheading).accessibilityHeading(.h3)
            }
        },
        Fixture("muted") {
            HeadingMutedFixture()
        },
        Fixture("long text wraps, or is kept to one line") {
            VStack(alignment: .leading) {
                Text("A heading that is long enough to need more than one line on a phone")
                    .monarchTextStyle(.heading)
                    .accessibilityAddTraits(.isHeader)
                Text("A heading that is long enough to need more than one line on a phone")
                    .monarchTextStyle(.heading)
                    .lineLimit(1)
                    .accessibilityAddTraits(.isHeader)
            }
        },
    ]
}

/// The type roles that the heading entry documents, largest first.
enum HeadingRoles {
    static let all: [MonarchTextStyle] = [.display, .title, .heading, .subheading, .bodyStrong, .label]
}

private struct HeadingMutedFixture: View {
    @Environment(\.monarchTheme) private var theme

    var body: some View {
        Text("Nothing here yet")
            .monarchTextStyle(.subheading)
            .foregroundStyle(theme.colors.textMuted)
            .accessibilityAddTraits(.isHeader)
    }
}

@Suite("Heading")
struct HeadingTests {
    @Test("The heading roles have the sizes of the web heading sizes the inventory names")
    func sizesMatchTheWebScale() {
        #expect(MonarchTextStyle.display.spec.size == CGFloat(MonarchTokens.FontSize.xl4))
        #expect(MonarchTextStyle.title.spec.size == CGFloat(MonarchTokens.FontSize.xl2))
        #expect(MonarchTextStyle.heading.spec.size == CGFloat(MonarchTokens.FontSize.xl))
        #expect(MonarchTextStyle.subheading.spec.size == CGFloat(MonarchTokens.FontSize.lg))
        #expect(MonarchTextStyle.bodyStrong.spec.size == CGFloat(MonarchTokens.FontSize.base))
        #expect(MonarchTextStyle.label.spec.size == CGFloat(MonarchTokens.FontSize.sm))
    }

    @Test("The heading roles get smaller in order and none is lighter than medium")
    func rolesAreOrdered() {
        let sizes = HeadingRoles.all.map(\.spec.size)
        #expect(sizes == sizes.sorted(by: >))
        #expect(Set(sizes).count == sizes.count)
        for role in HeadingRoles.all {
            #expect(role.spec.weight >= MonarchTokens.FontWeight.medium)
        }
    }

    @Test("Large heading roles are set tighter than running text")
    func largeRolesAreTight() {
        #expect(MonarchTextStyle.display.spec.lineHeight == CGFloat(MonarchTokens.LineHeight.tight))
        #expect(MonarchTextStyle.title.spec.lineHeight == CGFloat(MonarchTokens.LineHeight.tight))
        #expect(MonarchTextStyle.heading.spec.lineHeight < MonarchTextStyle.body.spec.lineHeight)
        #expect(MonarchTextStyle.display.spec.letterSpacing < MonarchTextStyle.body.spec.letterSpacing)
    }

    @MainActor
    @Test("A larger heading role takes more height")
    func largerRoleIsTaller() {
        let heights = HeadingRoles.all.map { role in
            Host.layout(Text("Invoices").monarchTextStyle(role)).height
        }
        #expect(heights == heights.sorted(by: >))
        #expect((heights.first ?? 0) > (heights.last ?? 0))
    }
}
