package dev.monarchui.components.layout.careersjoblist

import androidx.compose.runtime.Immutable

/**
 * Replacements for the built-in strings of a `MonarchCareersJobList`. Every `null` uses the built-in string named
 * after it in `strings_careersjoblist.xml`.
 *
 * @property title the heading of the list: "Open roles".
 * @property empty the message without roles: "No open roles right now. Check back soon."
 * @property responsibilities the heading of the duties in the details: "What you’ll do".
 * @property qualifications the heading of the requirements: "What we’re looking for".
 * @property mustHave the heading of the must-have skills: "Must-have skills".
 * @property niceToHave the heading of the optional skills: "Nice to have".
 * @property benefits the heading of the benefits: "Benefits".
 * @property apply the button of a role with an application link: "Apply now".
 * @property applyByEmail the button of a role applied to by email: "Apply by email".
 */
@Immutable
public class MonarchCareersJobListLabels(
    public val title: String? = null,
    public val empty: String? = null,
    public val responsibilities: String? = null,
    public val qualifications: String? = null,
    public val mustHave: String? = null,
    public val niceToHave: String? = null,
    public val benefits: String? = null,
    public val apply: String? = null,
    public val applyByEmail: String? = null,
)
