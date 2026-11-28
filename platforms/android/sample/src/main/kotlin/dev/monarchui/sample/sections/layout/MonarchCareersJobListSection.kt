package dev.monarchui.sample.sections.layout

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import dev.monarchui.components.layout.careersjoblist.MonarchCareerJob
import dev.monarchui.components.layout.careersjoblist.MonarchCareersJobList
import dev.monarchui.components.typography.text.MonarchText
import dev.monarchui.sample.CataloguePage

/** The catalogue page of CareersJobList. A card opens the details; applying names the role below. */
object MonarchCareersJobListSection : CataloguePage {
    override val title = "CareersJobList"

    private val jobs = listOf(
        MonarchCareerJob(
            id = "j1",
            title = "Product Designer",
            department = "Design",
            location = "Remote",
            employmentType = "Full time",
            experience = "3+ years",
            salary = "USD 90,000 – 120,000",
            summary = "Shape how teams plan and track their work, from research to shipped flows.",
            responsibilities = listOf("Design new flows end to end", "Run research sessions with customers"),
            qualifications = listOf("Three years of product design", "A portfolio of shipped work"),
            mustHaveSkills = listOf("Prototyping", "User research"),
            niceToHaveSkills = listOf("Motion design"),
            benefits = listOf("Learning budget", "Flexible hours"),
            applyLink = "apply-designer",
        ),
        MonarchCareerJob(
            id = "j2",
            title = "Support Specialist",
            department = "Customer support",
            location = "Lisbon",
            employmentType = "Part time",
            summary = "Help customers get the most out of the product.",
            mustHaveSkills = listOf("Written English", "Patience"),
        ),
        MonarchCareerJob(
            id = "j3",
            title = "Backend Engineer",
            department = "Engineering",
            location = "Hybrid · Berlin",
            employmentType = "Full time",
            summary = "Build the APIs that power scheduling and billing.",
            mustHaveSkills = listOf("TypeScript", "PostgreSQL"),
            applyLink = "apply-backend",
        ),
    )

    @Composable
    override fun Content() {
        var open by rememberSaveable { mutableStateOf<String?>(null) }
        var applied by rememberSaveable { mutableStateOf("") }
        MonarchCareersJobList(
            jobs = jobs,
            openJob = open,
            onOpenJobChange = { open = it },
            onApply = {
                applied = it.title
                open = null
            },
            organizationName = "Northwind",
            contactEmail = "jobs@example.com",
        )
        if (applied.isNotEmpty()) MonarchText(text = "Applied to $applied")
    }
}
