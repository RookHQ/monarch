import MonarchTokens
@_spi(Experimental) import MonarchUI
import SwiftUI

/// Curated docs previews, only for the ids that have no web primary example (the iOS preview of every other id is its first inventory example, which is the web example from the fixture). For a primitive, the first inventory example teaches the code but does not show the
/// component (a lone "Save" button). A showcase shows it in a few representative states, with real copy and real
/// SF Symbols, written against the public API only. The preview host uses a showcase when one exists for the id and
/// the first inventory example otherwise, which is right for rich components and for recipes. Same ids and states as
/// the Android showcases.
///
/// Rules: one realistic scenario per component, with real copy and real SF Symbols; states appear as they would in an
/// app (an error on a field, a disabled row), never labelled as test states ("Small", "Disabled"); nothing wider than
/// the column (rows wrap). `icon-button` and `textarea` have no inventory entry of their own (they are
/// `MonarchButton(isIconOnly:)` and `MonarchTextField(axis: .vertical)`): their showcases are for the review sheet.
@MainActor
enum Showcases {
    /// Showcases that open a presentation: the preview is the whole screen cut to the presentation, like an example
    /// with `previewPresented`.
    static let presenting: Set<String> = ["lightbox"]

    static func view(for id: String) -> AnyView? {
        switch id {
        case "icon-button": AnyView(IconButtonShowcase())
        case "textarea": AnyView(TextAreaShowcase())
        case "select-compact": AnyView(SelectCompactShowcase())
        case "list-row", "list-item": AnyView(ListItemShowcase())
        case "context-menu": AnyView(ContextMenuShowcase())
        default: nil
        }
    }
}

private struct Flow: Layout {
    var spacing: CGFloat = 12

    func sizeThatFits(proposal: ProposedViewSize, subviews: Subviews, cache: inout ()) -> CGSize {
        arrange(proposal.width ?? .infinity, subviews).size
    }

    func placeSubviews(in bounds: CGRect, proposal: ProposedViewSize, subviews: Subviews, cache: inout ()) {
        let result = arrange(bounds.width, subviews)
        for (index, point) in result.points.enumerated() {
            subviews[index].place(
                at: CGPoint(x: bounds.minX + point.x, y: bounds.minY + point.y),
                proposal: .unspecified
            )
        }
    }

    private func arrange(_ width: CGFloat, _ subviews: Subviews) -> (points: [CGPoint], size: CGSize) {
        var points: [CGPoint] = []
        var x: CGFloat = 0
        var y: CGFloat = 0
        var rowHeight: CGFloat = 0
        var maxX: CGFloat = 0
        for subview in subviews {
            let size = subview.sizeThatFits(.unspecified)
            if x > 0, x + size.width > width {
                x = 0
                y += rowHeight + spacing
                rowHeight = 0
            }
            points.append(CGPoint(x: x, y: y))
            x += size.width + spacing
            rowHeight = max(rowHeight, size.height)
            maxX = max(maxX, x - spacing)
        }
        return (points, CGSize(width: maxX, height: y + rowHeight))
    }
}

/// The column every showcase sits in: full width, 16 points between groups.
private struct Column<Content: View>: View {
    @ViewBuilder var content: Content
    var body: some View {
        VStack(alignment: .leading, spacing: 16) { content }
            .frame(maxWidth: .infinity, alignment: .leading)
    }
}

private struct ButtonShowcase: View {
    var body: some View {
        Column {
            Flow {
                MonarchButton("Save template", systemImage: "checkmark", variant: .primary) {}
                MonarchButton("Preview", variant: .secondary) {}
                MonarchButton("Cancel", variant: .ghost) {}
            }
            Flow {
                MonarchButton("Delete", systemImage: "trash", variant: .danger) {}
                MonarchButton("Approve", variant: .success) {}
            }
            Flow {
                MonarchButton("Publishing", variant: .primary, isLoading: true) {}
                MonarchButton("Send invoice", systemImage: "paperplane") {}.disabled(true)
            }
            MonarchButton("Continue", trailingSystemImage: "arrow.right", variant: .primary, fullWidth: true) {}
        }
    }
}

