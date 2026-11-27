package com.rookhq.monarch.components.layout.careersjoblist

import androidx.compose.runtime.Immutable

/**
 * One open role of a [MonarchCareersJobList].
 *
 * @property id identifies the role, for `openJob`.
 * @property title the job title.
 * @property department the team, shown in small capitals above the title.
 * @property location where the role is based, such as "Remote".
 * @property employmentType such as "Full time".
 * @property experience such as "3+ years".
 * @property salary the range, shown exactly as given.
 * @property summary a sentence or two on the role; the card shows two lines, the details all of it.
 * @property responsibilities what the person will do, listed in the details.
 * @property qualifications what the team is looking for, listed in the details.
 * @property mustHaveSkills skills shown as tags on the card and in the details.
 * @property niceToHaveSkills skills shown only in the details.
 * @property benefits listed in the details.
 * @property applyLink an app-defined application destination. Without it, applying is by email when the list has
 * a `contactEmail`.
 */
@Immutable
public class MonarchCareerJob(
    public val id: String,
    public val title: String,
    public val department: String? = null,
    public val location: String? = null,
    public val employmentType: String? = null,
    public val experience: String? = null,
    public val salary: String? = null,
    public val summary: String? = null,
    public val responsibilities: List<String> = emptyList(),
    public val qualifications: List<String> = emptyList(),
    public val mustHaveSkills: List<String> = emptyList(),
    public val niceToHaveSkills: List<String> = emptyList(),
    public val benefits: List<String> = emptyList(),
    public val applyLink: String? = null,
)
