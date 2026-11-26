import Foundation
import MonarchTokens
import SwiftUI

/// Component tokens and logic for ``MonarchAiComputerView``.
enum MonarchAiComputerViewTokens {
    /// The gap between the sections of the view.
    static let spacing: CGFloat = MonarchTokens.Space.space4
    /// The gap inside a section.
    static let innerSpacing: CGFloat = MonarchTokens.Space.space2
    /// The gap between the lines of a log row.
    static let rowLineSpacing: CGFloat = MonarchTokens.Space.space0_5
    /// The padding inside a log row.
    static let rowPadding: CGFloat = MonarchTokens.Space.space2
    /// The corner radius of the screenshot box and of the log rows.
    static let cornerRadius: CGFloat = MonarchTokens.Radius.lg
    /// The diameter of the marker on an action's point.
    static let markerSize: CGFloat = MonarchTokens.Space.space6
    /// The stroke of the marker of the latest action.
    static let markerWidth: CGFloat = MonarchTokens.Space.space0_5
    /// The stroke of the marker of the selected action.
    static let selectedMarkerWidth: CGFloat = MonarchTokens.Space.space1
    /// How much wider than a marker its light halo is, so it reads on any screenshot.
    static let haloWidth: CGFloat = MonarchTokens.Space.space0_5
    /// The side of the agent pointer glyph.
    static let pointerSize: CGFloat = MonarchTokens.Space.space5
    /// The aspect ratio of the box before the screenshot size is known (16:10).
    static let placeholderAspectRatio: CGFloat = 1.6
    /// How many action ids are remembered for announcements before they are pruned.
    static let maxRemembered = 2000

    /// The SF Symbol of the agent pointer.
    static let pointerSymbol = "cursorarrow"
    /// The SF Symbol of the zoom button.
    static let zoomSymbol = "arrow.up.left.and.arrow.down.right"
    /// The SF Symbol of a selected log row.
    static let selectedSymbol = "checkmark"
    /// The SF Symbol of a hidden screenshot.
    static let hiddenSymbol = "eye.slash"
    /// The SF Symbol of the take over button.
    static let takeOverSymbol = "person"
    /// The SF Symbol of the hand back button.
    static let handBackSymbol = "arrow.uturn.backward"
    /// The SF Symbol of the pause button.
    static let pauseSymbol = "pause"
    /// The SF Symbol of the resume button.
    static let resumeSymbol = "play"
    /// The SF Symbol of the stop button.
    static let stopSymbol = "stop"

    // MARK: - Status

    /// The session is over.
    static func isFinished(_ status: MonarchAiComputerViewStatus) -> Bool {
        [.done, .failed, .stopped].contains(status)
    }

    /// The agent is acting.
    static func isAgentActing(_ status: MonarchAiComputerViewStatus) -> Bool {
        [.starting, .working].contains(status)
    }

    /// The agent holds control, acting or not.
    static func isAgentHolding(_ status: MonarchAiComputerViewStatus) -> Bool {
        [.starting, .working, .paused, .waitingForYou].contains(status)
    }

    /// The SF Symbol of a status; nil for starting, which shows a spinner.
    static func symbol(_ status: MonarchAiComputerViewStatus) -> String? {
        switch status {
        case .starting: nil
        case .working: "cpu"
        case .paused: "pause.circle"
        case .waitingForYou: "exclamationmark.triangle"
        case .takenOver: "person.circle"
        case .done: "checkmark.circle"
        case .failed: "xmark.octagon"
        case .stopped: "stop.circle"
        }
    }

    /// The tone of a status icon.
    static func tone(_ status: MonarchAiComputerViewStatus) -> MonarchTone {
        switch status {
        case .starting, .working: .ai
        case .takenOver, .stopped: .neutral
        case .paused, .waitingForYou: .warning
        case .done: .success
        case .failed: .danger
        }
    }

    /// The control banner: who is in control, or nil once finished.
    static func banner(
        _ status: MonarchAiComputerViewStatus,
        surface: MonarchAiComputerViewSurface,
        labels: MonarchAiComputerViewLabels
    ) -> String? {
        if status == .takenOver {
            return labels.youInControl?(surface) ?? MonarchAiComputerViewStrings.youInControl(surface)
        }
        if isAgentHolding(status) {
            return labels.agentInControl?(surface) ?? MonarchAiComputerViewStrings.agentInControl(surface)
        }
        return nil
    }

    /// Which control buttons show.
    struct Controls: Equatable {
        var canPause = false
        var canResume = false
        var canTakeOver = false
        var canHandBack = false
        var canStop = false

        /// Whether any button shows.
        var isEmpty: Bool { !(canPause || canResume || canTakeOver || canHandBack || canStop) }
    }

    /// The buttons for a status and the handlers the app gave.
    static func controls(
        _ status: MonarchAiComputerViewStatus,
        hasPause: Bool,
        hasTakeOver: Bool,
        hasHandBack: Bool,
        hasStop: Bool
    ) -> Controls {
        Controls(
            canPause: hasPause && isAgentActing(status),
            canResume: hasPause && status == .paused,
            canTakeOver: hasTakeOver && isAgentHolding(status),
            canHandBack: hasHandBack && status == .takenOver,
            canStop: hasStop && !isFinished(status)
        )
    }

