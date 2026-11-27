import MonarchTokens
import SwiftUI

@testable import MonarchUI

enum SkeletonFixtures: ComponentFixtures {
    static let id = "skeleton"
    static let fixtures = [
        Fixture("a card while its content loads") {
            MonarchCard {
                Text("Ada Lovelace").monarchTextStyle(.bodyStrong)
                Text("Joined in March, 12 open tickets").monarchTextStyle(.caption)
            }
            .redacted(reason: .placeholder)
            .padding()
        },
        Fixture("the same card loaded") {
            MonarchCard {
                Text("Ada Lovelace").monarchTextStyle(.bodyStrong)
                Text("Joined in March, 12 open tickets").monarchTextStyle(.caption)
            }
            .padding()
        },
    ]
}
