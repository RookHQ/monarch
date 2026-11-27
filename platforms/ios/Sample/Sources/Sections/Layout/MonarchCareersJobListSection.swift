import MonarchUI
import SwiftUI

struct MonarchCareersJobListSection: CataloguePage {
    static let title = "Careers job list"

    var body: some View {
        CatalogueCaption("Tap a role for its details")
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
                    responsibilities: ["Design new flows end to end", "Run research sessions with customers"],
                    mustHaveSkills: ["Prototyping", "User research"],
                    applyURL: URL(string: "https://example.com/apply")
                ),
                MonarchCareerJob(
                    "j2",
                    title: "Support Specialist",
                    department: "Customer support",
                    location: "Lisbon",
                    employmentType: "Part time",
                    summary: "Help customers get the most out of the product.",
                    mustHaveSkills: ["Written English", "Patience"]
                ),
            ],
            organizationName: "Northwind",
            contactEmail: "jobs@example.com"
        )

        CatalogueCaption("No open roles")
        MonarchCareersJobList(jobs: [], labels: MonarchCareersJobListLabels(title: "Internships"))
    }
}
