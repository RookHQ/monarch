import SwiftUI

/// The decisions of ``MonarchRoadmap``. Sizes and colours of the chart itself
/// come from ``MonarchGanttTimeline``.
enum MonarchRoadmapTokens {
    /// The zoom a roadmap opens at: nine months visible.
    static let defaultZoom = MonarchGanttTimelineZoom.quarter

    /// The bar tone of a project: `tone` when the app gives one, otherwise
    /// from `health`. At risk is warning, off track is danger, and on track,
    /// not reported and no health keep the accent colour (`nil`), as on the web.
    /// The gap inside the progress and health beside a row name.
    static let metaSpacing: CGFloat = 6

    /// The badge tone of a health.
    static func healthTone(_ health: MonarchRoadmapHealth) -> MonarchTone {
        switch health {
        case .onTrack: .success
        case .atRisk: .warning
        case .offTrack: .danger
        case .notReported: .neutral
        }
    }

    static func barTone(_ tone: MonarchTone?, health: MonarchRoadmapHealth?) -> MonarchTone? {
        if let tone { return tone }
        switch health {
        case .atRisk: return .warning
        case .offTrack: return .danger
        case .onTrack, .notReported, nil: return nil
        }
    }
}
