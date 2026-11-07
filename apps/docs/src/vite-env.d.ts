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
