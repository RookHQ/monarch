'use client';

import {
  forwardRef,
  useEffect,
  useId,
  useMemo,
  useState,
  type HTMLAttributes,
  type MouseEvent,
} from 'react';
import type { Editor } from '@tiptap/core';
import type { Node as PMNode } from '@tiptap/pm/model';
import { cx } from '../../../internal/cx.js';

/** One heading in the outline. */
export interface PageOutlineHeading {
  /** The heading element's DOM id (static pages), or a stable key (editor). */
  id: string;
  /** 1–6. */
  level: number;
  text: string;
  /** Document position of the heading (editor only). */
  pos?: number | undefined;
}

/** Word count and reading time of a document. */
export interface PageOutlineStats {
  words: number;
  minutes: number;
}

const WORDS_PER_MINUTE = 220;

const slug = (text: string) =>
  text
    .toLowerCase()
    .normalize('NFKD')
    .replace(/[^\p{L}\p{N}]+/gu, '-')
    .replace(/^-|-$/g, '');

/**
 * Walks a ProseMirror document for headings up to `maxLevel`. Empty headings
 * are skipped, so one being typed never flashes in as a blank row. Ids are
 * slugs of the text, made unique with a counter.
 */
export function collectOutline(doc: PMNode, maxLevel = 3): PageOutlineHeading[] {
  const out: PageOutlineHeading[] = [];
  const seen = new Map<string, number>();
  const used = new Set<string>();
  doc.descendants((node, pos) => {
    if (node.type.name !== 'heading') return true;
    const level = Number(node.attrs.level) || 1;
    const text = node.textContent.trim();
    if (level <= maxLevel && text) {
      const base = slug(text) || 'section';
      // "Intro", "Intro", "Intro 2" must not give two headings the id `intro-2`:
      // count on until the id is free.
      let n = seen.get(base) ?? 0;
      let id = n ? `${base}-${n + 1}` : base;
      while (used.has(id)) {
        n += 1;
        id = `${base}-${n + 1}`;
      }
      seen.set(base, n + 1);
      used.add(id);
      out.push({ id, level, text, pos });
    }
    return false;
  });
  return out;
}

/** Words and minutes to read (at least one) for a ProseMirror document. */
export function outlineStats(doc: PMNode): PageOutlineStats {
  const words = doc.textBetween(0, doc.content.size, ' ', ' ').split(/\s+/).filter(Boolean).length;
  return { words, minutes: Math.max(1, Math.round(words / WORDS_PER_MINUTE)) };
}

/** Every string the outline shows or announces. */
export interface PageOutlineMessages {
  /** Visible heading and the navigation landmark's name. Default "On this page". */
  title: string;
  /** Shown when there are no headings. */
  empty: string;
  /** Footer stats. Default "1,234 words · 6 min read". */
  stats: (stats: PageOutlineStats) => string;
}

export const DEFAULT_PAGE_OUTLINE_MESSAGES: PageOutlineMessages = {
  title: 'On this page',
  empty: 'Headings you add appear here.',
  stats: ({ words, minutes }) =>
    `${words.toLocaleString()} ${words === 1 ? 'word' : 'words'} · ${minutes} min read`,
};

export interface PageOutlineProps extends Omit<HTMLAttributes<HTMLElement>, 'onSelect'> {
  /** A Tiptap editor: the outline follows its headings as people type. */
  editor?: Editor | null | undefined;
  /** Headings of a static page; each `id` is the heading element's DOM id. Ignored when `editor` is set. */
  headings?: readonly PageOutlineHeading[] | undefined;
  /** Deepest heading level listed. Default 3. */
  maxLevel?: number | undefined;
  /** Controlled active heading id. Default: scroll-spy (and the caret, in an editor). */
  activeId?: string | null | undefined;
  /** Called when the active heading changes. */
  onActiveChange?: ((id: string | null) => void) | undefined;
  /** Called when someone picks a heading, after the outline scrolls to it. */
  onNavigate?: ((heading: PageOutlineHeading) => void) | undefined;
  /** Scrolling element for scroll-spy. Default the viewport. */
  scrollRoot?: (() => Element | null) | undefined;
  /** Show word count and reading time (editor only). */
  showStats?: boolean | undefined;
  /** Hide the visible title (the landmark keeps its name). */
  hideTitle?: boolean | undefined;
  messages?: Partial<PageOutlineMessages> | undefined;
}

