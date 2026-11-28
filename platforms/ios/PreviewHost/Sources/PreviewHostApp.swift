import MonarchFontsPoppins
import MonarchTokens
import MonarchUI
import SwiftUI
import UIKit

/// Shows one inventory example, for the documentation previews.
///
/// Launch arguments:
///   -preview <id>        the inventory id of the component
///   -example <n>         that inventory example, counted from 0. Without it, a curated showcase of the component if
///                        `Showcases` has one, otherwise the first example
///   -theme <name>        light, dark, hc-light or hc-dark (default light)
///
/// The text is set in Poppins (`MonarchFontsPoppins`), as D10.4 asks for the docs previews; the regression
/// baselines keep the system font.
///
/// The example sits at the top of the screen on the theme's canvas, in a column 354 points wide with 24 points
/// of padding on every side. Once the layout has settled the app writes the rectangle of that padded column,
/// in points on the screen, to `Documents/preview-rect.txt` as `<id> <x> <y> <width> <height> <capped>`.
/// `Scripts/build-previews.sh` takes a simulator screenshot and crops it to that rectangle.
@main
struct PreviewHostApp: App {
    init() {
        // The same dates and numbers on every machine: UTC and British English, as the web previews (24-hour
        // clock, Monday first). `Scripts/build-previews.sh` also passes
        // -AppleLocale en_GB -AppleLanguages (en-GB) -AppleICUForce24HourTime YES, which fixes `Locale.current`.
        NSTimeZone.default = TimeZone(identifier: "UTC")!
    }

    var body: some Scene {
        WindowGroup { PreviewRoot() }
    }
}

private struct PreviewRequest {
    let id: String
    let index: Int?
    let theme: String
    /// `-offset <points>`: show only the part of the example from this height on (a strip of a tall preview).
    let offset: CGFloat

    init() {
        let arguments = ProcessInfo.processInfo.arguments
        func value(_ flag: String) -> String? {
            guard let at = arguments.firstIndex(of: flag), at + 1 < arguments.count else { return nil }
            return arguments[at + 1]
        }
        id = value("-preview") ?? ""
        index = value("-example").flatMap(Int.init)
        theme = value("-theme") ?? "light"
        offset = value("-offset").flatMap(Double.init).map { CGFloat($0) } ?? 0
    }

    var scheme: MonarchSchemePreference { theme.hasSuffix("dark") ? .dark : .light }
    var contrast: MonarchContrastPreference { theme.hasPrefix("hc-") ? .more : .standard }
}

private struct PreviewRoot: View {
    private let request = PreviewRequest()

    /// The text size of the screenshot: `PREVIEW_SIZE` (through `SIMCTL_CHILD_PREVIEW_SIZE`) as `ax1` to `ax5` for the
    /// accessibility sizes, `large` (the default) otherwise. Used to look at the largest sizes in simulator images.
    private static var textSize: DynamicTypeSize {
        switch ProcessInfo.processInfo.environment["PREVIEW_SIZE"] {
        case "ax1": .accessibility1
        case "ax2": .accessibility2
        case "ax3": .accessibility3
        case "ax4": .accessibility4
        case "ax5": .accessibility5
        default: .large
        }
    }

    /// Gregorian, weeks start on Monday, as the web previews.
    private static var calendar: Calendar {
        var calendar = Calendar(identifier: .gregorian)
        calendar.firstWeekday = 2
        calendar.minimumDaysInFirstWeek = 4
        return calendar
    }

    var body: some View {
        PreviewScreen(request: request)
            .monarchTheme(scheme: request.scheme, contrast: request.contrast, typography: .poppins)
            // The window follows too, so a system alert, menu or sheet is drawn in the same scheme.
            .preferredColorScheme(request.scheme == .dark ? .dark : .light)
            .dynamicTypeSize(Self.textSize)
            .environment(\.locale, Locale(identifier: "en_GB"))
            .environment(\.calendar, Self.calendar)
            .environment(\.timeZone, TimeZone(identifier: "UTC")!)
    }
}

/// The example sits at the top of the screen in a column `columnWidth` wide, as tall as it is (a screen-level
/// recipe is cut at `maxHeight`), with `padding` around it.
private struct PreviewScreen: View {
    let request: PreviewRequest
    @Environment(\.monarchTheme) private var theme
    /// The height the example takes at the column width, measured with nothing limiting it.
    @State private var natural: CGFloat = 0
    /// The padded column on screen, in points.
    @State private var frame: CGRect = .zero

    /// A screen-level recipe (taller than `maxHeight` in the column) is shown full bleed instead: the whole
    /// screen width, no padding, so its own background and margins are what the reader sees.
    @State private var isScreen = false

    /// Examples whose component focuses a field on appear.
    static let dismissesKeyboard: Set<String> = ["ai-inline-prompt"]

    static let columnWidth: CGFloat = 354
    static let padding: CGFloat = 24
    static let screenWidth: CGFloat = 402
    /// A screen-level recipe fills the screen; the crop stops here.
    static let maxHeight: CGFloat = 740