    /// What is announced when the status changes from `before` to `after`, or nil when nothing changed.
    static func statusAnnouncement(
        from before: MonarchAiComputerViewStatus,
        to after: MonarchAiComputerViewStatus,
        surface: MonarchAiComputerViewSurface,
        labels: MonarchAiComputerViewLabels
    ) -> String? {
        guard before != after else { return nil }
        if after == .takenOver {
            return labels.announceTakenOver?(surface) ?? MonarchAiComputerViewStrings.announceTakenOver(surface)
        }
        if before == .takenOver, isAgentActing(after) {
            return labels.announceReleased?(surface) ?? MonarchAiComputerViewStrings.announceReleased(surface)
        }
        let name = statusName(after, labels: labels)
        return labels.announceStatus?(name) ?? MonarchAiComputerViewStrings.announceStatus(name)
    }

    /// The name of a status.
    static func statusName(_ status: MonarchAiComputerViewStatus, labels: MonarchAiComputerViewLabels) -> String {
        labels.status?(status) ?? MonarchAiComputerViewStrings.status(status)
    }

    // MARK: - Actions

    /// The label of an action as it may be shown: a sensitive one is never named.
    static func actionLabel(_ action: MonarchAiComputerAction, labels: MonarchAiComputerViewLabels) -> String {
        action.isSensitive ? (labels.sensitiveLabel ?? MonarchAiComputerViewStrings.sensitiveLabel) : action.label
    }

    /// What the log may show under an action's label.
    struct Detail: Equatable {
        /// The detail, or nil when there is none or it is withheld.
        let text: String?
        /// A detail exists (or the action is sensitive) and is withheld.
        let isHidden: Bool
    }

    /// A sensitive action never shows its detail; typed text shows only with `showsTypedText`.
    static func detail(_ action: MonarchAiComputerAction, showsTypedText: Bool) -> Detail {
        if action.isSensitive { return Detail(text: nil, isHidden: true) }
        guard let detail = action.detail, !detail.isEmpty else { return Detail(text: nil, isHidden: false) }
        if action.kind == .type, !showsTypedText { return Detail(text: nil, isHidden: true) }
        return Detail(text: detail, isHidden: false)
    }

    /// The SF Symbol of an action kind.
    static func symbol(_ kind: MonarchAiComputerActionKind) -> String {
        switch kind {
        case .click: "cursorarrow.click"
        case .type: "keyboard"
        case .scroll: "arrow.up.and.down"
        case .navigate: "globe"
        case .key: "character.textbox"
        case .wait: "clock"
        case .read: "eye"
        case .other: "circle.fill"
        }
    }

    /// The spoken name of a log row: "Click: Clicked Sign in".
    static func rowLabel(_ action: MonarchAiComputerAction, labels: MonarchAiComputerViewLabels) -> String {
        let kind = labels.kind?(action.kind) ?? MonarchAiComputerViewStrings.kind(action.kind)
        return "\(kind): \(actionLabel(action, labels: labels))"
    }

    /// The spoken value of a log row: the detail (or "Hidden") and the time.
    static func rowValue(
        _ action: MonarchAiComputerAction,
        showsTypedText: Bool,
        time: String,
        labels: MonarchAiComputerViewLabels
    ) -> String {
        let detail = detail(action, showsTypedText: showsTypedText)
        var parts: [String] = []
        if let text = detail.text {
            parts.append(text)
        } else if detail.isHidden {
            parts.append(labels.hidden ?? MonarchAiComputerViewStrings.hidden)
        }
        parts.append(time)
        return parts.joined(separator: ", ")
    }

    /// The time of an action or a screenshot, with seconds, in the given locale and time zone.
    static func time(_ date: Date, locale: Locale, timeZone: TimeZone) -> String {
        var style = Date.FormatStyle(date: .omitted, time: .standard)
        style.locale = locale
        style.timeZone = timeZone
        return date.formatted(style)
    }

    /// How many of the oldest actions are behind "Show earlier".
    static func hiddenBefore(total: Int, visible: Int, showsAll: Bool) -> Int {
        showsAll ? 0 : max(0, total - max(1, visible))
    }

    /// The newest action that has a point, which the frame marks.
    static func lastPointAction(_ actions: [MonarchAiComputerAction]) -> MonarchAiComputerAction? {
        actions.last { $0.point != nil }
    }

    /// The actions that are not in `known`, in order.
    static func freshActions(_ actions: [MonarchAiComputerAction], known: Set<String>) -> [MonarchAiComputerAction] {
        actions.filter { !known.contains($0.id) }
    }

    /// What is announced for new actions: the label alone for one, the count and the latest for more.
    static func actionAnnouncement(count: Int, latest: String, labels: MonarchAiComputerViewLabels) -> String? {
        guard count > 0 else { return nil }
        if count == 1 { return latest }
        return labels.announceActions?(count, latest) ?? MonarchAiComputerViewStrings.announceActions(count, latest)
    }

    // MARK: - Frame

