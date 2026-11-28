import SwiftUI

/// A floating chat launcher with an unread count. A tap opens the chat in a sheet that stops at half height, so
/// the screen behind stays visible and usable, and can be pulled to full height.
///
/// ```swift
/// HostScreen()
///     .overlay(alignment: .bottomTrailing) {
///         MonarchChatDock(isPresented: $showsChat, unread: unread) {
///             ChatInbox()
///         }
///         .padding()
///     }
/// ```
///
/// The chat is the app's own view, such as screens built from the collaboration components or the Rook Chat
/// app's shared view. It keeps its state in the app's model, because a sheet's view is rebuilt each time it opens.
///
/// Accessibility: the launcher is a button named "Rook Chat" (or `title`), with the value "3 unread"; the badge
/// is not read on its own. The sheet has the system's drag indicator and closes with the standard dismiss
/// gesture or the Escape key.
public struct MonarchChatDock<Chat: View>: View {
    private let isPresented: Binding<Bool>
    private let unread: Int
    private let title: String?
    private let unreadLabel: String?
    private let chat: Chat

    @Environment(\.monarchTheme) private var theme
    @ScaledMetric(relativeTo: .body) private var scale: CGFloat = 1

    /// A chat dock.
    ///
    /// - Parameters:
    ///   - isPresented: Whether the chat is open.
    ///   - unread: The number of unread conversations or messages, shown as a badge on the launcher.
    ///   - title: Replaces the built-in name "Rook Chat".
    ///   - unreadLabel: Replaces the built-in spoken value "%d unread"; `%d` stands for the number.
    ///   - chat: The chat.
    public init(
        isPresented: Binding<Bool>,
        unread: Int = 0,
        title: String? = nil,
        unreadLabel: String? = nil,
        @ViewBuilder chat: () -> Chat
    ) {
        self.isPresented = isPresented
        self.unread = unread
        self.title = title
        self.unreadLabel = unreadLabel
        self.chat = chat()
    }

    /// The content of the view.
    public var body: some View {
        Button {
            isPresented.wrappedValue.toggle()
        } label: {
            MonarchIcon(systemName: MonarchChatDockTokens.symbol, size: .lg)
                .foregroundStyle(theme.colors[role: MonarchChatDockTokens.launcherForeground])
                .frame(
                    width: MonarchChatDockTokens.launcherSize * scale,
                    height: MonarchChatDockTokens.launcherSize * scale
                )
                .monarchElevation(
                    MonarchChatDockTokens.elevation,
                    fill: MonarchChatDockTokens.launcherFill,
                    in: RoundedRectangle(cornerRadius: MonarchChatDockTokens.launcherRadius, style: .continuous)
                )
                .overlay(alignment: .topTrailing) { badge }
        }
        .buttonStyle(.plain)
        .accessibilityLabel(title ?? MonarchChatDockStrings.label)
        .accessibilityValue(MonarchChatDockTokens.accessibilityValue(unread: unread, format: unreadLabel))
        .sheet(isPresented: isPresented) {
            chat
                .presentationDetents([.medium, .large])
                .presentationDragIndicator(.visible)
                .presentationBackgroundInteraction(.enabled(upThrough: .medium))
                .presentationBackground(theme.colors.bgSurface)
        }
    }

    @ViewBuilder private var badge: some View {
        if let text = MonarchChatDockTokens.badgeText(unread) {
            Text(verbatim: text)
                .monarchTextStyle(.caption, weight: .semibold)
                .monospacedDigit()
                .foregroundStyle(theme.colors[role: MonarchChatDockTokens.badgeForeground])
                .padding(.horizontal, MonarchChatDockTokens.badgeHorizontalPadding)
                .frame(
                    minWidth: MonarchChatDockTokens.badgeMinimumSize,
                    minHeight: MonarchChatDockTokens.badgeMinimumSize
                )
                .background(theme.colors[role: MonarchChatDockTokens.badgeFill], in: Capsule())
                .padding(.trailing, -MonarchChatDockTokens.badgeOffset)
                .padding(.top, -MonarchChatDockTokens.badgeOffset)
                .accessibilityHidden(true)
        }
    }
}

#if DEBUG
    #Preview("MonarchChatDock") {
        MonarchPreviewMatrix {
            MonarchChatDock(isPresented: .constant(false), unread: 3) {
                Text("Chat")
            }
        }
    }
#endif
