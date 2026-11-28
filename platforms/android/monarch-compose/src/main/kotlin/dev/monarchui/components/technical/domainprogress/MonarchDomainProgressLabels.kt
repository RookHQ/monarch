package dev.monarchui.components.technical.domainprogress

import androidx.compose.runtime.Immutable

/**
 * Replacements for the built-in strings of a `MonarchDomainProgress`. Every `null` uses the built-in string in
 * `strings_domainprogress.xml`.
 *
 * @property title the title of a step, from the step, its state and the stage: "DNS record found", "Ownership record
 * needed", "Waiting for your DNS record", "HTTPS certificate issued", "HTTPS certificate", "Live".
 * @property detail the line under a step, from the step, its state and the host: "Being issued now, usually 1 to 5
 * minutes."
 * @property state the spoken state of a step: "done", "in progress", "needs attention", "not started".
 * @property listLabel the spoken name of the steps, from the host: "Progress for book.example.com".
 * @property announcement announced when the active step or its state changes, from the position (from 1), the
 * count, the title and the spoken state: "Step 2 of 3: HTTPS certificate, in progress."
 * @property dnsSeen the DNS line, from the host and what was found: "Public DNS for book.example.com shows CNAME →
 * domains.example.net."
 * @property noRecord what public DNS shows when it shows nothing: "no record yet".
 * @property rechecking the "keeps checking" sentence, from how the user is told: "We keep checking automatically,
 * even after you close this page."
 * @property checked the last check line, from the relative time: "Checked 2 minutes ago."
 */
@Immutable
public class MonarchDomainProgressLabels(
    public val title: ((step: MonarchDomainStep, state: MonarchDomainStepState, stage: MonarchDomainStage) -> String)? =
        null,
    public val detail: ((step: MonarchDomainStep, state: MonarchDomainStepState, host: String) -> String)? = null,
    public val state: ((state: MonarchDomainStepState) -> String)? = null,
    public val listLabel: ((host: String) -> String)? = null,
    public val announcement: ((position: Int, count: Int, title: String, state: String) -> String)? = null,
    public val dnsSeen: ((host: String, seen: String) -> String)? = null,
    public val noRecord: String? = null,
    public val rechecking: ((notify: MonarchDomainNotify?) -> String)? = null,
    public val checked: ((time: String) -> String)? = null,
)
