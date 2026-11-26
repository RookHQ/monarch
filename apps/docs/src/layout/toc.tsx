import { useEffect, useState } from 'react';
import { href } from '../lib/router.js';

export interface TocEntry {
  id: string;
  label: string;
}

/**
 * "On this page": the page's h2 headings, read from the rendered article so
 * every page type gets one. The current section is marked while scrolling.
 */
export function Toc({ path, root }: { path: string; root: HTMLElement | null }) {
  const [entries, setEntries] = useState<TocEntry[]>([]);
  const [active, setActive] = useState<string | null>(null);

  useEffect(() => {
    if (!root) return;
    let key = '';
    const read = () => {
      const next = [...root.querySelectorAll<HTMLElement>('[data-toc]')].map((el) => ({
        id: el.id,
        label: el.dataset.toc || el.textContent || el.id,
      }));
      const nextKey = next.map((e) => `${e.id}:${e.label}`).join('|');
      if (nextKey === key) return;
      key = nextKey;
      setEntries(next);
    };
    read();
    // Sections can mount after lazy content resolves.
    const mo = new MutationObserver(read);
    mo.observe(root, { childList: true, subtree: true });
    return () => mo.disconnect();
  }, [root]);

  useEffect(() => {
    if (!root || entries.length === 0) return;
    const targets = entries
      .map((e) => document.getElementById(e.id))
      .filter((el): el is HTMLElement => Boolean(el));
    const io = new IntersectionObserver(
      (records) => {
        const visible = records.filter((r) => r.isIntersecting);
        if (visible[0]) setActive(visible[0].target.id);
      },
      { rootMargin: '0px 0px -70% 0px' },
    );
    for (const t of targets) io.observe(t);
    return () => io.disconnect();
  }, [root, entries]);

  if (entries.length < 2) return null;
  return (
    <nav className="docs-toc" aria-label="On this page">
      <p className="docs-toc__title">On this page</p>
      <ul className="docs-toc__list">
        {entries.map((e) => (
          <li key={e.id}>
            <a
              className="docs-toc__link"
              href={href(path, e.id)}
              aria-current={active === e.id ? 'location' : undefined}
            >
              {e.label}
            </a>
          </li>
        ))}
      </ul>
    </nav>
  );
}
