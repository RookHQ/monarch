import SwiftUI

/// The details of one role, shown in a sheet: its sections, then the apply actions.
struct MonarchCareersJobDetail: View {
    let job: MonarchCareerJob
    let contactEmail: String?
    let organizationName: String?
    let labels: MonarchCareersJobListLabels
    let onClose: () -> Void

    @Environment(\.monarchTheme) private var theme

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(alignment: .leading, spacing: MonarchCareersJobListTokens.detailSpacing) {
                    VStack(alignment: .leading, spacing: MonarchCareersJobListTokens.lineSpacing) {
                        if let department = job.department {
                            Text(department)
                                .textCase(.uppercase)
                                .monarchTextStyle(.caption, weight: MonarchCareersJobListTokens.departmentWeight)
                                .foregroundStyle(theme.colors[role: MonarchCareersJobListTokens.subtleColor])
                        }
                        Text(job.title)
                            .monarchTextStyle(.title)
                            .foregroundStyle(theme.colors.textDefault)
                            .accessibilityAddTraits(.isHeader)
                        MonarchCareersJobMeta(job: job)
                        if let summary = job.summary {
                            Text(summary)
                                .monarchTextStyle(.body)
                                .foregroundStyle(theme.colors[role: MonarchCareersJobListTokens.supportingColor])
                        }
                    }
                    list(labels.responsibilities ?? MonarchCareersJobListStrings.responsibilities, job.responsibilities)
                    list(labels.qualifications ?? MonarchCareersJobListStrings.qualifications, job.qualifications)
                    tags(labels.mustHave ?? MonarchCareersJobListStrings.mustHave, job.mustHaveSkills)
                    tags(labels.niceToHave ?? MonarchCareersJobListStrings.niceToHave, job.niceToHaveSkills)
                    list(labels.benefits ?? MonarchCareersJobListStrings.benefits, job.benefits)
                }
                .padding()
                .frame(maxWidth: .infinity, alignment: .leading)
            }
            .background(theme.colors.bgCanvas)
            .safeAreaInset(edge: .bottom) { actions }
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button(labels.close ?? MonarchCareersJobListStrings.close, action: onClose)
                }
            }
        }
    }

    @ViewBuilder private var actions: some View {
        let mail = contactEmail.flatMap { MonarchCareersJobListTokens.mailURL(email: $0, job: job) }
        if job.applyURL != nil || mail != nil {
            VStack(spacing: MonarchCareersJobListTokens.lineSpacing) {
                if let organizationName {
                    Text(String(format: labels.applyingTo ?? MonarchCareersJobListStrings.applyingTo, organizationName))
                        .monarchTextStyle(.caption)
                        .foregroundStyle(theme.colors[role: MonarchCareersJobListTokens.supportingColor])
                }
                if let apply = job.applyURL {
                    Link(destination: apply) {
                        Text(labels.apply ?? MonarchCareersJobListStrings.apply).frame(maxWidth: .infinity)
                    }
                    .buttonStyle(.monarch(.primary, size: .lg, fullWidth: true))
                    if let mail {
                        Link(labels.emailInstead ?? MonarchCareersJobListStrings.emailInstead, destination: mail)
                            .buttonStyle(.monarch(.ghost, fullWidth: true))
                    }
                } else if let mail {
                    Link(destination: mail) {
                        Text(labels.applyByEmail ?? MonarchCareersJobListStrings.applyByEmail).frame(
                            maxWidth: .infinity
                        )
                    }
                    .buttonStyle(.monarch(.primary, size: .lg, fullWidth: true))
                }
            }
            .padding()
            .background(.bar)
        }
    }

    @ViewBuilder private func list(_ title: String, _ items: [String]) -> some View {
        if !items.isEmpty {
            VStack(alignment: .leading, spacing: MonarchCareersJobListTokens.lineSpacing) {
                Text(title)
                    .monarchTextStyle(.subheading)
                    .foregroundStyle(theme.colors.textDefault)
                    .accessibilityAddTraits(.isHeader)
                ForEach(Array(items.enumerated()), id: \.offset) { _, item in
                    Label {
                        Text(item).monarchTextStyle(.body).foregroundStyle(theme.colors.textDefault)
                    } icon: {
                        MonarchIcon(systemName: "circle.fill", size: .sm)
                            .scaleEffect(MonarchCareersJobListTokens.bulletScale)
                            .foregroundStyle(theme.colors[role: MonarchCareersJobListTokens.subtleColor])
                    }
                }
            }
        }
    }

    @ViewBuilder private func tags(_ title: String, _ items: [String]) -> some View {
        if !items.isEmpty {
            VStack(alignment: .leading, spacing: MonarchCareersJobListTokens.lineSpacing) {
                Text(title)
                    .monarchTextStyle(.subheading)
                    .foregroundStyle(theme.colors.textDefault)
                    .accessibilityAddTraits(.isHeader)
                MonarchFlowLayout(spacing: MonarchCareersJobListTokens.lineSpacing) {
                    ForEach(items, id: \.self) { MonarchTag($0, size: .sm) }
                }
            }
        }
    }
}

/// The meta line of a role: location, employment type and experience, then the salary.
struct MonarchCareersJobMeta: View {
    let job: MonarchCareerJob

    @Environment(\.monarchTheme) private var theme

    var body: some View {
        VStack(alignment: .leading, spacing: MonarchCareersJobListTokens.metaLineSpacing) {
            let meta = MonarchCareersJobListTokens.meta(job)
            if !meta.isEmpty {
                MonarchFlowLayout(spacing: MonarchCareersJobListTokens.metaSpacing) {
                    ForEach(meta, id: \.self) { item in
                        Text(item)
                            .monarchTextStyle(.bodySmall)
                            .foregroundStyle(theme.colors[role: MonarchCareersJobListTokens.supportingColor])
                    }
                }
            }
            if let salary = job.salary {
                Text(salary)
                    .monarchTextStyle(.bodySmall)
                    .foregroundStyle(theme.colors[role: MonarchCareersJobListTokens.supportingColor])
            }
        }
    }
}
