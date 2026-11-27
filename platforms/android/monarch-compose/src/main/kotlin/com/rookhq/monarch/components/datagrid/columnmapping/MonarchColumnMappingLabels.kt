package com.rookhq.monarch.components.datagrid.columnmapping

import androidx.compose.runtime.Immutable

/**
 * Replacements for the built-in strings of a `MonarchColumnMapping`. Every
 * `null` keeps the built-in string `monarch_columnmapping_<key>`.
 *
 * @property label the name of the list, "Column mapping".
 * @property fieldFor the label of a column's picker, given the column: "Field for E-mail".
 * @property skip the option that leaves a column out, "Don’t import".
 * @property required the mark of a required field in the picker, "required".
 * @property exactMatch the badge of an exact suggestion, "Matched".
 * @property suggested the badge of a close suggestion, "Suggested".
 * @property noSamples shown for a column without example values, "No values".
 * @property summary the count, given the mapped columns and all columns: "3 of 5 columns mapped".
 * @property missing the error, given the required fields that are not mapped: "Map a column to: Email.".
 * @property duplicate the error under a column, given the field and the other columns: "Email is also chosen
 * for Mail.".
 * @property useSuggestions the button that sets every picker to its suggestion.
 * @property skipAll the button that leaves every column out, "Don’t import any".
 * @property position the name of a column with a blank header, given its position from 1: "#3".
 * @property columnInFile the caption above a file column's name, "Column in file".
 * @property exampleValues the caption above a file column's example values, "Example values".
 * @property importAs the caption above a column's picker, "Import as".
 */
@Immutable
public class MonarchColumnMappingLabels(
    public val label: String? = null,
    public val fieldFor: ((String) -> String)? = null,
    public val skip: String? = null,
    public val required: String? = null,
    public val exactMatch: String? = null,
    public val suggested: String? = null,
    public val noSamples: String? = null,
    public val summary: ((Int, Int) -> String)? = null,
    public val missing: ((String) -> String)? = null,
    public val duplicate: ((String, String) -> String)? = null,
    public val useSuggestions: String? = null,
    public val skipAll: String? = null,
    public val position: ((Int) -> String)? = null,
    public val columnInFile: String? = null,
    public val exampleValues: String? = null,
    public val importAs: String? = null,
)
