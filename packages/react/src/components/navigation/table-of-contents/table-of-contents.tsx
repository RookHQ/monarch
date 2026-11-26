'use client';

import {
  forwardRef,
  useCallback,
  useEffect,
  useId,
  useMemo,
  useRef,
  useState,
  type HTMLAttributes,
  type MouseEvent,
  type ReactNode,
  type RefObject,
} from 'react';
import { cx } from '../../../internal/cx.js';

export interface TableOfContentsItem {
  /** The id of the heading (or section) in the page. */
  id: string;
  /** Link text, usually the heading text. */
  label: ReactNode;
  /** Heading level (2 for `h2`, 3 for `h3`…). Deeper levels nest under the one before. Default 2. */
  level?: number;
}

/**
 * Reads headings with an `id` inside `root` into TableOfContents items.
 * Default selector `h2[id], h3[id]`.
 */
export function collectHeadings(
  root: ParentNode,
  selector = 'h2[id], h3[id]',
): TableOfContentsItem[] {
  return Array.from(root.querySelectorAll<HTMLElement>(selector))
    .filter((el) => el.id)
    .map((el) => {
      const level = /^H([1-6])$/.exec(el.tagName)?.[1];
      return {
        id: el.id,
        label: (el.textContent ?? '').trim(),
        level: level ? Number(level) : 2,
      };
    });
}

export interface UseScrollSpyOptions {
  /**
   * Height in pixels of anything fixed at the top (a sticky header). A
   * section becomes current once its heading passes this line. Default 0.
   */
  offset?: number | undefined;
  /** IntersectionObserver `rootMargin`. Default `-{offset}px 0px -60% 0px`: the top 40% of the view. */
  rootMargin?: string | undefined;
  /** The scrolling element when it is not the window. */
  scrollRoot?: RefObject<HTMLElement | null> | undefined;
}

