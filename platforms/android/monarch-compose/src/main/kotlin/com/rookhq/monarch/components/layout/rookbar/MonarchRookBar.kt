package com.rookhq.monarch.components.layout.rookbar

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import com.rookhq.monarch.R
import com.rookhq.monarch.accessibility.monarchFocusRing
import com.rookhq.monarch.accessibility.monarchMinimumTouchTarget
import com.rookhq.monarch.components.actions.button.MonarchButton
import com.rookhq.monarch.components.actions.button.MonarchButtonVariant
import com.rookhq.monarch.components.actions.iconbutton.MonarchIconButton
import com.rookhq.monarch.components.layout.divider.MonarchDivider
import com.rookhq.monarch.components.navigation.megamenu.MonarchMegaMenu
import com.rookhq.monarch.components.navigation.megamenu.MonarchMegaMenuItem
import com.rookhq.monarch.components.overlays.bottomsheet.MonarchBottomSheet
import com.rookhq.monarch.components.typography.text.MonarchText
import com.rookhq.monarch.foundation.MonarchTextStyle
import com.rookhq.monarch.foundation.icon.MonarchIcon
import com.rookhq.monarch.internal.MonarchGlyphs
import com.rookhq.monarch.internal.MonarchShapeDefaults
import com.rookhq.monarch.internal.monarchString
import com.rookhq.monarch.theme.MonarchTheme

/**
 * The Rook company bar for the public, signed-out screens of a Rook app: the app's wordmark, the account buttons
 * and a menu button that opens the company menu (products, services, pricing, news) in a full-height bottom sheet.
 *
 * Rook-scoped: it carries the Rook company structure, so apps outside Rook use [MonarchMegaMenu] in their own
 * navigation instead. Built from Compose layouts, `MonarchIconButton`, `MonarchButton`, `MonarchBottomSheet` and
 * `MonarchMegaMenu`. The data (apps, links, posts) comes from the app; the bar never fetches. "Log in" and
 * "Sign up" sit in the bar when it is wide enough and the font scale allows, and are always in the sheet. The bar
 * is a traversal group named "Rook".
 *
 * @param brand the app's name beside its mark, such as "Northwind".
 * @param items the company menu: entries with sections and a featured card, or direct links.
 * @param menuOpen whether the menu sheet is shown.
 * @param onMenuOpenChange called with `true` by the menu button and `false` when the sheet is dismissed.
 * @param onNavigate called with the link the user taps in the menu; the sheet closes.
 * @param modifier the modifier of the bar.
 * @param account the account controls; `null` shows none.
 * @param onAccountAction called with the account action the user taps.
 * @param brandMark the app's mark before the name, such as its icon.
 * @param onBrandClick called when the user taps the brand, normally to go home; `null` makes it plain text.
 * @param labels replacements for the built-in strings.
 */
@Composable
public fun MonarchRookBar(
    brand: String,
    items: List<MonarchMegaMenuItem>,
    menuOpen: Boolean,
    onMenuOpenChange: (Boolean) -> Unit,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier,
    account: MonarchRookBarAccount? = null,
    onAccountAction: (MonarchRookBarAction) -> Unit = {},
    brandMark: (@Composable () -> Unit)? = null,
    onBrandClick: (() -> Unit)? = null,
    labels: MonarchRookBarLabels = MonarchRookBarLabels(),
) {
    val colors = MonarchTheme.colors
    val nav = monarchString(labels.nav, R.string.monarch_rookbar_nav)
    val fontScale = LocalDensity.current.fontScale
    Column(modifier.fillMaxWidth().background(MonarchRookBarTokens.fill(colors))) {
        BoxWithConstraints(
            Modifier
                .fillMaxWidth()
                .semantics {
                    isTraversalGroup = true
                    paneTitle = nav
                },
        ) {
            val fits = fontScale <= MonarchRookBarTokens.INLINE_ACTIONS_MAX_FONT_SCALE
            val inlineSignIn = fits && maxWidth >= MonarchRookBarTokens.InlineActionsWidth
            val inlineSignUp = fits && maxWidth >= MonarchRookBarTokens.InlineSignUpWidth
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = MonarchRookBarTokens.Height)
                    .padding(start = MonarchRookBarTokens.PaddingStart, end = MonarchRookBarTokens.PaddingEnd),
                horizontalArrangement = Arrangement.spacedBy(MonarchRookBarTokens.ActionGap),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Brand(brand, brandMark, onBrandClick, Modifier.weight(1f))
                if (inlineSignIn && account == MonarchRookBarAccount.SignedOut) {
                    MonarchButton(
                        text = monarchString(labels.signIn, R.string.monarch_rookbar_sign_in),
                        onClick = { onAccountAction(MonarchRookBarAction.SignIn) },
                        variant = MonarchButtonVariant.Ghost,
                    )
                }
                if (inlineSignUp && account == MonarchRookBarAccount.SignedOut) {
                    SignUpButton(monarchString(labels.signUp, R.string.monarch_rookbar_sign_up)) {
                        onAccountAction(MonarchRookBarAction.SignUp)
                    }
                }
                MonarchIconButton(
                    label = monarchString(labels.openMenu, R.string.monarch_rookbar_open_menu),
                    onClick = { onMenuOpenChange(true) },
                ) {
                    MonarchIcon(imageVector = MonarchGlyphs.Menu, contentDescription = null, tint = colors.textDefault)
                }
            }
        }
        MonarchDivider()
    }
    if (menuOpen) {
        MenuSheet(nav, items, account, labels, onMenuOpenChange, onNavigate, onAccountAction)
    }
}

