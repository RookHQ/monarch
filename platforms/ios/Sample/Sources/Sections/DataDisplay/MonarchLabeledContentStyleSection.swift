import MonarchUI
import SwiftUI

struct MonarchLabeledContentStyleSection: CataloguePage {
    static let title = "DescriptionList (LabeledContent style)"

    @Environment(\.monarchTheme) private var theme
    @State private var layout = MonarchLabeledContentLayout.stacked

    var body: some View {
        Picker("Layout", selection: $layout) {
            Text("Stacked").tag(MonarchLabeledContentLayout.stacked)
            Text("Inline").tag(MonarchLabeledContentLayout.inline)
        }
        .pickerStyle(.segmented)
        VStack(alignment: .leading, spacing: theme.spacing.space4) {
            LabeledContent("Owner", value: "Ada Lovelace")
            LabeledContent("Stage") { MonarchBadge("Negotiation", tone: .info) }
            LabeledContent("Customer ID") {
                HStack(spacing: theme.spacing.space1) {
                    Text("cus_01HZX4Q8").monarchTextStyle(.code)
                    MonarchCopyButton(
                        value: "cus_01HZX4Q8",
                        target: "Customer ID",
                        variant: .ghost,
                        size: .sm,
                        isIconOnly: true
                    )
                }
            }
            LabeledContent("Close date") {
                Text("—").accessibilityLabel("Not set")
            }
            LabeledContent("Notes") {
                MonarchTruncatedText(
                    "Renewal depends on the security review, which the customer moved to the second week of May.",
                    lineLimit: 1
                )
            }
        }
        .labeledContentStyle(.monarch(layout))
        CatalogueCaption("Two columns")
        Grid(alignment: .topLeading, horizontalSpacing: theme.spacing.space4, verticalSpacing: theme.spacing.space4) {
            GridRow {
                LabeledContent("Created", value: "12 March")
                LabeledContent("Updated", value: "Today")
            }
            GridRow {
                LabeledContent("Seats", value: "24")
                LabeledContent("Plan", value: "Team")
            }
        }
        .labeledContentStyle(.monarch)
    }
}
