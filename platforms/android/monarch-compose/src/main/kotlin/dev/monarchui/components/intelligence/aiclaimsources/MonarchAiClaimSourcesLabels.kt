package dev.monarchui.components.intelligence.aiclaimsources

import androidx.compose.runtime.Immutable
import dev.monarchui.components.intelligence.aiconversation.MonarchAiSourceKind

/**
 * Replacements for the built-in strings of a `MonarchAiClaimSources`. Every
 * `null` keeps the built-in string `monarch_aiclaimsources_<key>`.
 *
 * @property heading the heading, "Where this came from".
 * @property summary the line under the heading, given the counts: "5 claims: 3 supported, 1 partly, 1 without a
 * source".
 * @property filterLabel the spoken name of the filter, "Show claims".
 * @property all the filter that shows every claim, "All".
 * @property needsChecking the filter that shows the claims that are not fully supported, "Needs checking".
 * @property support the support level beside each claim, in plain words: "Partly supported".
 * @property note the sentence at the top of an open claim that is not fully supported.
 * @property passages the number of passages of a claim: "2 passages".
 * @property evidenceLabel the spoken name of a claim's passages, given the claim: "Passages for: …".
 * @property stance the stance of a passage: "Supports the claim".
 * @property unknownSource the title of a passage whose source is not in `sources`, "Unknown source".
 * @property published the publication date of a source, given the formatted date: "Published Mar 4, 2026".
 * @property kind the name of a source's kind: "Document".
 * @property openSource the button that opens a passage's source, "Open source".
 * @property fromSource tells apart the actions of each passage for TalkBack, given the source's title:
 * "from Pricing".
 * @property copyQuote the button that copies a passage, "Copy quote".
 * @property copied the copy button after a copy, "Copied".
 * @property copiedAnnouncement announced after a copy, "Quote copied to clipboard".
 * @property flag the button that reports a problem with a claim, "Report a problem".
 * @property verify the button that checks an unchecked claim again, "Check again".
 * @property empty shown when there are no claims at all.
 * @property noneNeedChecking shown when Needs checking has nothing to show.
 * @property noEvidence shown in an open claim that has no passages and no note.
 * @property showing announced when the filter changes, given the shown and total counts: "Showing 2 of 5 claims".
 * @property expanded the state of an open claim, "Expanded".
 * @property collapsed the state of a closed claim, "Collapsed".
 */
@Immutable
public class MonarchAiClaimSourcesLabels(
    public val heading: String? = null,
    public val summary: ((MonarchAiClaimCounts) -> String)? = null,
    public val filterLabel: String? = null,
    public val all: String? = null,
    public val needsChecking: String? = null,
    public val support: ((MonarchAiClaimSupport) -> String)? = null,
    public val note: ((MonarchAiClaimSupport) -> String)? = null,
    public val passages: ((count: Int) -> String)? = null,
    public val evidenceLabel: ((claim: String) -> String)? = null,
    public val stance: ((MonarchAiClaimEvidenceStance) -> String)? = null,
    public val unknownSource: String? = null,
    public val published: ((date: String) -> String)? = null,
    public val kind: ((MonarchAiSourceKind) -> String)? = null,
    public val openSource: String? = null,
    public val fromSource: ((title: String) -> String)? = null,
    public val copyQuote: String? = null,
    public val copied: String? = null,
    public val copiedAnnouncement: String? = null,
    public val flag: String? = null,
    public val verify: String? = null,
    public val empty: String? = null,
    public val noneNeedChecking: String? = null,
    public val noEvidence: String? = null,
    public val showing: ((shown: Int, total: Int) -> String)? = null,
    public val expanded: String? = null,
    public val collapsed: String? = null,
)
