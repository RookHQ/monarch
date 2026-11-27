/**
 * Names and meanings of the native platforms, coverage types and statuses,
 * shared by the catalogue, the component pages and the landing pages. The
 * coverage meanings are those of docs/architecture/NATIVE-PARITY.md.
 */
import type { Coverage, Platform, Status } from '../../../plugins/native.js';

export type { Coverage, Platform, Status };
export const PLATFORMS: readonly Platform[] = ['android', 'ios'];
export const PLATFORM_NAMES: Readonly<Record<Platform, string>> = {
  android: 'Android',
  ios: 'iOS',
};

/**
 * The generated API references of the libraries, published by the docs deploy
 * (DocC at /ios/api/, Dokka at /android/api/). They are added to the site after
 * the build, so the link check skips them.
 */
export const API_REFERENCE = {
  ios: { path: '/ios/api/', tool: 'DocC' },
  android: { path: '/android/api/', tool: 'Dokka' },
} as const;

export const REPO_FILE = 'https://github.com/RookHQ/monarch/blob/main/';

export const COVERAGE: Readonly<Record<Coverage, { label: string; meaning: string }>> = {
  build: {
    label: 'Built by Monarch',
    meaning: 'Monarch builds a component: custom, or a wrapper with its own API.',
  },
  style: {
    label: 'Style on the platform control',
    meaning: 'Monarch ships a style or theme layer on the platform control, not a new component.',
  },
  native: {
    label: 'Platform control',
    meaning: 'The platform component or API is the answer; Monarch documents how to use it.',
  },
  compose: {
    label: 'Composition',
    meaning: 'A documented composition of other Monarch native components.',
  },
  'web-only': {
    label: 'Web only',
    meaning: 'Has no meaning in a native app; the entry gives the reason.',
  },
};
export const COVERAGE_ORDER: readonly Coverage[] = [
  'build',
  'style',
  'native',
  'compose',
  'web-only',
];

export const STATUS: Readonly<
  Record<
    Status,
    { label: string; meaning: string; tone: 'success' | 'warning' | 'neutral' | 'danger' }
  >
> = {
  available: {
    label: 'Available',
    meaning: 'Documented in the inventory and usable today, with compiled examples.',
    tone: 'success',
  },
  experimental: {
    label: 'Experimental',
    meaning: 'In the library behind an opt-in. It can change in any release.',
    tone: 'warning',
  },
  planned: {
    label: 'Planned',
    meaning: 'In the parity plan only. It does not exist on the platform yet.',
    tone: 'neutral',
  },
  deprecated: {
    label: 'Deprecated',
    meaning:
      'Still works and has a named replacement. It is removed no earlier than 90 days after the release that deprecates it.',
    tone: 'danger',
  },
};
/** An experimental domain composite in the patterns layer (ADR 0017). */
export const PATTERN = {
  label: 'Pattern (experimental)',
  meaning:
    'A domain composite in the patterns layer. It is experimental: it can change in any release, and it is not part of the stable component layer.',
} as const;

export const STATUS_ORDER: readonly Status[] = [
  'available',
  'experimental',
  'planned',
  'deprecated',
];
