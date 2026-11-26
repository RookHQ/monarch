import MonarchFontsPoppins
import MonarchTokens
@_spi(Experimental) import MonarchUI
import SwiftUI

/// The docs landing page's category tiles and hero cards: one small composition per category, in the
/// same story as the showcase screen (Acme Studio). The preview host shows them by id
/// (`-preview landing-tile-actions`, `-preview landing-float-toggle`) and
/// `apps/docs/scripts/capture-native-screens.sh` captures them. The Android sample draws the same
/// compositions (`LandingCompositions.kt`).
///
/// A tile is drawn in a fixed 354 x 260 point box, centred; never scaled, never clipped.
@MainActor
enum LandingCompositions {
    static let tileSize = CGSize(width: 354, height: 260)

    static func view(for id: String) -> AnyView? {
        switch id {
        case "landing-tile-actions": tile(ActionsTile())
        case "landing-tile-forms": tile(FormsTile())
        case "landing-tile-navigation": tile(NavigationTile())
        case "landing-tile-data-display": tile(DataDisplayTile())
        case "landing-tile-feedback": tile(FeedbackTile())
        case "landing-tile-overlays": tile(OverlaysTile())
        case "landing-tile-theming": tile(ThemingTile())
        case "landing-tile-intelligence": tile(IntelligenceTile())
        case "landing-float-toggle": float(ToggleFloat())
        case "landing-float-progress": float(ProgressFloat())
        case "landing-float-badges": float(BadgesFloat())
        default: nil
        }
    }

    private static func tile(_ content: some View) -> AnyView {
        AnyView(content.frame(width: tileSize.width, height: tileSize.height))
    }

    private static func float(_ content: some View) -> AnyView {
        AnyView(content.frame(width: 240).frame(maxWidth: .infinity, alignment: .leading))
    }
}

private struct ActionsTile: View {
    @Environment(\.monarchTheme) private var theme

    var body: some View {
        VStack(spacing: theme.spacing.space3) {
            HStack(spacing: theme.spacing.space2) {
                MonarchButton("Invite people", variant: .primary) {}
                MonarchButton("View rollout") {}
            }
            HStack(spacing: theme.spacing.space2) {
                MonarchButton("Cancel", variant: .ghost) {}
                MonarchButton("Remove", variant: .danger) {}
            }
            HStack(spacing: theme.spacing.space2) {
                MonarchButton("Saving", isLoading: true) {}
                MonarchButton("Archive") {}.disabled(true)
            }
        }
    }
}

private struct FormsTile: View {
    @Environment(\.monarchTheme) private var theme
    @State private var name = "Acme Studio"
    @State private var email = "billing@acme"
    @State private var workspaceID = "ws_4f2a"

    var body: some View {
        VStack(alignment: .leading, spacing: theme.spacing.space3) {
            MonarchTextField("Workspace name", text: $name)
            MonarchTextField("Billing email", text: $email, error: "Enter a complete email address.")
            MonarchTextField("Workspace ID", text: $workspaceID).disabled(true)
        }
    }
}

private struct NavigationTile: View {
    @Environment(\.monarchTheme) private var theme
    @State private var tab = "overview"

    var body: some View {
        VStack(spacing: theme.spacing.space4) {
            MonarchSegmentedControl(
                "Section",
                selection: $tab,
                items: [
                    MonarchSegmentedItem("Overview", value: "overview"),
                    MonarchSegmentedItem("Activity", value: "activity"),
                    MonarchSegmentedItem("Settings", value: "settings"),
                ]
            )
            MonarchKeyValue([
                MonarchKeyValueItem("Plan", value: "Business"),
                MonarchKeyValueItem("Seats", value: "12 of 20"),
                MonarchKeyValueItem("Region", value: "Frankfurt"),
            ])
        }
    }
}

private struct DataDisplayTile: View {
    var body: some View {
        // The chart draws its own card; its plot is 140 points high so the whole card fits the tile box.
        MonarchBarChart(
            "Active seats",
            categories: ["Jan", "Feb", "Mar", "Apr", "May", "Jun"],
            series: [MonarchBarChartSeries("seats", label: "Seats", values: [6, 7, 9, 10, 11, 12])],
            description: "Last 6 months",
            isTableToggleShown: false,
            height: 120
        )
    }
}

private struct FeedbackTile: View {
    @Environment(\.monarchTheme) private var theme

