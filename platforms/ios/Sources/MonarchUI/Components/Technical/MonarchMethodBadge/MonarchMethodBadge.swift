import SwiftUI

/// An HTTP method label, such as GET or DELETE, for endpoint lists, API
/// references and request logs.
///
/// ```swift
/// MonarchMethodBadge("GET")
/// MonarchMethodBadge("purge", tone: .danger)
/// ```
///
/// The method is shown in upper case in the code typeface. Each common method
/// has its own tone: GET is success, POST is info, PUT and PATCH are warning,
/// DELETE is danger, and every other method is neutral. The badge has a
/// minimum width, so badges in a column line up.
///
/// Accessibility: the badge is read as the method name, which is always the
/// visible text, so the colour is never the only signal. The text scales with
/// Dynamic Type.
public struct MonarchMethodBadge: View {
    private let method: String
    private let tone: MonarchTone?

    @ScaledMetric(relativeTo: .body) private var minimumWidth = MonarchMethodBadgeTokens.minimumWidth

    /// A method badge.
    ///
    /// - Parameters:
    ///   - method: The HTTP method, in any case. It is shown in upper case.
    ///   - tone: Replaces the tone chosen from the method.
    public init(_ method: String, tone: MonarchTone? = nil) {
        self.method = method
        self.tone = tone
    }

    /// The tone a method is drawn in when none is given: GET is success,
    /// POST is info, PUT and PATCH are warning, DELETE is danger, and every
    /// other method is neutral. The method is matched in any case.
    public nonisolated static func tone(for method: String) -> MonarchTone {
        MonarchMethodBadgeTokens.tone(for: method)
    }

    /// The content of the view.
    public var body: some View {
        let text = MonarchMethodBadgeTokens.text(method)
        let appearance = MonarchMethodBadgeTokens.appearance(tone: tone ?? MonarchMethodBadgeTokens.tone(for: text))
        Text(text)
            .monarchTextStyle(MonarchMethodBadgeTokens.textStyle, weight: MonarchMethodBadgeTokens.fontWeight)
            .multilineTextAlignment(.center)
            .padding(.horizontal, MonarchMethodBadgeTokens.horizontalPadding)
            .padding(.vertical, MonarchMethodBadgeTokens.verticalPadding)
            .frame(minWidth: minimumWidth)
            .monarchSurface(appearance, in: MonarchMethodBadgeTokens.shape)
    }
}

#if DEBUG
    #Preview("MonarchMethodBadge") {
        MonarchPreviewMatrix {
            VStack(alignment: .leading) {
                ForEach(["GET", "POST", "PUT", "PATCH", "DELETE", "HEAD", "OPTIONS", "purge"], id: \.self) { method in
                    MonarchMethodBadge(method)
                }
                MonarchMethodBadge("GET", tone: .info)
            }
        }
    }
#endif
