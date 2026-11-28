import MonarchUI
import SwiftUI

/// Native adoption: `confirmationDialog`, and a popover kept as a popover.
struct PopconfirmSection: CataloguePage {
    static let title = "Popconfirm (confirmationDialog, popover)"

    @Environment(\.monarchTheme) private var theme
    @State private var isConfirmingDialog = false
    @State private var isConfirmingPopover = false
    @State private var result = "Nothing removed yet"

    var body: some View {
        Text(result).monarchTextStyle(.bodyStrong)

        CatalogueCaption("A sheet on iPhone, a popover anchored to the button on iPad")
        MonarchButton("Remove filter", systemImage: "xmark", variant: .ghost, size: .sm) { isConfirmingDialog = true }
            .confirmationDialog(
                "Remove this filter?",
                isPresented: $isConfirmingDialog,
                titleVisibility: .visible,
                actions: {
                    Button("Remove", role: .destructive) { result = "Filter removed" }
                    Button("Cancel", role: .cancel) {}
                },
                message: {
                    Text("The list shows every status again.")
                }
            )

        CatalogueCaption("Anchored to the button at every width")
        MonarchButton("Revoke invitation", variant: .ghost) { isConfirmingPopover = true }
            .popover(isPresented: $isConfirmingPopover, arrowEdge: .top) {
                VStack(alignment: .leading, spacing: theme.spacing.space3) {
                    Text("Revoke this invitation?")
                        .monarchTextStyle(.bodyStrong)
                        .accessibilityAddTraits(.isHeader)
                    Text("The link in the email stops working.").monarchTextStyle(.body)
                    HStack {
                        MonarchButton("Cancel", size: .sm) { isConfirmingPopover = false }
                        MonarchButton("Revoke", variant: .danger, size: .sm) {
                            result = "Invitation revoked"
                            isConfirmingPopover = false
                        }
                    }
                }
                .foregroundStyle(theme.colors.textDefault)
                .padding(theme.spacing.space4)
                .presentationCompactAdaptation(.popover)
                .presentationBackground(theme.colors.bgRaised)
            }
    }
}
