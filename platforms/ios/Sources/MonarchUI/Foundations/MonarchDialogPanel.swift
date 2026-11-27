import MonarchTokens
import SwiftUI

/// The card of a Monarch dialog: a title, an optional message, optional content, and a footer bar of actions.
///
/// It is the same card the web dialog draws, so a confirmation or a prompt reads the same on every platform: a
/// title and a quiet message on the surface, the content below, and the actions on a muted bar that is cut off by a
/// hairline. Use it through ``SwiftUICore/View/monarchDialog(isPresented:title:message:onDismiss:content:footer:)``
/// to show it over the screen, or place it in a layout yourself.
///
/// ```swift
/// MonarchDialogPanel("Name this view", message: "Saved views appear in the sidebar.") {
///     MonarchTextField("View name", text: $name, isRequired: true)
/// } footer: {
///     MonarchButton("Cancel") { isAsking = false }
///     MonarchButton("Save view", variant: .primary) { save() }
/// }
/// ```
///
/// Accessibility: the card is a modal group, so VoiceOver stays inside it, reads the title first as a heading, and
/// the escape gesture runs `onEscape`.
public struct MonarchDialogPanel<Content: View, Footer: View>: View {
    private let title: String
    private let message: String?
    private let content: Content
    private let footer: Footer
    private let onEscape: (@MainActor () -> Void)?
    private let onClose: (@MainActor () -> Void)?
    private let hasFooter: Bool

    @Environment(\.monarchTheme) private var theme

    /// A dialog card.
    ///
    /// - Parameters:
    ///   - title: The heading.
    ///   - message: A line or two under the heading.
    ///   - onEscape: Runs for the VoiceOver escape gesture; usually the dialog's cancel action.
    ///   - onClose: When given, a close button (an X, named "Close") sits at the end of the heading.
    ///   - hasFooter: Whether the footer bar is drawn. False for a dialog whose controls are all in the content.
    ///   - content: What sits between the message and the footer, such as a field.
    ///   - footer: The actions, laid out by the caller; a row of buttons ends the bar.
    public init(
        _ title: String,
        message: String? = nil,
        onEscape: (@MainActor () -> Void)? = nil,
        onClose: (@MainActor () -> Void)? = nil,
        hasFooter: Bool = true,
        @ViewBuilder content: () -> Content,
        @ViewBuilder footer: () -> Footer
    ) {
        self.hasFooter = hasFooter
        self.title = title
        self.message = message
        self.onEscape = onEscape
        self.onClose = onClose
        self.content = content()
        self.footer = footer()
    }

    /// The content of the view.
    public var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            VStack(alignment: .leading, spacing: theme.spacing.space3) {
                VStack(alignment: .leading, spacing: theme.spacing.space1) {
                    HStack(alignment: .top, spacing: theme.spacing.space2) {
                        Text(title)
                            .monarchTextStyle(.heading)
                            .foregroundStyle(theme.colors[role: \.textDefault])
                            .accessibilityAddTraits(.isHeader)
                        Spacer(minLength: 0)
                        if let onClose {
                            Button(action: onClose) {
                                MonarchIcon(systemName: "xmark", size: .sm)
                                    .foregroundStyle(theme.colors[role: \.textMuted])
                                    .frame(width: 44, height: 44)
                                    .contentShape(Rectangle())
                            }
                            .buttonStyle(.plain)
                            .accessibilityLabel(MonarchDialogStrings.close)
                            .padding(.top, -theme.spacing.space3)
                            .padding(.trailing, -theme.spacing.space3)
                            .padding(.bottom, -theme.spacing.space3)
                        }
                    }
                    if let message {
                        Text(message)
                            .monarchTextStyle(.body)
                            .foregroundStyle(theme.colors[role: \.textMuted])
                            .fixedSize(horizontal: false, vertical: true)
                    }
                }
                content
            }
            .frame(maxWidth: .infinity, alignment: .leading)
            .padding(theme.spacing.space5)
            if hasFooter {
                VStack(alignment: .trailing, spacing: theme.spacing.space2) {
                    footer
                }
                .frame(maxWidth: .infinity, alignment: .trailing)
                .padding(.horizontal, theme.spacing.space5)
                .padding(.vertical, theme.spacing.space3)
                .background(theme.colors[role: \.bgSubtle])
                .overlay(alignment: .top) {
                    Rectangle().fill(theme.colors[role: \.borderSubtle]).frame(height: MonarchShapes.borderWidth)
                }
            }
        }
        .clipShape(MonarchShapes.container)
        .monarchElevation(.lg, fill: \.bgSurface, in: MonarchShapes.container)
        .overlay(
            MonarchShapes.container.stroke(theme.colors[role: \.borderSubtle], lineWidth: MonarchShapes.borderWidth)
        )
        .accessibilityElement(children: .contain)
        .accessibilityAddTraits(.isModal)
        .accessibilityAction(.escape) { onEscape?() }
    }
}

