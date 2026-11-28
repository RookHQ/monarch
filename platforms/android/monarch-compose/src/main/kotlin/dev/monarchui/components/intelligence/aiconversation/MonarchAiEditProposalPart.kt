package dev.monarchui.components.intelligence.aiconversation

/**
 * An edit the reply proposes to something outside the conversation.
 *
 * @property id the id of the proposal.
 * @property target what is being edited, for example a document id.
 * @property diff the change, in the shape the product uses.
 * @property state what happened to the proposal.
 */
public data class MonarchAiEditProposalPart(
    public val id: String,
    public val target: String,
    public val diff: MonarchAiJSON,
    public val state: MonarchAiEditProposalState = MonarchAiEditProposalState.Proposed,
) : MonarchAiPart {
    override val type: String get() = "edit-proposal"
}
