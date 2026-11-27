// swift-tools-version: 6.0
//
// Monarch for iOS. Two libraries: `MonarchTokens` (generated constants,
// Foundation only) and `MonarchUI` (SwiftUI theme, foundations, components).
//
// Tools version 6.0 selects the Swift 6 language mode, which includes complete
// strict concurrency checking, for every target below.

import Foundation
import PackageDescription

/// Language settings shared by every target.
let monarchSwiftSettings: [SwiftSetting] = [
    // `any` must be written for existential types.
    .enableUpcomingFeature("ExistentialAny")
]

let package = Package(
    name: "Monarch",
    defaultLocalization: "en",
    platforms: [
        // The supported platform. Change the deployment target here.
        .iOS(.v17),
        // Build convenience only, so `swift build` and `swift test` run on a
        // Mac without a simulator. macOS is not a supported product.
        .macOS(.v14),
    ],
    products: [
        .library(name: "MonarchTokens", targets: ["MonarchTokens"]),
        .library(name: "MonarchUI", targets: ["MonarchUI"]),
        // Optional: the Poppins typeface (SIL OFL 1.1). Apps that keep the system font do not link it.
        .library(name: "MonarchFontsPoppins", targets: ["MonarchFontsPoppins"]),
    ],
    targets: [
        .target(
            name: "MonarchTokens",
            swiftSettings: monarchSwiftSettings
        ),
        .target(
            name: "MonarchUI",
            dependencies: ["MonarchTokens"],
            resources: [.process("Resources")],
            swiftSettings: monarchSwiftSettings
        ),
        .target(
            name: "MonarchFontsPoppins",
            dependencies: ["MonarchUI"],
            resources: [.copy("Fonts")],
            swiftSettings: monarchSwiftSettings
        ),
        .testTarget(
            name: "MonarchFontsPoppinsTests",
            dependencies: ["MonarchFontsPoppins", "MonarchUI"],
            swiftSettings: monarchSwiftSettings
        ),
        .testTarget(
            name: "MonarchTokensTests",
            dependencies: ["MonarchTokens"],
            swiftSettings: monarchSwiftSettings
        ),
        .testTarget(
            name: "MonarchUITests",
            dependencies: ["MonarchUI", "MonarchTokens"],
            swiftSettings: monarchSwiftSettings
        ),
    ]
)

// emit-root-package: begin test-only (left out of the repository-root manifest)
// Screenshot tests: fixtures rendered on the iOS Simulator and compared with
// recorded images (`Scripts/verify.sh --snapshots`). Never run on the host,
// whose fonts differ. Declared once Tests/MonarchUISnapshotTests exists, so the
// package builds before the first snapshot test is added.
if FileManager.default.fileExists(atPath: Context.packageDirectory + "/Tests/MonarchUISnapshotTests") {
    package.targets.append(
        .testTarget(
            name: "MonarchUISnapshotTests",
            dependencies: ["MonarchUI", "MonarchTokens"],
            swiftSettings: monarchSwiftSettings
        )
    )
}
// emit-root-package: end test-only
