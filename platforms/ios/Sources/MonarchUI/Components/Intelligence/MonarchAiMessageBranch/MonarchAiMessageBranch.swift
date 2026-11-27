import SwiftUI

/// The "‹ 2 / 3 ›" switcher between alternative versions of a message, made
/// when a reply is regenerated or a prompt is edited and resent.
///
/// ```swift
/// MonarchAiMessageBranch(selection: $version, count: 3)
/// ```
///
/// `selection` is the version shown, counted from 0. With fewer than two
/// versions nothing is shown. Disable it with `disabled(_:)`, for example
/// while a reply streams.
///
/// Accessibility: a group named "Versions" with a "Previous version" and a
/// "Next version" button, each with a 44-point touch target. The position
/// is read as "Version 2 of 3", and announced after a button changes it.
/// The chevrons mirror in right-to-left layouts.
public struct MonarchAiMessageBranch: View {
    @Binding private var selection: Int
    private let count: Int
    private let showsSingle: Bool
    private let label: String?
    private let previousLabel: String?
    private let nextLabel: String?
    private let formatPosition: ((Int, Int) -> String)?
    private let formatAnnouncement: ((Int, Int) -> String)?

    @Environment(\.monarchTheme) private var theme
    @Environment(\.isEnabled) private var isEnabled
    @Environment(\.monarchBranchIsVertical) private var isVertical
    @State private var pendingTarget: Int?

    /// A version switcher.
    ///
    /// - Parameters:
    ///   - selection: The version shown, counted from 0.
    ///   - count: How many versions exist.
    ///   - showsSingle: Whether the switcher is shown when there is only one version.
    ///   - label: Replaces the built-in spoken name "Versions".
    ///   - previousLabel: Replaces the built-in spoken name "Previous version".
    ///   - nextLabel: Replaces the built-in spoken name "Next version".
    ///   - formatPosition: Replaces the built-in visible position "2 / 3". Receives the version, counted
    ///     from 1, and the count.
    ///   - formatAnnouncement: Replaces the built-in spoken position "Version 2 of 3". Receives the
    ///     version, counted from 1, and the count.
    public init(
        selection: Binding<Int>,
        count: Int,
        showsSingle: Bool = false,
        label: String? = nil,
        previousLabel: String? = nil,
        nextLabel: String? = nil,
        formatPosition: ((Int, Int) -> String)? = nil,
        formatAnnouncement: ((Int, Int) -> String)? = nil
    ) {
        _selection = selection
        self.count = count
        self.showsSingle = showsSingle
        self.label = label
        self.previousLabel = previousLabel
        self.nextLabel = nextLabel
        self.formatPosition = formatPosition
        self.formatAnnouncement = formatAnnouncement
    }

    private func announcement(for index: Int) -> String {
        let total = MonarchAiMessageBranchTokens.total(count)
        return formatAnnouncement?(index + 1, total) ?? MonarchAiMessageBranchStrings.announcement(index + 1, of: total)
    }

    /// The content of the view.
    public var body: some View {
        let total = MonarchAiMessageBranchTokens.total(count)
        let current = MonarchAiMessageBranchTokens.current(selection, count: count)
        if MonarchAiMessageBranchTokens.isVisible(count: count, showsSingle: showsSingle) {
            HStack(spacing: MonarchAiMessageBranchTokens.spacing) {
                if !isVertical {
                    stepButton(
                        MonarchAiMessageBranchTokens.previousSymbol,
                        label: previousLabel ?? MonarchAiMessageBranchStrings.previous,
                        step: -1
                    )
                    .disabled(!MonarchAiMessageBranchTokens.canGoBack(index: selection, count: count))
                }
                Text(
                    formatPosition?(current + 1, total)
                        ?? MonarchAiMessageBranchStrings.position(current + 1, of: total)
                )
                .monarchTextStyle(.caption)
                .monospacedDigit()
                .frame(minWidth: isVertical ? nil : MonarchAiMessageBranchTokens.positionMinWidth)
                .foregroundStyle(
                    theme.colors[role: MonarchAiMessageBranchTokens.positionRole(isEnabled: isEnabled)]
                )
                .accessibilityLabel(announcement(for: current))
                if isVertical {
                    // As the inline diff on the web: the position at the start, up and down arrows at the end.
                    Spacer(minLength: 0)
                    stepButton(
                        "chevron.up",
                        label: previousLabel ?? MonarchAiMessageBranchStrings.previous,
                        step: -1
                    )
                    .disabled(!MonarchAiMessageBranchTokens.canGoBack(index: selection, count: count))
                    stepButton(
                        "chevron.down",
                        label: nextLabel ?? MonarchAiMessageBranchStrings.next,
                        step: 1
                    )
                    .disabled(!MonarchAiMessageBranchTokens.canGoForward(index: selection, count: count))
                } else {
                    stepButton(
                        MonarchAiMessageBranchTokens.nextSymbol,
                        label: nextLabel ?? MonarchAiMessageBranchStrings.next,
                        step: 1
                    )
                    .disabled(!MonarchAiMessageBranchTokens.canGoForward(index: selection, count: count))
                }
            }
            .accessibilityElement(children: .contain)
            .accessibilityLabel(label ?? MonarchAiMessageBranchStrings.label)
            .onChange(of: selection) { _, newValue in
                // Only a change a button asked for is announced, not one made elsewhere.
                guard pendingTarget == newValue else { return }
                pendingTarget = nil
                AccessibilityNotification.Announcement(announcement(for: newValue)).post()
            }
        }
    }

    private func stepButton(_ symbol: String, label: String, step: Int) -> some View {
        Button {
            guard let target = MonarchAiMessageBranchTokens.target(index: selection, count: count, step: step)
            else { return }
            pendingTarget = target
            selection = target
        } label: {
            MonarchIcon(systemName: symbol, size: .sm)
        }
        .buttonStyle(.monarch(.ghost, size: .sm))
        .accessibilityLabel(label)
    }
}

#if DEBUG
    #Preview("MonarchAiMessageBranch") {
        MonarchPreviewMatrix {
            VStack(alignment: .leading) {
                MonarchAiMessageBranch(selection: .constant(0), count: 3)
                MonarchAiMessageBranch(selection: .constant(1), count: 3)
                MonarchAiMessageBranch(selection: .constant(2), count: 3)
                MonarchAiMessageBranch(selection: .constant(0), count: 1, showsSingle: true)
                MonarchAiMessageBranch(selection: .constant(1), count: 12).disabled(true)
            }
        }
    }
#endif

private struct MonarchBranchIsVerticalKey: EnvironmentKey {
    static let defaultValue = false
}

extension EnvironmentValues {
    /// Draws a branch control with its position first and up and down arrows after it, as the inline diff does.
    var monarchBranchIsVertical: Bool {
        get { self[MonarchBranchIsVerticalKey.self] }
        set { self[MonarchBranchIsVerticalKey.self] = newValue }
    }
}
