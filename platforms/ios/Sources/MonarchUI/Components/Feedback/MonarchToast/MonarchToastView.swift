import SwiftUI

/// One toast on its raised surface: the icon of the tone, the title, the
/// message, the action and the dismiss button.
struct MonarchToastView: View {
    let toast: MonarchToast
    let dismissLabel: String?
    let onDismiss: @MainActor () -> Void

    @Environment(\.monarchTheme) private var theme

    var body: some View {
        HStack(alignment: .center, spacing: MonarchToastTokens.itemSpacing) {
            ViewThatFits(in: .horizontal) {
                HStack(spacing: MonarchToastTokens.itemSpacing) {
                    text
                    Spacer(minLength: 0)
                    actionButton
                }
                VStack(alignment: .leading, spacing: MonarchToastTokens.itemSpacing) {
                    text
                    actionButton
                }
            }
            .frame(maxWidth: .infinity, alignment: .leading)
            Button(action: onDismiss) {
                MonarchIcon(systemName: MonarchToastTokens.dismissSymbol, size: .sm)
                    .foregroundStyle(theme.colors.textMuted)
                    .monarchMinimumHitTarget()
            }
            .buttonStyle(.plain)
            .accessibilityLabel(dismissLabel ?? MonarchToastStrings.dismiss)
        }
        .padding(.leading, MonarchToastTokens.horizontalPadding)
        .padding(.trailing, MonarchToastTokens.itemSpacing)
        .padding(.vertical, MonarchToastTokens.verticalPadding)
        .frame(maxWidth: MonarchToastTokens.maxWidth, alignment: .leading)
        .monarchElevation(.presented, fill: MonarchToastTokens.background, in: MonarchShapes.container)
        .overlay(alignment: .leading) {
            Rectangle()
                .fill(theme.colors[role: MonarchToastTokens.accentRole(tone: toast.tone)])
                .frame(width: MonarchToastTokens.accentWidth)
                .allowsHitTesting(false)
        }
        .clipShape(MonarchShapes.container)
        .overlay {
            MonarchShapes.container
                .strokeBorder(
                    theme.colors[role: MonarchToastTokens.border],
                    lineWidth: MonarchToastTokens.borderWidth
                )
                .allowsHitTesting(false)
        }
        .accessibilityElement(children: .contain)
    }

    private var text: some View {
        HStack(alignment: .firstTextBaseline, spacing: MonarchToastTokens.itemSpacing) {
            if toast.tone != .neutral {
                icon.foregroundStyle(theme.colors[role: MonarchToastTokens.iconRole(tone: toast.tone)])
            }
            VStack(alignment: .leading, spacing: MonarchToastTokens.textSpacing) {
                Text(toast.title)
                    .monarchTextStyle(.bodyStrong)
                    .foregroundStyle(theme.colors.textDefault)
                if let message = toast.message {
                    Text(message)
                        .monarchTextStyle(.caption)
                        .foregroundStyle(theme.colors.textMuted)
                }
            }
        }
        .accessibilityElement(children: .combine)
    }

    @ViewBuilder private var icon: some View {
        let symbol = MonarchToastTokens.symbol(tone: toast.tone)
        if let spoken = MonarchToastStrings.toneName(toast.tone) {
            MonarchIcon(systemName: symbol, label: spoken)
        } else {
            MonarchIcon(systemName: symbol)
        }
    }

    @ViewBuilder private var actionButton: some View {
        if let actionTitle = toast.actionTitle, let action = toast.action {
            Button(actionTitle) {
                action()
                onDismiss()
            }
            .buttonStyle(.monarch(.secondary, size: .sm))
        }
    }
}

#if DEBUG
    #Preview("MonarchToast") {
        MonarchPreviewMatrix {
            VStack {
                ForEach(MonarchTone.allCases, id: \.self) { tone in
                    MonarchToastView(
                        toast: MonarchToast("Ticket archived", message: "You can undo this.", tone: tone),
                        dismissLabel: nil,
                        onDismiss: {}
                    )
                }
                MonarchToastView(
                    toast: MonarchToast("Ticket archived", actionTitle: "Undo") {},
                    dismissLabel: nil,
                    onDismiss: {}
                )
            }
        }
    }
#endif
