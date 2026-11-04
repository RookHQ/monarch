/// <reference types="vite/client" />
/** A guide rendered at build time (plugins/guides.ts). */
declare module '*.md?guide' {
  const guide: { title: string; lead: string | null; html: string };
  export default guide;
}

/** Built by plugins/monarch-docs.ts from the docs files, the registry and the react package. */
declare module 'virtual:monarch-docs-index' {
  export interface DocsPageMeta {
    id: string;
    title: string;
    summary: string;
    /** `components/actions/button` */
    folder: string;
    layer: 'components' | 'patterns';
    family: string;
    /** Package entry the exports come from: `.`, `./data`, `./marketing` … */
    entry: string;
    /** Every value export of the folder, primary first. */
    exports: string[];
    exampleTitles: string[];
  }
  export interface RegistryRef {
    id: string;
    name: string;
    status: string;
  }
  export const reactVersion: string;
  export const pages: DocsPageMeta[];
  export const registry: RegistryRef[];
}
