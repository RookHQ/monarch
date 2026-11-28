---
'@rookcom/monarch-tokens': patch
'@rookcom/monarch-conformance': patch
---

Contrast guarantees now hold for the colours the CSS ships. `deriveIntent`, `resolveTheme` and `deriveTheme` measure every colour at the precision `toCss` prints (one decimal per HSL channel), and the tests check the emitted stylesheet strings. Before, rounding could drop a pair just below its target: the high-contrast light success fill with white text was 6.97:1 instead of 7:1. A few derived colours move by one or two 8-bit steps (success fills in high-contrast light, info fills and text in high-contrast dark, `info-strong-inverse` in light). New export: `emitted(color)`.

Dark theme: `--mn-color-bg-subtle` is now `neutral-850` (it was the same colour as `--mn-color-bg-surface`), and `--mn-color-border-default` is `hsl(220 14% 22.5%)` (it was the same colour as `--mn-color-bg-muted`), so subtle panels and default borders stay visible on those fills. As in the light theme, `border-default` is decorative; `border-input` carries the 3:1 boundary for controls (WCAG 1.4.11).

Native token files: `MonarchThemeTokens.withAI(_:)` (iOS) and `withAi(ai:)` (Android) apply a custom AI colour, such as `MonarchBrandTheme.aiAccent`; new `Width` and `Layout` length groups (content widths and the 42rem reading measure, in points and dp). The conformance package's bundled colour list follows the new `info-strong-inverse` value.

MONARCH-COMPAT: the dark theme's `bg-subtle` and `border-default` change (the fill and the border are no longer the same colour as `bg-surface` and `bg-muted`), as do a few high-contrast and `info` colours by one or two 8-bit steps. Update visual baselines of screens that use them; no code change is needed. `emitted` is a new export, so nothing else changes in the API.
