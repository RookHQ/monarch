/**
 * Shapes of `@rookcom/monarch-react/manifest.json` entries. The site never
 * imports the whole manifest: each docs chunk carries the entries for its own
 * exports (`apiEntries`, added by plugins/monarch-docs.ts).
 */
export interface PropDoc {
  name: string;
  type: string;
  required: boolean;
  description: string;
  /** Default value as source text, when the component declares one. */
  default?: string;
  /** Literal values of a string or number union, expanded from its alias. */
  values?: (string | number)[];
}
export interface ExportDoc {
  name: string;
  kind: 'component' | 'hook' | 'utility';
  description: string;
  className?: string;
  /** @deprecated Means the file has 'use client'; see `usesClient`. */
  clientOnly: boolean;
  /** The source file has a 'use client' directive. Server-render safe either way. */
  usesClient?: boolean;
  /** `general` for everyone; `rook` for exports tied to Rook products. */
  scope?: 'general' | 'rook';
  /** The exact import line. */
  import?: string;
  /** Stylesheet imports the export needs. */
  css?: string[];
  componentCss?: string[];
  /** Docs page URL. */
  docs?: string;
  props: PropDoc[];
}
