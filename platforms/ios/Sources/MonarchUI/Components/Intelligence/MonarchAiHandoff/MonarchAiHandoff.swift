import SwiftUI

/// A marker in a thread where one agent hands the work to another agent or
/// to a person: both pictures with an arrow, a sentence saying who handed off
/// to whom, the reason and the time.
///
/// ```swift
/// MonarchAiHandoff(
///     from: MonarchAiAgentRef(id: "research", name: "Research agent"),
///     to: .person(name: "Sam Lee"),
///     reason: "Needs a person to approve the refund",
///     date: event.date
/// )
/// ```
///
/// Agents are rounded squares and people are circles, so the direction of
/// the handoff shows at a glance; a handoff to an agent has AI-coloured
/// dashed lines and a handoff to a person neutral ones. The time is relative
/// to `now`, in the locale and calendar of the environment. At accessibility
/// text sizes the side lines are dropped so the sentence keeps the width.
///
/// Accessibility: the marker is one element, read as "Handoff", the
/// sentence with "(AI agent)" after each agent's name, "Reason:" and the
/// reason, then the time. The pictures and the arrow are hidden. It is not
/// interactive.
@_spi(Experimental) public struct MonarchAiHandoff: View {
    private let from: MonarchAiAgentRef
    private let to: MonarchAiHandoffTarget
    private let reason: String?
    private let date: Date?
    private let now: Date?
    private let labels: MonarchAiHandoffLabels

    @Environment(\.monarchTheme) private var theme
    @Environment(\.locale) private var locale
    @Environment(\.calendar) private var calendar
    @Environment(\.timeZone) private var timeZone
    @Environment(\.dynamicTypeSize) private var dynamicTypeSize
    @ScaledMetric(relativeTo: .body) private var bodySize = MonarchTextStyle.body.spec.size

    /// A handoff marker.
    ///
    /// - Parameters:
    ///   - from: The agent that hands the work over.
    ///   - to: The agent or the person that takes it.
    ///   - reason: Why the work moved, already localised.
    ///   - date: When it happened, shown as a relative time.
    ///   - now: The reference time for `date`. The default is the current time.
    ///   - labels: Replacements for the built-in strings.
    public init(
        from: MonarchAiAgentRef,
        to: MonarchAiHandoffTarget,
        reason: String? = nil,
        date: Date? = nil,
        now: Date? = nil,
        labels: MonarchAiHandoffLabels = MonarchAiHandoffLabels()
    ) {
        self.from = from
        self.to = to
        self.reason = reason
        self.date = date
        self.now = now
        self.labels = labels
    }

    private typealias Tokens = MonarchAiHandoffTokens

    private var timeText: String? {
        guard let date else { return nil }
        var zoned = calendar
        zoned.timeZone = timeZone
        return Tokens.timeText(date, now: now ?? Date(), locale: locale, calendar: zoned)
    }

    private var reasonText: String? {
        guard let reason, !reason.isEmpty else { return nil }
        return reason
    }

    /// The content of the view.
    public var body: some View {
        let showsLines = Tokens.showsLines(isAccessibilitySize: dynamicTypeSize.isAccessibilitySize)
        let time = timeText
        HStack(spacing: Tokens.lineSpacing) {
            if showsLines { line }
            content(time: time)
                .layoutPriority(1)
            if showsLines { line }
        }
        .padding(.vertical, Tokens.verticalPadding)
        .frame(maxWidth: .infinity)
        .accessibilityElement(children: .ignore)
        .accessibilityLabel(
            Tokens.accessibilityLabel(
                label: labels.label ?? MonarchAiHandoffStrings.label,
                sentence: Tokens.spokenSentence(from: from.name, to: to, labels: labels),
                reasonPrefix: labels.reason ?? MonarchAiHandoffStrings.reason,
                reason: reasonText,
                time: time
            )
        )
    }

    /// The semibold face of the body style, for the agent names.
    private var strongFont: Font {
        if theme.typography.fontFamily != nil, let face = theme.typography.face(for: .semibold) {
            return .custom(face, fixedSize: bodySize)
        }
        return .system(size: bodySize, weight: .semibold)
    }