    /// What the screenshot box shows.
    enum FrameState: Equatable {
        /// No screenshot yet.
        case none
        /// The screenshot may show private information.
        case hidden
        /// The screenshot address is not allowed.
        case unavailable
        /// The screenshot shows.
        case shown
    }

    /// The state of the box. A sensitive screenshot, or one taken right after a
    /// sensitive action, is hidden. Only http and https addresses load.
    static func frameState(_ frame: MonarchAiComputerFrame?, latest: MonarchAiComputerAction?) -> FrameState {
        guard let frame else { return .none }
        if frame.isSensitive || latest?.isSensitive == true { return .hidden }
        if frame.image != nil { return .shown }
        return isSafe(frame.url) ? .shown : .unavailable
    }

    /// Whether a screenshot address may be loaded: http or https only.
    static func isSafe(_ url: URL?) -> Bool {
        guard let scheme = url?.scheme?.lowercased() else { return false }
        return scheme == "http" || scheme == "https"
    }

    /// The text in the box when no screenshot shows.
    static func placeholder(_ state: FrameState, labels: MonarchAiComputerViewLabels) -> String? {
        switch state {
        case .shown: nil
        case .none: labels.noFrame ?? MonarchAiComputerViewStrings.noFrame
        case .hidden: labels.frameHidden ?? MonarchAiComputerViewStrings.frameHidden
        case .unavailable: labels.frameUnavailable ?? MonarchAiComputerViewStrings.frameUnavailable
        }
    }

    /// What VoiceOver reads for the box: the screenshot's description (or why
    /// none shows) and the latest action.
    static func frameDescription(
        _ frame: MonarchAiComputerFrame?,
        state: FrameState,
        surface: MonarchAiComputerViewSurface,
        latest: MonarchAiComputerAction?,
        labels: MonarchAiComputerViewLabels
    ) -> String {
        let description: String
        switch state {
        case .shown:
            let alt = frame?.alt?.trimmingCharacters(in: .whitespacesAndNewlines) ?? ""
            description =
                alt.isEmpty ? (labels.frameAlt?(surface) ?? MonarchAiComputerViewStrings.frameAlt(surface)) : alt
        default:
            description = placeholder(state, labels: labels) ?? ""
        }
        guard let latest else { return description }
        let action = actionLabel(latest, labels: labels)
        return labels.latestAction?(description, action)
            ?? MonarchAiComputerViewStrings.latestAction(description, action)
    }

    /// A point as fractions of the screenshot (0 to 1 from the top-left
    /// corner), clamped to its edges; nil when the point or the size is not usable.
    static func position(_ point: MonarchAiComputerPoint?, width: Double, height: Double) -> UnitPoint? {
        guard let point, point.x.isFinite, point.y.isFinite, width.isFinite, height.isFinite, width > 0, height > 0
        else { return nil }
        return UnitPoint(x: min(1, max(0, point.x / width)), y: min(1, max(0, point.y / height)))
    }

    /// The aspect ratio of the box: the screenshot's, or 16:10 when it is unknown.
    static func aspectRatio(_ frame: MonarchAiComputerFrame?) -> CGFloat {
        guard let frame, frame.width > 0, frame.height > 0, frame.width.isFinite, frame.height.isFinite else {
            return placeholderAspectRatio
        }
        return frame.width / frame.height
    }

    /// The address without any `user:password@` part, so credentials never show.
    static func displayURL(_ url: String) -> String {
        guard let range = url.range(of: "://") else { return url }
        let rest = url[range.upperBound...]
        let authorityEnd = rest.firstIndex { "/?#".contains($0) } ?? rest.endIndex
        guard let at = rest[..<authorityEnd].lastIndex(of: "@") else { return url }
        return String(url[..<range.upperBound]) + String(rest[rest.index(after: at)...])
    }

    // MARK: - Colours

    /// The colour roles of a log row.
    struct RowAppearance: Equatable {
        let background: MonarchColorRole
        let border: MonarchColorRole
        let icon: MonarchColorRole
    }

    /// The roles of a log row; the selected row has the accent background and border.
    static func rowAppearance(isSelected: Bool) -> RowAppearance {
        isSelected
            ? RowAppearance(background: \.accentSubtle, border: \.accentBorder, icon: \.accentStrong)
            : RowAppearance(background: \.bgSurface, border: \.borderSubtle, icon: \.textMuted)
    }

    /// The marker of the latest action.
    static var lastMarkerRole: MonarchColorRole { \.aiStrong }
    /// The marker of the selected action.
    static var selectedMarkerRole: MonarchColorRole { \.accentStrong }
    /// The agent pointer.
    static var pointerRole: MonarchColorRole { \.textDefault }
    /// The halo behind the markers and the pointer, so they read on any screenshot.
    static var haloRole: MonarchColorRole { \.bgSurface }
    /// The box behind the screenshot.
    static var boxRole: MonarchColorRole { \.bgMuted }
    /// The border of the box.
    static var boxBorderRole: MonarchColorRole { \.borderDefault }
    /// The border of the screenshot while the agent is in control, as the web draws its window.
    static var agentBoxBorderRole: MonarchColorRole { \.aiBorder }
}
