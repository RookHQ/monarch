/**
 * Shared rules for the shell's iframe hosts (ChatDock, EmbeddedAppFrame,
 * useEmbedReady). Framework-free so they are unit-tested in isolation.
 *
 * - Origins are compared exactly. An allow list only keeps entries that are a
 *   bare origin (`new URL(s).origin === s`), so `*`, paths, `null` and
 *   look-alikes are dropped and can never match.
 * - Nothing is ever posted to `*`.
 */

/** Exact origins from a list; anything that is not a bare http(s) origin is dropped. */
export function exactOrigins(list: readonly string[] | undefined | null): ReadonlySet<string> {
  const out = new Set<string>();
  for (const raw of list ?? []) {
    if (typeof raw !== 'string') continue;
    const s = raw.trim();
    try {
      const u = new URL(s);
      if ((u.protocol === 'https:' || u.protocol === 'http:') && u.origin === s) out.add(s);
    } catch {
      /* not an origin */
    }
  }
  return out;
}

/** The origin of an absolute http(s) URL, or null. Relative URLs resolve against `base`. */
export function originOf(url: string | undefined | null, base?: string): string | null {
  if (!url) return null;
  try {
    const u = base ? new URL(url, base) : new URL(url);
    if (u.protocol !== 'https:' && u.protocol !== 'http:') {
      // blob: URLs carry their creator's origin (used by same-origin previews).
      return u.protocol === 'blob:' && u.origin !== 'null' ? u.origin : null;
    }
    return u.origin;
  } catch {
    return null;
  }
}

/** The document's own URL, for resolving relative `src` values (server: none). */
export function documentBase(): string | undefined {
  return typeof window === 'undefined' ? undefined : window.location.href;
}

/** A plain (non-array) object, as every protocol message must be. */
export function isRecord(v: unknown): v is Record<string, unknown> {
  return !!v && typeof v === 'object' && !Array.isArray(v);
}

/** True when `data` has exactly these keys (no more, no fewer). */
export function hasExactKeys(data: Record<string, unknown>, keys: readonly string[]): boolean {
  const own = Object.keys(data);
  return (
    own.length === keys.length && keys.every((k) => Object.prototype.hasOwnProperty.call(data, k))
  );
}

/** A bounded non-empty string. */
export function boundedString(v: unknown, max = 512): v is string {
  return typeof v === 'string' && v.length > 0 && v.length <= max;
}

/** An absolute http(s) URL of at most 2048 characters. */
export function httpUrl(v: unknown): v is string {
  if (!boundedString(v, 2048)) return false;
  try {
    const u = new URL(v);
    return u.protocol === 'https:' || u.protocol === 'http:';
  } catch {
    return false;
  }
}

/** The sandbox tokens an embedded Rook page may be given. */
export type EmbedSandboxToken =
  | 'allow-downloads'
  | 'allow-forms'
  | 'allow-modals'
  | 'allow-popups'
  | 'allow-popups-to-escape-sandbox'
  | 'allow-same-origin'
  | 'allow-scripts'
  | 'allow-storage-access-by-user-activation';

const SANDBOX_TOKENS: ReadonlySet<string> = new Set<EmbedSandboxToken>([
  'allow-downloads',
  'allow-forms',
  'allow-modals',
  'allow-popups',
  'allow-popups-to-escape-sandbox',
  'allow-same-origin',
  'allow-scripts',
  'allow-storage-access-by-user-activation',
]);

/**
 * The `sandbox` attribute value: known tokens only, deduplicated. Tokens that
 * would let the frame navigate the host page (`allow-top-navigation*`) are not
 * in the type and are dropped if passed anyway.
 */
export function sandboxValue(tokens: readonly string[]): string {
  return [...new Set(tokens.filter((t) => SANDBOX_TOKENS.has(t)))].join(' ');
}

/** The `allow` (permissions policy) attribute value: feature names only. */
export function allowValue(features: readonly string[]): string | undefined {
  const clean = [...new Set(features.map((f) => f.trim()).filter((f) => /^[a-z][a-z-]*$/.test(f)))];
  return clean.length ? clean.join('; ') : undefined;
}
