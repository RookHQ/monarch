import Foundation
import Testing

/// Leaked platform defaults (CROSS-PLATFORM.md section 6) that can be found by reading the sources: a component
/// draws in Monarch's colours and styles, never in the system accent, a named system colour, a UIKit semantic
/// colour or a stock button style, and the theme sets the tint so every system control (`Toggle`, `ProgressView`,
/// `DatePicker`) follows the Monarch accent. The sources are read from the package on disk; nothing is listed here,
/// so a new component is covered as soon as its file exists.
@Suite("Leaked defaults")
struct LeakedDefaultRulesTests {
    /// `platforms/ios`, from this file's path.
    private static let packageRoot = URL(fileURLWithPath: #filePath)
        .deletingLastPathComponent().deletingLastPathComponent().deletingLastPathComponent().deletingLastPathComponent()

    private static func swiftFiles(_ folder: String) -> [URL] {
        let root = packageRoot.appendingPathComponent(folder)
        guard let walker = FileManager.default.enumerator(at: root, includingPropertiesForKeys: nil) else { return [] }
        return walker.compactMap { $0 as? URL }.filter { $0.pathExtension == "swift" }
            .sorted { $0.path < $1.path }
    }

    /// Lines of code (not documentation comments) that match `pattern`, as `File.swift:line`.
    private static func offenders(_ pattern: String, in files: [URL]) throws -> [String] {
        let expression = try NSRegularExpression(pattern: pattern)
        return try files.flatMap { file -> [String] in
            let lines = try String(contentsOf: file, encoding: .utf8).components(separatedBy: "\n")
            return lines.enumerated().compactMap { index, line in
                let trimmed = line.trimmingCharacters(in: .whitespaces)
                if trimmed.hasPrefix("//") { return nil }
                let range = NSRange(line.startIndex..., in: line)
                return expression.firstMatch(in: line, range: range) == nil
                    ? nil : "\(file.lastPathComponent):\(index + 1)"
            }
        }
    }

    @Test("The sources are where this test expects them")
    func sourcesExist() {
        #expect(Self.swiftFiles("Sources/MonarchUI/Components").count > 100)
    }

    @Test("Components never use the system accent or a named system colour")
    func noSystemColours() throws {
        let files = Self.swiftFiles("Sources/MonarchUI/Components")
        let pattern = #"\.accentColor\b|\bColor\.(blue|green|red|orange|yellow|purple|pink|gray|black|white)\b"#
        #expect(try Self.offenders(pattern, in: files) == [], "Use theme.colors roles")
    }

    @Test("Components never use a UIKit or AppKit semantic colour")
    func noPlatformSemanticColours() throws {
        let files = Self.swiftFiles("Sources/MonarchUI/Components")
        let pattern = #"\b(UIColor|NSColor)\.(system\w+|label|secondaryLabel|tertiaryLabel|separator|link)\b"#
        #expect(try Self.offenders(pattern, in: files) == [], "Use theme.colors roles")
    }

    @Test("Components never use a stock button style")
    func noStockButtonStyles() throws {
        let files = Self.swiftFiles("Sources/MonarchUI/Components")
        let pattern = #"\.buttonStyle\(\.(borderedProminent|bordered)\)"#
        #expect(try Self.offenders(pattern, in: files) == [], "Use .monarch(...) or MonarchButton")
    }

    @Test("The theme tints system controls with the Monarch accent")
    func themeSetsTheTint() throws {
        let modifier = Self.packageRoot.appendingPathComponent("Sources/MonarchUI/Theme/MonarchThemeModifier.swift")
        #expect(try String(contentsOf: modifier, encoding: .utf8).contains(".tint(theme.colors.accentStrong)"))
    }
}
