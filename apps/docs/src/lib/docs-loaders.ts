import type { DocsModule } from './docs.js';

/** Every docs file's loader, keyed by component id. Imported lazily by `docs.ts`. */
const loaders = import.meta.glob<DocsModule>('../../../../packages/react/src/**/*.docs.tsx');

export const loaderById: Record<string, () => Promise<DocsModule>> = {};
for (const [file, load] of Object.entries(loaders)) {
  const id = /([^/]+)\.docs\.tsx$/.exec(file)?.[1];
  if (id) loaderById[id] = load;
}
