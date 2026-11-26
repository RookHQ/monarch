import MonarchTokens
import SwiftUI

/// Component tokens and rules for ``MonarchPreJoinLobby``.
enum MonarchPreJoinLobbyTokens {
    /// The gap between the parts of the lobby.
    static let stackSpacing: CGFloat = MonarchTokens.Space.space4
    /// The gap between the placeholder's symbol and text, and between avatars and text.
    static let innerSpacing: CGFloat = MonarchTokens.Space.space2
    /// Padding inside the preview around the placeholder.
    static let previewPadding: CGFloat = MonarchTokens.Space.space4
    /// The preview's width over its height when the text fits: a 4:3 camera frame.
    static let previewAspectRatio: CGFloat = 4.0 / 3.0
    /// The most avatars shown before "+N".
    static let maxVisibleParticipants = 5
    /// The role of the preview's fill while no video is shown.
    static var previewFill: MonarchColorRole { \.bgMuted }
    /// The role of the placeholder text and the participants text.
    static var muted: MonarchColorRole { \.textMuted }

    /// What the preview area shows.
    enum Preview: Equatable {
        /// The app's camera preview.
        case video
        /// The camera is off, or the call is audio only.
        case cameraOff
        /// The camera is on but its preview is not ready.
        case starting
        /// There is a problem, such as blocked access.
        case noPreview
    }

    /// The preview: a problem wins, then the camera state.
    static func preview(hasError: Bool, isCameraOn: Bool?, isCameraStarting: Bool) -> Preview {
        if hasError { return .noPreview }
        guard isCameraOn == true else { return .cameraOff }
        return isCameraStarting ? .starting : .video
    }

    /// The placeholder text of a preview state; `nil` while the video shows.
    static func placeholderText(_ preview: Preview, labels: MonarchPreJoinLobbyLabels) -> String? {
        switch preview {
        case .video: nil
        case .cameraOff: labels.cameraOff ?? MonarchPreJoinLobbyStrings.cameraOff
        case .starting: labels.startingCamera ?? MonarchPreJoinLobbyStrings.startingCamera
        case .noPreview: labels.noPreview ?? MonarchPreJoinLobbyStrings.noPreview
        }
    }

    /// The placeholder symbol of a preview state.
    static func placeholderSymbol(_ preview: Preview) -> String {
        switch preview {
        case .noPreview: "exclamationmark.triangle"
        case .starting: "video"
        case .video, .cameraOff: "video.slash"
        }
    }

    /// Whether the microphone meter shows: only while the microphone is on.
    static func showsMeter(isMicOn: Bool) -> Bool { isMicOn }

    /// The level for the meter, kept between 0 and 1.
    static func clampedLevel(_ level: Double) -> Double {
        guard level.isFinite else { return 0 }
        return min(max(level, 0), 1)
    }

    /// The participants text; `nil` when nobody is in the call.
    static func alreadyInText(_ count: Int, labels: MonarchPreJoinLobbyLabels) -> String? {
        guard count > 0 else { return nil }
        return labels.alreadyIn?(count) ?? MonarchPreJoinLobbyStrings.alreadyIn(count)
    }

    /// The join button's title: "Joining…" while joining, else the app's title or "Join now".
    static func joinTitle(isJoining: Bool, joinTitle: String?, labels: MonarchPreJoinLobbyLabels) -> String {
        if isJoining { return labels.joining ?? MonarchPreJoinLobbyStrings.joining }
        return joinTitle ?? labels.join ?? MonarchPreJoinLobbyStrings.join
    }
}
