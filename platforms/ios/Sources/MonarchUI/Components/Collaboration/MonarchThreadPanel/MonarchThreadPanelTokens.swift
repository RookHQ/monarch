import MonarchTokens
import SwiftUI

/// Component tokens and rules for ``MonarchThreadPanel``.
enum MonarchThreadPanelTokens {
    /// The gap around the reply count's text.
    static let dividerSpacing: CGFloat = MonarchTokens.Space.space2
    /// Padding above and below the reply count divider.
    static let dividerPadding: CGFloat = MonarchTokens.Space.space2
    /// Padding beside the divider, the states and the bottom bar, matching the message rows.
    static let horizontalPadding: CGFloat = MonarchTokens.Space.space4
    /// Padding above and below a state and the notice.
    static let statePadding: CGFloat = MonarchTokens.Space.space4
    /// The gap between the typing indicator and the reply box.
    static let bottomSpacing: CGFloat = MonarchTokens.Space.space1
    /// Padding above and below the bottom bar.
    static let bottomPadding: CGFloat = MonarchTokens.Space.space2
    /// The rows of the loading placeholder.
    static let placeholderRows = 3
    /// The symbol of the follow button while not following.
    static let followSymbol = "bell"
    /// The symbol of the follow button while following.
    static let followingSymbol = "bell"
    /// The role of the reply count, the notice and the deleted text.
    static var muted: MonarchColorRole { \.textMuted }
    /// The role of the divider's lines and the bottom bar's top border.
    static var line: MonarchColorRole { \.borderSubtle }
    /// The role of a load failure without a retry.
    static var failure: MonarchColorRole { \.dangerStrong }

    /// The reply count shown in the divider: the app's count, or the loaded replies.
    static func count(replyCount: Int?, loaded: Int) -> Int {
        max(0, replyCount ?? loaded)
    }

    /// "3 replies".
    static func replyCountText(_ count: Int, labels: MonarchThreadPanelLabels) -> String {
        labels.replyCount?(count) ?? MonarchThreadPanelStrings.replyCount(count)
    }

    /// The strings of the list of replies: the app's, named "Replies" unless it names the list itself.
    static func listLabels(_ labels: MonarchThreadPanelLabels) -> MonarchVirtualMessageListLabels {
        var list = labels.list ?? MonarchVirtualMessageListLabels()
        if list.list == nil { list.list = labels.replies ?? MonarchThreadPanelStrings.replies }
        return list
    }

    /// The SF Symbol of the Close button in the toolbar.
    static let closeSymbol = "xmark"

    /// The follow button's title and symbol.
    static func followButton(isFollowing: Bool, labels: MonarchThreadPanelLabels) -> (title: String, symbol: String) {
        isFollowing
            ? (labels.following ?? MonarchThreadPanelStrings.following, followingSymbol)
            : (labels.follow ?? MonarchThreadPanelStrings.follow, followSymbol)
    }

    /// Whether the follow button is shown: only for a thread that is ready and with the app's action.
    static func showsFollow(status: MonarchThreadPanelStatus, hasAction: Bool) -> Bool {
        hasAction && status == .ready
    }

    /// Announced after the follow state changed (`isFollowing`) or failed to change.
    static func followNotice(isFollowing: Bool, didFail: Bool, labels: MonarchThreadPanelLabels) -> String {
        if didFail { return labels.followFailed ?? MonarchThreadPanelStrings.followFailed }
        return isFollowing
            ? labels.followedNotice ?? MonarchThreadPanelStrings.followedNotice
            : labels.unfollowedNotice ?? MonarchThreadPanelStrings.unfollowedNotice
    }

    /// What starts the seen timer: the count, and whether the replies are on screen in an active scene.
    struct Seen: Hashable {
        /// The reply count that is reported.
        let count: Int
        /// Whether the timer runs.
        let isArmed: Bool
    }

    /// The seen timer runs while the thread is ready, the app wants it and the scene is active.
    static func seen(count: Int, status: MonarchThreadPanelStatus, isActive: Bool, hasAction: Bool) -> Seen {
        Seen(count: count, isArmed: hasAction && status == .ready && isActive)
    }
}
