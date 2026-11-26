package com.rookhq.monarch.components.forms.termsacceptance

import androidx.compose.runtime.Immutable

/**
 * What was accepted, reported by `MonarchTermsAcceptance` on every change so
 * the app can store proof of acceptance.
 *
 * @property versions the version of each document that has one, keyed by `MonarchTermsDocument.key`.
 * @property dateMillis when the box changed, in epoch milliseconds (UTC).
 */
@Immutable
public class MonarchTermsAcceptanceDetails(public val versions: Map<String, String>, public val dateMillis: Long)
