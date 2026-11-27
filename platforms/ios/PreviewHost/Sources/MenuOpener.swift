import UIKit

/// Opens the first menu on screen, for the documentation previews. SwiftUI's `Menu` and `contextMenu` have no state
/// that presents them, so the host asks the context-menu interaction UIKit attaches to them to open at its view.
/// This uses a private selector; it exists only in this host app, never in the library.
@MainActor
enum MenuOpener {
    static func open() {
        guard let window = UIApplication.shared.connectedScenes
            .compactMap({ ($0 as? UIWindowScene)?.windows.first(where: \.isKeyWindow) }).first else { return }
        guard let (view, interaction) = find(in: window) else {
            var lines: [String] = []
            dump(window, into: &lines, depth: 0)
            if let docs = FileManager.default.urls(for: .documentDirectory, in: .userDomainMask).first {
                try? lines.joined(separator: "\n").write(
                    to: docs.appendingPathComponent("menu-debug.txt"), atomically: true, encoding: .utf8)
            }
            return
        }
        let selector = NSSelectorFromString("_presentMenuAtLocation:")
        guard let interaction = interaction as? NSObject, interaction.responds(to: selector) else { return }
        typealias Present = @convention(c) (AnyObject, Selector, CGPoint) -> Void
        let function = unsafeBitCast(interaction.method(for: selector), to: Present.self)
        function(interaction, selector, CGPoint(x: view.bounds.midX, y: view.bounds.midY))
    }

    /// Activates the first button in the accessibility tree, as a person would with VoiceOver.
    static func tapFirstButton() {
        guard let window = UIApplication.shared.connectedScenes
            .compactMap({ ($0 as? UIWindowScene)?.windows.first(where: \.isKeyWindow) }).first else { return }
        _ = activate(in: window, depth: 0)
    }

    private static func activate(in object: NSObject, depth: Int) -> Bool {
        if depth > 40 { return false }
        if object.isAccessibilityElement, object.accessibilityTraits.contains(.button), object.accessibilityActivate() {
            return true
        }
        if let elements = object.accessibilityElements {
            for case let element as NSObject in elements where activate(in: element, depth: depth + 1) { return true }
        }
        if let view = object as? UIView {
            for subview in view.subviews where activate(in: subview, depth: depth + 1) { return true }
        }
        let count = object.accessibilityElementCount()
        if count != NSNotFound, count > 0 {
            for index in 0..<count {
                if let element = object.accessibilityElement(at: index) as? NSObject,
                    activate(in: element, depth: depth + 1)
                {
                    return true
                }
            }
        }
        return false
    }

    private static func dump(_ view: UIView, into lines: inout [String], depth: Int) {
        let kinds = view.interactions.map { String(describing: type(of: $0)) }
        lines.append(String(repeating: " ", count: depth) + String(describing: type(of: view)) + " " + kinds.joined(separator: ","))
        for subview in view.subviews { dump(subview, into: &lines, depth: depth + 1) }
    }

    private static func find(in view: UIView) -> (UIView, UIInteraction)? {
        for subview in view.subviews {
            if let found = find(in: subview) { return found }
        }
        if let interaction = view.interactions.first(where: { $0.responds(to: NSSelectorFromString("_presentMenuAtLocation:")) }) {
            return (view, interaction)
        }
        return nil
    }
}

/// Opens every sheet on screen at its largest size, for the documentation previews: a sheet that rests at the medium
/// detent shows only half of the content the web example shows. Host only, never in the library.
@MainActor
enum SheetExpander {
    static func expandAll() {
        guard let window = UIApplication.shared.connectedScenes
            .compactMap({ ($0 as? UIWindowScene)?.windows.first(where: \.isKeyWindow) }).first else { return }
        var controller = window.rootViewController
        while let current = controller {
            if let sheet = current.presentedViewController?.sheetPresentationController,
                sheet.detents.contains(where: { $0.identifier == .large })
            {
                sheet.animateChanges { sheet.selectedDetentIdentifier = .large }
            }
            controller = current.presentedViewController
        }
    }
}
