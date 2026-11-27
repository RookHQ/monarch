import Foundation
import MonarchTokens
import SwiftUI
import Testing

@testable import MonarchUI

enum MonarchFaviconFixtures: ComponentFixtures {
    static let id = "favicon"
    static let fixtures = [
        Fixture("letter tile in every size") {
            HStack {
                ForEach(MonarchFaviconSize.allCases, id: \.self) { size in
                    MonarchFavicon(image: nil, name: "example.com", size: size)
                }
            }
        },
        Fixture("image in every shape") {
            HStack {
                ForEach(MonarchFaviconShape.allCases, id: \.self) { shape in
                    MonarchFavicon(image: Image(systemName: "globe"), name: "Globe", size: .lg, shape: shape)
                }
            }
        },
        Fixture("url that is not loaded") {
            HStack {
                MonarchFavicon(url: nil, name: "Acme", size: .lg)
                MonarchFavicon(url: URL(string: "file:///icon.png"), name: "files", size: .lg)
            }
        },
        Fixture("labelled, no name") {
            MonarchFavicon(image: nil, name: "", label: "Unknown site", size: .lg, shape: .circle)
        },
    ]
}

@Suite("MonarchFavicon")
struct MonarchFaviconTests {
    @Test("The host name of a URL or domain is lower-cased, without www")
    func hostname() {
        #expect(MonarchFaviconTokens.hostname("https://www.Example.com/a?b#c") == "example.com")
        #expect(MonarchFaviconTokens.hostname("http://user@shop.example.org:8080/x") == "shop.example.org")
        #expect(MonarchFaviconTokens.hostname("www.Example.com") == "example.com")
        #expect(MonarchFaviconTokens.hostname("  example.com ") == "example.com")
        #expect(MonarchFaviconTokens.hostname("Acme Inc") == "Acme Inc")
        #expect(MonarchFaviconTokens.hostname("   ").isEmpty)
    }

    @Test("The fallback letter is the first letter or digit, upper-cased")
    func letter() {
        #expect(MonarchFaviconTokens.letter(for: "example.com") == "E")
        #expect(MonarchFaviconTokens.letter(for: "https://www.monarchui.dev/docs") == "M")
        #expect(MonarchFaviconTokens.letter(for: "9gag.com") == "9")
        #expect(MonarchFaviconTokens.letter(for: "— élan") == "É")
        #expect(MonarchFaviconTokens.letter(for: "") == "?")
        #expect(MonarchFaviconTokens.letter(for: "...") == "?")
    }

    @Test("Only http and https URLs with a host are loaded")
    func loadableURL() {
        let secure = URL(string: "https://example.com/favicon.ico")
        #expect(MonarchFaviconTokens.loadableURL(secure) == secure)
        #expect(MonarchFaviconTokens.loadableURL(URL(string: "HTTP://example.com/i.png")) != nil)
        #expect(MonarchFaviconTokens.loadableURL(URL(string: "file:///tmp/icon.png")) == nil)
        #expect(MonarchFaviconTokens.loadableURL(URL(string: "data:image/png;base64,AAAA")) == nil)
        #expect(MonarchFaviconTokens.loadableURL(URL(string: "javascript:alert(1)")) == nil)
        #expect(MonarchFaviconTokens.loadableURL(URL(string: "/relative/icon.png")) == nil)
        #expect(MonarchFaviconTokens.loadableURL(nil) == nil)
    }

    @Test("Sizes are 16, 20 and 24 points")
    func sizes() {
        #expect(MonarchFaviconSize.allCases.map(MonarchFaviconTokens.side) == [16, 20, 24])
    }

    @Test("Shapes differ in their corner radius")
    func shapes() {
        #expect(MonarchFaviconTokens.cornerRadius(.square, side: 16) == 0)
        #expect(MonarchFaviconTokens.cornerRadius(.rounded, side: 16) == 4)
        #expect(MonarchFaviconTokens.cornerRadius(.circle, side: 16) == 8)
        #expect(MonarchFaviconTokens.cornerRadius(.circle, side: 24) == 12)
    }

    @Test("The letter is smaller than its tile and uses the muted roles")
    func letterTile() {
        #expect(MonarchFaviconTokens.letterScale > 0 && MonarchFaviconTokens.letterScale < 1)
        #expect(MonarchFaviconTokens.tile == \MonarchColorTokens.bgMuted)
        #expect(MonarchFaviconTokens.letter == \MonarchColorTokens.textMuted)
    }
}
