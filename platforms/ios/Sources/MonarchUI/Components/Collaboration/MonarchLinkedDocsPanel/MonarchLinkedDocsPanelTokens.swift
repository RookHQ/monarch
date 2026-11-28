import Foundation
import MonarchTokens
import SwiftUI

/// Component tokens and pure logic for ``MonarchLinkedDocsPanel``.
enum MonarchLinkedDocsPanelTokens {
    /// The gap between the filter, the notices and the list.
    static let sectionSpacing: CGFloat = MonarchTokens.Space.space3
    /// The gap between rows, around the divider.
    static let rowSpacing: CGFloat = MonarchTokens.Space.space2
    /// The gap between the icon, the text and the button of a row.
    static let iconSpacing: CGFloat = MonarchTokens.Space.space3
    /// The gap between the lines of a row.
    static let lineSpacing: CGFloat = MonarchTokens.Space.space0_5
    /// The SF Symbol of a link row.
    static let linkSymbol = "link"
    /// The glyph of a linked document.
    static let docSymbol = "doc.text"
    /// The SF Symbol of the add menu.
    static let addSymbol = "plus"
    /// The SF Symbol of the unlink button.
    static let unlinkSymbol = "xmark"
    /// The SF Symbol of the link doc action.
    static let linkDocSymbol = "doc.text"
    /// The SF Symbol of the new doc action.
    static let newDocSymbol = "square.and.pencil"
    /// The key of the filter option for rows in no space.
    static let noSpaceKey = "\u{0}none"

    /// What the panel shows.
    enum Phase: Equatable {
        /// The rows.
        case list
        /// The loading indicator: there are no items yet.
        case loading
        /// The app's notice in place of the list.
        case notice
        /// Nothing is linked.
        case empty
    }

    /// The phase: the notice wins, then loading (no items yet), then empty.
    static func phase(items: [MonarchLinkedDocsPanelItem]?, notice: String?) -> Phase {
        if let notice, !notice.isEmpty { return .notice }
        guard let items else { return .loading }
        return items.isEmpty ? .empty : .list
    }

    /// A full http or https address with a host, or `nil`. Surrounding spaces are ignored.
    static func parseLinkURL(_ raw: String) -> URL? {
        let text = raw.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !text.isEmpty, let url = URL(string: text), let scheme = url.scheme?.lowercased(),
            scheme == "http" || scheme == "https", let host = url.host, !host.isEmpty
        else { return nil }
        return url
    }

    /// The host of a URL without "www.", the default title and secondary line of a link.
    static func host(_ url: URL?) -> String? {
        guard let host = url?.host, !host.isEmpty else { return nil }
        return host.hasPrefix("www.") ? String(host.dropFirst(4)) : host
    }

    /// The title of a new link: the typed title, or the host.
    static func linkTitle(_ title: String, url: URL) -> String {
        let trimmed = title.trimmingCharacters(in: .whitespacesAndNewlines)
        return trimmed.isEmpty ? host(url) ?? url.absoluteString : trimmed
    }

    /// The secondary line of a row: its meta, or the host of a link.
    static func meta(_ item: MonarchLinkedDocsPanelItem) -> String? {
        if let meta = item.meta, !meta.isEmpty { return meta }
        return item.kind == .link ? host(item.url) : nil
    }

    /// One option of the space filter.
    struct Space: Equatable, Identifiable {
        /// The space id, or ``noSpaceKey``.
        let key: String
        /// The name shown.
        let name: String
        /// How many rows are in it.
        let count: Int
        var id: String { key }
    }

    /// The spaces of the rows, sorted by name in the locale, with "No space" last.
    static func spaces(_ items: [MonarchLinkedDocsPanelItem], noSpace: String, locale: Locale) -> [Space] {
        var counts: [String: (name: String, count: Int)] = [:]
        for item in items {
            let key = item.spaceID ?? noSpaceKey
            let name = item.spaceID == nil ? noSpace : item.spaceName ?? noSpace
            counts[key, default: (name, 0)].count += 1
        }
        return counts.map { Space(key: $0.key, name: $0.value.name, count: $0.value.count) }
            .sorted { lhs, rhs in
                if lhs.key == noSpaceKey { return false }
                if rhs.key == noSpaceKey { return true }
                return lhs.name.compare(rhs.name, locale: locale) == .orderedAscending
            }
    }

    /// Whether the space filter is shown: it is on and the rows span more than one space.
    static func showsFilter(isEnabled: Bool, spaces: [Space]) -> Bool {
        isEnabled && spaces.count > 1
    }

    /// The rows the filter keeps. An empty key, or a space no row is in, keeps every row.
    static func filtered(_ items: [MonarchLinkedDocsPanelItem], key: String, spaces: [Space])
        -> [MonarchLinkedDocsPanelItem]
    {
        guard !key.isEmpty, spaces.contains(where: { $0.key == key }) else { return items }
        return items.filter { ($0.spaceID ?? noSpaceKey) == key }
    }

    /// The doc options not linked yet, matching the query in their label or description.
    static func availableOptions(
        _ options: [MonarchLinkedDocsPanelOption],
        items: [MonarchLinkedDocsPanelItem],
        query: String,
        locale: Locale
    ) -> [MonarchLinkedDocsPanelOption] {
        let linked = Set(items.compactMap { $0.kind == .doc ? $0.docID : nil })
        let trimmed = query.trimmingCharacters(in: .whitespacesAndNewlines)
        return options.filter { option in
            guard !linked.contains(option.docID) else { return false }
            guard !trimmed.isEmpty else { return true }
            return [option.label, option.description ?? ""].contains {
                $0.range(of: trimmed, options: [.caseInsensitive, .diacriticInsensitive], locale: locale) != nil
            }
        }
    }

    /// What VoiceOver reads after a row's title: its secondary line and status.
    static func accessibilityValue(meta: String?, status: String?) -> String {
        [meta, status].compactMap { $0 }.filter { !$0.isEmpty }.joined(separator: ", ")
    }
}
