import Foundation
import SwiftUI
import Testing

@testable import MonarchUI

/// A URL from a literal that is known to be valid.
private func link(_ string: String) -> URL {
    URL(string: string) ?? URL(fileURLWithPath: "/")
}

private let blog = link("https://www.example.com/blog/march-release")
private let unsafe = link("javascript:alert(1)")

enum MonarchLinkCardFixtures: ComponentFixtures {
    static let id = "link-card"
    static let fixtures = [
        Fixture("medium with picture, video, dismiss") {
            MonarchLinkCard(
                "Release notes for March: faster search, shared views and a new public API for every plan",
                url: blog,
                description:
                    "Search is now three times faster, views can be shared with guests, and the API reaches every object.",
                image: Image(systemName: "photo"),
                imageSize: CGSize(width: 1200, height: 630),
                isVideo: true
            ) {}
        },
        Fixture("small with thumbnail and without picture") {
            VStack {
                MonarchLinkCard(
                    "Pricing",
                    url: blog,
                    description: "Plans for teams of every size.",
                    siteName: "Example",
                    image: Image(systemName: "photo"),
                    size: .sm
                )
                MonarchLinkCard("Contact us", url: link("mailto:hello@example.com"), image: nil, size: .sm) {}
            }
        },
        Fixture("unsafe link, no picture, loading URL placeholder") {
            VStack {
                MonarchLinkCard("Not opened", url: unsafe, description: "Shown as plain text.", image: nil)
                MonarchLinkCard("Loading picture", url: blog, imageURL: nil, faviconURL: nil)
            }
        },
    ]
}

@Suite("MonarchLinkCard")
struct MonarchLinkCardTests {
    @Test("Pictures keep their ratio between near square and a wide banner")
    func ratio() {
        #expect(MonarchLinkCardTokens.imageRatio(CGSize(width: 1200, height: 630)) == CGFloat(1200) / 630)
        #expect(MonarchLinkCardTokens.imageRatio(CGSize(width: 100, height: 400)) == 1.2)
        #expect(MonarchLinkCardTokens.imageRatio(CGSize(width: 3000, height: 300)) == 2.2)
        #expect(MonarchLinkCardTokens.imageRatio(nil) == 2.2)
        #expect(MonarchLinkCardTokens.imageRatio(CGSize(width: 0, height: 10)) == 2.2)
    }

    @Test("Only http, https and mailto links open")
    func safeLinks() {
        #expect(MonarchLinkCardTokens.isSafeLink(blog))
        #expect(MonarchLinkCardTokens.isSafeLink(link("HTTP://example.com")))
        #expect(MonarchLinkCardTokens.isSafeLink(link("mailto:a@example.com")))
        #expect(!MonarchLinkCardTokens.isSafeLink(unsafe))
        #expect(!MonarchLinkCardTokens.isSafeLink(link("file:///etc/hosts")))
        #expect(!MonarchLinkCardTokens.isSafeLink(link("relative/path")))
    }

    @Test("The site is the given name, or the host name without www")
    func site() {
        #expect(MonarchLinkCardTokens.siteName(nil, url: blog) == "example.com")
        #expect(MonarchLinkCardTokens.siteName("", url: blog) == "example.com")
        #expect(MonarchLinkCardTokens.siteName("Example blog", url: blog) == "Example blog")
    }

    @Test("VoiceOver reads the site, title, description, then Video")
    func accessibilityLabel() {
        let full = MonarchLinkCardTokens.accessibilityLabel(
            site: "example.com",
            title: "March",
            description: "New API",
            isVideo: true,
            videoLabel: "Video"
        )
        #expect(full == "example.com, March, New API, Video")
        let short = MonarchLinkCardTokens.accessibilityLabel(
            site: "",
            title: "March",
            description: nil,
            isVideo: false,
            videoLabel: "Video"
        )
        #expect(short == "March")
    }

    @Test("Text leaves room for the remove button only when there is no picture under it")
    func dismissInset() {
        #expect(MonarchLinkCardTokens.textTrailingInset(hasDismiss: false, hasImage: false) == 0)
        #expect(MonarchLinkCardTokens.textTrailingInset(hasDismiss: true, hasImage: true) == 0)
        #expect(
            MonarchLinkCardTokens.textTrailingInset(hasDismiss: true, hasImage: false)
                == MonarchAccessibility.minimumHitTarget - MonarchCardTokens.padding
        )
    }

    @Test("The built-in strings resolve, with the title in the remove label")
    func strings() {
        #expect(MonarchLinkCardStrings.video == "Video")
        #expect(MonarchLinkCardStrings.dismiss("March") == "Remove the preview of March")
    }

    @Test("A medium card with a picture is taller than one without")
    @MainActor
    func pictureTakesSpace() {
        let with = Host.layout(MonarchLinkCard("March", url: blog, image: Image(systemName: "photo")))
        let without = Host.layout(MonarchLinkCard("March", url: blog, image: nil))
        #expect(with.height > without.height)
    }
}
