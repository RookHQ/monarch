package dev.monarchui.components.workflow.approvalqueue

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.painter.Painter
import dev.monarchui.components.datadisplay.statusbadge.MonarchStatusDefinition
import dev.monarchui.components.workflow.approvaltimeline.MonarchApprovalPerson

/**
 * One request waiting for a decision in a `MonarchApprovalQueue`.
 *
 * @property id a stable, unique id.
 * @property requesterName the name of the person who asked, already localised.
 * @property title what is asked, such as "Expense claim" or "Leave, 3 days", already localised.
 * @property requesterImage the requester's picture, loaded by the app. Initials while it is `null`.
 * @property requesterSeed picks the colour of the initials, so a renamed person keeps their colour. The name by
 * default.
 * @property requesterDetail a second line under the name, such as a team or an employee code.
 * @property summary short facts, such as an amount or a period.
 * @property submittedMillis when it was submitted, in epoch milliseconds (UTC). Shown as a relative time.
 * @property overdue whether it is past its deadline: adds a Past due badge.
 * @property badges extra badges, such as "Resubmitted". Each shows its label, so the tone is never the only signal.
 * @property searchText the text the search matches. The requester's name and the title when `null`.
 * @property canDecide whether the viewer can decide it. A request they can only see has no Approve, no Send back
 * and a disabled checkbox; Review stays.
 *
 * The requester is also available as one `MonarchApprovalPerson`, the type the
 * approval timeline and iOS use: build the item from one with the secondary
 * constructor, and read it back as [requester].
 */
@Immutable
public class MonarchApprovalQueueItem(
    public val id: String,
    public val requesterName: String,
    public val title: String,
    public val requesterImage: Painter? = null,
    public val requesterSeed: String? = null,
    public val requesterDetail: String? = null,
    public val summary: String? = null,
    public val submittedMillis: Long? = null,
    public val overdue: Boolean = false,
    public val badges: List<MonarchStatusDefinition> = emptyList(),
    public val searchText: String? = null,
    public val canDecide: Boolean = true,
) {
    /**
     * An item whose requester is [requester], as in `MonarchApprovalTimeline`
     * and on iOS.
     */
    public constructor(
        id: String,
        requester: MonarchApprovalPerson,
        title: String,
        requesterDetail: String? = null,
        summary: String? = null,
        submittedMillis: Long? = null,
        overdue: Boolean = false,
        badges: List<MonarchStatusDefinition> = emptyList(),
        searchText: String? = null,
        canDecide: Boolean = true,
    ) : this(
        id = id,
        requesterName = requester.name,
        title = title,
        requesterImage = requester.image,
        requesterSeed = requester.seed,
        requesterDetail = requesterDetail,
        summary = summary,
        submittedMillis = submittedMillis,
        overdue = overdue,
        badges = badges,
        searchText = searchText,
        canDecide = canDecide,
    )

    /** The person who asked: the name, picture and colour seed as one value. */
    public val requester: MonarchApprovalPerson
        get() = MonarchApprovalPerson(name = requesterName, image = requesterImage, seed = requesterSeed)
}
