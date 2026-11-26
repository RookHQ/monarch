/**
 * The neutral accent: a quiet slate blue for teams outside Rook.
 *
 * `monarch.css` defaults to Rook Red, the Rook brand's primary colour. A team
 * that is not building a Rook product can load `css/products/neutral.css` and
 * set `data-mn-product="neutral"` to get an accent that carries no brand.
 * It is emitted like a product accent (same selectors, same contrast engine,
 * same tests) but it is not a registry product: it never appears in
 * `products`, the registry or the conformance product list.
 *
 * Teams with their own brand colour should use `deriveTheme(hex)` and
 * `brandThemeCss` instead (docs/guides/theming.md).
 */
import { hsl, type Hsl } from './color.js';

/** The `data-mn-product` value and file name (`css/products/neutral.css`). */
export const NEUTRAL_ACCENT_ID = 'neutral';

/**
 * Slate blue at 218°, low saturation, so it reads as "interface" rather than
 * "brand". Kept clear of the status colours and the AI purple (tested in
 * test/neutral.test.ts).
 */
export const neutralAccent: Hsl = hsl(218, 25, 38);
