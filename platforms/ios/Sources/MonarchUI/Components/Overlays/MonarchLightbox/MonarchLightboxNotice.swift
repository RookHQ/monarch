import SwiftUI

/// The text shown instead of media that has no source or did not load.
struct MonarchLightboxNotice: View {
    let text: String

    @Environment(\.monarchTheme) private var theme

    var body: some View {
        Text(text)
            .monarchTextStyle(.body)
            .multilineTextAlignment(.center)
            .foregroundStyle(theme.colors.textMuted)
            .padding(MonarchLightboxTokens.noticePadding)
            .background(theme.colors[role: MonarchLightboxTokens.raised], in: MonarchLightboxTokens.mediaShape)
            .frame(maxWidth: .infinity, maxHeight: .infinity)
    }
}
