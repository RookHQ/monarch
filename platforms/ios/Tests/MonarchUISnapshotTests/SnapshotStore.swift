import Foundation
import Testing

/// The pinned inputs of every snapshot. A render never reads the device's
/// locale, calendar, time zone or text size.
enum SnapshotEnvironment {
    static let locale = Locale(identifier: "en_US")
    static let timeZone = TimeZone(identifier: "UTC") ?? .gmt
    static let calendar: Calendar = {
        var calendar = Calendar(identifier: .gregorian)
        calendar.locale = locale
        calendar.timeZone = timeZone
        return calendar
    }()
    /// The clock a component's fixtures are drawn with, where they show "today" or a relative time. A component
    /// that takes its own `today` or `now` is given it in its fixtures instead; `SnapshotClockTests` fails for a
    /// fixture that still follows the real date.
    static let pinnedNow: [String: Date] = [
        "mini-month": Date(timeIntervalSince1970: 1_793_750_400),
        "slot-picker": Date(timeIntervalSince1970: 1_793_750_400),
        "consent-history": Date(timeIntervalSinceReferenceDate: 800_000_000),
        "ai-assistant-panel": Date(timeIntervalSince1970: 1_793_869_800),
        "api-key-row": Date(timeIntervalSince1970: 1_794_666_600),
        "autosave-indicator": Date(timeIntervalSince1970: 1_800_003_600),
        "impersonation-banner": Date(timeIntervalSince1970: 1_800_000_000),
        "provenance": Date(timeIntervalSince1970: 1_800_000_000),
        "save-status-indicator": Date(timeIntervalSince1970: 1_800_000_000),
        "widget": Date(timeIntervalSince1970: 1_800_000_000),
    ]
    /// The width of the canvas in points, an iPhone in portrait.
    static let width: Double = 390
    /// Scales tried in order for a stored image; the first whose PNG fits the limit wins.
    static let scales: [Double] = [2, 1.5, 1, 0.75, 0.5]
    /// The most a stored PNG may take.
    static let maxBytes = 40_000
}

/// One stored image: a component in a theme under a condition.
struct SnapshotKey: Hashable, Sendable, CustomTestStringConvertible {
    let id: String
    let theme: String
    /// "default" or "x2" (the largest Dynamic Type, accessibility5; named x2 like the Android 2.0 font scale).
    let size: String
    /// "ltr" or "rtl".
    let direction: String

    var testDescription: String { fileName }

    /// `<id>.<theme>[.x2][.rtl].png`, the same shape as the Android baselines.
    var fileName: String {
        "\(id).\(theme)\(size == "x2" ? ".x2" : "")\(direction == "rtl" ? ".rtl" : "").png"
    }

    /// The path of the image relative to `platforms/ios`. These are regression baselines (every fixture of a
    /// component stacked in one image), not docs previews; the docs previews come from `Scripts/build-previews.sh`.
    var relativePath: String { "Tests/Snapshots/\(id)/\(fileName)" }
}

