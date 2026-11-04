/** Joins class names, skipping falsy values. Tiny on purpose: no dependency. */
export function cx(...parts: Array<string | false | null | undefined>): string | undefined {
  const out = parts.filter(Boolean).join(' ');
  return out === '' ? undefined : out;
}
