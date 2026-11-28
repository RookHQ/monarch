import MonarchTokens
import SwiftUI

@testable import MonarchUI

// The native overlays have no Monarch view of their own: the system draws the alert, sheet, menu and tooltip. These
// fixtures draw what the app puts in them, with the Monarch theme, so the screenshot tests cover it in every theme.

enum DialogFixtures: ComponentFixtures {
    static let id = "dialog"
    static let fixtures = [
        Fixture("content of a sheet with Cancel and Save") {
            DialogSheetContent()
        }
    ]
}

private struct DialogSheetContent: View {
    @Environment(\.monarchTheme) private var theme
    @State private var name = "Ada Lovelace"

    var body: some View {
        NavigationStack {
            VStack(alignment: .leading, spacing: theme.spacing.space4) {
                MonarchTextField("Name", text: $name)
                Spacer()
            }
            .padding(theme.spacing.space4)
            .background(theme.colors.bgCanvas)
            .navigationTitle("Edit profile")
            #if os(iOS)
                .navigationBarTitleDisplayMode(.inline)
            #endif
            .toolbar {
                ToolbarItem(placement: .cancellationAction) { Button("Cancel") {} }
                ToolbarItem(placement: .confirmationAction) { Button("Save") {} }
            }
        }
        .frame(height: 220)
    }
}

enum SheetFixtures: ComponentFixtures {
    static let id = "sheet"
    static let fixtures = [
        Fixture("form content") {
            SheetFormContent()
        }
    ]
}

private struct SheetFormContent: View {
    @Environment(\.monarchTheme) private var theme
    @State private var name = "Ada Lovelace"

    var body: some View {
        VStack(alignment: .leading, spacing: theme.spacing.space4) {
            Text("Edit details")
                .monarchTextStyle(.heading)
                .accessibilityAddTraits(.isHeader)
            MonarchTextField("Name", text: $name)
            MonarchButton("Done", variant: .primary, fullWidth: true) {}
        }
        .foregroundStyle(theme.colors.textDefault)
        .padding(theme.spacing.space4)
        .frame(maxWidth: .infinity, alignment: .top)
        .background(theme.colors.bgRaised)
    }
}

enum DropdownMenuFixtures: ComponentFixtures {
    static let id = "dropdown-menu"
    static let fixtures = [
        Fixture("the closed trigger") {
            Menu {
                Button("Rename", systemImage: "pencil") {}
                Button("Delete", systemImage: "trash", role: .destructive) {}
            } label: {
                Label("Actions", systemImage: "ellipsis.circle")
            }
            .buttonStyle(.monarch())
        },
        Fixture("the closed trigger, disabled") {
            Menu {
                Button("Rename") {}
            } label: {
                Label("Actions", systemImage: "ellipsis.circle")
            }
            .buttonStyle(.monarch())
            .disabled(true)
        },
    ]
}

enum TooltipFixtures: ComponentFixtures {
    static let id = "tooltip"
    static let fixtures = [
        Fixture("an icon-only button with help text") {
            Button {
            } label: {
                MonarchIcon(systemName: "line.3.horizontal.decrease")
            }
            .buttonStyle(.monarch(.ghost))
            .accessibilityLabel("Filter")
            .help("Shows only unpaid invoices")
        }
    ]
}
