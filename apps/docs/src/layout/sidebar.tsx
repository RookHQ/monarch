import { useEffect, useId, useRef, useState } from 'react';
import { href } from '../lib/router.js';
import { NAV, type NavGroup } from '../app/navigation.js';
import { ChevronDownIcon } from '../ui/icons.js';

function Group({
  group,
  path,
  defaultOpen,
}: {
  group: NavGroup;
  path: string;
  defaultOpen: boolean;
}) {
  const [open, setOpen] = useState(defaultOpen);
  const listId = useId();
  return (
    <div className="docs-sidenav__group" data-open={open || undefined}>
      <h2 className="docs-sidenav__heading">
        <button
          type="button"
          className="docs-sidenav__toggle"
          aria-expanded={open}
          aria-controls={listId}
          onClick={() => setOpen((o) => !o)}
        >
          {group.heading}
          <ChevronDownIcon className="docs-sidenav__chevron" />
        </button>
      </h2>
      <ul id={listId} className="docs-sidenav__list" hidden={!open}>
        {group.items.map((item) => (
          <li key={item.path}>
            <a
              className="docs-sidenav__link"
              href={href(item.path)}
              aria-current={path === item.path ? 'page' : undefined}
            >
              {item.label}
            </a>
          </li>
        ))}
      </ul>
    </div>
  );
}

/**
 * Grouped documentation navigation. In the wide layout every group starts
 * open; in the drawer (`collapsed`) only the current page's group does.
 */
export function Sidebar({ path, collapsed = false }: { path: string; collapsed?: boolean }) {
  const ref = useRef<HTMLElement>(null);
  // Keep the current page in view in a long list, scrolling only the list itself.
  useEffect(() => {
    const nav = ref.current;
    const box = nav?.closest<HTMLElement>('[data-scroll-box]');
    const active = nav?.querySelector<HTMLElement>('[aria-current="page"]');
    if (!box || !active) return;
    const top = active.getBoundingClientRect().top - box.getBoundingClientRect().top;
    if (top < 0 || top > box.clientHeight - active.offsetHeight)
      box.scrollTop += top - box.clientHeight / 3;
  }, [path]);
  return (
    <nav ref={ref} className="docs-sidenav" aria-label="Documentation">
      {NAV.map((g) => (
        <Group
          key={g.id}
          group={g}
          path={path}
          defaultOpen={!collapsed || g.items.some((i) => i.path === path)}
        />
      ))}
    </nav>
  );
}
