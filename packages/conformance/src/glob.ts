/**
 * Minimal glob matching for include/exclude and exception paths.
 * Supports `**`, `*`, `?` and `{a,b}`. Paths use forward slashes.
 */
const cache = new Map<string, RegExp>();

export function globToRegExp(glob: string): RegExp {
  const hit = cache.get(glob);
  if (hit) return hit;
  let re = '';
  let i = 0;
  const g = glob.replace(/^\.\//, '');
  while (i < g.length) {
    const ch = g[i] as string;
    if (ch === '*') {
      if (g[i + 1] === '*') {
        // `**/` matches zero or more directories; trailing `**` matches everything.
        if (g[i + 2] === '/') {
          re += '(?:.*/)?';
          i += 3;
        } else {
          re += '.*';
          i += 2;
        }
      } else {
        re += '[^/]*';
        i += 1;
      }
    } else if (ch === '?') {
      re += '[^/]';
      i += 1;
    } else if (ch === '{') {
      const end = g.indexOf('}', i);
      if (end === -1) {
        re += '\\{';
        i += 1;
      } else {
        re +=
          '(?:' +
          g
            .slice(i + 1, end)
            .split(',')
            .map(escape)
            .join('|') +
          ')';
        i = end + 1;
      }
    } else {
      re += escape(ch);
      i += 1;
    }
  }
  // A bare directory pattern ("src/legacy") also matches everything inside it.
  const out = new RegExp(`^${re}(?:/.*)?$`);
  cache.set(glob, out);
  return out;
}

function escape(s: string): string {
  return s.replace(/[.+^${}()|[\]\\]/g, '\\$&');
}

export function matchesAny(path: string, globs: readonly string[]): boolean {
  return globs.some((g) => globToRegExp(g).test(path));
}
