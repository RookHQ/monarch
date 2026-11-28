import MonarchUI
import SwiftUI

/// Recipe: tap a person's name to preview their profile.
struct UserHoverCardSection: CataloguePage {
    static let title = "User hover card (recipe)"

    @Environment(\.monarchTheme) private var theme
    @State private var isShown = false
    @State private var isLoading = false
    @State private var result = "Tap the name"

    var body: some View {
        CatalogueCaption("A name in a sentence")
        HStack(spacing: theme.spacing.space1) {
            Text("Assigned to").monarchTextStyle(.body)
            Button("Sam Rivera") {
                isLoading = true
                isShown = true
                Task {
                    try? await Task.sleep(for: .milliseconds(600))
                    isLoading = false
                }
            }
            .buttonStyle(.monarchLink(.inline))
            .monarchTextStyle(.body)
            .accessibilityHint("Shows a profile preview")
            .popover(isPresented: $isShown, arrowEdge: .top) {
                card
                    .presentationCompactAdaptation(.popover)
                    .presentationBackground(theme.colors.bgRaised)
            }
        }
        Text(result).monarchTextStyle(.caption)
    }

    private var card: some View {
        VStack(alignment: .leading, spacing: theme.spacing.space3) {
            HStack(spacing: theme.spacing.space3) {
                MonarchAvatar(image: nil, name: "Sam Rivera", seed: "user-42", size: .lg, status: .busy)
                VStack(alignment: .leading, spacing: theme.spacing.space0_5) {
                    Text("Sam Rivera")
                        .monarchTextStyle(.subheading)
                        .accessibilityAddTraits(.isHeader)
                    Text("@sam · Design lead").monarchTextStyle(.caption)
                    Text("In a meeting until 3 pm").monarchTextStyle(.caption)
                }
            }
            if isLoading {
                Text("Lisbon · 14:05 local time · Design team")
                    .monarchTextStyle(.caption)
                    .redacted(reason: .placeholder)
                    .accessibilityLabel("Loading profile")
            } else {
                Text("Lisbon · 14:05 local time · Design team").monarchTextStyle(.caption)
            }
            HStack(spacing: theme.spacing.space2) {
                MonarchButton("Message", systemImage: "bubble.left", variant: .secondary, size: .sm) {
                    result = "Message to Sam"
                    isShown = false
                }
                MonarchButton("View profile", variant: .ghost, size: .sm) {
                    result = "Sam's profile opened"
                    isShown = false
                }
            }
        }
        .foregroundStyle(theme.colors.textDefault)
        .padding(theme.spacing.space4)
        .accessibilityElement(children: .contain)
        .accessibilityLabel("Profile of Sam Rivera")
    }
}
