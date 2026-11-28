import MonarchTokens
import MonarchUI
import SwiftUI

/// The docs landing page's showcase: the Overview screen of the workspace Acme Studio, built from
/// Monarch components. A tab bar on iPhone; a sidebar beside the content on iPad (regular width).
/// The Android sample draws the same screen with the same data (`ShowcaseScreen.kt`).
///
/// The catalogue opens it when launched with `-MonarchShowcase YES`; `apps/docs/scripts/capture-native-screens.sh`
/// takes the docs screenshots that way.
struct ShowcaseApp: View {
    @Environment(\.horizontalSizeClass) private var sizeClass
    @State private var selection: ShowcaseDestination? = .overview

    var body: some View {
        if sizeClass == .regular {
            NavigationSplitView {
                List(ShowcaseDestination.allCases, selection: $selection) { destination in
                    Label(destination.title, systemImage: destination.systemImage).tag(destination)
                }
                .navigationTitle("Acme Studio")
            } detail: {
                NavigationStack { OverviewScreen(maxWidth: 640) }
            }
            .navigationSplitViewStyle(.balanced)
        } else {
            TabView(selection: $selection) {
                ForEach(ShowcaseDestination.allCases) { destination in
                    NavigationStack { OverviewScreen(maxWidth: nil) }
                        .tabItem { Label(destination.title, systemImage: destination.systemImage) }
                        .tag(Optional(destination))
                }
            }
        }
    }
}

/// The destinations of the showcase's navigation. Every one shows the Overview: it is a picture, not an app.
enum ShowcaseDestination: String, CaseIterable, Identifiable, Hashable {
    case overview, activity, settings

    var id: Self { self }
    var title: String {
        switch self {
        case .overview: "Overview"
        case .activity: "Activity"
        case .settings: "Settings"
        }
    }

    var systemImage: String {
        switch self {
        case .overview: "house"
        case .activity: "bell"
        case .settings: "gearshape"
        }
    }
}

/// The people on the release team, in the order the avatar group shows them.
let showcaseReleaseTeam = ["Ana Silva", "Ben Okafor", "Chen Wei", "Dana Novak", "Eli Park"]

/// One row of the Recent activity card.
struct ShowcaseActivity: Identifiable {
    let title: String
    let detail: String
    let status: String
    let tone: MonarchTone
    var id: String { title }
}

let showcaseRecentActivity = [
    ShowcaseActivity(title: "Invoice 1042 paid", detail: "Ana Silva · 2 h ago", status: "Paid", tone: .success),
    ShowcaseActivity(title: "Chen Wei joined", detail: "Invited by Ben Okafor · Yesterday", status: "New", tone: .info),
    ShowcaseActivity(
        title: "Release 2.3 rolled back",
        detail: "Dana Novak · Monday",
        status: "Reverted",
        tone: .warning
    ),
]

/// The Overview screen: workspace, settings, rollout, recent activity and the main action.
struct OverviewScreen: View {
    let maxWidth: CGFloat?
    @Environment(\.monarchTheme) private var theme
    @State private var name = "Acme Studio"
    @State private var region = "Europe (Frankfurt)"
    @State private var digest = true

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: theme.spacing.space6) {
                WorkspaceHeaderCard()
                ShowcaseSection("Workspace") {
                    MonarchCard {
                        VStack(alignment: .leading, spacing: theme.spacing.space4) {
                            MonarchTextField("Workspace name", text: $name)
                            MonarchSelectField(
                                "Region",
                                selection: $region,
                                options: [
                                    MonarchSelectOption("Europe (Frankfurt)", value: "Europe (Frankfurt)"),
                                    MonarchSelectOption("United States (Virginia)", value: "United States (Virginia)"),
                                ]
                            )
                            DigestRow(isOn: $digest)
                        }
                    }
                }
                MonarchCard {
                    VStack(alignment: .leading, spacing: theme.spacing.space4) {
                        RolloutSummary()
                        HStack {
                            MonarchAvatarGroup(
                                "Release team",
                                showcaseReleaseTeam,
                                id: \.self,
                                maxVisible: 3,
                                size: .sm
                            ) {
                                MonarchAvatar(image: nil, name: $0, size: .sm)
                            }
                            Spacer()
                            MonarchButton("View rollout", size: .sm) {}
                        }
                    }
                }
                ShowcaseSection("Recent activity") { RecentActivityCard() }
                MonarchButton("Invite people", variant: .primary, fullWidth: true) {}
            }
            .padding(.horizontal, theme.spacing.space4)
            .padding(.bottom, theme.spacing.space6)
            .frame(maxWidth: maxWidth ?? .infinity)
            .frame(maxWidth: .infinity)
        }
        .background(theme.colors.bgCanvas)
        .navigationTitle("Overview")
    }
}

/// A section: a heading in the muted label style above its content.
struct ShowcaseSection<Content: View>: View {
    let title: String
    @ViewBuilder let content: Content
    @Environment(\.monarchTheme) private var theme

    init(_ title: String, @ViewBuilder content: () -> Content) {
        self.title = title
        self.content = content()
    }

    var body: some View {
        VStack(alignment: .leading, spacing: theme.spacing.space2) {
            Text(title)
                .monarchTextStyle(.label)
                .foregroundStyle(theme.colors.textMuted)
                .accessibilityAddTraits(.isHeader)
            content
        }
    }
}

/// Acme Studio, its plan and its status.
struct WorkspaceHeaderCard: View {
    @Environment(\.monarchTheme) private var theme

    var body: some View {
        MonarchCard {
            HStack(spacing: theme.spacing.space3) {
                MonarchAvatar(image: nil, name: "Acme Studio", shape: .square, size: .md, isDecorative: true)
                VStack(alignment: .leading, spacing: 2) {
                    Text("Acme Studio").monarchTextStyle(.subheading)
                    Text("Business plan · 12 seats")
                        .monarchTextStyle(.caption)
                        .foregroundStyle(theme.colors.textMuted)
                }
                Spacer(minLength: theme.spacing.space2)
                MonarchBadge("Active", tone: .success)
            }
        }
    }
}

/// The Weekly digest setting, shared by the screen and the toggle float.
struct DigestRow: View {
    @Binding var isOn: Bool

    var body: some View {
        MonarchFormField("Weekly digest", helper: "Sent on Monday mornings.", layout: .row) {
            Toggle("Weekly digest", isOn: $isOn).labelsHidden()
        }
    }
}

/// Release 2.4 with its status and progress, shared by the screen and the progress float.
struct RolloutSummary: View {
    @Environment(\.monarchTheme) private var theme

    var body: some View {
        VStack(alignment: .leading, spacing: theme.spacing.space4) {
            HStack {
                Text("Release 2.4").monarchTextStyle(.subheading)
                Spacer()
                MonarchBadge("Rolling out", tone: .info)
            }
            ProgressView("3 of 5 regions", value: 0.6)
                .progressViewStyle(.monarchLinear)
        }
    }
}

/// The Recent activity card: list rows flush with the card's edges.
struct RecentActivityCard: View {
    var items: [ShowcaseActivity] = showcaseRecentActivity

    var body: some View {
        MonarchCard(padding: .flush) {
            VStack(spacing: 0) {
                ForEach(items) { item in
                    MonarchListRow(item.title, subtitle: item.detail, showsDivider: item.id != items.last?.id) {
                        MonarchBadge(item.status, tone: item.tone)
                    }
                }
            }
        }
    }
}