@Composable
private fun Brand(brand: String, mark: (@Composable () -> Unit)?, onClick: (() -> Unit)?, modifier: Modifier) {
    val colors = MonarchTheme.colors
    val interaction = if (onClick != null) {
        Modifier
            .monarchMinimumTouchTarget()
            .monarchFocusRing(MonarchShapeDefaults.Control)
            .clickable(role = Role.Button, onClick = onClick)
    } else {
        Modifier
    }
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        Row(
            modifier = interaction.semantics(mergeDescendants = true) {},
            horizontalArrangement = Arrangement.spacedBy(MonarchRookBarTokens.BrandGap),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CompositionLocalProvider(LocalContentColor provides colors.textDefault) { mark?.invoke() }
            MonarchText(
                text = brand,
                style = MonarchTextStyle.Subheading,
                color = colors.textDefault,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                heading = false,
            )
        }
    }
}

@Composable
private fun SignUpButton(text: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    MonarchButton(
        text = text,
        onClick = onClick,
        modifier = modifier,
        variant = MonarchRookBarTokens.SignUpVariant,
        shape = MonarchTheme.shapes.full,
    )
}

@Composable
private fun MenuSheet(
    nav: String,
    items: List<MonarchMegaMenuItem>,
    account: MonarchRookBarAccount?,
    labels: MonarchRookBarLabels,
    onMenuOpenChange: (Boolean) -> Unit,
    onNavigate: (String) -> Unit,
    onAccountAction: (MonarchRookBarAction) -> Unit,
) {
    var expanded by rememberSaveable { mutableStateOf<String?>(null) }
    MonarchBottomSheet(
        onDismissRequest = { onMenuOpenChange(false) },
        modifier = Modifier.semantics { paneTitle = nav },
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(MonarchRookBarTokens.SheetGap),
        ) {
            MonarchMegaMenu(
                items = items,
                expandedItem = expanded,
                onExpandedItemChange = { expanded = it },
                onNavigate = { link ->
                    onMenuOpenChange(false)
                    onNavigate(link)
                },
            )
            if (account != null) {
                MonarchDivider()
                AccountActions(account, labels) { action ->
                    onMenuOpenChange(false)
                    onAccountAction(action)
                }
            }
        }
    }
}

@Composable
private fun AccountActions(
    account: MonarchRookBarAccount,
    labels: MonarchRookBarLabels,
    onAction: (MonarchRookBarAction) -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(bottom = MonarchRookBarTokens.SheetGap),
        verticalArrangement = Arrangement.spacedBy(MonarchRookBarTokens.BrandGap),
    ) {
        when (account) {
            MonarchRookBarAccount.SignedOut -> {
                MonarchButton(
                    text = monarchString(labels.signIn, R.string.monarch_rookbar_sign_in),
                    onClick = { onAction(MonarchRookBarAction.SignIn) },
                    modifier = Modifier.fillMaxWidth(),
                )
                SignUpButton(
                    text = monarchString(labels.signUp, R.string.monarch_rookbar_sign_up),
                    modifier = Modifier.fillMaxWidth(),
                ) { onAction(MonarchRookBarAction.SignUp) }
            }
            is MonarchRookBarAccount.SignedIn -> {
                MonarchText(text = account.name, style = MonarchTextStyle.BodyStrong)
                MonarchButton(
                    text = monarchString(labels.dashboard, R.string.monarch_rookbar_dashboard),
                    onClick = { onAction(MonarchRookBarAction.Dashboard) },
                    modifier = Modifier.fillMaxWidth(),
                    variant = MonarchButtonVariant.Primary,
                )
                MonarchButton(
                    text = monarchString(labels.account, R.string.monarch_rookbar_account),
                    onClick = { onAction(MonarchRookBarAction.Account) },
                    modifier = Modifier.fillMaxWidth(),
                )
                MonarchButton(
                    text = monarchString(labels.signOut, R.string.monarch_rookbar_sign_out),
                    onClick = { onAction(MonarchRookBarAction.SignOut) },
                    modifier = Modifier.fillMaxWidth(),
                    variant = MonarchButtonVariant.Ghost,
                )
            }
        }
    }
}
