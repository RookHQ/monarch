package dev.monarchui.components.layout.careersjoblist

import androidx.compose.runtime.Composable
import dev.monarchui.internal.preview.MonarchPreviews
import dev.monarchui.internal.preview.PreviewFrame

@MonarchPreviews
@Composable
internal fun MonarchCareersJobListPreview() {
    PreviewFrame {
        MonarchCareersJobList(
            jobs = listOf(
                MonarchCareerJob(
                    id = "j1",
                    title = "Product Designer",
                    department = "Design",
                    location = "Remote",
                    employmentType = "Full time",
                    salary = "USD 90,000 – 120,000",
                    summary = "Shape how teams plan and track their work, from research to shipped flows.",
                    mustHaveSkills = listOf("Prototyping", "User research"),
                ),
            ),
            openJob = null,
            onOpenJobChange = {},
            onApply = {},
        )
        MonarchCareersJobList(jobs = emptyList(), openJob = null, onOpenJobChange = {}, onApply = {})
    }
}
