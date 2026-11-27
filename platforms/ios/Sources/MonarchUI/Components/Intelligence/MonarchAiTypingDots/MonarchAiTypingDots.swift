import SwiftUI

/// Three dots that show the assistant is writing, for places with no room
/// for a label: a chat dock, a conversation row, a small message bubble.
///
/// ```swift
/// MonarchAiTypingDots()
/// MonarchAiTypingDots(label: "Ada is typing", size: .sm)
/// ```
///
/// The dots rise and brighten one after another. With Reduce Motion on they
/// are still.
///
/// Accessibility: the dots are one element whose label is `label`, by
/// default "Assistant is typing". The label is announced when the dots
/// appear; pass `isAnnounced: false` when something else already announces
/// the state. The dots scale with Dynamic Type.
public struct MonarchAiTypingDots: View {
    private let label: String?
    private let isAnnounced: Bool

    @ScaledMetric private var dot: CGFloat
    @Environment(\.accessibilityReduceMotion) private var reduceMotion

    /// Typing dots.
    ///
    /// - Parameters:
    ///   - label: Replaces the built-in spoken label "Assistant is typing".
    ///   - size: The size of the dots.
    ///   - isAnnounced: Whether the label is announced when the dots appear.
    public init(label: String? = nil, size: MonarchAiTypingDotsSize = .md, isAnnounced: Bool = true) {
        self.label = label
        self.isAnnounced = isAnnounced
        _dot = ScaledMetric(wrappedValue: MonarchAiTypingDotsTokens.dotSize(size), relativeTo: .body)
    }

    private var spokenLabel: String { label ?? MonarchAiTypingDotsStrings.typing }

    /// The content of the view.
    public var body: some View {
        Group {
            if reduceMotion {
                MonarchAiTypingDotsFrame(dot: dot, time: nil)
            } else {
                TimelineView(.animation) { context in
                    MonarchAiTypingDotsFrame(dot: dot, time: context.date.timeIntervalSinceReferenceDate)
                }
            }
        }
        .accessibilityElement(children: .ignore)
        .accessibilityLabel(spokenLabel)
        .onAppear {
            if isAnnounced {
                AccessibilityNotification.Announcement(spokenLabel).post()
            }
        }
    }
}

#if DEBUG
    #Preview("MonarchAiTypingDots") {
        MonarchPreviewMatrix {
            HStack {
                MonarchAiTypingDots(size: .sm, isAnnounced: false)
                MonarchAiTypingDots(isAnnounced: false)
            }
        }
    }
#endif
