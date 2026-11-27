import Foundation
import MonarchTokens
import SwiftUI

/// Component tokens and pure logic for ``MonarchCallStage``.
enum MonarchCallStageTokens {
    /// The padding of the full stage.
    static let stagePadding: CGFloat = MonarchTokens.Space.space3
    /// The padding of the docked and picture in picture surfaces.
    static let compactPadding: CGFloat = MonarchTokens.Space.space2
    /// The gap between the header, notices, tiles and controls, and between tiles.
    static let spacing: CGFloat = MonarchTokens.Space.space2
    /// The gap between the parts of the header line.
    static let metaSpacing: CGFloat = MonarchTokens.Space.space1_5
    /// The corner radius of the stage.
    static let cornerRadius: CGFloat = MonarchTokens.Radius.xl
    /// The width of a filmstrip tile: 160 points.
    static let stripTileWidth: CGFloat = MonarchTokens.Space.space20 * 2
    /// The most grid columns on a compact surface or a narrow screen.
    static let compactColumns = 2
    /// The SF Symbols of the header buttons and notices.
    static let pictureInPictureSymbol = "pip.enter"
    static let shrinkSymbol = "arrow.down.right.and.arrow.up.left"
    static let expandSymbol = "arrow.up.left.and.arrow.down.right"
    static let minimizeSymbol = "chevron.down"
    static let offlineSymbol = "wifi.slash"
    static let mutedSymbol = "mic.slash"

    /// The background of the stage.
    static var background: MonarchColorRole { \.bgSubtle }
    /// The colour of the title.
    static var titleRole: MonarchColorRole { \.textDefault }
    /// The colour of the timer and the head count.
    static var metaRole: MonarchColorRole { \.textMuted }
    /// The colour of the header buttons.
    static var toolRole: MonarchColorRole { \.textMuted }

    /// A button in the header.
    enum Tool: Hashable, CaseIterable {
        case pictureInPicture
        case shrink
        case expand
        case minimize
    }

    /// The header buttons for a surface and the callbacks the app gave:
    /// picture in picture except inside it, Shrink on the stage, Expand and
    /// Minimize when docked.
    static func tools(
        surface: MonarchCallStageSurface,
        hasPictureInPicture: Bool,
        hasSurfaceChange: Bool,
        hasMinimize: Bool
    ) -> [Tool] {
        var tools: [Tool] = []
        if hasPictureInPicture && surface != .pip { tools.append(.pictureInPicture) }
        if hasSurfaceChange && surface == .stage { tools.append(.shrink) }
        if hasSurfaceChange && surface == .docked { tools.append(.expand) }
        if hasMinimize && surface == .docked { tools.append(.minimize) }
        return tools
    }

    /// Whether the surface is compact: docked or picture in picture.
    static func isCompact(_ surface: MonarchCallStageSurface) -> Bool {
        surface != .stage
    }

    /// The padding of the stage on a surface.
    static func padding(_ surface: MonarchCallStageSurface) -> CGFloat {
        isCompact(surface) ? compactPadding : stagePadding
    }

    /// The grid columns shown: the layout's, at most two on a compact
    /// surface or a narrow screen, at least one.
    static func columns(_ columns: Int, surface: MonarchCallStageSurface, isNarrow: Bool) -> Int {
        let capped = isCompact(surface) || isNarrow ? min(columns, compactColumns) : columns
        return max(1, capped)
    }

    /// Whether the filmstrip stands beside the main tile: on the full stage of a wide screen.
    static func stripIsBeside(surface: MonarchCallStageSurface, isNarrow: Bool) -> Bool {
        surface == .stage && !isNarrow
    }

    /// Whether tiles show their Pin and full screen buttons: only on the full stage.
    static func showsTileActions(_ surface: MonarchCallStageSurface) -> Bool {
        !isCompact(surface)
    }

    /// The layout shown: the app's, or the automatic one from the participants.
    static func layout(
        _ given: MonarchCallStageLayout?,
        participants: [MonarchCallStageParticipant],
        mode: MonarchCallControlsLayout,
        pinnedID: String?,
        activeSpeakerID: String?
    ) -> MonarchCallStageLayout {
        if let given { return given }
        return .automatic(
            selfID: participants.first(where: \.isSelf)?.id,
            ids: participants.map(\.id),
            mode: mode,
            pinnedID: pinnedID,
            sharerID: participants.first(where: \.isScreen)?.id,
            activeSpeakerID: activeSpeakerID
        )
    }

    /// The head count line: "Waiting for others…" while you are alone, else "3 in call".
    static func headCount(
        _ participants: [MonarchCallStageParticipant],
        locale: Locale,
        labels: MonarchCallStageLabels
    ) -> String {
        guard participants.contains(where: { !$0.isSelf }) else {
            return labels.waiting ?? MonarchCallStageStrings.waiting
        }
        let count = participants.count
        return labels.inCall?(count) ?? MonarchCallStageStrings.inCall(count.formatted(.number.locale(locale)))
    }

    /// What is announced when the stage changes, or `nil` for nothing: a
    /// new head count, losing the connection, a new error, starting to talk
    /// while muted. The error and connection notices come first.
    static func announcement(from old: Snapshot, to new: Snapshot) -> String? {
        if let error = new.error, error != old.error { return error }
        if new.reconnecting != nil && old.reconnecting == nil { return new.reconnecting }
        if new.mutedHint != nil && old.mutedHint == nil { return new.mutedHint }
        if new.headCount != old.headCount { return new.headCount }
        return nil
    }

    /// What the stage says, compared to decide what to announce.
    struct Snapshot: Equatable {
        /// The head count line.
        let headCount: String
        /// The reconnecting notice, when shown.
        let reconnecting: String?
        /// The error, when shown.
        let error: String?
        /// The muted hint, when shown.
        let mutedHint: String?
    }

    /// The label of a header button.
    static func toolLabel(_ tool: Tool, labels: MonarchCallStageLabels) -> String {
        switch tool {
        case .pictureInPicture: labels.pictureInPicture ?? MonarchCallStageStrings.pictureInPicture
        case .shrink: labels.shrink ?? MonarchCallStageStrings.shrink
        case .expand: labels.expand ?? MonarchCallStageStrings.expand
        case .minimize: labels.minimize ?? MonarchCallStageStrings.minimize
        }
    }

    /// The SF Symbol of a header button.
    static func toolSymbol(_ tool: Tool) -> String {
        switch tool {
        case .pictureInPicture: pictureInPictureSymbol
        case .shrink: shrinkSymbol
        case .expand: expandSymbol
        case .minimize: minimizeSymbol
        }
    }
}
