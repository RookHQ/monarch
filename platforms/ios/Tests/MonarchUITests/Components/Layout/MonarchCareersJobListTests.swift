import MonarchTokens
import SwiftUI
import Testing

@testable import MonarchUI

enum MonarchCareersJobListFixtures: ComponentFixtures {
    static let id = "careers-job-list"
    static let jobs = [
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
            qualifications: ["Three years of product design", "A portfolio of shipped work"],
            mustHaveSkills: ["Prototyping", "User research"],
            niceToHaveSkills: ["Motion design"],
            benefits: ["Learning budget", "Flexible hours"],
            applyURL: URL(string: "https://example.com/apply-designer")
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
        MonarchCareerJob(
            "j3",
            title: "Backend Engineer",
            department: "Engineering",
            location: "Hybrid · Berlin",
            employmentType: "Full time",
            summary: "Build the APIs that power scheduling and billing.",
            mustHaveSkills: ["TypeScript", "PostgreSQL"],
            applyURL: URL(string: "https://example.com/apply-backend")
        ),
    ]
    static let fixtures = [
        Fixture("three roles") {
            MonarchCareersJobList(jobs: jobs, organizationName: "Northwind", contactEmail: "jobs@example.com")
        },
        Fixture("single column, own title") {
            MonarchCareersJobList(
                jobs: Array(jobs.prefix(2)),
                columns: 1,
                labels: MonarchCareersJobListLabels(title: "Featured roles")
            )
        },
        Fixture("no open roles") {
            MonarchCareersJobList(jobs: [], labels: MonarchCareersJobListLabels(title: "Internships"))
        },
        Fixture("details sheet content") {
            MonarchCareersJobDetail(
                job: jobs[0],
                contactEmail: "jobs@example.com",
                organizationName: "Northwind",
                labels: MonarchCareersJobListLabels()
            ) {}
            .frame(height: MonarchTokens.Space.space24 * 6)
        },
    ]
}

@Suite("MonarchCareersJobList")
struct MonarchCareersJobListTests {
    @Test("The count says position or positions")
    func count() {
        #expect(MonarchCareersJobListTokens.count(1) == "1 position")
        #expect(MonarchCareersJobListTokens.count(3) == "3 positions")
    }

    @Test("The meta line keeps location, employment type and experience in order")
    func meta() {
        #expect(
            MonarchCareersJobListTokens.meta(MonarchCareersJobListFixtures.jobs[0]) == [
                "Remote", "Full time", "3+ years",
            ]
        )
        #expect(MonarchCareersJobListTokens.meta(MonarchCareerJob("x", title: "Role")).isEmpty)
    }

    @Test("A card is read as its title, team, meta, salary and summary")
    func accessibilityLabel() {
        let label = MonarchCareersJobListTokens.accessibilityLabel(MonarchCareersJobListFixtures.jobs[1])
        #expect(
            label
                == "Support Specialist, Customer support, Lisbon, Part time, Help customers get the most out of the product."
        )
    }

    @Test("One column on a phone and at accessibility sizes, up to three at regular width")
    func columns() {
        #expect(
            MonarchCareersJobListTokens.columns(requested: 2, isRegularWidth: false, isAccessibilitySize: false) == 1
        )
        #expect(
            MonarchCareersJobListTokens.columns(requested: 2, isRegularWidth: true, isAccessibilitySize: false) == 2
        )
        #expect(
            MonarchCareersJobListTokens.columns(requested: 5, isRegularWidth: true, isAccessibilitySize: false) == 3
        )
        #expect(MonarchCareersJobListTokens.columns(requested: 2, isRegularWidth: true, isAccessibilitySize: true) == 1)
    }

    @Test("The application email has the role in its subject")
    func mail() {
        let url = MonarchCareersJobListTokens.mailURL(
            email: "jobs@example.com",
            job: MonarchCareersJobListFixtures.jobs[0]
        )
        #expect(url?.scheme == "mailto")
        #expect(url?.absoluteString.contains("jobs@example.com") == true)
        #expect(url?.absoluteString.contains("Product%20Designer") == true)
    }

    @Test("The built-in strings resolve")
    func strings() {
        #expect(MonarchCareersJobListStrings.title == "Open roles")
        #expect(MonarchCareersJobListStrings.empty == "No open roles right now. Check back soon.")
        #expect(MonarchCareersJobListStrings.apply == "Apply now")
        #expect(MonarchCareersJobListStrings.emailInstead == "Email instead")
        #expect(MonarchCareersJobListStrings.responsibilities == "What you’ll do")
        #expect(MonarchCareersJobListStrings.close == "Close")
        #expect(String(format: MonarchCareersJobListStrings.applyingTo, "Northwind") == "Applying to Northwind")
    }
}
