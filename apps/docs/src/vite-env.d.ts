/// <reference types="vite/client" />
/** A guide rendered at build time (plugins/guides.ts). */
declare module '*.md?guide' {
  const guide: { title: string; lead: string | null; html: string };
  export default guide;
}

/**
 * Built by plugins/monarch-docs.ts from the docs files and the react package:
 * the index the shell (sidebar, routes, titles) needs. Part of the entry chunk.
 */
declare module 'virtual:monarch-docs-index' {
  export interface DocsIndexEntry {
    id: string;
    title: string;
    layer: 'components' | 'patterns';
    family: string;
    /** Package entry the exports come from: `./data`, `./marketing` … Absent for `.`. */
    entry?: string;
  }
  export const reactVersion: string;
  /** Examples across every docs file. */
  export const exampleCount: number;
  /** Catalog components complete (registry/catalog.json), and the catalog size. */
  export const catalogComplete: number;
  export const catalogTotal: number;
  export const pages: DocsIndexEntry[];
}

/**
 * Built by plugins/monarch-docs.ts: per-page details and registry entries.
 * Loaded lazily (`loadDetails()` in src/lib/docs.ts), never by the shell.
 */
declare module 'virtual:monarch-docs-details' {
  export interface DocsPageDetails {
    summary: string;
    /** Every value export of the folder, primary first. */
    exports: string[];
    exampleCount: number;
  }
  export interface RegistryRef {
    id: string;
    name: string;
    status: string;
  }
  export const details: Record<string, DocsPageDetails>;
  export const registry: RegistryRef[];
}

/** The same details for just the listed pages: `virtual:monarch-docs-details?pages=a,b`. */
declare module 'virtual:monarch-docs-details?*' {
  import type { DocsPageDetails } from 'virtual:monarch-docs-details';
  export const details: Record<string, DocsPageDetails>;
}

/** Analytics IDs from the build environment; empty strings when unset. */
declare const __DOCS_GTM_ID__: string;
declare const __DOCS_CLARITY_ID__: string;

// The native modules take their types from plugins/native.ts. An ambient module
// declaration cannot import a relative module, so they use `import()` types.
/* eslint-disable @typescript-eslint/consistent-type-imports */

/**
 * Built by plugins/native.ts from `platforms/android` and `platforms/ios`.
 * The native guides that exist in the repository; part of the entry chunk.
 */
declare module 'virtual:monarch-native-nav' {
  export const guides: string[];
}

/** Versions, requirements and counts per platform, read at build time. */
declare module 'virtual:monarch-native-facts' {
  const facts: import('../plugins/native.js').NativeFacts;
  export default facts;
}

/** One platform's catalogue: a trimmed row per inventory entry and per planned component. */
declare module 'virtual:monarch-native-catalogue/*' {
  const rows: import('../plugins/native.js').CatalogueRow[];
  export default rows;
}

/** Both platforms' documentation of one web component id; one chunk per id. */
declare module 'virtual:monarch-native-component/*' {
  const docs: import('../plugins/native.js').NativeComponentDocs;
  export default docs;
}

/** README sections, foundation entries and examples for a platform's landing page. */
declare module 'virtual:monarch-native-platform/*' {
  const platform: {
    /** Rendered README sections, by heading slug. A section the README lacks is absent. */
    sections: Record<string, string>;
    foundations: {
      id: string;
      title: string;
      docs: import('../plugins/native.js').NativePlatformDocs;
    }[];
    examples: {
      id: string;
      title: string;
      name: string | null;
      example: import('../plugins/native.js').InventoryEntry['examples'][number];
    }[];
    previews: { id: string; title: string; previews: import('../plugins/native.js').Previews }[];
    /** What the platform's landing page reads (plugins/native.ts). */
    landing: {
      groups: { group: string; count: number }[];
      install: { lang: string; code: string } | null;
      code: {
        id: string;
        title: string;
        example: import('../plugins/native.js').InventoryEntry['examples'][number];
      }[];
    };
  };
  export default platform;
}

/** A native guide rendered at build time, or null when its file does not exist. */
declare module 'virtual:monarch-native-guide/*' {
  const guide: { title: string; lead: string | null; html: string } | null;
  export default guide;
}

/** A platform's CHANGELOG.md rendered at build time, or null when it does not exist. */
declare module 'virtual:monarch-native-changelog/*' {
  const changelog: { title: string; lead: string | null; html: string } | null;
  export default changelog;
}
