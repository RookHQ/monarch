/**
 * A cached `Intl.Collator` that never throws.
 *
 * `new Intl.Collator('not a locale')` and `a.localeCompare(b, 'not a locale')`
 * throw a RangeError, which takes the whole tree down when the locale comes
 * from a prop or from data. `localeCompare` with a locale argument also builds
 * a collator on every call, which is slow inside a sort. An unknown locale
 * falls back to the runtime's default order.
 */
const collators = new Map<string, Intl.Collator>();

export function safeCollator(locale?: string | undefined): Intl.Collator {
  const key = locale ?? '';
  let collator = collators.get(key);
  if (!collator) {
    try {
      collator = new Intl.Collator(locale);
    } catch {
      collator = new Intl.Collator();
    }
    collators.set(key, collator);
  }
  return collator;
}
