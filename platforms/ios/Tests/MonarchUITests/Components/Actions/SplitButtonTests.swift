import SwiftUI
import Testing

@testable import MonarchUI

/// A split button as the inventory recipe builds it: a standard `Menu`
/// with a primary action, so a tap runs the main action and a long press
/// opens the alternatives, with the Monarch button style.
private struct SplitButtonFixture: View {
    let title: String
    var variant: MonarchButtonVariant = .primary
    var size: MonarchButtonSize = .md

    var body: some View {
        Menu {
            Button("Save as draft") {}
            Button("Save and close") {}
        } label: {
            Text(title)
        } primaryAction: {
        }
        .buttonStyle(.monarch(variant, size: size))
    }
}

/// The second recipe: a main button joined to a separate arrow menu, for
/// when the alternatives must be visible.
private struct JoinedSplitButtonFixture: View {
    let title: String
    var variant: MonarchButtonVariant = .secondary

    var body: some View {
        HStack(spacing: MonarchShapes.borderWidth) {
            Button(title) {}
                .buttonStyle(.monarch(variant))
            Menu {
                Button("Save as draft") {}
                Button("Save and close") {}
            } label: {
                MonarchIcon(systemName: "chevron.down", size: .sm)
            }
            .buttonStyle(.monarch(variant))
            .accessibilityLabel("More options for \(title)")
        }
        .accessibilityElement(children: .contain)
        .accessibilityLabel(title)
    }
}

enum SplitButtonFixtures: ComponentFixtures {
    static let id = "split-button"
    static let fixtures = [
        Fixture("menu with a primary action, every variant") {
            VStack(alignment: .leading) {
                ForEach(MonarchButtonVariant.allCases, id: \.self) { variant in
                    SplitButtonFixture(title: "Save", variant: variant)
                }
            }
        },
        Fixture("every size") {
            HStack {
                ForEach(MonarchButtonSize.allCases, id: \.self) { size in
                    SplitButtonFixture(title: "Send", size: size)
                }
            }
        },
        Fixture("joined main button and arrow") {
            VStack(alignment: .leading) {
                JoinedSplitButtonFixture(title: "Save")
                JoinedSplitButtonFixture(title: "Merge", variant: .primary)
            }
        },
        Fixture("disabled") {
            VStack(alignment: .leading) {
                SplitButtonFixture(title: "Save").disabled(true)
                JoinedSplitButtonFixture(title: "Save").disabled(true)
            }
        },
        Fixture("long title") {
            JoinedSplitButtonFixture(title: "Publish the release notes to every workspace")
        },
    ]
}

@Suite("SplitButton")
struct SplitButtonTests {
    @Test("Both recipes are drawn")
    @MainActor
    func draws() {
        #expect(Host.image(SplitButtonFixture(title: "Save")) != nil)
        #expect(Host.image(JoinedSplitButtonFixture(title: "Save")) != nil)
    }

    #if os(iOS)
        @Test("The main button and the arrow each keep a 44-point touch target")
        @MainActor
        func touchTargets() {
            let menu = Host.layout(SplitButtonFixture(title: "Save", size: .sm))
            let arrow = Host.layout(
                Menu("") {
                    Button("Save as draft") {}
                } primaryAction: {
                }
                .labelStyle(.iconOnly)
                .buttonStyle(.monarch(.secondary, size: .sm))
            )
            #expect(menu.height >= MonarchAccessibility.minimumHitTarget)
            #expect(arrow.height >= MonarchAccessibility.minimumHitTarget)
        }
    #endif
}