private struct IconButtonShowcase: View {
    var body: some View {
        HStack(spacing: 8) {
            MonarchButton("Share", systemImage: "square.and.arrow.up", isIconOnly: true) {}
            MonarchButton("Edit", systemImage: "pencil", isIconOnly: true) {}
            MonarchButton("Duplicate", systemImage: "plus.square.on.square", variant: .ghost, isIconOnly: true) {}
            MonarchButton("Delete", systemImage: "trash", variant: .ghost, isIconOnly: true) {}
            Spacer()
            MonarchButton("New invoice", systemImage: "plus", variant: .primary, isIconOnly: true) {}
        }
    }
}

private struct LinkShowcase: View {
    var body: some View {
        Column {
            Flow {
                Button("Read the billing guide") {}.buttonStyle(.monarchLink())
                Button("Manage plan") {}.buttonStyle(.monarchLink(.standalone))
                Button("Skip for now") {}.buttonStyle(.monarchLink(.subtle))
            }
            Button("Open the status page") {}.buttonStyle(.monarchLink(isExternal: true))
            Button("Download the contract") {}.buttonStyle(.monarchLink()).disabled(true)
        }
        .monarchTextStyle(.body)
    }
}

private struct SkeletonShowcase: View {
    var body: some View {
        MonarchCard {
            HStack(spacing: 12) {
                MonarchAvatar(image: nil, name: "Ada Lovelace", size: .lg)
                VStack(alignment: .leading, spacing: 8) {
                    Text("Ada Lovelace").monarchTextStyle(.bodyStrong)
                    Text("Joined in March, 12 open tickets").monarchTextStyle(.caption)
                }
            }
            Text("Last seen on the billing page.").monarchTextStyle(.body)
            Text("Prefers email over phone calls.").monarchTextStyle(.body)
        }
        .redacted(reason: .placeholder)
    }
}

private struct KbdShowcase: View {
    var body: some View {
        Column {
            row("Search", "⌘K")
            row("New message", "⇧⌘N")
            row("Close", "⎋")
        }
    }

    private func row(_ title: String, _ keys: String) -> some View {
        HStack {
            Text(title).monarchTextStyle(.body)
            Spacer()
            MonarchKbd(keys, isAlwaysVisible: true)
        }
    }
}

private struct CodeBlockShowcase: View {
    var body: some View {
        MonarchCodeBlock(
            "let invoice = Invoice(\n    customer: \"cus_1042\",\n    amount: 4_800\n)\ntry await client.send(invoice)",
            language: "swift",
            title: "CreateInvoice.swift",
            showsLineNumbers: true
        )
    }
}

private struct StepperShowcase: View {
    private let steps = [
        MonarchStepperStep(id: "account", label: "Account"),
        MonarchStepperStep(id: "plan", label: "Plan"),
        MonarchStepperStep(id: "payment", label: "Payment"),
        MonarchStepperStep(id: "confirm", label: "Confirm"),
    ]
    var body: some View {
        MonarchStepper(steps, current: 1, orientation: .vertical)
    }
}

private struct ProgressShowcase: View {
    var body: some View {
        Column {
            ProgressView("Uploading files", value: 0.64).progressViewStyle(.monarchLinear)
            ProgressView("Backup complete", value: 1).progressViewStyle(.monarchLinear(tone: .success))
            ProgressView("Storage almost full", value: 0.92).progressViewStyle(.monarchLinear(tone: .warning))
            ProgressView("Preparing export").progressViewStyle(.monarchLinear)
        }
    }
}

private struct TextFieldShowcase: View {
    @State private var name = "Sales team signature"
    @State private var email = "priya@example"
    @State private var domain = ""
    var body: some View {
        Column {
            MonarchTextField("Template name", text: $name, helper: "Visible to your organization.", isRequired: true)
            MonarchTextField("Work email", text: $email, error: "Enter a valid email address.")
            MonarchTextField("Domain", text: $domain, prompt: "help.example.com").disabled(true)
        }
    }
}

