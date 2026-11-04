import type { ElementType } from 'react';

/**
 * Element used for links in marketing navigation. Pass your router's link
 * (for example Next.js `Link`); it receives `href`, `className` and children.
 * Defaults to `a`.
 */
export type LinkComponent = ElementType;

export interface SiteLink {
  label: string;
  href: string;
  /** Short line shown under the label in menus. */
  description?: string;
  /** Marks the current page (`aria-current="page"`). */
  current?: boolean;
  /** Opens in a new tab, with "opens in a new tab" for screen readers. */
  external?: boolean;
}

export interface SiteNavGroup {
  label: string;
  items: SiteLink[];
}

export type SiteNavItem = SiteLink | SiteNavGroup;

export function isGroup(item: SiteNavItem): item is SiteNavGroup {
  return 'items' in item;
}
