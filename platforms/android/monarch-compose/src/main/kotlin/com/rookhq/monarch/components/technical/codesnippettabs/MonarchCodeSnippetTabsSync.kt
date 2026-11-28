package com.rookhq.monarch.components.technical.codesnippettabs

import android.content.Context
import androidx.compose.runtime.mutableStateMapOf
import androidx.core.content.edit

/**
 * The choices shared by every `MonarchCodeSnippetTabs` with the same sync key:
 * held as snapshot state, so every set on screen follows a change, and kept
 * in `SharedPreferences`, so the choice is restored on the next launch.
 */
internal object MonarchCodeSnippetTabsSync {
    /** The preferences file. */
    const val FILE: String = "monarch_code_snippet_tabs"

    /** The prefix of each stored key; the sync key follows it. */
    const val KEY_PREFIX: String = "monarch.codeSnippetTabs."

    private val choices = mutableStateMapOf<String, String>()

    /** The current choice for [key]: the one made in this process, or the stored one. */
    fun choice(context: Context, key: String): String? = choices[key] ?: stored(context, key)?.also {
        choices[key] = it
    }

    /** Makes [value] the choice for [key] everywhere and stores it. */
    fun choose(context: Context, key: String, value: String) {
        choices[key] = value
        context.applicationContext.getSharedPreferences(FILE, Context.MODE_PRIVATE).edit {
            putString(KEY_PREFIX + key, value)
        }
    }

    private fun stored(context: Context, key: String): String? =
        context.applicationContext.getSharedPreferences(FILE, Context.MODE_PRIVATE).getString(KEY_PREFIX + key, null)
}