private struct FormFieldShowcase: View {
    @State private var name = ""
    @State private var sync = true
    var body: some View {
        Column {
            MonarchFormField(
                "Project name",
                helper: "Shown on invoices.",
                error: "Enter a name for the project.",
                isRequired: true
            ) {
                MonarchTextField("Project name", text: $name, prompt: "Acme renewal", labelVisibility: .hidden)
            }
            MonarchFormField("Sync with calendar", helper: "Adds due dates to your calendar.", layout: .row) {
                Toggle("Sync with calendar", isOn: $sync).labelsHidden()
            }
        }
    }
}
private struct TagShowcase: View {
    @State private var open = true
    @State private var closed = false
    @State private var mine = false
    var body: some View {
        Column {
            Flow(spacing: 8) {
                MonarchTag("Draft")
                MonarchTag("In review", tone: .info)
                MonarchTag("Published", tone: .success)
                MonarchTag("At risk", tone: .warning)
                MonarchTag("Blocked", tone: .danger)
            }
            Flow(spacing: 8) {
                MonarchTag("design") {}
                MonarchTag("billing") {}
                MonarchTag("onboarding") {}
            }
            Flow(spacing: 8) {
                MonarchTag("Open", isSelected: $open)
                MonarchTag("Closed", isSelected: $closed)
                MonarchTag("Assigned to me", isSelected: $mine)
            }
        }
    }
}

private struct BadgeShowcase: View {
    var body: some View {
        Column {
            Flow(spacing: 8) {
                MonarchBadge("New")
                MonarchBadge("Active", tone: .success)
                MonarchBadge("Pending", tone: .warning)
                MonarchBadge("Failed", tone: .danger)
                MonarchBadge("Beta", tone: .info)
            }
            Flow(spacing: 8) {
                MonarchBadge("Pro", tone: .accent, variant: .outline)
                MonarchBadge("Paid", tone: .success, variant: .outline)
                MonarchBadge("Overdue", tone: .danger, variant: .outline)
                MonarchBadge("Verified", tone: .info, systemImage: "checkmark.seal")
            }
        }
    }
}

private struct AvatarShowcase: View {
    private let people = ["Ana Silva", "Ben Okafor", "Chen Wei", "Dara Novak", "Eli Park"]
    var body: some View {
        Column {
            HStack(spacing: 16) {
                MonarchAvatar(image: nil, name: "Ana Silva", size: .lg, status: .online)
                MonarchAvatar(image: nil, name: "Ben Okafor", size: .lg, status: .away)
                MonarchAvatar(image: nil, name: "Chen Wei", size: .lg, status: .busy)
                MonarchAvatar(image: nil, name: "Rook Labs", systemImage: "building.2", shape: .square, size: .lg)
            }
            HStack(spacing: 12) {
                MonarchAvatarGroup("Project members", people, id: \.self, maxVisible: 3) { person in
                    MonarchAvatar(image: nil, name: person)
                }
                Text("5 members").fontWeight(.regular).monarchTextStyle(.label)
            }
        }
    }
}

private struct CardShowcase: View {
    var body: some View {
        Column {
            MonarchCard {
                Text("Quarterly review").monarchTextStyle(.bodyStrong)
                Text("Prepared for the leadership team. Last edited by Ana Silva.").fontWeight(.regular)
                    .monarchTextStyle(.label)
                HStack(spacing: 8) {
                    MonarchButton("Open", variant: .primary, size: .sm) {}
                    MonarchButton("Share", size: .sm) {}
                }
            }
            MonarchCard {
                HStack {
                    Text("Invoice 1042").monarchTextStyle(.bodyStrong)
                    Spacer()
                    MonarchBadge("Due in 3 days", tone: .warning)
                }
                Text("Acme Ltd, 12 seats").fontWeight(.regular).monarchTextStyle(.label)
            }
        }
    }
}