    /// The sentence with both names in semibold, as on the web.
    private var sentenceText: Text {
        let sentence = Tokens.sentence(from: from.name, to: to, labels: labels)
        var pieces: [(String, Bool)] = [(sentence, false)]
        for name in [from.name, to.name] where !name.isEmpty {
            var next: [(String, Bool)] = []
            var isDone = false
            for (piece, isStrong) in pieces {
                if !isDone, !isStrong, let range = piece.range(of: name) {
                    next.append((String(piece[..<range.lowerBound]), false))
                    next.append((name, true))
                    next.append((String(piece[range.upperBound...]), false))
                    isDone = true
                } else {
                    next.append((piece, isStrong))
                }
            }
            pieces = next
        }
        return pieces.filter { !$0.0.isEmpty }.reduce(Text("")) { result, piece in
            result + (piece.1 ? Text(piece.0).font(strongFont) : Text(piece.0))
        }
    }

    private func content(time: String?) -> some View {
        let shape = RoundedRectangle(cornerRadius: Tokens.cornerRadius, style: .continuous)
        return HStack(alignment: .center, spacing: Tokens.bodySpacing) {
            pictures
            VStack(alignment: .leading, spacing: Tokens.textSpacing) {
                sentenceText
                    .monarchTextStyle(.body, weight: Tokens.sentenceWeight)
                    .foregroundStyle(theme.colors[role: Tokens.sentence])
                if let reasonText {
                    Text(reasonText)
                        .monarchTextStyle(.caption)
                        .foregroundStyle(theme.colors[role: Tokens.reason])
                }
                if let time {
                    Text(time)
                        .monarchTextStyle(.caption)
                        .foregroundStyle(theme.colors[role: Tokens.time])
                }
            }
            .fixedSize(horizontal: false, vertical: true)
        }
        .padding(.horizontal, Tokens.horizontalPadding)
        .padding(.vertical, Tokens.bodyVerticalPadding)
        .overlay(
            shape.strokeBorder(theme.colors[role: Tokens.lineRole(isPerson: to.isPerson)], lineWidth: Tokens.lineWidth)
        )
    }

    private var pictures: some View {
        HStack(spacing: Tokens.pictureSpacing) {
            MonarchAiAgentAvatar(from, size: Tokens.avatarSize, isDecorative: true)
            MonarchIcon(systemName: Tokens.arrowSymbol, size: .sm)
                .foregroundStyle(theme.colors[role: Tokens.arrowRole(isPerson: to.isPerson)])
            switch to {
            case .agent(let agent):
                MonarchAiAgentAvatar(agent, size: Tokens.avatarSize, isDecorative: true)
            case .person(let name, let avatarURL):
                MonarchAvatar(url: avatarURL, name: name, size: Tokens.avatarSize, isDecorative: true)
            }
        }
        .accessibilityHidden(true)
    }

    private var line: some View {
        MonarchAiHandoffLine()
            .stroke(
                theme.colors[role: Tokens.lineRole(isPerson: to.isPerson)],
                style: StrokeStyle(lineWidth: Tokens.lineWidth, dash: [Tokens.dash, Tokens.dash])
            )
            .frame(minWidth: Tokens.lineMinWidth)
            .frame(height: Tokens.lineWidth)
            .accessibilityHidden(true)
    }
}

/// A horizontal line through the middle of its frame, so it can be dashed.
private struct MonarchAiHandoffLine: Shape {
    func path(in rect: CGRect) -> Path {
        var path = Path()
        path.move(to: CGPoint(x: rect.minX, y: rect.midY))
        path.addLine(to: CGPoint(x: rect.maxX, y: rect.midY))
        return path
    }
}

#if DEBUG
    #Preview("MonarchAiHandoff") {
        let research = MonarchAiAgentRef(id: "research", name: "Research agent", codename: "Ra")
        let now = Date()
        MonarchPreviewMatrix {
            VStack {
                MonarchAiHandoff(
                    from: research,
                    to: .agent(MonarchAiAgentRef(id: "writer", name: "Writing agent", codename: "Wa")),
                    reason: "Sources are ready to draft from",
                    date: now.addingTimeInterval(-180),
                    now: now
                )
                MonarchAiHandoff(from: research, to: .person(name: "Sam Lee"), reason: "Needs approval")
            }
        }
    }
#endif
