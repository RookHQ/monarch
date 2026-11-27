import Foundation
import MonarchTokens
import SwiftUI
import Testing

@testable import MonarchUI

private struct StopFailure: LocalizedError {
    var errorDescription: String? { "The session could not be closed." }
}

enum MonarchImpersonationBannerFixtures: ComponentFixtures {
    static let id = "impersonation-banner"
    static let fixtures = [
        Fixture("name, detail, start and reason") {
            MonarchImpersonationBanner(
                name: "Sam Lee",
                detail: "sam@example.com",
                reason: "Ticket 4821",
                startedAt: Date(timeIntervalSince1970: 1_800_000_000).addingTimeInterval(-300)
            ) {}
        },
        Fixture("name only") {
            MonarchImpersonationBanner(name: "Sam Lee") {}
        },
        Fixture("stopping") {
            MonarchImpersonationBanner(name: "Sam Lee", isStopping: true) {}
        },
        Fixture("failed stop, long name") {
            MonarchImpersonationBanner(
                name: "Alexandra Konstantinopoulou-Whitfield",
                errorMessage: "Couldn’t stop. Try again."
            )
        },
    ]
}

@Suite("MonarchImpersonationBanner")
struct MonarchImpersonationBannerTests {
    private let labels = MonarchImpersonationBannerLabels()

    @Test("The main line names the person, split so the name can be bold")
    func title() {
        #expect(MonarchImpersonationBannerTokens.titleText(name: "Sam", labels: labels) == "You are viewing as Sam")
        let parts = MonarchImpersonationBannerTokens.titleParts(name: "Sam", labels: labels)
        #expect(parts?.prefix == "You are viewing as ")
        #expect(parts?.suffix == "")
        let custom = MonarchImpersonationBannerLabels(viewingAs: { "Vous êtes \($0)" })
        #expect(MonarchImpersonationBannerTokens.titleParts(name: "Sam", labels: custom) == nil)
        #expect(MonarchImpersonationBannerTokens.titleText(name: "Sam", labels: custom) == "Vous êtes Sam")
    }

    @Test("The stop button names the person")
    func stopLabel() {
        #expect(MonarchImpersonationBannerTokens.stopLabel(name: "Sam", labels: labels) == "Stop viewing as Sam")
    }

    @Test("The reason line has its prefix; an empty reason is none")
    func reason() {
        #expect(MonarchImpersonationBannerTokens.reasonText("Ticket 4821", labels: labels) == "Reason: Ticket 4821")
        #expect(MonarchImpersonationBannerTokens.reasonText("", labels: labels) == nil)
        #expect(MonarchImpersonationBannerTokens.reasonText(nil, labels: labels) == nil)
    }

    @Test("A failure reads the error's description, or the fallback")
    func errorMessage() {
        #expect(
            MonarchImpersonationBannerTokens.errorMessage(StopFailure(), labels: labels)
                == "The session could not be closed."
        )
        #expect(
            MonarchImpersonationBannerTokens.errorMessage(CancellationError(), labels: labels)
                == "Couldn’t stop. Try again."
        )
    }

    @Test("Inverse colours with a warning edge")
    func appearance() {
        let appearance = MonarchImpersonationBannerTokens.appearance
        #expect(appearance.background == \MonarchColorTokens.bgInverse)
        #expect(appearance.text == \MonarchColorTokens.textInverse)
        #expect(appearance.muted == \MonarchColorTokens.textInverseMuted)
        #expect(appearance.edge == \MonarchColorTokens.warningSolid)
    }

    @Test("The built-in strings come from the component's table")
    func strings() {
        #expect(MonarchImpersonationBannerStrings.stop == "Stop")
        #expect(MonarchImpersonationBannerStrings.started == "Started")
        #expect(MonarchImpersonationBannerStrings.reason == "Reason:")
    }
}