private struct AccordionShowcase: View {
    @State private var open: Set<String> = ["billing"]
    private let items: [(id: String, title: String, text: String)] = [
        ("billing", "How does billing work?", "Invoices are sent monthly to the billing contact."),
        ("cancel", "Can I cancel at any time?", "Yes, at the end of the current period."),
        ("export", "Can I export my data?", "Yes, as CSV from the settings page."),
    ]
    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            MonarchDivider()
            ForEach(items, id: \.id) { item in
                DisclosureGroup(
                    item.title,
                    isExpanded: Binding(
                        get: { open.contains(item.id) },
                        set: { isOpen in
                            if isOpen { open.insert(item.id) } else { open.remove(item.id) }
                        }
                    )
                ) {
                    Text(item.text)
                }
                MonarchDivider()
            }
        }
        .disclosureGroupStyle(.monarch)
    }
}

private struct StatCardShowcase: View {
    var body: some View {
        Column {
            MonarchStatCard(
                "Open tickets",
                value: 128,
                change: MonarchMetricChange(-0.09, comparison: "vs last week", goodDirection: .down),
                hint: "38 waiting on a customer"
            )
            MonarchStatCard(
                "Error rate",
                value: 0.042,
                format: .percent,
                hint: "Above the 2% target",
                tone: .danger
            )
        }
    }
}

private struct BreadcrumbShowcase: View {
    var body: some View {
        MonarchBreadcrumb(["Drive", "Clients", "Acme Ltd", "Contracts"].map { MonarchBreadcrumbItem($0) }) { _ in }
            .frame(maxWidth: .infinity, alignment: .leading)
    }
}

private struct CalloutBannerShowcase: View {
    var body: some View {
        Column {
            MonarchCalloutBanner(
                "Trial ending",
                message: "Your trial ends in 3 days. Add a payment method to keep your projects.",
                tone: .info,
                actionTitle: "Add payment",
                action: {}
            )
            MonarchCalloutBanner(
                message: "Invoice 1042 was sent to Acme Ltd.",
                systemImage: "checkmark.circle",
                tone: .success,
                onDismiss: {}
            )
            MonarchCalloutBanner(
                "Payment problem",
                message: "Two payments failed. Update the card to avoid a pause.",
                tone: .warning
            )
        }
    }
}

private struct ToastShowcase: View {
    @State private var toasts = MonarchToastCenter()
    var body: some View {
        Color.clear
            .frame(height: 96)
            .monarchToastHost(toasts)
            .task {
                toasts.show(
                    "Ticket archived",
                    message: "Moved to Archive.",
                    tone: .success,
                    actionTitle: "Undo",
                    duration: 3600
                ) {}
            }
    }
}

private struct SpinnerShowcase: View {
    var body: some View {
        Column {
            HStack(spacing: 12) {
                ProgressView().progressViewStyle(.monarchCircular(tone: .neutral, size: .sm))
                Text("Loading invoices").fontWeight(.regular).monarchTextStyle(.label)
            }
            MonarchButton("Syncing", variant: .secondary, isLoading: true) {}
        }
    }
}

private struct EmptyStateShowcase: View {
    var body: some View {
        MonarchEmptyState(
            "No invoices yet",
            message: "Invoices you send appear here.",
            systemImage: "doc.text",
            actionTitle: "New invoice"
        ) {}
    }
}

private struct TextAreaShowcase: View {
    @State private var notes = "Customer asked for a call on Thursday morning. Send the contract draft before."
    var body: some View {
        MonarchTextField(
            "Notes",
            text: $notes,
            helper: "Only your team sees these notes.",
            axis: .vertical,
            lineLimit: 3...6
        )
    }
}

private struct NumberInputShowcase: View {
    @State private var seats: Double? = 12
    @State private var tooMany: Double? = 80
    var body: some View {
        Column {
            MonarchNumberInput(
                "Seats",
                value: $seats,
                minimum: 1,
                maximum: 50,
                helper: "Billed per seat each month."
            )
            MonarchNumberInput(
                "Seats on the new plan",
                value: $tooMany,
                minimum: 1,
                maximum: 50,
                error: "Enter a number from 1 to 50."
            )
        }
    }
}

