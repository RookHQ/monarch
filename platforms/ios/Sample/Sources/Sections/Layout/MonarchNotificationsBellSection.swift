import MonarchUI
import SwiftUI

struct MonarchNotificationsBellSection: CataloguePage {
    static let title = "Notifications bell"

    @Environment(\.monarchTheme) private var theme
    @State private var unread = 3
    @State private var showsNotifications = false

    var body: some View {
        CatalogueCaption("Icon")
        HStack(spacing: theme.spacing.space4) {
            MonarchNotificationsBell(unreadCount: unread) { showsNotifications = true }
            MonarchNotificationsBell {}
            MonarchNotificationsBell(unreadCount: 250) {}
            MonarchNotificationsBell(unreadCount: 3) {}.disabled(true)
        }

        CatalogueCaption("Row")
        MonarchNotificationsBell(unreadCount: unread, variant: .row) { showsNotifications = true }
        MonarchNotificationsBell(variant: .row, title: "Updates") {}

        CatalogueCaption("Change the count")
        Stepper("Unread: \(unread)", value: $unread, in: 0...120)
            .monarchTextStyle(.body)
            .sheet(isPresented: $showsNotifications) {
                VStack(spacing: theme.spacing.space4) {
                    Text("The app presents its notifications here.").monarchTextStyle(.body)
                    MonarchButton("Mark all as read", variant: .primary) {
                        unread = 0
                        showsNotifications = false
                    }
                }
                .foregroundStyle(theme.colors.textDefault)
                .padding(theme.spacing.space6)
                .presentationDetents([.medium])
            }
    }
}
