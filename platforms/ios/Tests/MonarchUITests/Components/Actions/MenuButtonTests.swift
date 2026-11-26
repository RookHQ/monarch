import SwiftUI
import Testing

@testable import MonarchUI

/// A menu button as the inventory recipe builds it: a standard `Menu` whose
/// label is a title and a chevron, with the Monarch button style.
private struct MenuButtonFixture: View {
    let title: String
    var variant: MonarchButtonVariant = .secondary
    var size: MonarchButtonSize = .md

    var body: some View {
        Menu {
            Button("Edit", systemImage: "pencil") {}
            Button("Duplicate", systemImage: "plus.square.on.square") {}
            Divider()
            Button("Delete", systemImage: "trash", role: .destructive) {}
        } label: {
            HStack(spacing: MonarchButtonTokens.iconSpacing) {
                Text(title)
                MonarchIcon(systemName: "chevron.down", size: .sm)
            }
        }
        .buttonStyle(.monarch(variant, size: size))
    }
}

/// The icon-only form: a ghost button with a spoken name.
private struct IconMenuButtonFixture: View {
    var body: some View {
        Menu {
            Button("Rename") {}
            Button("Archive") {}
        } label: {
            MonarchIcon(systemName: "ellipsis")
        }
        .buttonStyle(.monarch(.ghost))
        .accessibilityLabel("More actions")
    }
}

enum MenuButtonFixtures: ComponentFixtures {
    static let id = "menu-button"
    static let fixtures = [
        Fixture("every variant") {
            VStack(alignment: .leading) {
                ForEach(MonarchButtonVariant.allCases, id: \.self) { variant in
                    MenuButtonFixture(title: "Actions", variant: variant)
                }
            }
        },
        Fixture("every size") {
            HStack {
                ForEach(MonarchButtonSize.allCases, id: \.self) { size in
                    MenuButtonFixture(title: "Sort", size: size)
                }
            }
        },
        Fixture("icon only") { IconMenuButtonFixture() },
        Fixture("disabled") { MenuButtonFixture(title: "Actions").disabled(true) },
        Fixture("long title") {
            MenuButtonFixture(title: "Export every invoice of this workspace in another format")
        },
    ]
}

@Suite("MenuButton")
struct MenuButtonTests {
    @Test("A menu button is drawn, at every variant")
    @MainActor
    func draws() {
        for variant in MonarchButtonVariant.allCases {
            #expect(Host.image(MenuButtonFixture(title: "Actions", variant: variant)) != nil)
        }
    }

    #if os(iOS)
        @Test("The trigger keeps a 44-point touch target, also when icon only")
        @MainActor
        func touchTarget() {
            let titled = Host.layout(MenuButtonFixture(title: "Actions", size: .sm))
            let icon = Host.layout(IconMenuButtonFixture())
            #expect(titled.height >= MonarchAccessibility.minimumHitTarget)
            #expect(icon.height >= MonarchAccessibility.minimumHitTarget)
        }
    #endif
}