interface TreeNode {
  heading: PageOutlineHeading;
  children: TreeNode[];
}

function toTree(list: readonly PageOutlineHeading[]): TreeNode[] {
  const root: TreeNode[] = [];
  const stack: TreeNode[] = [];
  for (const heading of list) {
    const node: TreeNode = { heading, children: [] };
    while (stack.length && (stack[stack.length - 1] as TreeNode).heading.level >= heading.level)
      stack.pop();
    const parent = stack[stack.length - 1];
    (parent ? parent.children : root).push(node);
    stack.push(node);
  }
  return root;
}

const reduceMotion = () =>
  typeof window !== 'undefined' &&
  typeof window.matchMedia === 'function' &&
  window.matchMedia('(prefers-reduced-motion: reduce)').matches;

/**
 * A document's outline: its headings as a nested list of links, with the
 * section being read marked as current. Works from a live Tiptap editor
 * (updates as people type, follows the caret) or from a static page's
 * headings (scroll-spy). Picking an entry scrolls to the heading and moves
 * focus there.
 */
export const PageOutline = forwardRef<HTMLElement, PageOutlineProps>(function PageOutline(
  {
    editor,
    headings: headingsProp,
    maxLevel = 3,
    activeId: activeProp,
    onActiveChange,
    onNavigate,
    scrollRoot,
    showStats = false,
    hideTitle = false,
    messages,
    className,
    ...rest
  },
  ref,
) {
  const m = { ...DEFAULT_PAGE_OUTLINE_MESSAGES, ...messages };
  const titleId = `mn-page-outline-${useId().replace(/:/g, '')}-title`;
  const [fromEditor, setFromEditor] = useState<{
    headings: PageOutlineHeading[];
    /** Null unless `showStats`: counting words walks the whole document on every change. */
    stats: PageOutlineStats | null;
  } | null>(null);
  const [spied, setSpied] = useState<string | null>(null);

  // Editor: recompute on every change; the caret sets the active entry.
  useEffect(() => {
    if (!editor || editor.isDestroyed) {
      setFromEditor(null);
      return;
    }
    const recompute = () =>
      setFromEditor({
        headings: collectOutline(editor.state.doc, maxLevel),
        stats: showStats ? outlineStats(editor.state.doc) : null,
      });
    const caret = () => {
      if (!editor.isFocused) return;
      const { from } = editor.state.selection;
      const at = collectOutline(editor.state.doc, maxLevel)
        .filter((h) => (h.pos ?? 0) <= from)
        .pop();
      setSpied(at?.id ?? null);
    };
    recompute();
    editor.on('update', recompute);
    editor.on('selectionUpdate', caret);
    return () => {
      editor.off('update', recompute);
      editor.off('selectionUpdate', caret);
    };
  }, [editor, maxLevel, showStats]);

  const headings = useMemo(
    () =>
      editor
        ? (fromEditor?.headings ?? [])
        : (headingsProp ?? []).filter((h) => h.level <= maxLevel && h.text.trim()),
    [editor, fromEditor, headingsProp, maxLevel],
  );

  const targetOf = (h: PageOutlineHeading): HTMLElement | null => {
    if (typeof document === 'undefined') return null;
    if (editor && !editor.isDestroyed && typeof h.pos === 'number') {
      try {
        const dom = editor.view.nodeDOM(h.pos);
        return dom instanceof HTMLElement ? dom : null;
      } catch {
        return null;
      }
    }
    return document.getElementById(h.id);
  };

  // Scroll-spy: the current section is the last heading above the top 30%
  // of the scroll area.
  const key = headings.map((h) => h.id).join('|');
  useEffect(() => {
    if (typeof window === 'undefined' || typeof IntersectionObserver === 'undefined') return;
    const targets = headings
      .map((h) => ({ h, el: targetOf(h) }))
      .filter((t): t is { h: PageOutlineHeading; el: HTMLElement } => Boolean(t.el));
    if (!targets.length) return;
    const above = new Set<string>();
    const observer = new IntersectionObserver(
      (entries) => {
        for (const entry of entries) {
          const t = targets.find((x) => x.el === entry.target);
          if (!t) continue;
          const rootTop = entry.rootBounds?.top ?? 0;
          if (entry.isIntersecting || entry.boundingClientRect.top < rootTop) above.add(t.h.id);
          else above.delete(t.h.id);
        }
        const current = targets.filter((t) => above.has(t.h.id)).pop();
        setSpied(current?.h.id ?? targets[0]?.h.id ?? null);
      },
      { root: scrollRoot?.() ?? null, rootMargin: '0px 0px -70% 0px', threshold: 0 },
    );
    for (const t of targets) observer.observe(t.el);
    return () => observer.disconnect();
    // eslint-disable-next-line react-hooks/exhaustive-deps -- targets follow the heading ids.
  }, [key, editor, scrollRoot]);

  const active = activeProp !== undefined ? activeProp : spied;
  useEffect(() => {
    if (activeProp === undefined) onActiveChange?.(spied);
    // eslint-disable-next-line react-hooks/exhaustive-deps -- report spy changes only.
  }, [spied]);

  const go = (h: PageOutlineHeading, e: MouseEvent) => {
    const behavior: ScrollBehavior = reduceMotion() ? 'auto' : 'smooth';
    if (editor && !editor.isDestroyed && typeof h.pos === 'number') {
      e.preventDefault();
      editor
        .chain()
        .focus()
        .setTextSelection(h.pos + 1)
        .scrollIntoView()
        .run();
      targetOf(h)?.scrollIntoView?.({ behavior, block: 'start' });
    } else {
      const el = targetOf(h);
      if (el) {
        e.preventDefault();
        el.scrollIntoView?.({ behavior, block: 'start' });
        if (!el.hasAttribute('tabindex')) el.setAttribute('tabindex', '-1');
        el.focus({ preventScroll: true });
        if (typeof history !== 'undefined') history.replaceState(null, '', `#${h.id}`);
      }
    }
    if (activeProp === undefined) setSpied(h.id);
    onNavigate?.(h);
  };

  const renderList = (nodes: TreeNode[], depth: number) => (
    <ul className="mn-page-outline__list" data-depth={depth}>
      {nodes.map(({ heading: h, children }) => (
        <li key={h.id} className="mn-page-outline__item">
          <a
            className="mn-page-outline__link"
            href={`#${h.id}`}
            data-level={h.level}
            aria-current={h.id === active ? 'location' : undefined}
            onClick={(e) => go(h, e)}
          >
            {h.text}
          </a>
          {children.length ? renderList(children, depth + 1) : null}
        </li>
      ))}
    </ul>
  );

  return (
    <nav
      ref={ref}
      {...rest}
      className={cx('mn-page-outline', className)}
      aria-labelledby={hideTitle ? undefined : titleId}
      aria-label={hideTitle ? m.title : undefined}
    >
      {hideTitle ? null : (
        <p id={titleId} className="mn-page-outline__title">
          {m.title}
        </p>
      )}
      {headings.length ? (
        renderList(toTree(headings), 0)
      ) : (
        <p className="mn-page-outline__empty">{m.empty}</p>
      )}
      {showStats && fromEditor?.stats ? (
        <p className="mn-page-outline__stats">{m.stats(fromEditor.stats)}</p>
      ) : null}
    </nav>
  );
});
