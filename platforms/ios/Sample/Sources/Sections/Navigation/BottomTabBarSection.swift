import MonarchUI
import SwiftUI

/// Native adoption: `TabView` with tab items, tinted by the theme.
struct BottomTabBarSection: CataloguePage {
    static let title = "Bottom tab bar (TabView)"

    @Environment(\.monarchTheme) private var theme
    @State private var showsDemo = false
    @State private var tab = "Home"

    var body: some View {
        Text("Three to five destinations. The system draws the bar, the badge and the safe-area insets.")
            .monarchTextStyle(.body)
        MonarchButton("Show the tab bar", variant: .primary) { showsDemo = true }
            .fullScreenCover(isPresented: $showsDemo) {
                TabView(selection: $tab) {
                    page("Home")
                        .tabItem { Label("Home", systemImage: "house") }
                        .tag("Home")
                    page("Inbox")
                        .tabItem { Label("Inbox", systemImage: "tray") }
                        .badge(3)
                        .tag("Inbox")
                    page("Settings")
                        .tabItem { Label("Settings", systemImage: "gearshape") }
                        .tag("Settings")
                }
            }
    }

    private func page(_ title: String) -> some View {
        VStack(spacing: theme.spacing.space4) {
            Text(title).monarchTextStyle(.title)
            MonarchButton("Close") { showsDemo = false }
        }
        .foregroundStyle(theme.colors.textDefault)
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .background(theme.colors.bgCanvas)
    }
}
