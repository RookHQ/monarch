import { version } from 'react';

/**
 * `inert` as a prop that works on React 18 and 19. React 19 treats it as a
 * boolean attribute; React 18 passes unknown attributes through as strings and
 * needs an empty string. Spread the result onto the element.
 */
export function inertProps(): Record<string, unknown> {
  return Number.parseInt(version, 10) >= 19 ? { inert: true } : { inert: '' };
}

/**
 * Makes everything outside `keep` inert (unreachable by focus, pointer and
 * assistive technology), as a modal needs. Walks from each kept element up to
 * `<body>` and marks every sibling that holds none of them. Returns a function
 * that restores exactly the elements it changed.
 */
export function inertOthers(keep: ReadonlyArray<Element | null | undefined>): () => void {
  const kept = keep.filter((el): el is Element => !!el && el.isConnected);
  const holdsKept = (el: Element) => kept.some((k) => el === k || el.contains(k));
  const changed: Element[] = [];
  const visited = new Set<Element>();
  for (const el of kept) {
    let node: Element | null = el;
    while (node && node !== document.body && node.parentElement) {
      const parent: Element = node.parentElement;
      if (!visited.has(parent)) {
        visited.add(parent);
        for (const sibling of Array.from(parent.children)) {
          if (holdsKept(sibling) || sibling.hasAttribute('inert')) continue;
          if (sibling.tagName === 'SCRIPT' || sibling.tagName === 'STYLE') continue;
          sibling.setAttribute('inert', '');
          changed.push(sibling);
        }
      }
      node = parent;
    }
  }
  return () => {
    for (const el of changed) el.removeAttribute('inert');
  };
}

const TABBABLE =
  'a[href], area[href], button:not([disabled]), input:not([disabled]):not([type="hidden"]), select:not([disabled]), textarea:not([disabled]), iframe, [contenteditable="true"], [tabindex]:not([tabindex="-1"])';

/** Tabbable descendants of `root`, in document order, skipping inert and hidden ones. */
export function tabbables(root: Element): HTMLElement[] {
  return Array.from(root.querySelectorAll<HTMLElement>(TABBABLE)).filter(
    (el) => !el.closest('[inert]') && !el.closest('[hidden]'),
  );
}