    var body: some View {
        VStack(spacing: theme.spacing.space2) {
            MonarchBanner("Release 2.4 is rolling out", message: "3 of 5 regions are done.", tone: .info)
            MonarchBanner("2 seats left", message: "Add seats before you invite more people.", tone: .warning)
            MonarchBanner("Invoice 1042 paid", message: "A receipt went to billing@acme.com.", tone: .success)
        }
    }
}

private struct OverlaysTile: View {
    @Environment(\.monarchTheme) private var theme

    var body: some View {
        ZStack(alignment: .bottom) {
            VStack {
                RecentActivityCard(items: Array(showcaseRecentActivity.prefix(2)))
                Spacer(minLength: 0)
            }
            theme.colors.overlayScrim
            VStack(alignment: .leading, spacing: theme.spacing.space3) {
                Capsule()
                    .fill(theme.colors.borderDefault)
                    .frame(width: 36, height: 4)
                    .frame(maxWidth: .infinity)
                Text("Remove Dana Novak?").monarchTextStyle(.subheading)
                Text("They lose access to Acme Studio.")
                    .monarchTextStyle(.body)
                    .foregroundStyle(theme.colors.textMuted)
                HStack(spacing: theme.spacing.space2) {
                    MonarchButton("Cancel", fullWidth: true) {}
                    MonarchButton("Remove", variant: .danger, fullWidth: true) {}
                }
                .padding(.top, theme.spacing.space1)
            }
            .padding(.horizontal, theme.spacing.space5)
            .padding(.top, theme.spacing.space2)
            .padding(.bottom, theme.spacing.space5)
            .background(
                UnevenRoundedRectangle(topLeadingRadius: theme.radius.xl, topTrailingRadius: theme.radius.xl)
                    .fill(theme.colors.bgRaised)
            )
        }
        .clipShape(RoundedRectangle(cornerRadius: theme.radius.xl))
    }
}

private struct ThemingTile: View {
    @Environment(\.monarchTheme) private var theme

    var body: some View {
        VStack(spacing: theme.spacing.space2) {
            ThemeRow(title: "Light").monarchTheme(scheme: .light, contrast: .standard, typography: .poppins)
            ThemeRow(title: "Dark").monarchTheme(scheme: .dark, contrast: .standard, typography: .poppins)
            ThemeRow(title: "High contrast").monarchTheme(scheme: .light, contrast: .more, typography: .poppins)
        }
    }

    private struct ThemeRow: View {
        let title: String
        @Environment(\.monarchTheme) private var theme

        var body: some View {
            HStack(spacing: theme.spacing.space2) {
                Text(title).monarchTextStyle(.bodyStrong)
                Spacer()
                MonarchBadge("Active", tone: .success)
                MonarchButton("Invite", variant: .primary, size: .sm) {}
            }
            .padding(.horizontal, theme.spacing.space4)
            .padding(.vertical, theme.spacing.space2)
            .background(
                RoundedRectangle(cornerRadius: theme.radius.lg).fill(theme.colors.bgSurface)
            )
            .overlay(
                RoundedRectangle(cornerRadius: theme.radius.lg).strokeBorder(theme.colors.borderSubtle)
            )
            .environment(\.colorScheme, theme.isDark ? .dark : .light)
        }
    }
}

private struct IntelligenceTile: View {
    @Environment(\.monarchTheme) private var theme
    @State private var prompt = "Summarise the Q3 report for the team"

    var body: some View {
        VStack(alignment: .leading, spacing: theme.spacing.space3) {
            MonarchAttachmentChip("Q3 report.pdf", size: 2_400_000)
            MonarchAiPromptInput("Message the assistant", text: $prompt, onSubmit: { _ in })
        }
    }
}

private struct ToggleFloat: View {
    @State private var digest = true

    var body: some View {
        MonarchCard { DigestRow(isOn: $digest) }
    }
}

private struct ProgressFloat: View {
    var body: some View {
        MonarchCard { RolloutSummary() }
    }
}

private struct BadgesFloat: View {
    @Environment(\.monarchTheme) private var theme

    var body: some View {
        MonarchCard {
            HStack(spacing: theme.spacing.space2) {
                MonarchBadge("Paid", tone: .success)
                MonarchBadge("New", tone: .info)
                MonarchBadge("Reverted", tone: .warning)
            }
        }
    }
}