extension MonarchDialogPanel where Content == EmptyView {
    /// A dialog card with no content between the message and the footer.
    public init(
        _ title: String,
        message: String? = nil,
        onEscape: (@MainActor () -> Void)? = nil,
        onClose: (@MainActor () -> Void)? = nil,
        @ViewBuilder footer: () -> Footer
    ) {
        self.init(
            title,
            message: message,
            onEscape: onEscape,
            onClose: onClose,
            content: { EmptyView() },
            footer: footer
        )
    }
}

/// Shows a ``MonarchDialogPanel`` over the whole screen, on a dimmed backdrop, while `isPresented` is true. It is a
/// full-screen cover with a clear background, so the system moves VoiceOver focus into it and back.
struct MonarchDialogModifier<DialogContent: View, DialogFooter: View>: ViewModifier {
    @Binding var isPresented: Bool
    let title: String
    let message: String?
    let onDismiss: (@MainActor () -> Void)?
    let showsCloseButton: Bool
    let hasFooter: Bool
    let dialogContent: DialogContent
    let dialogFooter: DialogFooter

    @Environment(\.monarchTheme) private var theme

    func body(content: Content) -> some View {
        let cover = ZStack {
            Color.black.opacity(0.4)
                .ignoresSafeArea()
                .accessibilityHidden(true)
            ScrollView {
                MonarchDialogPanel(
                    title,
                    message: message,
                    onEscape: { dismiss() },
                    onClose: showsCloseButton ? { @MainActor in dismiss() } : nil,
                    hasFooter: hasFooter,
                    content: { dialogContent },
                    footer: { dialogFooter }
                )
                .padding(theme.spacing.space4)
                .frame(maxWidth: theme.spacing.space24 * 5)
                .containerRelativeFrame(.vertical, alignment: .center)
            }
            .scrollBounceBehavior(.basedOnSize)
            .contentMargins(.vertical, theme.spacing.space4, for: .scrollContent)
        }
        .presentationBackground(.clear)
        #if os(iOS)
            return content.fullScreenCover(isPresented: $isPresented, onDismiss: onDismiss) { cover }
        #else
            return content.sheet(isPresented: $isPresented, onDismiss: onDismiss) { cover }
        #endif
    }

    @MainActor private func dismiss() {
        isPresented = false
    }
}

extension View {
    /// Shows a Monarch dialog over this view while `isPresented` is true.
    ///
    /// The dialog is the same card as the web's: heading, message, content and a footer bar of actions. Unlike the
    /// system alert and confirmation dialog it can hold a labelled field and any number of buttons in the order the
    /// design gives them. The footer's buttons close it by setting `isPresented` to false.
    ///
    /// - Parameters:
    ///   - isPresented: Whether the dialog is shown.
    ///   - title: The heading.
    ///   - message: A line or two under the heading.
    ///   - showsCloseButton: Whether the heading has a close button (an X), as the web's dialogs do.
    ///   - onDismiss: Runs when the dialog has closed.
    ///   - content: What sits between the message and the footer.
    ///   - footer: The actions.
    /// - Returns: The view with the dialog over it.
    public func monarchDialog<DialogContent: View, DialogFooter: View>(
        isPresented: Binding<Bool>,
        title: String,
        message: String? = nil,
        showsCloseButton: Bool = false,
        onDismiss: (@MainActor () -> Void)? = nil,
        @ViewBuilder content: () -> DialogContent,
        @ViewBuilder footer: () -> DialogFooter
    ) -> some View {
        modifier(
            MonarchDialogModifier(
                isPresented: isPresented,
                title: title,
                message: message,
                onDismiss: onDismiss,
                showsCloseButton: showsCloseButton,
                hasFooter: true,
                dialogContent: content(),
                dialogFooter: footer()
            )
        )
    }
}

/// The strings of the dialog card.
enum MonarchDialogStrings {
    /// The name of the close button.
    static var close: String {
        MonarchStrings.string("monarch.dialogPanel.close", table: "MonarchDialogPanel")
    }
}

extension View {
    /// Shows a Monarch dialog that has no footer of actions, such as a share dialog whose controls sit in the content.
    ///
    /// - Parameters:
    ///   - isPresented: Whether the dialog is shown.
    ///   - title: The heading.
    ///   - message: A line or two under the heading.
    ///   - showsCloseButton: Whether the heading has a close button (an X).
    ///   - onDismiss: Runs when the dialog has closed.
    ///   - content: What sits under the message.
    /// - Returns: The view with the dialog over it.
    public func monarchDialog<DialogContent: View>(
        isPresented: Binding<Bool>,
        title: String,
        message: String? = nil,
        showsCloseButton: Bool = false,
        onDismiss: (@MainActor () -> Void)? = nil,
        @ViewBuilder content: () -> DialogContent
    ) -> some View {
        modifier(
            MonarchDialogModifier(
                isPresented: isPresented,
                title: title,
                message: message,
                onDismiss: onDismiss,
                showsCloseButton: showsCloseButton,
                hasFooter: false,
                dialogContent: content(),
                dialogFooter: EmptyView()
            )
        )
    }
}
