export * from './color.js';
export * as primitives from './primitives.js';
export { brand, defaultAccent } from './primitives.js';
export { NEUTRAL_ACCENT_ID, neutralAccent } from './neutral.js';
export {
  THEME_IDS,
  INTENT_IDS,
  themes,
  deriveIntent,
  resolveTheme,
  type ThemeId,
  type IntentId,
  type IntentRole,
  type IntentScale,
  type IntentReport,
  type ThemeDefinition,
  type ResolvedTheme,
  type NeutralRoles,
} from './themes.js';
export {
  buildMonarchCss,
  buildProductCss,
  buildCompatCss,
  compatPrimary,
  themeSelectors,
  productSelectors,
  varName,
  NEUTRAL_ROLES,
  INTENT_ROLES,
  DATA_PALETTE_ORDER,
  dataPalette,
} from './css.js';
export * from './appearance.js';
export { products, productIds, type ProductId, type ProductAccent } from './generated/products.js';
export {
  deriveTheme,
  brandThemeCss,
  parseHexColor,
  AI_MIN_DISTANCE,
  type BrandAiCheck,
  type BrandAiNeighbour,
  type BrandTheme,
  type BrandThemeMode,
  type BrandThemeVars,
  type BrandThemeCssOptions,
  type DeriveThemeOptions,
} from './brand.js';
