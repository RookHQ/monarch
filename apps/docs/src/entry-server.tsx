/**
 * Server entry for the prerender step (scripts/prerender.mjs): renders one
 * route of the site to HTML, waiting for its lazy page chunk and docs file.
 * A component page's headline example is part of the HTML (examples are
 * deterministic: the visual baselines depend on it); the other previews mount
 * in the browser near the viewport (features/examples/use-near-viewport.ts).
 */
import { PassThrough } from 'node:stream';
import { StrictMode } from 'react';
import { prerenderToNodeStream } from 'react-dom/static';
import { App } from './app/app.js';
import { NAV } from './app/navigation.js';
import { ROUTES, resolveRoute } from './app/routes.js';
import { ROUTE_ALIASES, pageById } from './lib/docs.js';

export { ROUTES, ROUTE_ALIASES };

/** The path the not-found page is rendered for (written to 404.html). */
export const NOT_FOUND_PATH = '/404';

export interface RenderedRoute {
  html: string;
  title: string;
  /** One line about the page from the site's own data, if there is one. */
  description: string | null;
  /** Build-manifest keys of the modules the page renders, for preload links. */
  modules: string[];
}

function describe(path: string): string | null {
  if (path.startsWith('/components/')) return pageById[path.slice(12)]?.summary ?? null;
  for (const group of NAV)
    for (const item of group.items) if (item.path === path) return item.description ?? null;
  return null;
}

export async function render(path: string): Promise<RenderedRoute> {
  const route = resolveRoute(path);
  const { prelude } = await prerenderToNodeStream(
    <StrictMode>
      <App initial={{ path, anchor: null }} />
    </StrictMode>,
  );
  const chunks: Buffer[] = [];
  const sink = prelude.pipe(new PassThrough());
  for await (const chunk of sink) chunks.push(Buffer.from(chunk as Uint8Array));
  const meta = route.docsId ? pageById[route.docsId] : undefined;
  return {
    html: Buffer.concat(chunks).toString('utf8'),
    title: route.title,
    description: describe(path),
    modules: [
      ...route.files.map((f) => `src/${f.replace(/^\.\.\//, '')}`),
      ...(meta ? [`../../packages/react/src/${meta.folder}/${meta.id}.docs.tsx`] : []),
    ],
  };
}
