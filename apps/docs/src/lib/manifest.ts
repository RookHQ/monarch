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
}
export interface ExportDoc {
  name: string;
  kind: 'component' | 'hook' | 'utility';
  description: string;
  className?: string;
  clientOnly: boolean;
  props: PropDoc[];
}
