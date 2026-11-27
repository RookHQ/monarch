import MonarchUI
import SwiftUI

/// Native adoption: the app's top-level `TabView`, which adapts to the device.
struct TopNavSection: CataloguePage {
    static let title = "Top navigation (adaptive TabView)"

    @Environment(\.monarchTheme) private var theme
    @State private var showsDemo = false
    @State private var tab = "Tickets"

    private let destinations = [
        ("Tickets", "ticket"), ("Customers", "person.2"), ("Reports", "chart.bar"), ("Settings", "gearshape"),
    ]

    var body: some View {
        Text(
            "A row of destinations is the app's TabView. On iPadOS 18 and later the sidebarAdaptable style puts the tabs at the top and lets them become a sidebar; on iOS 17 they are the bottom tab bar."
        )
        .monarchTextStyle(.body)
        MonarchButton("Show the navigation", variant: .primary) { showsDemo = true }
            .fullScreenCover(isPresented: $showsDemo) {
                if #available(iOS 18.0, *) {
                    tabs.tabViewStyle(.sidebarAdaptable)
                } else {
                    tabs
                }
            }
    }

    private var tabs: some View {
        TabView(selection: $tab) {
            ForEach(destinations, id: \.0) { destination in
                VStack(spacing: theme.spacing.space4) {
                    Text(destination.0).monarchTextStyle(.title)
                    MonarchButton("Close") { showsDemo = false }
                }
                .foregroundStyle(theme.colors.textDefault)
                .frame(maxWidth: .infinity, maxHeight: .infinity)
                .background(theme.colors.bgCanvas)
                .tabItem { Label(destination.0, systemImage: destination.1) }
                .tag(destination.0)
            }
        }
    }
}
