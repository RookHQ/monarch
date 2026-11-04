/**
 * The client-side search index: one entry per page in the navigation
 * (foundations, every component and pattern, marketing, guides), built from
 * the docs index. CommandPalette's default filter ranks label matches above
 * description and keyword matches (export names, family).
 */
import { NAV } from '../../app/navigation.js';

export interface SearchEntry {
  id: string;
  path: string;
  label: string;
  description?: string;
  group: string;
  keywords?: readonly string[];
}

export function searchItems(): SearchEntry[] {
  const seen = new Set<string>();
  const out: SearchEntry[] = [];
  for (const group of NAV) {
    for (const item of group.items) {
      if (seen.has(item.path)) continue;
      seen.add(item.path);
      out.push({
        id: item.path,
        path: item.path,
        // "Overview" alone is ambiguous across groups.
        label: item.label === 'Overview' ? `${group.heading} overview` : item.label,
        group: group.heading,
        // Results show a short hint; the full description is searched as keywords,
        // so long summaries never squeeze the name.
        ...(item.hint ? { description: item.hint } : {}),
        keywords: [...(item.keywords ?? []), ...(item.description ? [item.description] : [])],
      });
    }
  }
  return out;
}
