import Foundation
import SwiftUI
import Testing

@testable import MonarchUI

private let file = URL(fileURLWithPath: NSTemporaryDirectory()).appendingPathComponent("receipt-1042.pdf")

enum MonarchReceiptPreviewFixtures: ComponentFixtures {
    static let id = "receipt-preview"
    static let fixtures = [
        Fixture("local file, every variant and size") {
            VStack(alignment: .leading) {
                ForEach(MonarchButtonVariant.allCases, id: \.self) { variant in
                    ForEach(MonarchButtonSize.allCases, id: \.self) { size in
                        MonarchReceiptPreview(url: file, variant: variant, size: size)
                    }
                }
            }
        },
        Fixture("loader, custom labels, no receipt, disabled") {
            VStack(alignment: .leading) {
                MonarchReceiptPreview(labels: MonarchReceiptPreviewLabels(view: "Open the scanned invoice for March")) {
                    file
                }
                MonarchReceiptPreview(url: nil)
                MonarchReceiptPreview(url: URL(string: "https://example.com/r.pdf"))
                MonarchReceiptPreview(url: file).disabled(true)
            }
        },
    ]
}

@Suite("MonarchReceiptPreview")
struct MonarchReceiptPreviewTests {
    @Test("A local file is previewed directly; a loader runs on press; anything else is no receipt")
    func action() {
        #expect(MonarchReceiptPreviewTokens.action(url: file, hasLoader: false) == .preview(file))
        #expect(MonarchReceiptPreviewTokens.action(url: nil, hasLoader: true) == .load)
        #expect(MonarchReceiptPreviewTokens.action(url: nil, hasLoader: false) == .none)
        #expect(
            MonarchReceiptPreviewTokens.action(url: URL(string: "https://example.com/r.pdf"), hasLoader: false) == .none
        )
    }

    @Test("The file is handed back only when the preview closes")
    func closedFile() {
        #expect(MonarchReceiptPreviewTokens.closedFile(old: file, new: nil) == file)
        #expect(MonarchReceiptPreviewTokens.closedFile(old: nil, new: file) == nil)
        #expect(MonarchReceiptPreviewTokens.closedFile(old: nil, new: nil) == nil)
    }

    @Test("The built-in strings resolve")
    func strings() {
        #expect(MonarchReceiptPreviewStrings.view == "View receipt")
        #expect(MonarchReceiptPreviewStrings.none == "No receipt")
        #expect(MonarchReceiptPreviewStrings.loading == "Loading receipt")
        #expect(MonarchReceiptPreviewStrings.loadError == "The receipt could not be loaded.")
    }

    @Test("No receipt is a line of text, lower than the button")
    @MainActor
    func noneIsText() {
        let none = Host.layout(MonarchReceiptPreview(url: nil).fixedSize())
        let button = Host.layout(MonarchReceiptPreview(url: file).fixedSize())
        #expect(none.height < button.height)
    }

    #if os(iOS)
        @Test("The button keeps a 44-point touch target")
        @MainActor
        func hitTarget() {
            let size = Host.layout(MonarchReceiptPreview(url: file).fixedSize())
            #expect(size.height >= MonarchAccessibility.minimumHitTarget)
        }
    #endif
}
