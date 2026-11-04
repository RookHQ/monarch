import manifest from '@rookcom/monarch-react/manifest.json';

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

export const version: string = manifest.version;
export const exportsByName: Record<string, ExportDoc> = Object.fromEntries(
  (manifest.components as ExportDoc[]).map((c) => [c.name, c]),
);