    var body: some View {
        let examples = InventoryExamples.all.filter { $0.id == request.id }
        // An example with a presentation (a sheet, alert, dialog, popover or menu) is shown with it open: it is the
        // first example that has `previewPresented` in the inventory file, built with `PreviewMode.presented`.
        let chosen: InventoryExample? = {
            if let index = request.index { return examples.indices.contains(index) ? examples[index] : nil }
            // The first example is the web primary example (the fixture); the preview always shows it.
            return examples.first
        }()
        // The docs landing page's compositions (`landing-tile-*`, `landing-float-*`) come before the showcases.
        let showcase =
            request.index == nil
            ? (LandingCompositions.view(for: request.id) ?? Showcases.view(for: request.id)) : nil
        let presented = chosen?.presented == true || showcase != nil && Showcases.presenting.contains(request.id)
        let content: AnyView? = {
            if let index = request.index, !examples.indices.contains(index) { return nil }
            if let showcase { return showcase }
            PreviewMode.presented = chosen?.presented == true || chosen?.acts == true
            return chosen?.make()
        }()
        ZStack(alignment: .top) {
            theme.colors.bgCanvas.ignoresSafeArea()
            if let content {
                content
                    // A picker that opens a sheet of its own is drawn open (`previewTap` in the inventory file).
                    .environment(\.monarchStartsPresented, chosen?.taps == true)
                    .frame(
                        width: isScreen ? Self.screenWidth : Self.columnWidth,
                        height: PreviewHeights.height(for: request.id),
                        alignment: .top
                    )
                    .fixedSize(horizontal: false, vertical: true)
                    .onGeometryChange(for: CGFloat.self) {
                        $0.size.height
                    } action: {
                        natural = $0
                        if $0 > Self.maxHeight, ScreenRecipes.ids.contains(request.id) { isScreen = true }
                    }
                    // A tall example is shown in strips of `maxHeight`; this run shows the one that starts at
                    // `offset`. The layout box keeps the natural height, the window moves over it.
                    .frame(
                        width: isScreen ? Self.screenWidth : Self.columnWidth,
                        height: natural > 0 ? natural : nil,
                        alignment: .top
                    )
                    .offset(y: -request.offset)
                    .frame(
                        width: isScreen ? Self.screenWidth : Self.columnWidth,
                        height: natural > 0 ? min(max(natural - request.offset, 0), Self.maxHeight) : nil,
                        alignment: .top
                    )
                    .modifier(ClipWhen(isOn: natural > Self.maxHeight || request.offset > 0))
                    .background(theme.colors.bgCanvas)
                    .padding(.horizontal, isScreen ? 0 : Self.padding)
                    .padding(.top, isScreen || request.offset > 0 ? 0 : Self.padding)
                    .padding(.bottom, isScreen || natural - request.offset > Self.maxHeight ? 0 : Self.padding)
                    .onGeometryChange(for: CGRect.self) {
                        $0.frame(in: .global)
                    } action: {
                        frame = $0
                    }
                    .task {
                        // Fonts, images and entrance animations settle first.
                        if presented {
                            // The presentation opens by state; a menu is opened by the host, then given time to settle.
                            try? await Task.sleep(for: .seconds(1.2))
                            if chosen?.opensMenu == true { MenuOpener.open() }
                            try? await Task.sleep(for: .seconds(1.0))
                            SheetExpander.expandAll()
                            try? await Task.sleep(for: .seconds(0.8))
                            // An alert with a text field raises the keyboard; the image shows the alert alone.
                            for _ in 0..<2 {
                                UIApplication.shared.sendAction(
                                    #selector(UIResponder.resignFirstResponder),
                                    to: nil,
                                    from: nil,
                                    for: nil
                                )
                                for case let scene as UIWindowScene in UIApplication.shared.connectedScenes {
                                    for window in scene.windows { window.endEditing(true) }
                                }
                                try? await Task.sleep(for: .seconds(0.6))
                            }
                            reportScreen(menu: chosen?.opensMenu == true || request.id == "lightbox" || request.id == "receipt-preview")
                        } else {
                            try? await Task.sleep(for: .seconds(1.5))
                            // A component that focuses its field when it appears raises the keyboard and pushes the
                            // screen up; the image shows the example alone.
                            if Self.dismissesKeyboard.contains(request.id) {
                                for case let scene as UIWindowScene in UIApplication.shared.connectedScenes {
                                    for window in scene.windows { window.endEditing(true) }
                                }
                                try? await Task.sleep(for: .seconds(1.0))
                            }
                            report(frame, capped: natural > Self.maxHeight)
                        }
                    }
            } else {
                Text("No example for \u{201C}\(request.id)\u{201D}").padding()
            }
        }
    }

    /// A presentation covers the window outside the column: the capture is the whole screen (the last field is 1).
    /// With `menu`, the capture keeps every band of the presentation, such as the control a menu opened from beside the menu,
    /// or the bars around a full-screen viewer (a last field of `menu`).
    private func reportScreen(menu: Bool = false) {
        guard let documents = FileManager.default.urls(for: .documentDirectory, in: .userDomainMask).first else {
            return
        }
        let screen = UIScreen.main.bounds
        let line =
            "\(request.id) 0.0 0.0 \(screen.width) \(screen.height) 0 1 \(menu ? "menu" : "0") \(frame.minY)\n"
        try? line.write(to: documents.appendingPathComponent("preview-rect.txt"), atomically: true, encoding: .utf8)
    }

    private func report(_ rect: CGRect, capped: Bool) {
        guard let documents = FileManager.default.urls(for: .documentDirectory, in: .userDomainMask).first else {
            return
        }
        let line =
            "\(request.id) \(rect.minX) \(rect.minY) \(rect.width) \(rect.height) \(capped ? 1 : 0) 0 \(natural)\n"
        try? line.write(to: documents.appendingPathComponent("preview-rect.txt"), atomically: true, encoding: .utf8)
    }
}

/// Clips the content to its frame only when `isOn`.
private struct ClipWhen: ViewModifier {
    let isOn: Bool

    func body(content: Content) -> some View {
        if isOn { content.clipped() } else { content }
    }
}
