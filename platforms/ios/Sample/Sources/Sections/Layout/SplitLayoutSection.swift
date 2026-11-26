import MonarchUI
import SwiftUI

/// Native adoption: `NavigationSplitView` with two columns.
struct SplitLayoutSection: CataloguePage {
    static let title = "Split layout (NavigationSplitView)"

    @Environment(\.monarchTheme) private var theme
    @State private var showsDemo = false
    @State private var selection: String?

    private let messages = ["Welcome to Monarch", "Your invoice is ready", "Design review at 3"]

    var body: some View {
        Text("Two panes on an iPad and on a large iPhone in landscape; one pane with a back button on a phone.")
            .monarchTextStyle(.body)
        MonarchButton("Show the list and detail", variant: .primary) { showsDemo = true }
            .fullScreenCover(isPresented: $showsDemo) {
                NavigationSplitView(
                    sidebar: {
                        List(messages, id: \.self, selection: $selection) { message in
                            Text(message).monarchTextStyle(.body)
                        }
                        .navigationTitle("Inbox")
                        .navigationSplitViewColumnWidth(
                            min: theme.spacing.space24 * 2,
                            ideal: theme.spacing.space24 * 3
                        )
                        .toolbar {
                            ToolbarItem(placement: .cancellationAction) {
                                Button("Close") { showsDemo = false }
                            }
                        }
                    },
                    detail: {
                        Text(selection ?? "Select a message")
                            .monarchTextStyle(.body)
                            .foregroundStyle(selection == nil ? theme.colors.textMuted : theme.colors.textDefault)
                    }
                )
                .navigationSplitViewStyle(.balanced)
            }
    }
}
