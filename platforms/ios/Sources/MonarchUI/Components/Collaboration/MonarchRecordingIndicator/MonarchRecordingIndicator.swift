import SwiftUI

/// A "REC" capsule with the recorded time, shown while a call or a session
/// is being recorded.
///
/// ```swift
/// MonarchRecordingIndicator(isRecording: isRecording, since: startedAt) {
///     stopRecording()
/// }
/// ```
///
/// While `isRecording` is false nothing is drawn, but keep the view in the
/// layout so that the start and the stop are announced. Pass `since` for a
/// time that advances by itself, or `elapsed` when the product counts the
/// time, for example without the pauses. `elapsed` takes precedence.
///
/// Accessibility: the capsule is one element read as "Recording" (or
/// "Recording, Paused") with the time in words as its value. Starting,
/// pausing, resuming and stopping are each announced once; the advancing
/// time is not. The state is text and a glyph, not colour alone. The dot
/// stops pulsing when Reduce Motion is on. The stop button has a 44-point
/// touch target.
public struct MonarchRecordingIndicator: View {
    private let isRecording: Bool
    private let since: Date?
    private let elapsed: TimeInterval?
    private let variant: MonarchRecordingIndicatorVariant
    private let isPaused: Bool
    private let labels: MonarchRecordingIndicatorLabels
    private let showsElapsed: Bool?
    private let onStop: (@MainActor () -> Void)?

    /// A recording indicator.
    ///
    /// - Parameters:
    ///   - isRecording: Whether recording is on.
    ///   - since: When recording started. The time advances from it.
    ///   - elapsed: The recorded time in seconds, when the product counts it.
    ///     It takes precedence over `since` and does not advance by itself.
    ///   - variant: How the capsule is drawn: `subtle` on the app's
    ///     surfaces, `solid` over video.
    ///   - isPaused: Whether recording is paused. The capsule says so and
    ///     the time stops.
    ///   - labels: Replacements for the built-in strings.
    ///   - showsElapsed: Whether the time is shown. By default it is shown
    ///     when `since` or `elapsed` is given.
    ///   - onStop: Stops recording. Adds a stop button.
    public init(
        isRecording: Bool,
        since: Date? = nil,
        elapsed: TimeInterval? = nil,
        variant: MonarchRecordingIndicatorVariant = .subtle,
        isPaused: Bool = false,
        labels: MonarchRecordingIndicatorLabels = MonarchRecordingIndicatorLabels(),
        showsElapsed: Bool? = nil,
        onStop: (@MainActor () -> Void)? = nil
    ) {
        self.isRecording = isRecording
        self.since = since
        self.elapsed = elapsed
        self.variant = variant
        self.isPaused = isPaused
        self.labels = labels
        self.showsElapsed = showsElapsed
        self.onStop = onStop
    }

    /// The content of the view.
    public var body: some View {
        HStack(spacing: MonarchRecordingIndicatorTokens.stopSpacing) {
            if isRecording {
                capsule
                if let onStop {
                    Button(action: onStop) {
                        MonarchIcon(systemName: MonarchRecordingIndicatorTokens.stopSymbol, size: .sm)
                    }
                    .buttonStyle(.monarch(.secondary, size: .sm))
                    .accessibilityLabel(labels.stop ?? MonarchRecordingIndicatorStrings.stop)
                }
            }
        }
        .onChange(of: MonarchRecordingIndicatorTokens.Phase(isRecording: isRecording, isPaused: isPaused)) { old, new in
            let announcement = MonarchRecordingIndicatorTokens.announcement(from: old, to: new, labels: labels)
            if let announcement {
                AccessibilityNotification.Announcement(announcement).post()
            }
        }
    }

