import Foundation
import SwiftUI

#if os(iOS)
    // UIKit for UIPrintInteractionController: SwiftUI has no print API on iOS.
    import UIKit
#endif

/// Renders an invoice to PDF pages with SwiftUI's `ImageRenderer` and hands
/// them to the system print panel.
@MainActor
enum MonarchInvoicePrinter {
    /// The view laid out at the printable width of a page and drawn as PDF
    /// pages, cut at the page height. Returns `nil` when nothing was drawn.
    static func pdf(
        _ view: some View,
        pageSize: CGSize = MonarchInvoiceViewTokens.printPageSize,
        margin: CGFloat = MonarchInvoiceViewTokens.printMargin
    ) -> Data? {
        let width = pageSize.width - margin * 2
        let pageHeight = pageSize.height - margin * 2
        let renderer = ImageRenderer(content: view.frame(width: width))
        renderer.proposedSize = ProposedViewSize(width: width, height: nil)
        let data = NSMutableData()
        renderer.render { size, draw in
            var box = CGRect(origin: .zero, size: pageSize)
            guard let consumer = CGDataConsumer(data: data as CFMutableData),
                let context = CGContext(consumer: consumer, mediaBox: &box, nil)
            else { return }
            let pages = MonarchInvoiceViewTokens.pageCount(contentHeight: size.height, pageHeight: pageHeight)
            for page in 0..<pages {
                context.beginPDFPage(nil)
                context.saveGState()
                context.clip(to: CGRect(x: margin, y: margin, width: width, height: pageHeight))
                // PDF space starts at the bottom. Move the slice of the view
                // for this page into the printable area.
                let offset = margin + pageHeight - size.height + CGFloat(page) * pageHeight
                context.translateBy(x: margin, y: offset)
                draw(context)
                context.restoreGState()
                context.endPDFPage()
            }
            context.closePDF()
        }
        return data.length > 0 ? data as Data : nil
    }

    /// Whether this platform has a system print panel.
    static var canPrint: Bool {
        #if os(iOS)
            true
        #else
            false
        #endif
    }

    #if os(iOS)
        /// Shows the system print panel for a PDF.
        static func present(_ pdf: Data, jobName: String) {
            let info = UIPrintInfo.printInfo()
            info.outputType = .general
            info.jobName = jobName
            let controller = UIPrintInteractionController.shared
            controller.printInfo = info
            controller.printingItem = pdf
            controller.present(animated: true)
        }
    #endif
}
