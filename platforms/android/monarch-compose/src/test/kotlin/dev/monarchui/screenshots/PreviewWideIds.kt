package dev.monarchui.screenshots

/**
 * Examples whose web version is a master-detail or multi-pane layout. Their docs preview is recorded in a tablet
 * window (840dp wide, [WidePreviewCaptureTest]) so both panes show, as on the web: that is the adaptive behaviour
 * of the component on a tablet, not a different scenario.
 */
internal object PreviewWideIds {
    val ids: Set<String> = setOf(
        "ai-chat-page",
        "ai-eval-review",
        "ai-agent-builder",
        "ai-copilot-layout",
        "split-inbox-layout",
        "split-layout",
        "template-picker",
        "settings-page",
        "resizable-panels",
        // The web shows the horizontal stepper with every step name.
        "stepper",
        // Four icon and label segments on one line need about 430dp, as on the web.
        "view-switcher",
        "insight-strip",
        "permission-matrix",
        "pricing-table",
        "timesheet-grid",
        "shift-roster",
        "team-calendar",
        "time-grid",
        "rook-rail",
        "top-nav",
        "report-viewer",
        "renewals-panel",
        "subscriptions-table",
        "rich-text-editor",
        "screen-share-view",
        // The web lays a day out as one row (switch, intervals, copy to all), which needs about 520dp.
        "weekly-hours-editor",
        // The web toolbar shows every tool on one line.
        "editor-toolbar",
    )
}
