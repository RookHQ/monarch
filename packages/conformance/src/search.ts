/**
 * `monarch search <query>`: ranks manifest exports against a query. General
 * components rank above Rook-specific ones, components above hooks and
 * utilities, and names above descriptions.
 */
import { scopeOf, summaryOf, type Manifest, type ManifestExport } from './manifest.js';

export interface SearchHit {
  readonly export: ManifestExport;
  readonly score: number;
}

export interface SearchOptions {
  readonly limit?: number;
  /** Only components (not hooks or utilities). */
  readonly componentsOnly?: boolean;
  /** Leave Rook-specific exports out entirely (they are ranked last otherwise). */
  readonly generalOnly?: boolean;
}

const words = (s: string): string[] =>
  s
    .replace(/([a-z0-9])([A-Z])/g, '$1 $2')
    .toLowerCase()
    .split(/[^a-z0-9]+/)
    .filter(Boolean);

/** Score of one export for the query words; 0 means no match. */
export function scoreExport(e: ManifestExport, query: string): number {
  const q = words(query);
  if (q.length === 0) return 0;
  const name = e.name.toLowerCase();
  const nameWords = new Set(words(e.name));
  const desc = e.description.toLowerCase();
  const joined = q.join('');
  let score = 0;
  if (name === joined) score += 100;
  else if (name.startsWith(joined)) score += 60;
  else if (name.includes(joined)) score += 40;
  for (const w of q) {
    if (nameWords.has(w)) score += 20;
    else if (name.includes(w)) score += 10;
    else if (desc.includes(w)) score += 4;
    else return 0; // every word must match somewhere
  }
  if (score === 0) return 0;
  if (e.kind === 'component') score += 5;
  if (scopeOf(e) === 'rook') score -= 50;
  // Shorter names are the more basic component (Select before SelectItem).
  score -= Math.min(e.name.length, 40) / 10;
  return Math.max(score, 0.1);
}

export function search(manifest: Manifest, query: string, opts: SearchOptions = {}): SearchHit[] {
  const hits: SearchHit[] = [];
  for (const e of manifest.components) {
    if (opts.componentsOnly && e.kind !== 'component') continue;
    if (opts.generalOnly && scopeOf(e) === 'rook') continue;
    const score = scoreExport(e, query);
    if (score > 0) hits.push({ export: e, score });
  }
  hits.sort((a, b) => b.score - a.score || a.export.name.localeCompare(b.export.name));
  return hits.slice(0, opts.limit ?? 20);
}

/** One line per hit: name, entry, kind, scope and the first sentence. */
export function searchText(hits: readonly SearchHit[]): string {
  if (hits.length === 0) return 'No matching exports.\n';
  const width = Math.max(...hits.map((h) => h.export.name.length));
  return (
    hits
      .map((h) => {
        const e = h.export;
        const tags = [e.entry === '.' ? 'core' : e.entry.slice(2), e.kind];
        if (scopeOf(e) === 'rook') tags.push('Rook-specific');
        return `${e.name.padEnd(width)}  ${tags.join(', ').padEnd(28)}  ${summaryOf(e.description)}`;
      })
      .join('\n') + '\n'
  );
}
