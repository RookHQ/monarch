package com.rookhq.monarch.components.feedback.emptystate

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.rookhq.monarch.foundation.MonarchShapes
import com.rookhq.monarch.foundation.MonarchTypography
import com.rookhq.monarch.internal.MonarchSurfaceColors
import com.rookhq.monarch.theme.MonarchColors
import androidx.compose.ui.unit.sp
import com.rookhq.monarch.tokens.MonarchTokens

/** Component tokens of MonarchEmptyState and MonarchErrorState, built from primitives and roles only. */
internal object MonarchEmptyStateTokens {
    /** The widest an empty state is: 448dp, 28rem on the web. */
    val MaxWidth: Dp = (MonarchTokens.Space.space24 * 4 + MonarchTokens.Space.space16).dp

    val ActionGap: Dp = MonarchTokens.Space.space2.dp

    fun padding(size: MonarchEmptyStateSize): PaddingValues = when (size) {
        MonarchEmptyStateSize.Sm ->
            PaddingValues(horizontal = MonarchTokens.Space.space3.dp, vertical = MonarchTokens.Space.space5.dp)
        MonarchEmptyStateSize.Md ->
            PaddingValues(horizontal = MonarchTokens.Space.space4.dp, vertical = MonarchTokens.Space.space8.dp)
        MonarchEmptyStateSize.Lg ->
            PaddingValues(horizontal = MonarchTokens.Space.space4.dp, vertical = MonarchTokens.Space.space20.dp)
    }

    fun gap(size: MonarchEmptyStateSize): Dp = when (size) {
        MonarchEmptyStateSize.Sm -> MonarchTokens.Space.space1.dp
        MonarchEmptyStateSize.Md, MonarchEmptyStateSize.Lg -> MonarchTokens.Space.space2.dp
    }

    /** The side of the icon tile of a full error page: 56dp, as the web error page draws it. */
    val PageTileSize: Dp = (MonarchTokens.Space.space12 + MonarchTokens.Space.space2).dp

    /** The side of the icon tile: 32dp when small, 40dp otherwise, as on the web. */
    fun tileSize(size: MonarchEmptyStateSize): Dp = when (size) {
        MonarchEmptyStateSize.Sm -> MonarchTokens.Space.space8.dp
        MonarchEmptyStateSize.Md, MonarchEmptyStateSize.Lg -> MonarchTokens.Space.space10.dp
    }

    fun tileShape(size: MonarchEmptyStateSize, shapes: MonarchShapes): Shape = when (size) {
        MonarchEmptyStateSize.Sm -> shapes.md
        MonarchEmptyStateSize.Md, MonarchEmptyStateSize.Lg -> shapes.lg
    }

    fun titleStyle(size: MonarchEmptyStateSize, typography: MonarchTypography): TextStyle = when (size) {
        MonarchEmptyStateSize.Sm -> typography.label
        MonarchEmptyStateSize.Md -> typography.bodyStrong
        MonarchEmptyStateSize.Lg -> typography.subheading
    }

    fun messageStyle(size: MonarchEmptyStateSize, typography: MonarchTypography): TextStyle = when (size) {
        MonarchEmptyStateSize.Sm -> typography.caption
        MonarchEmptyStateSize.Md, MonarchEmptyStateSize.Lg -> typography.body.copy(
            fontSize = MonarchTokens.FontSize.sm.sp,
            lineHeight = (MonarchTokens.FontSize.sm * MonarchTokens.LineHeight.normal).sp,
        )
    }

    /** The icon tile: the accent tint, or the success tint for an all-clear state. */
    fun tileColors(kind: MonarchEmptyStateKind, colors: MonarchColors): MonarchSurfaceColors = when (kind) {
        MonarchEmptyStateKind.FirstRun, MonarchEmptyStateKind.Filtered ->
            MonarchSurfaceColors(colors.accentSubtle, colors.accentStrong, null)
        MonarchEmptyStateKind.Success -> MonarchSurfaceColors(colors.successSubtle, colors.successStrong, null)
    }
}