    @ViewBuilder private var capsule: some View {
        let isTicking = MonarchRecordingIndicatorTokens.isTicking(isPaused: isPaused, since: since, elapsed: elapsed)
        if isTicking, let since {
            TimelineView(.periodic(from: since, by: MonarchRecordingIndicatorTokens.tick)) { context in
                capsule(now: context.date, isTicking: true)
            }
        } else {
            capsule(now: .now, isTicking: false)
        }
    }

    private func capsule(now: Date, isTicking: Bool) -> some View {
        let seconds = MonarchRecordingIndicatorTokens.seconds(since: since, elapsed: elapsed, now: now)
        let showsTime = MonarchRecordingIndicatorTokens.showsTime(
            showsElapsed: showsElapsed,
            since: since,
            elapsed: elapsed
        )
        return HStack(spacing: MonarchRecordingIndicatorTokens.contentSpacing) {
            MonarchRecordingMark(isPaused: isPaused)
            Text(MonarchRecordingIndicatorTokens.visibleText(isPaused: isPaused, labels: labels))
            if showsTime {
                Text(MonarchRecordingIndicatorTokens.timeText(seconds: seconds)).monospacedDigit()
            }
        }
        .monarchTextStyle(
            MonarchRecordingIndicatorTokens.textStyle,
            weight: MonarchRecordingIndicatorTokens.fontWeight
        )
        .padding(.horizontal, MonarchRecordingIndicatorTokens.horizontalPadding)
        .padding(.vertical, MonarchRecordingIndicatorTokens.verticalPadding)
        .monarchSurface(MonarchRecordingIndicatorTokens.appearance(variant: variant), in: Capsule())
        .accessibilityElement(children: .ignore)
        .accessibilityLabel(MonarchRecordingIndicatorTokens.accessibilityLabel(isPaused: isPaused, labels: labels))
        .accessibilityValue(showsTime ? MonarchRecordingIndicatorTokens.spokenTime(seconds: seconds) : "")
        .accessibilityAddTraits(isTicking && showsTime ? .updatesFrequently : [])
    }
}

/// The red dot, which pulses while recording, or a pause glyph while paused.
private struct MonarchRecordingMark: View {
    let isPaused: Bool

    @Environment(\.monarchTheme) private var theme
    @Environment(\.accessibilityReduceMotion) private var reduceMotion
    @ScaledMetric(relativeTo: .caption) private var dotSize = MonarchRecordingIndicatorTokens.dotSize

    var body: some View {
        if isPaused {
            MonarchIcon(systemName: MonarchRecordingIndicatorTokens.pausedSymbol, size: .sm)
        } else if reduceMotion {
            dot
        } else {
            MonarchRecordingPulse { dot }
        }
    }

    private var dot: some View {
        Circle()
            .fill(theme.colors[role: MonarchRecordingIndicatorTokens.dot])
            .frame(width: dotSize, height: dotSize)
            .accessibilityHidden(true)
    }
}

/// Shrinks and grows its content, again and again. It exists only while the
/// dot pulses, so it always starts fresh.
private struct MonarchRecordingPulse<Content: View>: View {
    @ViewBuilder let content: Content

    @State private var isAtRest = false

    var body: some View {
        content
            .scaleEffect(isAtRest ? MonarchRecordingIndicatorTokens.dotRestScale : 1)
            .onAppear {
                withAnimation(
                    .easeInOut(duration: MonarchRecordingIndicatorTokens.pulseDuration)
                        .repeatForever(autoreverses: true)
                ) {
                    isAtRest = true
                }
            }
    }
}

#if DEBUG
    #Preview("MonarchRecordingIndicator") {
        MonarchPreviewMatrix {
            VStack(alignment: .leading) {
                MonarchRecordingIndicator(isRecording: true)
                MonarchRecordingIndicator(isRecording: true, elapsed: 252) {}
                MonarchRecordingIndicator(isRecording: true, elapsed: 3723, isPaused: true)
                MonarchRecordingIndicator(isRecording: true, since: .now, variant: .solid) {}
            }
        }
    }
#endif
