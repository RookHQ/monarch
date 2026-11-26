import Foundation
import MonarchTokens
import SwiftUI

/// Component tokens and pure logic of ``MonarchAiMessage``.
enum MonarchAiMessageTokens {
    /// The gap between the parts of a message.
    static let spacing: CGFloat = MonarchTokens.Space.space2
    /// The gap between the items of the header and the footer.
    static let rowSpacing: CGFloat = MonarchTokens.Space.space2
    /// The gap between citation markers.
    static let citationSpacing: CGFloat = MonarchTokens.Space.space1
    /// The padding inside the person's bubble, across.
    static let bubblePaddingHorizontal: CGFloat = MonarchTokens.Space.space3
    /// The padding inside the person's bubble, down.
    static let bubblePaddingVertical: CGFloat = MonarchTokens.Space.space2
    /// The large corner radius of the person's bubble.
    static let bubbleRadius: CGFloat = MonarchTokens.Radius.xl
    /// The small corner radius of the bubble, at the bottom trailing corner, towards the person.
    static let bubbleTailRadius: CGFloat = MonarchTokens.Radius.sm
    /// The least room kept before the person's bubble, so it reads as theirs.
    static let bubbleLeadingInset: CGFloat = MonarchTokens.Space.space12
    /// The padding of the finish notice and the queued line.
    static let noticePadding: CGFloat = MonarchTokens.Space.space2
    /// The fill of the person's bubble.
    static var bubbleFill: MonarchColorRole { \.bgMuted }
    /// The colour of the agent's role and the time.
    static var metaText: MonarchColorRole { \.textMuted }
    /// The colour of a system or tool message.
    static var systemText: MonarchColorRole { \.textMuted }
    /// The colour of the finish notice symbol: it is a warning.
    static var finishSymbolColor: MonarchColorRole { \.warningStrong }
    /// The SF Symbol of the finish notice.
    static let finishSymbol = "exclamationmark.triangle"
    /// The SF Symbol of a message queued while offline.
    static let queuedSymbol = "clock"
    /// The SF Symbol of the resume button of a dropped reply.
    static let resumeSymbol = "play"

    // MARK: - Order of the parts

    /// One block of the message body, in the order of the parts.
    enum Block: Hashable {
        /// The part at this index, drawn by its component.
        case part(Int)
        /// Consecutive citations, by part index, drawn as one row of markers.
        case citations([Int])
        /// The list of every source, where the first source part stands.
        case sources
    }

    /// The blocks of a message: every part in order, with the sources as one
    /// list where the first source stands, consecutive citations of known
    /// sources as one row, empty finished text left out, artifacts only when
    /// they can be opened, and generative UI and edit proposals only when
    /// the app draws them.
    static func blocks(_ parts: [MonarchAiPart], showsArtifacts: Bool, drawsCustomParts: Bool) -> [Block] {
        let sourceIds = Set(
            parts.compactMap { part -> String? in
                if case .source(let source) = part { return source.id }
                return nil
            }
        )
        var blocks: [Block] = []
        var sourcesShown = false
        for (index, part) in parts.enumerated() {
            switch part {
            case .text(let text):
                if !text.text.isEmpty || text.state == .streaming { blocks.append(.part(index)) }
            case .source:
                if !sourcesShown {
                    sourcesShown = true
                    blocks.append(.sources)
                }
            case .citation(let citation):
                guard sourceIds.contains(citation.sourceId) else { continue }
                if case .citations(let previous) = blocks.last {
                    blocks[blocks.count - 1] = .citations(previous + [index])
                } else {
                    blocks.append(.citations([index]))
                }
            case .artifact:
                if showsArtifacts { blocks.append(.part(index)) }
            case .ui, .editProposal:
                if drawsCustomParts { blocks.append(.part(index)) }
            case .reasoning, .toolCall, .file, .plan, .memory, .error:
                blocks.append(.part(index))
            }
        }
        return blocks
    }

    /// Whether the reply as a whole is finished: no text or reasoning
    /// streams and no tool call is running or waiting.
    static func isReplyDone(_ message: MonarchAiConversationMessage) -> Bool {
        guard !message.isStreaming else { return false }
        return !message.toolCalls.contains { call in
            call.state == .running || call.state == .pendingApproval || call.state == .awaitingResult
        }
    }

    /// The index of the last text part, which announces the end of the reply.
    static func lastTextIndex(_ parts: [MonarchAiPart]) -> Int? {
        parts.lastIndex { part in
            if case .text = part { return true }
            return false
        }
    }

