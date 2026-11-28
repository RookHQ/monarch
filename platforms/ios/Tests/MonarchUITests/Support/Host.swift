import SwiftUI

#if canImport(UIKit)
    import UIKit
#elseif canImport(AppKit)
    import AppKit
#endif

/// Hosts SwiftUI views without a running app, so the tests evaluate view
/// bodies and layout for real instead of only constructing values.
@MainActor
enum Host {
    /// The width of the test canvas, an iPhone in portrait.
    static let width: CGFloat = 390

    /// Puts the view in a platform hosting view, runs layout, and returns the
    /// size the view takes at the canvas width.
    static func layout(_ view: some View) -> CGSize {
        let content = view.frame(width: width)
        #if canImport(UIKit)
            let controller = UIHostingController(rootView: content)
            // Measure the view itself. A window on a device-shaped screen has
            // safe-area insets, which the controller would otherwise add to
            // the fitting size (66 points on an iPhone 17 Pro).
            controller.safeAreaRegions = []
            let window = UIWindow(frame: CGRect(x: 0, y: 0, width: width, height: 844))
            window.rootViewController = controller
            window.isHidden = false
            controller.view.setNeedsLayout()
            controller.view.layoutIfNeeded()
            let size = controller.sizeThatFits(in: CGSize(width: width, height: .greatestFiniteMagnitude))
            window.isHidden = true
            window.rootViewController = nil
            return size
        #elseif canImport(AppKit)
            let hosting = NSHostingView(rootView: content)
            hosting.frame = CGRect(x: 0, y: 0, width: width, height: 844)
            hosting.layoutSubtreeIfNeeded()
            return hosting.fittingSize
        #endif
    }

    /// Draws the view to a bitmap with SwiftUI's own renderer.
    static func image(_ view: some View) -> CGImage? {
        let renderer = ImageRenderer(content: view.frame(width: width))
        renderer.scale = 2
        return renderer.cgImage
    }

    /// The width a view asks for when nothing constrains it: the length of
    /// one line of text, the natural width of a row.
    static func idealWidth(_ view: some View) -> CGFloat {
        let content = view.fixedSize()
        #if canImport(AppKit)
            return NSHostingView(rootView: content).fittingSize.width
        #elseif canImport(UIKit)
            let unbounded = CGSize(width: CGFloat.greatestFiniteMagnitude, height: .greatestFiniteMagnitude)
            return UIHostingController(rootView: content).sizeThatFits(in: unbounded).width
        #endif
    }
}
