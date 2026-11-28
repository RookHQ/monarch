package com.rookhq.monarch.components.layout.careersjoblist

import androidx.compose.runtime.Composable
import com.rookhq.monarch.internal.preview.MonarchPreviews
import com.rookhq.monarch.internal.preview.PreviewFrame

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