    /// Whether a tool call is drawn as an approval: it waits for approval and the app handles the decision.
    static func showsApproval(_ call: MonarchAiToolCallPart, handlesApproval: Bool) -> Bool {
        handlesApproval && call.state == .pendingApproval
    }

    /// The finish notice of a finished assistant reply that was cut short, filtered or refused, or `nil`.
    static func finishNotice(_ message: MonarchAiConversationMessage, labels: MonarchAiMessageLabels) -> String? {
        guard message.role == .assistant, isReplyDone(message), let reason = message.finishReason else { return nil }
        switch reason {
        case .length, .contentFilter, .refusal:
            return labels.finishNotice?(reason) ?? MonarchAiMessageStrings.finishNotice(reason)
        case .stop, .toolUse, .other:
            return nil
        }
    }

    /// The name of the message for VoiceOver: who said it.
    static func spokenRole(_ role: MonarchAiRole, labels: MonarchAiMessageLabels) -> String {
        switch role {
        case .user: labels.youSaid ?? MonarchAiMessageStrings.youSaid
        case .assistant: labels.assistantSaid ?? MonarchAiMessageStrings.assistantSaid
        case .system, .tool: labels.systemSaid ?? MonarchAiMessageStrings.systemSaid
        }
    }

    /// The name shown in the header: the agent's, or the built-in "Assistant".
    static func name(agent: MonarchAiAgentRef?, labels: MonarchAiMessageLabels) -> String {
        if let name = agent?.name, !name.isEmpty { return name }
        return labels.assistant ?? MonarchAiMessageStrings.assistant
    }

    /// The time of the message, such as "10:42 AM", in the given locale and
    /// time zone, or `nil` when it is hidden or `createdAt` is not a time.
    static func time(_ createdAt: String, isShown: Bool, locale: Locale, timeZone: TimeZone) -> String? {
        guard isShown, let date = MonarchAiTimestamp.date(from: createdAt) else { return nil }
        var calendar = Calendar(identifier: .gregorian)
        calendar.timeZone = timeZone
        let style = Date.FormatStyle(
            date: .omitted,
            time: .omitted,
            locale: locale,
            calendar: calendar,
            timeZone: timeZone
        ).monarchTime()
        return date.formatted(style)
    }

    /// The header read as one element: the name, the agent's role and the time.
    static func headerLabel(name: String, agentRole: String?, time: String?) -> String {
        [name, agentRole, time].compactMap { $0 }.filter { !$0.isEmpty }.joined(separator: ", ")
    }

    /// Whether the actions row is shown: turned on, not while editing,
    /// streaming or queued, and only for the person's and the assistant's messages.
    static func showsActions(_ message: MonarchAiConversationMessage, isShown: Bool, isEditing: Bool) -> Bool {
        guard isShown, !isEditing, !message.isStreaming, !message.isQueued else { return false }
        return message.role == .user || message.role == .assistant
    }

    /// The branch switcher is shown when the message has more than one alternative.
    static func branch(_ message: MonarchAiConversationMessage) -> MonarchAiBranchPosition? {
        guard let branch = message.branch, branch.count > 1 else { return nil }
        return branch
    }

    /// The source numbers of the message, from 1, in the order of its source parts.
    static func sourceNumbers(_ parts: [MonarchAiPart]) -> [String: Int] {
        var numbers: [String: Int] = [:]
        for part in parts {
            if case .source(let source) = part, numbers[source.id] == nil {
                numbers[source.id] = numbers.count + 1
            }
        }
        return numbers
    }

    /// The source a citation points at.
    static func source(_ id: String, in parts: [MonarchAiPart]) -> MonarchAiSourcePart? {
        for part in parts {
            if case .source(let source) = part, source.id == id { return source }
        }
        return nil
    }

    /// The address of a file part that can be opened: `http` or `https` only.
    static func fileURL(_ part: MonarchAiFilePart) -> URL? {
        guard let text = part.url, let url = URL(string: text), let scheme = url.scheme?.lowercased() else {
            return nil
        }
        return scheme == "http" || scheme == "https" ? url : nil
    }

    /// The retry delay of an error part, in seconds.
    static func retryAfter(_ part: MonarchAiErrorPart) -> TimeInterval? {
        part.retryAfterMs.map { TimeInterval($0) / 1000 }
    }
}