private struct SelectShowcase: View {
    @State private var region = "de"
    @State private var country = ""
    var body: some View {
        Column {
            MonarchSelectField(
                "Data region",
                selection: $region,
                options: [
                    MonarchSelectOption("United States", value: "us"),
                    MonarchSelectOption("Germany", value: "de"),
                    MonarchSelectOption("Slovenia", value: "si"),
                ],
                helper: "Where your files are stored."
            )
            MonarchSelectField(
                "Billing country",
                selection: $country,
                options: [
                    MonarchSelectOption("United States", value: "us"),
                    MonarchSelectOption("Germany", value: "de"),
                ],
                prompt: "Choose a country",
                error: "Choose a country to continue.",
                isRequired: true
            )
        }
    }
}

/// The compact style: filters above a list, as a toolbar or a panel header has them.
private struct SelectCompactShowcase: View {
    @State private var period = "30"
    @State private var status = "all"
    @ViewBuilder private var filters: some View {
        MonarchSelectField(
            "Period",
            selection: $period,
            options: [
                MonarchSelectOption("Last 7 days", value: "7"),
                MonarchSelectOption("Last 30 days", value: "30"),
                MonarchSelectOption("This quarter", value: "90"),
            ],
            style: .compact
        )
        MonarchSelectField(
            "Status",
            selection: $status,
            options: [
                MonarchSelectOption("All statuses", value: "all"),
                MonarchSelectOption("Paid", value: "paid"),
                MonarchSelectOption("Overdue", value: "overdue"),
            ],
            style: .compact
        )
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            ViewThatFits(in: .horizontal) {
                HStack(spacing: 8) { filters }
                VStack(alignment: .leading, spacing: 8) { filters }
            }
            Grouped {
                MonarchListRow("Invoice 1042", subtitle: "Acme Ltd", showsChevron: true)
                MonarchListRow("Invoice 1041", subtitle: "Globex", showsChevron: true, showsDivider: false)
            }
        }
    }
}

private struct SearchShowcase: View {
    @State private var query = "ana"
    private let people = ["Ana Silva", "Dana Novak", "Ivana Horvat", "Jordan Lee"]
    var body: some View {
        NavigationStack {
            List(people.filter { $0.localizedCaseInsensitiveContains(query) }, id: \.self) { name in
                MonarchListRow(name, systemImage: "person").monarchListRowStyle()
            }
            .listStyle(.plain)
            .monarchCanvas()
            .navigationTitle("People")
            .navigationBarTitleDisplayMode(.inline)
            .searchable(text: $query, placement: .navigationBarDrawer(displayMode: .always), prompt: "Search people")
        }
        .frame(height: 300)
    }
}

private struct CheckboxShowcase: View {
    @State private var receipts = true
    @State private var news = false
    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            Toggle("Email me receipts", isOn: $receipts)
            Toggle("Product updates", isOn: $news)
            Toggle("Security alerts (required by your plan)", isOn: .constant(true)).disabled(true)
        }
        .toggleStyle(.monarchCheckbox)
        .frame(maxWidth: .infinity, alignment: .leading)
    }
}

private struct RadioShowcase: View {
    @State private var channel = "email"
    private let options = [("email", "Email"), ("sms", "Text message"), ("none", "Don’t notify me")]
    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            ForEach(options, id: \.0) { value, title in
                Toggle(title, isOn: Binding(get: { channel == value }, set: { if $0 { channel = value } }))
            }
        }
        .toggleStyle(.monarchRadio)
        .frame(maxWidth: .infinity, alignment: .leading)
    }
}

private struct SwitchShowcase: View {
    @State private var alerts = true
    @State private var digest = false
    var body: some View {
        Column {
            MonarchFormField("Deploy alerts", helper: "A push notification when a deploy fails.", layout: .row) {
                Toggle("Deploy alerts", isOn: $alerts).labelsHidden()
            }
            MonarchFormField("Weekly digest", helper: "A summary every Monday.", layout: .row) {
                Toggle("Weekly digest", isOn: $digest).labelsHidden()
            }
            MonarchFormField("Audit log", helper: "Always on for the Business plan.", layout: .row) {
                Toggle("Audit log", isOn: .constant(true)).labelsHidden()
            }
            .disabled(true)
        }
    }
}

