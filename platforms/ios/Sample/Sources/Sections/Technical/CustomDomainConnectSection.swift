import MonarchUI
import SwiftUI

/// Recipe: connect a custom domain with a field, the DNS records, progress and a removal confirmation.
struct CustomDomainConnectSection: CataloguePage {
    static let title = "Custom domain connect (recipe)"

    @Environment(\.monarchTheme) private var theme
    @State private var domain = ""
    @State private var connected: String?
    @State private var stage = MonarchDomainStage.dns
    @State private var checks = 0
    @State private var isConfirmingRemoval = false

    /// What people paste, reduced to a host: no scheme, path or port.
    private var host: String {
        var text = domain.trimmingCharacters(in: .whitespaces).lowercased()
        if let range = text.range(of: "://") { text = String(text[range.upperBound...]) }
        if let cut = text.firstIndex(where: { $0 == "/" || $0 == "?" || $0 == "#" || $0 == ":" }) {
            text = String(text[..<cut])
        }
        return text.hasSuffix(".") ? String(text.dropLast()) : text
    }

    private var domainError: String? {
        guard !domain.isEmpty else { return nil }
        let labels = host.split(separator: ".", omittingEmptySubsequences: false)
        let isHost =
            labels.count >= 3
            && labels.allSatisfy { !$0.isEmpty && $0.allSatisfy { $0.isLetter || $0.isNumber || $0 == "-" } }
        return isHost ? nil : "Use a subdomain such as help.example.com."
    }

    var body: some View {
        if let connected {
            MonarchDomainProgress(connected, stage: stage, isRechecking: false)
            Text("DNS records")
                .monarchTextStyle(.subheading)
                .accessibilityAddTraits(.isHeader)
            Text("Add these records where the DNS for your domain is managed.")
                .monarchTextStyle(.body)
                .foregroundStyle(theme.colors.textMuted)
            MonarchDnsRecordTable(
                [
                    MonarchDnsRecord(
                        type: "CNAME",
                        name: connected,
                        value: "pages.example.net",
                        purpose: .routing,
                        isFound: checks > 0 ? true : nil
                    ),
                    MonarchDnsRecord(
                        type: "TXT",
                        name: "_verify.\(connected)",
                        value: "verify=4f1c9a2e7b",
                        purpose: .verification,
                        ttl: "Auto",
                        isFound: checks > 0 ? checks > 1 : nil
                    ),
                ],
                layout: .cards
            )
            MonarchButton(checks == 0 ? "Verify DNS" : "Check again", variant: .primary, fullWidth: true) {
                checks += 1
                stage = checks > 1 ? .live : .ownership
            }
            .disabled(stage == .live)
            MonarchButton("Remove domain", variant: .danger, fullWidth: true) { isConfirmingRemoval = true }
                .confirmationDialog(
                    "Remove \(connected)?",
                    isPresented: $isConfirmingRemoval,
                    titleVisibility: .visible
                ) {
                    Button("Remove domain", role: .destructive) {
                        self.connected = nil
                        stage = .dns
                        checks = 0
                    }
                } message: {
                    Text("The domain stops working here. You can connect it again later.")
                }
        } else {
            MonarchTextField(
                "Domain",
                text: $domain,
                prompt: "help.example.com",
                helper: "The address visitors will type, such as help.example.com.",
                error: domainError
            )
            .textInputAutocapitalization(.never)
            .autocorrectionDisabled()
            .keyboardType(.URL)
            MonarchButton("Connect domain", variant: .primary, fullWidth: true) { connected = host }
                .disabled(domain.isEmpty || domainError != nil)
        }
    }
}
