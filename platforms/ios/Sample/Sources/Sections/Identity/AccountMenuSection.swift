import MonarchUI
import SwiftUI

/// Recipe: the signed-in person's menu, a system Menu with a Monarch avatar as its button.
struct AccountMenuSection: CataloguePage {
    static let title = "Account menu (recipe)"

    @Environment(\.monarchTheme) private var theme
    @State private var workspace = "acme"
    @State private var scheme = MonarchSchemePreference.system
    @State private var last = "Nothing chosen yet."

    var body: some View {
        HStack {
            Text("Acme Inc.").monarchTextStyle(.subheading)
            Spacer()
            Menu {
                Section("Signed in as priya@example.com") {
                    Button("Profile", systemImage: "person.crop.circle") { last = "Profile" }
                    Button("Settings", systemImage: "gearshape") { last = "Settings" }
                }
                Picker("Switch workspace", selection: $workspace) {
                    Text("Acme Inc.").tag("acme")
                    Text("Globex").tag("globex")
                    Text("Initech").tag("initech")
                }
                .pickerStyle(.menu)
                Picker("Theme", selection: $scheme) {
                    Label("System", systemImage: "circle.lefthalf.filled").tag(MonarchSchemePreference.system)
                    Label("Light", systemImage: "sun.max").tag(MonarchSchemePreference.light)
                    Label("Dark", systemImage: "moon").tag(MonarchSchemePreference.dark)
                }
                .pickerStyle(.menu)
                Divider()
                Button("Sign out", systemImage: "rectangle.portrait.and.arrow.right", role: .destructive) {
                    last = "Sign out"
                }
            } label: {
                MonarchAvatar(image: nil, name: "Priya Raman", size: .sm, status: .online, isDecorative: true)
                    .monarchMinimumHitTarget()
            }
            .accessibilityLabel("Account menu for Priya Raman")
        }
        Text("Workspace: \(workspace). Theme: \(MonarchThemeToggle.storageValue(for: scheme)). Last action: \(last)")
            .monarchTextStyle(.caption)

        CatalogueCaption("Avatar and name as the button")
        Menu {
            Section("Signed in as priya@example.com") {
                Button("Profile", systemImage: "person.crop.circle") { last = "Profile" }
                Button("Sign out", systemImage: "rectangle.portrait.and.arrow.right", role: .destructive) {
                    last = "Sign out"
                }
            }
        } label: {
            HStack(spacing: theme.spacing.space2) {
                MonarchAvatar(image: nil, name: "Priya Raman", size: .sm, isDecorative: true)
                Text("Priya Raman")
                    .monarchTextStyle(.label)
                    .foregroundStyle(theme.colors.textDefault)
            }
            .monarchMinimumHitTarget()
        }
        .accessibilityLabel("Account menu for Priya Raman")
    }
}
