import MonarchUI
import SwiftUI

/// Native adoption: NavigationSplitView with an inspector for the context pane.
struct SplitInboxLayoutSection: CataloguePage {
    static let title = "Split inbox layout (NavigationSplitView)"

    @Environment(\.monarchTheme) private var theme
    @State private var showsDemo = false
    @State private var selection: String?
    @State private var showsContext = false

    private let threads = ["Refund request", "Login fails on Safari", "Invoice is wrong", "Add a seat"]

    var body: some View {
        Text("List and detail are columns; the context pane is an inspector. On iPhone one pane shows at a time.")
            .monarchTextStyle(.body)
        MonarchButton("Show the inbox", variant: .primary) { showsDemo = true }
            .fullScreenCover(isPresented: $showsDemo) {
                NavigationSplitView(
                    sidebar: {
                        List(threads, id: \.self, selection: $selection) { thread in
                            Text(thread).monarchTextStyle(.body)
                        }
                        .navigationTitle("Inbox")
                        .toolbar {
                            ToolbarItem(placement: .cancellationAction) {
                                Button("Close") { showsDemo = false }
                            }
                        }
                    },
                    detail: { detail }
                )
            }
    }

    private var detail: some View {
        Group {
            if let selection {
                VStack(alignment: .leading, spacing: theme.spacing.space3) {
                    Text(selection).monarchTextStyle(.title)
                    Text("The customer wrote in this morning.").monarchTextStyle(.body)
                }
                .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .topLeading)
                .padding(theme.spacing.space4)
            } else {
                MonarchEmptyState(
                    "No conversation selected",
                    message: "Choose a conversation from the list.",
                    size: .sm
                )
            }
        }
        .toolbar {
            ToolbarItem(placement: .primaryAction) {
                Button(showsContext ? "Hide details" : "Show details", systemImage: "sidebar.trailing") {
                    showsContext.toggle()
                }
            }
        }
        .inspector(isPresented: $showsContext) {
            List {
                LabeledContent("Customer since", value: "2021")
                LabeledContent("Plan", value: "Team")
            }
            .inspectorColumnWidth(min: theme.spacing.space24 * 2, ideal: theme.spacing.space24 * 3)
        }
    }
}