/// Where baselines live and what a run does with them.
enum SnapshotStore {
    /// `platforms/ios`, found from this file so tests run from any working directory.
    static let packageRoot: URL = URL(fileURLWithPath: #filePath)
        .deletingLastPathComponent().deletingLastPathComponent().deletingLastPathComponent()

    /// `Tests/Snapshots`: the baselines.
    static let baselines = packageRoot.appendingPathComponent("Tests/Snapshots", isDirectory: true)

    /// Actual and diff images of a failed compare.
    static let failures = packageRoot.appendingPathComponent(".build/snapshot-failures", isDirectory: true)

    /// `MONARCH_SNAPSHOT_RECORD=1` records instead of compares.
    static var isRecording: Bool {
        let value = ProcessInfo.processInfo.environment["MONARCH_SNAPSHOT_RECORD"] ?? ""
        return !value.isEmpty && value != "0"
    }

    static func baselineURL(_ key: SnapshotKey) -> URL { packageRoot.appendingPathComponent(key.relativePath) }

    /// The scale a baseline was recorded at, from its width, so a compare renders the same way.
    static func scale(ofBaselineWidth width: Int) -> Double? {
        SnapshotEnvironment.scales.first { abs(Int((SnapshotEnvironment.width * $0).rounded()) - width) <= 1 }
    }

    /// Components that cannot be shown in a repeatable image, with the reason. Each reason is a real one,
    /// checked by the harness tests; the other tests still cover these components.
    static let excluded: [String: String] = [
        "date-picker": "marks today from the real clock, so the image changes every day",
        "date-time-picker": "marks today from the real clock, so the image changes every day",
        "ai-context-divider": "its day divider shows today's date from the real clock",
        "notification-center": "groups notifications by day from the real clock",
        "ai-attachment-tray": "a horizontal scroll area: ImageRenderer draws nothing inside a ScrollView",
        "ai-follow-up-suggestions": "a horizontal scroll area: ImageRenderer draws nothing inside a ScrollView",
        "editor-toolbar": "a horizontal scroll area: ImageRenderer draws nothing inside a ScrollView",
        "insight-strip": "a horizontal scroll area: ImageRenderer draws nothing inside a ScrollView",
        "org-chart": "a pannable canvas in a ScrollView: ImageRenderer draws nothing inside it",
        "time-grid":
            "a horizontal scroll area of day columns with pinned hours: ImageRenderer draws nothing inside a ScrollView",
        "kanban-board": "a horizontal scroll area of columns: ImageRenderer draws nothing inside a ScrollView",
        "calendar-month-view":
            "a lazy horizontal pager: ImageRenderer draws nothing inside it (a core id; the docs preview is a real simulator capture)",
        "ai-agent-avatar":
            "animates from the wall clock (a shimmer, dots, a spinner), so a render differs between runs; Reduce Motion cannot be forced in a unit test",
        "ai-agent-card":
            "animates from the wall clock (a shimmer, dots, a spinner), so a render differs between runs; Reduce Motion cannot be forced in a unit test",
        "ai-agent-picker":
            "animates from the wall clock (a shimmer, dots, a spinner), so a render differs between runs; Reduce Motion cannot be forced in a unit test",
        "ai-draft-queue":
            "animates from the wall clock (a shimmer, dots, a spinner), so a render differs between runs; Reduce Motion cannot be forced in a unit test",
        "ai-image-generation":
            "animates from the wall clock (a shimmer, dots, a spinner), so a render differs between runs; Reduce Motion cannot be forced in a unit test",
        "ai-launcher":
            "animates from the wall clock (a shimmer, dots, a spinner), so a render differs between runs; Reduce Motion cannot be forced in a unit test",
        "ai-markdown":
            "animates from the wall clock (a shimmer, dots, a spinner), so a render differs between runs; Reduce Motion cannot be forced in a unit test",
        "ai-multi-agent-roster":
            "animates from the wall clock (a shimmer, dots, a spinner), so a render differs between runs; Reduce Motion cannot be forced in a unit test",
        "ai-reasoning":
            "animates from the wall clock (a shimmer, dots, a spinner), so a render differs between runs; Reduce Motion cannot be forced in a unit test",
        "ai-response-skeleton":
            "animates from the wall clock (a shimmer, dots, a spinner), so a render differs between runs; Reduce Motion cannot be forced in a unit test",
        "ai-shared-session":
            "animates from the wall clock (a shimmer, dots, a spinner), so a render differs between runs; Reduce Motion cannot be forced in a unit test",
        "ai-subagent-tree":
            "animates from the wall clock (a shimmer, dots, a spinner), so a render differs between runs; Reduce Motion cannot be forced in a unit test",
        "ai-thinking-indicator":
            "animates from the wall clock (a shimmer, dots, a spinner), so a render differs between runs; Reduce Motion cannot be forced in a unit test",
        "ai-typing-dots":
            "animates from the wall clock (a shimmer, dots, a spinner), so a render differs between runs; Reduce Motion cannot be forced in a unit test",
        "ai-voice-mode":
            "animates from the wall clock (a shimmer, dots, a spinner), so a render differs between runs; Reduce Motion cannot be forced in a unit test",
        "call-stage":
            "animates from the wall clock (a shimmer, dots, a spinner), so a render differs between runs; Reduce Motion cannot be forced in a unit test",
        "stage-journey":
            "animates from the wall clock (a shimmer, dots, a spinner), so a render differs between runs; Reduce Motion cannot be forced in a unit test",
        "world-map":
            "animates from the wall clock (a shimmer, dots, a spinner), so a render differs between runs; Reduce Motion cannot be forced in a unit test",
        "key-created-dialog": "the content lives in a presentation, which ImageRenderer cannot draw",
        "step-up-auth-dialog": "the content lives in a presentation, which ImageRenderer cannot draw",
    ]

    /// Components whose content has no height of its own (a lazy pager, a scroll area, a pane that fills what it
    /// is given): ImageRenderer draws them with no height, so one colour. They are drawn at this height in points.
    static let fixedHeights: [String: Double] = {
        var heights: [String: Double] = [:]
        for id in [
            "action-editor", "ai-computer-view", "ai-connector-picker",
            "ai-source-preview", "ai-thread", "block-editor", "business-hours-editor",
            "comments-panel", "gantt-timeline",
            "lightbox", "line-items-editor", "pricing-table", "query-builder",
            "schema-driven-form", "slot-picker", "thread-panel", "version-history-panel",
            "weekly-hours-editor",
        ] { heights[id] = 480 }
        heights["stepper"] = 160
        return heights
    }()

    /// The core set: the ids in `Tests/Snapshots/core-components.txt`, in the same style as Android's file.
    static func coreIDs(in file: URL = baselines.appendingPathComponent("core-components.txt")) -> [String] {
        guard let text = try? String(contentsOf: file, encoding: .utf8) else { return [] }
        return text.split(separator: "\n").compactMap { line in
            let id =
                line.split(separator: "#", maxSplits: 1, omittingEmptySubsequences: false).first?
                .trimmingCharacters(in: .whitespaces) ?? ""
            return id.isEmpty ? nil : id
        }
    }

    /// Components whose largest-text images cannot be drawn: ImageRenderer returns a blank image when the stacked
    /// fixtures are taller than its texture limit. Their other images are still compared.
    static let withoutLargestText: [String: String] = [
        "alert": "four banners at the largest Dynamic Type stack taller than ImageRenderer draws: a blank image"
    ]

    /// The keys of a component: light and dark at the default size, left to right;
    /// the core set adds hc-light and hc-dark, the largest Dynamic Type, and right to left.
    static func keys(for id: String, core: Set<String>) -> [SnapshotKey] {
        let basic = ["light", "dark"]
        guard core.contains(id) else {
            return basic.map { SnapshotKey(id: id, theme: $0, size: "default", direction: "ltr") }
        }
        var keys: [SnapshotKey] = []
        for theme in ["light", "dark", "hc-light", "hc-dark"] {
            for size in withoutLargestText[id] == nil ? ["default", "x2"] : ["default"] {
                for direction in ["ltr", "rtl"] {
                    keys.append(SnapshotKey(id: id, theme: theme, size: size, direction: direction))
                }
            }
        }
        return keys
    }

    /// Writes the actual and diff images of a failure and returns their paths.
    static func writeFailure(_ key: SnapshotKey, actual: Pixels, diff: Pixels?) -> [String] {
        try? FileManager.default.createDirectory(at: failures, withIntermediateDirectories: true)
        var written: [String] = []
        let stem = key.fileName.dropLast(4)
        if let data = actual.png() {
            let url = failures.appendingPathComponent("\(stem).actual.png")
            if (try? data.write(to: url)) != nil { written.append(url.path) }
        }
        if let data = diff?.png() {
            let url = failures.appendingPathComponent("\(stem).diff.png")
            if (try? data.write(to: url)) != nil { written.append(url.path) }
        }
        return written
    }
}