function useScrollSpyState(ids: readonly string[], options: UseScrollSpyOptions = {}) {
  const { offset = 0, rootMargin, scrollRoot } = options;
  const [active, setActive] = useState<string | null>(null);
  const key = ids.join('\u0000');

  useEffect(() => {
    if (typeof IntersectionObserver === 'undefined' || ids.length === 0) return;
    const elements = ids
      .map((id) => document.getElementById(id))
      .filter((el): el is HTMLElement => el !== null);
    if (elements.length === 0) return;
    const visible = new Map<string, boolean>();
    const root = scrollRoot?.current ?? null;
    const pick = () => {
      const first = elements.find((el) => visible.get(el.id));
      if (first) return first.id;
      // Nothing in the zone: the current section is the last heading above it.
      const top = (root?.getBoundingClientRect().top ?? 0) + offset + 1;
      let above: string | null = null;
      for (const el of elements) if (el.getBoundingClientRect().top < top) above = el.id;
      return above;
    };
    const observer = new IntersectionObserver(
      (entries) => {
        for (const e of entries) visible.set((e.target as HTMLElement).id, e.isIntersecting);
        const next = pick();
        if (next !== null) setActive(next);
      },
      { root, rootMargin: rootMargin ?? `-${offset}px 0px -60% 0px`, threshold: [0, 1] },
    );
    elements.forEach((el) => observer.observe(el));
    return () => observer.disconnect();
    // `key` stands for `ids`.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [key, offset, rootMargin, scrollRoot]);

  return [active, setActive] as const;
}

/**
 * The id of the section currently being read, from IntersectionObserver:
 * the first heading in the top part of the view, or else the last one
 * scrolled past. Null until known (and on the server).
 */
export function useScrollSpy(ids: readonly string[], options?: UseScrollSpyOptions): string | null {
  return useScrollSpyState(ids, options)[0];
}

export interface TableOfContentsMessages {
  /** Visible title and the navigation's name. Default "On this page". */
  title: string;
}

export const TABLE_OF_CONTENTS_MESSAGES: TableOfContentsMessages = { title: 'On this page' };

export interface TableOfContentsProps
  extends Omit<HTMLAttributes<HTMLElement>, 'children' | 'title'>, UseScrollSpyOptions {
  /** The entries. Pass them for server rendering; otherwise give `contentRef`. */
  items?: readonly TableOfContentsItem[] | undefined;
  /** Read the entries from headings inside this element (kept up to date as it changes). */
  contentRef?: RefObject<HTMLElement | null> | undefined;
  /** Which headings `contentRef` collects. Default `h2[id], h3[id]`. */
  selector?: string | undefined;
  /** Controlled current entry; otherwise scroll position decides. */
  activeId?: string | null | undefined;
  /** Called when the current entry changes, from scrolling or a click. */
  onActiveChange?: ((id: string) => void) | undefined;
  /** Write `#id` to the URL (with `history.replaceState`) when an entry is clicked. Default true. */
  updateHash?: boolean | undefined;
  /** Show the title above the list. Default true; when false it still names the navigation. */
  showTitle?: boolean | undefined;
  /** Visible and spoken strings, for translation. */
  messages?: Partial<TableOfContentsMessages> | undefined;
}

interface Node {
  item: TableOfContentsItem;
  children: Node[];
}

function buildTree(items: readonly TableOfContentsItem[]): Node[] {
  const roots: Node[] = [];
  const stack: Node[] = [];
  for (const item of items) {
    const node: Node = { item, children: [] };
    const level = item.level ?? 2;
    while (stack.length && ((stack[stack.length - 1] as Node).item.level ?? 2) >= level)
      stack.pop();
    const parent = stack[stack.length - 1];
    if (parent) parent.children.push(node);
    else roots.push(node);
    stack.push(node);
  }
  return roots;
}

function prefersReducedMotion(): boolean {
  return (
    typeof window !== 'undefined' &&
    typeof window.matchMedia === 'function' &&
    window.matchMedia('(prefers-reduced-motion: reduce)').matches
  );
}

/**
 * In-page navigation built from the page's headings: a list of links to
 * each section that highlights the one being read (scroll-spy with
 * IntersectionObserver, `aria-current="location"`). Clicking an entry
 * scrolls to its heading and moves focus there.
 */
export const TableOfContents = forwardRef<HTMLElement, TableOfContentsProps>(
  function TableOfContents(
    {
      items: itemsProp,
      contentRef,
      selector = 'h2[id], h3[id]',
      activeId: activeProp,
      onActiveChange,
      updateHash = true,
      showTitle = true,
      offset,
      rootMargin,
      scrollRoot,
      messages: messagesProp,
      className,
      'aria-label': ariaLabel,
      ...rest
    },
    ref,
  ) {
    const messages = { ...TABLE_OF_CONTENTS_MESSAGES, ...messagesProp };
    const titleId = `mn-toc-${useId().replace(/:/g, '')}`;
    const [collected, setCollected] = useState<TableOfContentsItem[]>([]);

    useEffect(() => {
      if (itemsProp || !contentRef?.current) return;
      const root = contentRef.current;
      const read = () =>
        setCollected((prev) => {
          const next = collectHeadings(root, selector);
          const same =
            prev.length === next.length &&
            prev.every(
              (p, i) =>
                p.id === next[i]?.id && p.label === next[i]?.label && p.level === next[i]?.level,
            );
          return same ? prev : next;
        });
      read();
      if (typeof MutationObserver === 'undefined') return;
      const observer = new MutationObserver(read);
      observer.observe(root, {
        childList: true,
        subtree: true,
        characterData: true,
        attributes: true,
        attributeFilter: ['id'],
      });
      return () => observer.disconnect();
    }, [itemsProp, contentRef, selector]);

    const items = itemsProp ?? collected;
    const ids = useMemo(() => items.map((i) => i.id), [items]);
    const [spied, setSpied] = useScrollSpyState(ids, { offset, rootMargin, scrollRoot });
    const active = activeProp !== undefined ? activeProp : spied;

    const lastReported = useRef<string | null>(null);
    useEffect(() => {
      if (spied && spied !== lastReported.current) {
        lastReported.current = spied;
        onActiveChange?.(spied);
      }
    }, [spied, onActiveChange]);

    const go = useCallback(
      (event: MouseEvent<HTMLAnchorElement>, id: string) => {
        // Leave modified clicks to the browser: open in a new tab or window, download.
        if (event.button !== 0 || event.metaKey || event.ctrlKey || event.shiftKey || event.altKey)
          return;
        const target = event.currentTarget.ownerDocument.getElementById(id);
        if (!target) return;
        event.preventDefault();
        target.scrollIntoView?.({
          behavior: prefersReducedMotion() ? 'auto' : 'smooth',
          block: 'start',
        });
        if (!target.hasAttribute('tabindex') && target.tabIndex < 0)
          target.setAttribute('tabindex', '-1');
        target.focus({ preventScroll: true });
        if (updateHash && typeof history !== 'undefined')
          history.replaceState(history.state, '', `#${encodeURIComponent(id)}`);
        setSpied(id);
      },
      [updateHash, setSpied],
    );

    const renderList = (nodes: Node[], depth: number) => (
      <ul className="mn-table-of-contents__list" data-depth={depth}>
        {nodes.map(({ item, children }) => {
          const current = item.id === active;
          return (
            <li key={item.id} className="mn-table-of-contents__entry">
              <a
                href={`#${encodeURIComponent(item.id)}`}
                className="mn-table-of-contents__link"
                data-depth={depth}
                data-active={current ? '' : undefined}
                aria-current={current ? 'location' : undefined}
                onClick={(e) => go(e, item.id)}
              >
                {item.label}
              </a>
              {children.length ? renderList(children, depth + 1) : null}
            </li>
          );
        })}
      </ul>
    );

    return (
      <nav
        ref={ref}
        {...rest}
        {...(ariaLabel
          ? { 'aria-label': ariaLabel }
          : showTitle
            ? { 'aria-labelledby': titleId }
            : { 'aria-label': messages.title })}
        className={cx('mn-table-of-contents', className)}
      >
        {showTitle ? (
          <p id={titleId} className="mn-table-of-contents__title">
            {messages.title}
          </p>
        ) : null}
        {items.length ? renderList(buildTree(items), 0) : null}
      </nav>
    );
  },
);
