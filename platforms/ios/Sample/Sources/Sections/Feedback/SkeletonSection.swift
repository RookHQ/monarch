import MonarchUI
import SwiftUI

/// Native adoption: the system placeholder redaction on the real layout.
struct SkeletonSection: CataloguePage {
    static let title = "Skeleton (redacted placeholder)"

    @Environment(\.monarchTheme) private var theme
    @State private var isLoading = true

    var body: some View {
        Toggle("Loading", isOn: $isLoading)
            .monarchTextStyle(.body)
        MonarchCard {
            HStack(spacing: theme.spacing.space3) {
                MonarchIcon(systemName: "person.crop.circle.fill", size: .lg)
                    .foregroundStyle(theme.colors.textMuted)
                VStack(alignment: .leading, spacing: theme.spacing.space1) {
                    Text("Ada Lovelace").monarchTextStyle(.bodyStrong)
                    Text("Joined in March, 12 open tickets").monarchTextStyle(.caption)
                }
                .foregroundStyle(theme.colors.textDefault)
            }
            MonarchBadge("Active", tone: .success)
                .unredacted()
        }
        .redacted(reason: isLoading ? .placeholder : [])
        .accessibilityElement(children: isLoading ? .ignore : .contain)
        .accessibilityLabel(isLoading ? "Loading contact" : "")
    }
}
