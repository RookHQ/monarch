package com.rookhq.monarch.components.identity.dataexportrequest

import androidx.compose.runtime.Immutable

/**
 * Replacements for the built-in strings of a `MonarchDataExportRequest`.
 * Every `null` keeps the built-in string `monarch_dataexportrequest_<key>`.
 *
 * @property title the card title, "Download your data".
 * @property description the sentence under the title.
 * @property status the badge text of each status: "Not requested", "Requested", "Preparing", "Ready", "Expired",
 * "Failed".
 * @property request the button that requests the first export.
 * @property requestAgain the button that requests a new export after one expired or failed.
 * @property download the download button.
 * @property scopeLegend the label of the scope checklist, "Include".
 * @property scopeRequired the error when no scope option is ticked.
 * @property formatLabel the label of the format menu, "Format".
 * @property requested the status line while requested, given the formatted request date or `null`.
 * @property preparing the status line while preparing, given the formatted request date or `null`.
 * @property preparingProgress the TalkBack name of the progress indicator.
 * @property ready the status line when ready, given the formatted ready and expiry dates, each or both `null`.
 * @property expired the status line when expired, given the formatted expiry date or `null`.
 * @property failed the status line when failed.
 * @property requestFailed shown when the request throws without a message.
 * @property date formats an epoch-millisecond date. `null` uses the medium date and short time of the device.
 */
@Immutable
public class MonarchDataExportRequestLabels(
    public val title: String? = null,
    public val description: String? = null,
    public val status: ((MonarchDataExportStatus) -> String)? = null,
    public val request: String? = null,
    public val requestAgain: String? = null,
    public val download: String? = null,
    public val scopeLegend: String? = null,
    public val scopeRequired: String? = null,
    public val formatLabel: String? = null,
    public val requested: ((date: String?) -> String)? = null,
    public val preparing: ((date: String?) -> String)? = null,
    public val preparingProgress: String? = null,
    public val ready: ((readyAt: String?, expiresAt: String?) -> String)? = null,
    public val expired: ((expiresAt: String?) -> String)? = null,
    public val failed: String? = null,
    public val requestFailed: String? = null,
    public val date: ((millis: Long) -> String)? = null,
)
