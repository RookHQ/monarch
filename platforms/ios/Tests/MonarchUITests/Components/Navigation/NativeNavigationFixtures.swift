import MonarchTokens
import SwiftUI

@testable import MonarchUI

enum BottomTabBarFixtures: ComponentFixtures {
    static let id = "bottom-tab-bar"
    static let fixtures = [
        Fixture("three destinations with a badge") {
            BottomTabBarContent()
        }
    ]
}

private struct BottomTabBarContent: View {
    @Environment(\.monarchTheme) private var theme
    @State private var tab = "home"

    var body: some View {
        TabView(selection: $tab) {
            Text("Home")
                .monarchTextStyle(.title)
                .frame(maxWidth: .infinity, maxHeight: .infinity)
                .monarchCanvas()
                .tabItem { Label("Home", systemImage: "house") }
                .tag("home")
            Text("Inbox")
                .monarchTextStyle(.title)
                .frame(maxWidth: .infinity, maxHeight: .infinity)
                .monarchCanvas()
                .tabItem { Label("Inbox", systemImage: "tray") }
                .badge(3)
                .tag("inbox")
            Text("Settings")
                .monarchTextStyle(.title)
                .frame(maxWidth: .infinity, maxHeight: .infinity)
                .monarchCanvas()
                .tabItem { Label("Settings", systemImage: "gearshape") }
                .tag("settings")
        }
        .frame(height: theme.spacing.space24 * 3)
    }
}

enum PaginationFixtures: ComponentFixtures {
    static let id = "pagination"
    static let fixtures = [
        Fixture("twenty invoices and a load more row") {
            List {
                ForEach(1...3, id: \.self) { number in
                    Text("Invoice \(number)").monarchTextStyle(.body)
                }
                MonarchLoadMore(hasMore: true, isLoading: false, loadedCount: 20, totalCount: 60) {}
                    .listRowSeparator(.hidden)
            }
            .listStyle(.plain)
            .monarchCanvas()
            .frame(height: 260)
        },
        Fixture("loading the next page") {
            List {
                ForEach(1...3, id: \.self) { number in
                    Text("Invoice \(number)").monarchTextStyle(.body)
                }
                MonarchLoadMore(hasMore: true, isLoading: true, loadedCount: 20, totalCount: 60) {}
                    .listRowSeparator(.hidden)
            }
            .listStyle(.plain)
            .monarchCanvas()
            .frame(height: 260)
        },
    ]
}
