import SwiftUI

/// Open roles as cards: the team, the title, where and how the role is worked, the salary, a summary and the
/// skills. A tap opens the role's details in a sheet with the apply actions.
///
/// ```swift
/// MonarchCareersJobList(jobs: jobs, organizationName: "Northwind", contactEmail: "jobs@example.com")
/// ```
///
/// One column on a phone; at regular width the cards sit in `columns` columns. With no roles the list shows a
/// short message under the title.
///
/// Accessibility: the title is a heading and the count is read after it. Each card is one button, read as the
/// title, the team, the meta items, the salary and the summary, with the hint "View details". The details sheet
/// has a Close button, headings for each section, and the apply link at the bottom.
public struct MonarchCareersJobList: View {
    private let jobs: [MonarchCareerJob]
    private let columns: Int
    private let organizationName: String?
    private let contactEmail: String?
    private let showsHeader: Bool
    private let labels: MonarchCareersJobListLabels

    @State private var openJob: MonarchCareerJob?
    @Environment(\.monarchTheme) private var theme
    @Environment(\.dynamicTypeSize) private var dynamicTypeSize
    #if os(iOS)
        @Environment(\.horizontalSizeClass) private var horizontalSizeClass
    #endif

    /// A list of open roles.
    ///
    /// - Parameters:
    ///   - jobs: The roles, in display order.
    ///   - columns: The number of columns at regular width, from one to three. One on a phone.
    ///   - organizationName: The employer, named above the apply actions.
    ///   - contactEmail: An address people can apply to, offered in the details.
    ///   - showsHeader: Whether the title and the count are shown.
    ///   - labels: Replacements for the built-in strings.
    public init(
        jobs: [MonarchCareerJob],
        columns: Int = 2,
        organizationName: String? = nil,
        contactEmail: String? = nil,
        showsHeader: Bool = true,
        labels: MonarchCareersJobListLabels = MonarchCareersJobListLabels()
    ) {
        self.jobs = jobs
        self.columns = columns
        self.organizationName = organizationName
        self.contactEmail = contactEmail
        self.showsHeader = showsHeader
        self.labels = labels
    }

    /// The content of the view.
    public var body: some View {
        VStack(alignment: .leading, spacing: MonarchCareersJobListTokens.spacing) {
            if showsHeader {
                HStack(alignment: .firstTextBaseline) {
                    Text(labels.title ?? MonarchCareersJobListStrings.title)
                        .monarchTextStyle(.heading)
                        .foregroundStyle(theme.colors.textDefault)
                        .accessibilityAddTraits(.isHeader)
                    Spacer(minLength: MonarchCareersJobListTokens.metaSpacing)
                    if !jobs.isEmpty {
                        Text(MonarchCareersJobListTokens.count(jobs.count))
                            .monarchTextStyle(.bodySmall)
                            .foregroundStyle(theme.colors[role: MonarchCareersJobListTokens.supportingColor])
                    }
                }
            }
            if jobs.isEmpty {
                Text(labels.empty ?? MonarchCareersJobListStrings.empty)
                    .monarchTextStyle(.body)
                    .foregroundStyle(theme.colors[role: MonarchCareersJobListTokens.supportingColor])
            } else {
                LazyVGrid(
                    columns: Array(
                        repeating: GridItem(
                            .flexible(),
                            spacing: MonarchCareersJobListTokens.cardSpacing,
                            alignment: .top
                        ),
                        count: MonarchCareersJobListTokens.columns(
                            requested: columns,
                            isRegularWidth: isRegularWidth,
                            isAccessibilitySize: dynamicTypeSize.isAccessibilitySize
                        )
                    ),
                    alignment: .leading,
                    spacing: MonarchCareersJobListTokens.cardSpacing
                ) {
                    ForEach(jobs) { job in card(job) }
                }
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .sheet(item: $openJob) { job in
            MonarchCareersJobDetail(
                job: job,
                contactEmail: contactEmail,
                organizationName: organizationName,
                labels: labels
            ) { openJob = nil }
        }
    }

    private func card(_ job: MonarchCareerJob) -> some View {
        Button {
            openJob = job
        } label: {
            VStack(alignment: .leading, spacing: MonarchCareersJobListTokens.lineSpacing) {
                HStack(alignment: .firstTextBaseline) {
                    if let department = job.department {
                        Text(department)
                            .textCase(.uppercase)
                            .monarchTextStyle(.caption, weight: MonarchCareersJobListTokens.departmentWeight)
                            .foregroundStyle(theme.colors[role: MonarchCareersJobListTokens.subtleColor])
                    }
                    Spacer(minLength: 0)
                    MonarchIcon(systemName: MonarchCareersJobListTokens.chevronSymbol, size: .sm)
                        .flipsForRightToLeftLayoutDirection(true)
                        .foregroundStyle(theme.colors[role: MonarchCareersJobListTokens.subtleColor])
                }
                Text(job.title)
                    .monarchTextStyle(.subheading)
                    .foregroundStyle(theme.colors.textDefault)
                MonarchCareersJobMeta(job: job)
                if let summary = job.summary {
                    Text(summary)
                        .monarchTextStyle(.bodySmall)
                        .foregroundStyle(theme.colors[role: MonarchCareersJobListTokens.supportingColor])
                        .lineLimit(MonarchCareersJobListTokens.summaryLines)
                }
                if !job.mustHaveSkills.isEmpty {
                    MonarchFlowLayout(spacing: MonarchCareersJobListTokens.lineSpacing) {
                        ForEach(job.mustHaveSkills, id: \.self) { MonarchTag($0, size: .sm) }
                    }
                }
            }
            .multilineTextAlignment(.leading)
            .padding(MonarchCareersJobListTokens.cardPadding)
            .frame(maxWidth: .infinity, alignment: .leading)
            .background(theme.colors.bgSurface, in: MonarchShapes.container)
            .overlay(
                MonarchShapes.container.strokeBorder(theme.colors.borderSubtle, lineWidth: MonarchShapes.borderWidth)
            )
            .contentShape(MonarchShapes.container)
        }
        .buttonStyle(.plain)
        .accessibilityElement(children: .ignore)
        .accessibilityLabel(MonarchCareersJobListTokens.accessibilityLabel(job))
        .accessibilityHint(labels.viewDetails ?? MonarchCareersJobListStrings.viewDetails)
        .accessibilityAddTraits(.isButton)
    }

    private var isRegularWidth: Bool {
        #if os(iOS)
            horizontalSizeClass == .regular
        #else
            false
        #endif
    }
}

#if DEBUG
    #Preview("MonarchCareersJobList") {
        MonarchPreviewMatrix {
            MonarchCareersJobList(
                jobs: [
                    MonarchCareerJob(
                        "j1",
                        title: "Product Designer",
                        department: "Design",
                        location: "Remote",
                        employmentType: "Full time",
                        experience: "3+ years",
                        salary: "USD 90,000 – 120,000",
                        summary: "Shape how teams plan and track their work, from research to shipped flows.",
                        mustHaveSkills: ["Prototyping", "User research"]
                    ),
                    MonarchCareerJob(
                        "j2",
                        title: "Support Specialist",
                        department: "Customer support",
                        location: "Lisbon"
                    ),
                ]
            )
        }
    }
#endif