private struct SliderShowcase: View {
    @State private var threshold = 80.0
    var body: some View {
        MonarchFormField("Storage alert", helper: "Notify the owners when storage passes this level.") {
            HStack(spacing: 12) {
                Slider(value: $threshold, in: 50...100) { Text("Storage alert") }
                Text("\(Int(threshold))%").fontWeight(.regular)
                    .monarchTextStyle(.label)
                    .monospacedDigit()
                    .frame(minWidth: 40, alignment: .trailing)
            }
        }
    }
}

private struct SegmentedShowcase: View {
    @State private var layout = "list"
    @State private var range = "open"
    var body: some View {
        Column {
            MonarchSegmentedControl(
                "View",
                selection: $layout,
                items: [
                    MonarchSegmentedItem("List", value: "list", systemImage: "list.bullet"),
                    MonarchSegmentedItem("Board", value: "board", systemImage: "rectangle.split.3x1"),
                    MonarchSegmentedItem("Grid", value: "grid", systemImage: "square.grid.2x2"),
                ]
            )
            MonarchSegmentedControl(
                "Tickets",
                selection: $range,
                items: [
                    MonarchSegmentedItem("Open", value: "open", count: "12"),
                    MonarchSegmentedItem("Waiting", value: "waiting", count: "3"),
                    MonarchSegmentedItem("Closed", value: "closed", count: "48"),
                ],
                size: .sm,
                fillsWidth: false
            )
        }
    }
}

private struct TabsShowcase: View {
    @State private var filter = "open"
    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            MonarchTabs(
                "Ticket filters",
                selection: $filter,
                items: [
                    MonarchTabsItem("Open", value: "open", count: "12"),
                    MonarchTabsItem("Waiting", value: "waiting", count: "3"),
                    MonarchTabsItem("Closed", value: "closed"),
                ]
            )
            Grouped {
                MonarchListRow("Printer offline", subtitle: "Acme Ltd · 2 h ago", showsChevron: true)
                MonarchListRow("Refund request", subtitle: "Globex · 5 h ago", showsChevron: true, showsDivider: false)
            }
        }
    }
}

/// Rows in a rounded, hairline group, as a settings or detail screen shows them.
private struct Grouped<Content: View>: View {
    @ViewBuilder var content: Content
    @Environment(\.monarchTheme) private var theme
    var body: some View {
        VStack(spacing: 0) { content }
            .background(theme.colors.bgSurface, in: RoundedRectangle(cornerRadius: 12, style: .continuous))
            .overlay(RoundedRectangle(cornerRadius: 12, style: .continuous).strokeBorder(theme.colors.borderSubtle))
    }
}

private struct ListItemShowcase: View {
    var body: some View {
        Grouped {
            MonarchListRow("Ana Silva", subtitle: "Owner", systemImage: "person", showsChevron: true)
            MonarchListRow("Notifications", systemImage: "bell") { MonarchBadge("3", tone: .info) }
            MonarchListRow(
                "Billing",
                subtitle: "Plan and invoices",
                systemImage: "creditcard",
                showsChevron: true,
                showsDivider: false
            )
        }
    }
}

private struct PaginationShowcase: View {
    var body: some View {
        Grouped {
            ForEach(1040..<1044) { number in
                MonarchListRow("Invoice \(number)", subtitle: "Acme Ltd", showsChevron: true)
            }
            MonarchLoadMore(hasMore: true, loadedCount: 4, totalCount: 24) {}
        }
    }
}

private struct DataTableShowcase: View {
    struct Ticket: Identifiable, Sendable {
        let id: Int
        let subject: String
        let status: String
    }
    @State private var selection: Set<Int> = [42]
    @State private var sort: MonarchDataTableSort?
    var body: some View {
        MonarchDataTable(
            "Open tickets",
            rows: [
                Ticket(id: 42, subject: "Printer offline", status: "Open"),
                Ticket(id: 43, subject: "Refund request", status: "Waiting"),
                Ticket(id: 44, subject: "Invoice address", status: "Open"),
            ],
            columns: [
                MonarchDataTableColumn("Subject", isHideable: false, value: { $0.subject }),
                MonarchDataTableColumn("Status", value: { $0.status }),
            ],
            sort: $sort,
            selection: $selection,
            layout: .list,
            rowActions: { _ in [MonarchDataTableRowAction("Assign to me", systemImage: "person.badge.plus") {}] },
            onOpen: { _ in }
        )
        .frame(height: 220)
    }
}

