import CoreText
import Foundation
import MonarchUI
import Testing

@testable import MonarchFontsPoppins

@Suite("MonarchPoppins")
struct MonarchPoppinsTests {
    @Test("registers the three faces and they resolve by PostScript name")
    func registersFaces() {
        #expect(MonarchPoppins.register())
        for (_, name) in MonarchPoppins.faces {
            let font = CTFontCreateWithName(name as CFString, 14, nil)
            let resolved = CTFontCopyPostScriptName(font) as String
            #expect(resolved == name)
        }
    }

    @Test("registering twice is safe")
    func registersTwice() {
        #expect(MonarchPoppins.register())
        #expect(MonarchPoppins.register())
    }

    @Test("the typography hook carries the family and the faces")
    func typographyHook() {
        let typography = MonarchTypography.poppins
        #expect(typography.fontFamily == "Poppins")
        #expect(typography.faces[500] == "Poppins-Medium")
        #expect(typography.codeFontFamily == nil)
    }

    @Test("the licence text ships")
    func licenceShips() throws {
        let url = try #require(MonarchPoppins.licenseURL)
        let text = try String(contentsOf: url, encoding: .utf8)
        #expect(text.contains("SIL OPEN FONT LICENSE Version 1.1"))
    }
}
