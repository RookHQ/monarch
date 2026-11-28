import Foundation
import UniformTypeIdentifiers

/// How a file name, a MIME type or a `UTType` maps to a family. The tables
/// are the ones the web component uses, so a file gets the same glyph on
/// every platform; `UTType` covers what the tables do not list.
enum MonarchFileTypeRules {
    /// Extensions by family. Lower case, without the dot.
    static let extensions: [MonarchFileTypeFamily: [String]] = [
        .image: ["png", "jpg", "jpeg", "gif", "webp", "avif", "svg", "bmp", "ico", "heic", "heif", "tif", "tiff"],
        .video: ["mp4", "m4v", "mov", "webm", "mkv", "avi", "ogv", "wmv"],
        .audio: ["mp3", "wav", "ogg", "oga", "m4a", "aac", "flac", "opus", "weba"],
        .pdf: ["pdf"],
        .document: ["doc", "docx", "odt", "rtf", "pages"],
        .spreadsheet: ["xls", "xlsx", "ods", "csv", "tsv", "numbers"],
        .presentation: ["ppt", "pptx", "odp", "key"],
        .archive: ["zip", "rar", "7z", "tar", "gz", "tgz", "bz2", "xz"],
        .code: [
            "js", "jsx", "ts", "tsx", "mjs", "cjs", "json", "html", "htm", "css", "scss", "py", "rb", "go", "rs",
            "java", "kt", "swift", "c", "h", "cpp", "cs", "php", "sh", "sql", "yaml", "yml", "xml", "toml",
        ],
        .text: ["txt", "md", "markdown", "log", "ini"],
    ]

    private static let familyByExtension: [String: MonarchFileTypeFamily] = {
        var table: [String: MonarchFileTypeFamily] = [:]
        for (family, list) in extensions {
            for item in list { table[item] = family }
        }
        return table
    }()

    /// MIME rules in order; the first rule with a matching fragment wins.
    private static let mimeFragments: [(fragments: [String], family: MonarchFileTypeFamily)] = [
        (["spreadsheet", "ms-excel", "text/csv", "tab-separated"], .spreadsheet),
        (["presentation", "ms-powerpoint"], .presentation),
        (["wordprocessing", "msword", "opendocument.text", "rtf"], .document),
        (["zip", "x-tar", "gzip", "x-7z", "x-rar", "x-bzip", "x-xz", "compressed"], .archive),
        (["javascript", "typescript", "json", "xml", "x-sh", "x-python", "html", "css", "x-yaml", "sql"], .code),
    ]

    /// Identifiers of word-processing types that `UTType` has no constant for.
    private static let documentIdentifiers = [
        "com.microsoft.word.doc",
        "org.openxmlformats.wordprocessingml.document",
        "org.oasis-open.opendocument.text",
        "com.apple.iwork.pages.sffpages",
        "com.apple.iwork.pages.pages",
    ]

    /// The lower-case extension of a file name or path, without the dot.
    /// Empty when there is none; a leading dot (`.env`) is not an extension.
    static func fileExtension(_ name: String?) -> String {
        guard let name, !name.isEmpty else { return "" }
        let base = name.split(whereSeparator: { $0 == "/" || $0 == "\\" }).last.map(String.init) ?? ""
        guard let dot = base.lastIndex(of: "."), dot != base.startIndex else { return "" }
        return base[base.index(after: dot)...].lowercased()
    }

    /// The MIME type without parameters, lower case: `text/csv; charset=utf-8`
    /// gives `text/csv`.
    static func normalizedMIMEType(_ contentType: String?) -> String {
        let first = (contentType ?? "").lowercased().split(separator: ";", omittingEmptySubsequences: false).first
        return first.map { $0.trimmingCharacters(in: .whitespaces) } ?? ""
    }

    /// The family a MIME type names, or `nil` when the tables do not know it.
    static func family(mimeType type: String) -> MonarchFileTypeFamily? {
        if type.isEmpty || type == "application/octet-stream" { return nil }
        if type.hasPrefix("image/") { return .image }
        if type.hasPrefix("video/") { return .video }
        if type.hasPrefix("audio/") { return .audio }
        if type == "application/pdf" { return .pdf }
        for rule in mimeFragments where rule.fragments.contains(where: { type.contains($0) }) {
            return rule.family
        }
        return type.hasPrefix("text/") ? .text : nil
    }

    /// The family of a file from its name and MIME type.
    static func family(fileName: String?, contentType: String?) -> MonarchFileTypeFamily {
        let type = normalizedMIMEType(contentType)
        if let family = family(mimeType: type) { return family }
        let pathExtension = fileExtension(fileName)
        if let family = familyByExtension[pathExtension] { return family }
        // Not in Monarch's tables: ask the system's type database.
        if !type.isEmpty, type != "application/octet-stream", let uniformType = UTType(mimeType: type) {
            let family = family(of: uniformType)
            if family != .other { return family }
        }
        if !pathExtension.isEmpty, let uniformType = UTType(filenameExtension: pathExtension) {
            return family(of: uniformType)
        }
        return .other
    }

    /// The family a uniform type belongs to. The order matters: CSV, RTF,
    /// HTML and source code all conform to text, so text is checked last.
    static func family(of type: UTType) -> MonarchFileTypeFamily {
        if type.conforms(to: .pdf) { return .pdf }
        if type.conforms(to: .image) { return .image }
        if type.conforms(to: .audio) { return .audio }
        if type.conforms(to: .movie) || type.conforms(to: .video) { return .video }
        if type.conforms(to: .spreadsheet) || type.conforms(to: .commaSeparatedText)
            || type.conforms(to: .tabSeparatedText)
        {
            return .spreadsheet
        }
        if type.conforms(to: .presentation) { return .presentation }
        if type.conforms(to: .archive) { return .archive }
        if type.conforms(to: .rtf) || type.conforms(to: .rtfd) || type.conforms(to: .flatRTFD) { return .document }
        if documentIdentifiers.contains(where: { identifier in
            UTType(identifier).map { type.conforms(to: $0) } ?? (type.identifier == identifier)
        }) {
            return .document
        }
        let code: [UTType] = [.sourceCode, .script, .json, .xml, .html, .yaml, .propertyList]
        if code.contains(where: { type.conforms(to: $0) }) { return .code }
        if type.conforms(to: .text) { return .text }
        return .other
    }
}