/// A quiet gradient that stands in for a photograph, so a preview of a media component shows a picture area and not a
/// stock SF Symbol drawn as large as the card. Drawn once, the same in light and dark.
@MainActor
private enum SamplePhoto {
    static let image: Image = {
        let size = CGSize(width: 1200, height: 630)
        let rendered = UIGraphicsImageRenderer(size: size).image { context in
            let colors = [UIColor(red: 0.74, green: 0.80, blue: 0.88, alpha: 1), UIColor(red: 0.86, green: 0.80, blue: 0.86, alpha: 1)]
            guard let gradient = CGGradient(
                colorsSpace: CGColorSpaceCreateDeviceRGB(),
                colors: colors.map(\.cgColor) as CFArray,
                locations: [0, 1]
            ) else { return }
            context.cgContext.drawLinearGradient(
                gradient, start: .zero, end: CGPoint(x: size.width, y: size.height), options: []
            )
        }
        return Image(uiImage: rendered)
    }()
}

private struct LinkCardShowcase: View {
    var body: some View {
        MonarchLinkCard(
            "Release notes for March",
            url: URL(string: "https://example.com/blog/march") ?? URL(fileURLWithPath: "/"),
            description: "Faster search, shared views and a new API.",
            image: SamplePhoto.image,
            imageSize: CGSize(width: 1200, height: 630)
        )
    }
}

private struct VideoThumbnailShowcase: View {
    var body: some View {
        MonarchVideoThumbnail("Product tour", image: SamplePhoto.image, duration: 65) {}
    }
}

private struct ScreenShareShowcase: View {
    @State private var zoom = 1.0
    var body: some View {
        MonarchScreenShareView(
            "Ana Flores",
            zoom: $zoom,
            isPictureInPictureActive: false,
            onPictureInPicture: {},
            video: { SamplePhoto.image.resizable().scaledToFit() }
        )
    }
}

private struct LightboxShowcase: View {
    @State private var isShown = true
    @State private var index = 0

    var body: some View {
        MonarchButton("View photos") {
            index = 0
            isShown = true
        }
        .monarchLightbox(
            isPresented: $isShown,
            items: [
                MonarchLightboxItem(
                    image: SamplePhoto.image,
                    alt: "Whiteboard sketch of the checkout flow",
                    caption: "Sketch from the planning day"
                ),
                MonarchLightboxItem(image: SamplePhoto.image, alt: "Team photo"),
            ],
            selection: $index
        )
    }
}
/// A quiet picture of a sign-in page (a card on a soft gradient), so the computer view shows a screenshot and not a
/// stock SF Symbol drawn as large as the pane.
@MainActor
private enum SampleScreen {
    static let image: Image = {
        let size = CGSize(width: 1280, height: 800)
        let rendered = UIGraphicsImageRenderer(size: size).image { context in
            let cg = context.cgContext
            let colors = [UIColor(red: 0.93, green: 0.94, blue: 0.97, alpha: 1), UIColor(red: 0.96, green: 0.93, blue: 0.96, alpha: 1)]
            if let gradient = CGGradient(
                colorsSpace: CGColorSpaceCreateDeviceRGB(), colors: colors.map(\.cgColor) as CFArray, locations: [0, 1]
            ) {
                cg.drawLinearGradient(gradient, start: .zero, end: CGPoint(x: size.width, y: size.height), options: [])
            }
            let card = CGRect(x: 400, y: 150, width: 480, height: 500)
            UIColor.white.setFill()
            UIBezierPath(roundedRect: card, cornerRadius: 24).fill()
            let line = UIColor(white: 0.86, alpha: 1)
            line.setFill()
            UIBezierPath(roundedRect: CGRect(x: 450, y: 220, width: 200, height: 28), cornerRadius: 8).fill()
            for row in 0..<2 {
                UIBezierPath(
                    roundedRect: CGRect(x: 450, y: 300 + CGFloat(row) * 90, width: 380, height: 56), cornerRadius: 12
                ).fill()
            }
            UIColor(red: 0.45, green: 0.47, blue: 0.55, alpha: 1).setFill()
            UIBezierPath(roundedRect: CGRect(x: 450, y: 520, width: 380, height: 60), cornerRadius: 14).fill()
        }
        return Image(uiImage: rendered)
    }()
}

