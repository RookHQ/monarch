import Foundation

/// Status and request logic for ``MonarchDataExportRequest``.
enum MonarchDataExportRequestTokens {
    /// The SF Symbol of the download button.
    static let downloadSymbol = "arrow.down.circle"
    /// The URL schemes a download link may use.
    static let downloadSchemes: Set<String> = ["https", "http"]

    /// The tone of a status badge.
    static func tone(_ status: MonarchDataExportStatus) -> MonarchTone {
        switch status {
        case .idle, .expired: .neutral
        case .requested, .preparing: .info
        case .ready: .success
        case .failed: .danger
        }
    }

    /// The SF Symbol of a status badge, so the status is not colour alone.
    static func symbol(_ status: MonarchDataExportStatus) -> String {
        switch status {
        case .idle: "circle.dashed"
        case .requested: "clock"
        case .preparing: "hourglass"
        case .ready: "checkmark.circle"
        case .expired: "clock.badge.xmark"
        case .failed: "exclamationmark.triangle"
        }
    }

    /// The badge text of a status.
    static func statusName(_ status: MonarchDataExportStatus, labels: MonarchDataExportRequestLabels) -> String {
        labels.statuses[status] ?? MonarchDataExportRequestStrings.status(status)
    }

    /// Whether the request form is shown: there is a handler and nothing is
    /// in progress or ready.
    static func canRequest(_ status: MonarchDataExportStatus, hasHandler: Bool) -> Bool {
        hasHandler && [.idle, .expired, .failed].contains(status)
    }

    /// Whether the preparation note is shown: before and during preparation.
    static func showsNote(_ status: MonarchDataExportStatus) -> Bool {
        [.idle, .requested, .preparing].contains(status)
    }

    /// A date as the medium date and short time in the locale and time zone.
    static func formatted(_ date: Date?, locale: Locale, timeZone: TimeZone) -> String? {
        guard let date else { return nil }
        let style = Date.FormatStyle(
            date: .abbreviated,
            time: .omitted,
            locale: locale,
            calendar: locale.calendar,
            timeZone: timeZone
        ).monarchTime()
        return date.formatted(style)
    }

    /// The status line for a status and its dates, or `nil` before a request.
    static func statusLine(
        _ status: MonarchDataExportStatus,
        requestedAt: Date?,
        readyAt: Date?,
        expiresAt: Date?,
        locale: Locale,
        timeZone: TimeZone,
        labels: MonarchDataExportRequestLabels
    ) -> String? {
        func when(_ date: Date?) -> String? { formatted(date, locale: locale, timeZone: timeZone) }
        switch status {
        case .idle:
            return nil
        case .requested:
            return labels.requested?(when(requestedAt)) ?? MonarchDataExportRequestStrings.requested(when(requestedAt))
        case .preparing:
            return labels.preparing?(when(requestedAt)) ?? MonarchDataExportRequestStrings.preparing(when(requestedAt))
        case .ready:
            return labels.ready?(when(readyAt), when(expiresAt))
                ?? MonarchDataExportRequestStrings.ready(when(readyAt), when(expiresAt))
        case .expired:
            return labels.expired?(when(expiresAt)) ?? MonarchDataExportRequestStrings.expired(when(expiresAt))
        case .failed:
            return labels.failed ?? MonarchDataExportRequestStrings.failed
        }
    }

    /// The details to request, or `nil` when there is a scope and nothing in it is chosen.
    static func details(
        scope: [String],
        chosen: Set<String>,
        hasFormats: Bool,
        format: String
    ) -> MonarchDataExportRequestDetails? {
        let ids = scope.filter(chosen.contains)
        if !scope.isEmpty && ids.isEmpty { return nil }
        return MonarchDataExportRequestDetails(scope: ids, format: hasFormats ? format : nil)
    }

    /// The download link when it is http or https; `nil` otherwise.
    static func safeDownloadURL(_ url: URL?) -> URL? {
        guard let url, let scheme = url.scheme?.lowercased(), downloadSchemes.contains(scheme) else { return nil }
        return url
    }

    /// Whether the download button is shown: the export is ready and there
    /// is a safe link or a handler.
    static func showsDownload(_ status: MonarchDataExportStatus, url: URL?, hasHandler: Bool) -> Bool {
        status == .ready && (safeDownloadURL(url) != nil || hasHandler)
    }

    /// The message for a failed request: the error's description when it
    /// has one, otherwise the built-in message.
    static func failureMessage(_ error: any Error, labels: MonarchDataExportRequestLabels) -> String {
        MonarchFailureMessage.message(
            for: error,
            fallback: labels.requestFailed ?? MonarchDataExportRequestStrings.requestFailed
        )
    }
}
