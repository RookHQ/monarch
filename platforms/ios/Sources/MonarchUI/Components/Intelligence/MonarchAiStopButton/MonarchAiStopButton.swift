import SwiftUI

/// The Stop control for a streaming assistant response, with a "Stopped."
/// state and an optional Continue action.
///
/// While the response runs it is a standard `Button` in the secondary
/// ``MonarchButtonStyle`` with a square stop symbol. Set `isStopped` once the
/// request has been aborted: the button is replaced by the text "Stopped."
/// and, when `onContinue` is given, a Continue button.
///
/// ```swift
/// MonarchAiStopButton(isStopped: isStopped, onContinue: { resume() }, action: { stop() })
/// ```
///
/// Disable the Stop button with the standard `disabled(_:)` modifier, for
/// example while the abort is in flight.
///
/// Accessibility: the Stop button is named by the stop text even when the
/// text is not shown, and the symbol is decorative. When `isStopped` becomes
/// true the stopped text is announced. Both buttons keep a 44-point touch
/// target and every text scales with Dynamic Type.
public struct MonarchAiStopButton: View {
    private let size: MonarchButtonSize
    private let isStopped: Bool
    private let stopLabel: String?
    private let stoppedLabel: String?
    private let continueLabel: String?
    private let showsLabel: Bool
    private let onContinue: (@MainActor () -> Void)?
    private let action: @MainActor () -> Void

    @Environment(\.monarchTheme) private var theme

    /// A stop button.
    ///
    /// - Parameters:
    ///   - size: The height and padding of the buttons.
    ///   - isStopped: Whether the response has stopped.
    ///   - stopLabel: Replaces the built-in "Stop" title.
    ///   - stoppedLabel: Replaces the built-in "Stopped." text.
    ///   - continueLabel: Replaces the built-in "Continue" title.
    ///   - showsLabel: Whether the stop title is visible beside the symbol.
    ///   - onContinue: Adds a Continue button to the stopped state.
    ///   - action: Runs when Stop is activated. Abort the request here.
    public init(
        size: MonarchButtonSize = .sm,
        isStopped: Bool = false,
        stopLabel: String? = nil,
        stoppedLabel: String? = nil,
        continueLabel: String? = nil,
        showsLabel: Bool = true,
        onContinue: (@MainActor () -> Void)? = nil,
        action: @escaping @MainActor () -> Void
    ) {
        self.size = size
        self.isStopped = isStopped
        self.stopLabel = stopLabel
        self.stoppedLabel = stoppedLabel
        self.continueLabel = continueLabel
        self.showsLabel = showsLabel
        self.onContinue = onContinue
        self.action = action
    }

    private var stopText: String { stopLabel ?? MonarchAiStopButtonStrings.stop }
    private var stoppedText: String { stoppedLabel ?? MonarchAiStopButtonStrings.stopped }

    /// The content of the view.
    public var body: some View {
        ViewThatFits(in: .horizontal) {
            HStack(spacing: MonarchAiStopButtonTokens.spacing) { parts }
            VStack(alignment: .leading, spacing: MonarchAiStopButtonTokens.spacing) { parts }
        }
        .onChange(of: isStopped) { _, stopped in
            if stopped {
                AccessibilityNotification.Announcement(stoppedText).post()
            }
        }
    }

    @ViewBuilder private var parts: some View {
        ForEach(
            MonarchAiStopButtonTokens.parts(isStopped: isStopped, hasContinue: onContinue != nil),
            id: \.self
        ) { part in
            switch part {
            case .stop: stopButton
            case .status: status
            case .resume: continueButton
            }
        }
    }

    private var stopButton: some View {
        Button(action: action) {
            HStack(spacing: MonarchAiStopButtonTokens.iconSpacing) {
                MonarchIcon(systemName: MonarchAiStopButtonTokens.symbol, size: .sm)
                if showsLabel {
                    Text(stopText)
                }
            }
        }
        .buttonStyle(.monarch(.secondary, size: size))
        .accessibilityLabel(stopText)
    }

    private var status: some View {
        Text(stoppedText)
            .monarchTextStyle(MonarchAiStopButtonTokens.statusTextStyle, weight: MonarchAiStopButtonTokens.statusWeight)
            .foregroundStyle(theme.colors[role: MonarchAiStopButtonTokens.statusColor])
    }

    @ViewBuilder private var continueButton: some View {
        if let onContinue {
            Button(continueLabel ?? MonarchAiStopButtonStrings.resume, action: onContinue)
                .buttonStyle(.monarch(.ghost, size: size))
        }
    }
}

#if DEBUG
    #Preview("MonarchAiStopButton") {
        MonarchPreviewMatrix {
            VStack(alignment: .leading) {
                MonarchAiStopButton {}
                MonarchAiStopButton(size: .md, showsLabel: false) {}
                MonarchAiStopButton {}.disabled(true)
                MonarchAiStopButton(isStopped: true) {}
                MonarchAiStopButton(isStopped: true, onContinue: {}, action: {})
            }
        }
    }
#endif