private struct ComputerViewShowcase: View {
    @State private var status = MonarchAiComputerViewStatus.working
    var body: some View {
        MonarchAiComputerView(
            status: status,
            frame: MonarchAiComputerFrame(
                image: SampleScreen.image,
                width: 1280,
                height: 800,
                capturedAt: Date(timeIntervalSinceReferenceDate: 800_000_000),
                alt: "The sign-in page"
            ),
            actions: [
                MonarchAiComputerAction(
                    id: "continue",
                    kind: .click,
                    label: "Clicked Continue",
                    date: Date(timeIntervalSinceReferenceDate: 800_000_000),
                    point: MonarchAiComputerPoint(x: 640, y: 550)
                )
            ],
            currentURL: "https://example.com/login",
            onPauseChange: { paused in status = paused ? .paused : .working },
            onTakeOver: { status = .takenOver },
            onHandBack: { status = .working },
            onStop: { status = .stopped }
        )
    }
}

private struct ImageGenerationShowcase: View {
    @State private var images = [
        MonarchAiGeneratedImage(id: "1", image: SamplePhoto.image, alt: "A lighthouse at dusk"),
        MonarchAiGeneratedImage(id: "2", image: SamplePhoto.image, alt: "A lighthouse in fog"),
    ]
    @State private var selection: String? = "1"
    var body: some View {
        MonarchAiImageGeneration(images: $images, status: .done, selection: $selection, onVariations: { _ in })
    }
}


/// The context menu of the web primary example, drawn open: SwiftUI's `contextMenu` has no state that opens it, and
/// the system menu dims the screen around it. The panel has the example's items (Rename with its pencil, Duplicate,
/// a divider, Delete in the danger colour) in the order the example gives them.
private struct ContextMenuShowcase: View {
    @Environment(\.monarchTheme) private var theme

    var body: some View {
        VStack(alignment: .leading, spacing: theme.spacing.space2) {
            VStack(spacing: theme.spacing.space2) {
                Text("Quarterly report.pdf").monarchTextStyle(.label).foregroundStyle(theme.colors.textDefault)
                Text("Touch and hold the file for its actions.")
                    .monarchTextStyle(.caption)
                    .foregroundStyle(theme.colors.textMuted)
            }
            .frame(maxWidth: .infinity, minHeight: 128)
            .padding(theme.spacing.space4)
            .background(theme.colors.bgSurface, in: RoundedRectangle(cornerRadius: theme.radius.lg))
            VStack(alignment: .leading, spacing: 0) {
                row("Rename", systemImage: "pencil", color: theme.colors.textDefault)
                row("Duplicate", systemImage: nil, color: theme.colors.textDefault)
                Divider()
                row("Delete", systemImage: "trash", color: theme.colors.dangerStrong)
            }
            .padding(.vertical, theme.spacing.space1)
            .frame(width: 220, alignment: .leading)
            .background(theme.colors.bgSurface, in: RoundedRectangle(cornerRadius: theme.radius.lg))
            .overlay(RoundedRectangle(cornerRadius: theme.radius.lg).strokeBorder(theme.colors.borderSubtle))
            Text("Last action: Nothing yet").monarchTextStyle(.caption).foregroundStyle(theme.colors.textDefault)
        }
        .frame(maxWidth: 384)
    }

    private func row(_ title: String, systemImage: String?, color: Color) -> some View {
        HStack(spacing: theme.spacing.space2) {
            if let systemImage { MonarchIcon(systemName: systemImage, size: .sm) }
            Text(title).monarchTextStyle(.body)
            Spacer(minLength: 0)
        }
        .foregroundStyle(color)
        .padding(.horizontal, theme.spacing.space3)
        .frame(minHeight: 44)
    }
}
