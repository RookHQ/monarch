import SwiftUI

/// How ``MonarchAiInlinePromptTextField`` draws and behaves.
struct MonarchAiInlinePromptTextFieldStyle: Equatable {
    /// The spoken name of the text.
    let title: String
    /// The title of the "Ask AI" menu item and VoiceOver action.
    let menuTitle: String
    /// The font size after Dynamic Type.
    let fontSize: CGFloat
    /// The theme's font family, or `nil` for the system font.
    let fontFamily: String?
    /// The extra space between lines.
    let lineSpacing: CGFloat
    /// The text colour.
    let textColor: Color
    /// Whether the layout is right to left.
    let isRightToLeft: Bool
    /// Whether the text can be edited.
    let isEditable: Bool
    /// Whether "Ask AI" is offered.
    let isMenuEnabled: Bool
}

#if os(iOS)
    // UIKit, because SwiftUI's TextEditor and Text with textSelection on
    // iOS 17 neither report the selected range nor let an app add items to
    // the text edit menu. UITextView does both, through
    // textView(_:editMenuForTextIn:suggestedActions:) (iOS 16).
    import UIKit

    /// The text view of ``MonarchAiInlinePromptTextView`` on iOS.
    struct MonarchAiInlinePromptTextField: UIViewRepresentable {
        @Binding var text: String
        let style: MonarchAiInlinePromptTextFieldStyle
        let onAskAi: @MainActor (String) -> Void

        func makeCoordinator() -> Coordinator {
            Coordinator(self)
        }

        func makeUIView(context: Context) -> UITextView {
            let view = UITextView()
            view.backgroundColor = .clear
            view.textContainerInset = .zero
            view.textContainer.lineFragmentPadding = 0
            view.isScrollEnabled = false
            view.adjustsFontForContentSizeCategory = false
            view.setContentCompressionResistancePriority(.defaultLow, for: .horizontal)
            view.delegate = context.coordinator
            context.coordinator.apply(style, to: view)
            view.text = text
            return view
        }

        func updateUIView(_ view: UITextView, context: Context) {
            context.coordinator.parent = self
            if view.text != text, view.markedTextRange == nil {
                view.text = text
            }
            context.coordinator.apply(style, to: view)
        }

        func sizeThatFits(_ proposal: ProposedViewSize, uiView: UITextView, context: Context) -> CGSize? {
            guard let width = proposal.width, width.isFinite, width > 0 else { return nil }
            let fitted = uiView.sizeThatFits(CGSize(width: width, height: .greatestFiniteMagnitude))
            return CGSize(width: width, height: fitted.height)
        }

        /// Reports edits and the selection, and builds the text menu.
        @MainActor
        final class Coordinator: NSObject, UITextViewDelegate {
            var parent: MonarchAiInlinePromptTextField
            private var applied: MonarchAiInlinePromptTextFieldStyle?

            init(_ parent: MonarchAiInlinePromptTextField) {
                self.parent = parent
            }

            func apply(_ style: MonarchAiInlinePromptTextFieldStyle, to view: UITextView) {
                guard style != applied else { return }
                applied = style
                let font =
                    style.fontFamily.flatMap { UIFont(name: $0, size: style.fontSize) }
                    ?? UIFont.systemFont(ofSize: style.fontSize)
                let paragraph = NSMutableParagraphStyle()
                paragraph.lineSpacing = style.lineSpacing
                paragraph.baseWritingDirection = style.isRightToLeft ? .rightToLeft : .leftToRight
                let attributes: [NSAttributedString.Key: Any] = [
                    .font: font,
                    .foregroundColor: UIColor(style.textColor),
                    .paragraphStyle: paragraph,
                ]
                view.font = font
                view.textColor = UIColor(style.textColor)
                view.typingAttributes = attributes
                if view.markedTextRange == nil {
                    view.textStorage.setAttributes(
                        attributes,
                        range: NSRange(location: 0, length: view.textStorage.length)
                    )
                }
                view.semanticContentAttribute = style.isRightToLeft ? .forceRightToLeft : .forceLeftToRight
                view.textAlignment = .natural
                view.isEditable = style.isEditable
                view.isSelectable = true
                view.accessibilityLabel = style.title
                refreshAccessibility(view)
            }

            func textViewDidChange(_ textView: UITextView) {
                let text = textView.text ?? ""
                if parent.text != text { parent.text = text }
                refreshAccessibility(textView)
            }

            func textViewDidChangeSelection(_ textView: UITextView) {
                refreshAccessibility(textView)
            }

            func textView(
                _ textView: UITextView,
                editMenuForTextIn range: NSRange,
                suggestedActions: [UIMenuElement]
            ) -> UIMenu? {
                guard let style = applied else { return nil }
                let selection = MonarchAiInlinePromptTokens.selection(
                    in: textView.text ?? "",
                    location: range.location,
                    length: range.length
                )
                guard MonarchAiInlinePromptTokens.showsMenuItem(isEnabled: style.isMenuEnabled, selection: selection),
                    let selection
                else { return nil }
                let ask = UIAction(
                    title: style.menuTitle,
                    image: UIImage(systemName: MonarchAiInlinePromptTokens.askSymbol)
                ) { [weak self] _ in
                    MainActor.assumeIsolated { self?.parent.onAskAi(selection) }
                }
                return UIMenu(children: suggestedActions + [ask])
            }

            private func refreshAccessibility(_ view: UITextView) {
                guard let style = applied, style.isMenuEnabled else {
                    view.accessibilityCustomActions = nil
                    return
                }
                let text = view.text ?? ""
                let range = view.selectedRange
                let selection = MonarchAiInlinePromptTokens.selection(
                    in: text,
                    location: range.location,
                    length: range.length
                )
                guard let value = MonarchAiInlinePromptTokens.accessibilityActionText(text, selection: selection) else {
                    view.accessibilityCustomActions = nil
                    return
                }
                view.accessibilityCustomActions = [
                    UIAccessibilityCustomAction(name: style.menuTitle) { [weak self] _ in
                        MainActor.assumeIsolated { self?.parent.onAskAi(value) }
                        return true
                    }
                ]
            }
        }
    }
#else
    /// The text of ``MonarchAiInlinePromptTextView`` where UIKit is not
    /// available: the macOS host, which exists only so the package builds and
    /// tests without a simulator. A `TextEditor` (or selectable `Text`) whose
    /// context menu passes the whole text, because SwiftUI does not report
    /// the selection.
    struct MonarchAiInlinePromptTextField: View {
        @Binding var text: String
        let style: MonarchAiInlinePromptTextFieldStyle
        let onAskAi: @MainActor (String) -> Void

        var body: some View {
            Group {
                if style.isEditable {
                    TextEditor(text: $text)
                        .scrollContentBackground(.hidden)
                } else {
                    Text(text).textSelection(.enabled)
                }
            }
            .monarchTextStyle(MonarchAiInlinePromptTokens.textStyle)
            .foregroundStyle(style.textColor)
            .accessibilityLabel(style.title)
            .contextMenu {
                if style.isMenuEnabled,
                    let value = MonarchAiInlinePromptTokens.accessibilityActionText(text, selection: nil)
                {
                    Button(style.menuTitle) { onAskAi(value) }
                }
            }
        }
    }
#endif
