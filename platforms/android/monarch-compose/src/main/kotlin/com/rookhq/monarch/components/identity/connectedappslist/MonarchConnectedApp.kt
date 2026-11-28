package com.rookhq.monarch.components.identity.connectedappslist

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.painter.Painter

/**
 * An app the person signed in to with their account, and what it can see,
 * for `MonarchConnectedAppsList`.
 *
 * @property id a stable id of the grant.
 * @property name the app's name. Always shown as text.
 * @property logo the app's logo, loaded by the app. `null` shows the first letter of [name].
 * @property domain the app's domain, shown under the name, such as "notes.example.com".
 * @property verified whether the identity provider has reviewed the app.
 * @property suspended whether the app is suspended and cannot use its access.
 * @property workspace the workspace the grant belongs to, when grants are per workspace.
 * @property scopes what the app can see or do, in plain words, such as "See your email address".
 * @property connectedAtMillis when the person first connected the app, in epoch milliseconds (UTC).
 * @property lastUsedAtMillis when the app last used its access, in epoch milliseconds (UTC).
 */
@Immutable
public class MonarchConnectedApp(
    public val id: String,
    public val name: String,
    public val logo: Painter? = null,
    public val domain: String? = null,
    public val verified: Boolean = false,
    public val suspended: Boolean = false,
    public val workspace: String? = null,
    public val scopes: List<String> = emptyList(),
    public val connectedAtMillis: Long? = null,
    public val lastUsedAtMillis: Long? = null,
)
