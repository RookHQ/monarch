import CoreGraphics

/// The ids whose first example is a whole screen: a `NavigationStack`, `NavigationSplitView` or `TabView`. A screen
/// taller than the crop is shown full bleed (no padding) so its own margins are what the reader sees; every other
/// component keeps the 24 point padding even when it is cut at the height cap.
/// Derived from the examples with `NavigationStack|NavigationSplitView|TabView` in their code (an example that is a plain
/// view in a scroll container, such as a record page or a rule builder, keeps the padded column).
enum ScreenRecipes {
    static let ids: Set<String> = [
        "ai-agent-builder",
        "ai-chat-page",
        "ai-conversation-header",
        "ai-conversation-list",
        "ai-eval-review",
        "ai-memory-panel",
        "ai-settings-section",
        "app-shell",
        "approval-queue",
        "back-link",
        "booking-flow",
        "bottom-tab-bar",
        "changelog-feed",
        "command-palette",
        "comments-panel",
        "create-workspace-dialog",
        "dashboard-operational",
        "dashboard-pipeline",
        "delete-account-flow",
        "delete-workspace-flow",
        "faceted-filter-panel",
        "filter-bar",
        "form-builder",
        "gdpr-erase-flow",
        "help-center-home",
        "invite-members-dialog",
        "log-drawer",
        "marketing-site-shell",
        "notification-center",
        "page-header",
        "privacy-choices-link",
        "quick-switcher",
        "record-header",
        "remove-member-flow",
        "reschedule-dialog",
        "responsive-dialog",
        "search-input",
        "settings-section",
        "share-dialog",
        "support-panel",
        "toolbar",
        "top-nav",
        "transfer-ownership-flow",
        "unsaved-changes-guard",
        "wizard-dialog",
    ]
}

/// Examples whose root is a scroll container (`List`, `ScrollView`, `NavigationStack`) with no height of its own: laid
/// out at their natural height they collapse to an empty strip, so the preview gives them a fixed height.
enum PreviewHeights {
    /// The frame of an example: the entry of `points`, else 1500 for a screen-level recipe (the rest of the frame is
    /// trimmed off the image again), else `nil`: the example's own height.
    static func height(for id: String) -> CGFloat? {
        points[id] ?? (ScreenRecipes.ids.contains(id) ? 1500 : nil)
    }

    static let points: [String: CGFloat] = [
        "toolbar": 420,
        "inbox-row": 300,
        "record-header": 700,
        "table-toolbar": 780,
        "version-history-panel": 800,
        "schema-driven-form": 1100,
        "template-picker": 1400,
        "reorderable-list": 280,
        "business-hours-editor": 2900,
        "ai-connector-picker": 1500,
        "column-chooser": 900,
        "status-page-view": 2300,
        "rule-builder": 1700,
        "ai-conversation-header": 240,
        "app-shell": 700,
        "bottom-tab-bar": 360,
        "form-builder": 1500,
        "ai-agent-builder": 1900,
        "ai-eval-review": 1900,
        "report-viewer": 900,
        "support-panel": 700,
        "dashboard-operational": 2800,
        "dashboard-pipeline": 2800,
        "dashboard-portfolio": 2400,
        "dashboard-personal-work": 2000,
        "notification-center": 900,
        "changelog-feed": 900,
        "faceted-filter-panel": 1300,
        "approval-chain-editor": 1100,
        "sequence-builder": 1300,
        "escalation-policy-editor": 1500,
        "marketing-site-shell": 330,
    ]
}
