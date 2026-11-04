'use client';

import { useEffect, useId, useRef, useState, type HTMLAttributes, type ReactNode } from 'react';
import * as PopoverPrimitive from '@radix-ui/react-popover';
import { cx } from '../../../internal/cx.js';
import { ChevronDownGlyph, CloseGlyph, MenuGlyph } from '../../../internal/icons.js';
import { SiteLinkView } from './marketing-site-shell.link.js';
import { isGroup, type LinkComponent, type SiteNavItem } from './marketing-site-shell.types.js';

export interface SiteHeaderProps extends HTMLAttributes<HTMLElement> {
  /**
   * Product wordmark, normally a link home: `<a href="/"><img alt="Rook Dossier" …/></a>`.
   * Monarch ships no artwork; use the product's logo from the Rook brand kit.
   */
  brand: ReactNode;
  /** Top-level links and groups. Groups open a disclosure panel of links. */
  nav?: SiteNavItem[];
  /** Calls to action on the trailing edge, e.g. "Log in" and one primary Button. */
  actions?: ReactNode;
  /** Accessible name of the navigation landmark. */
  navLabel?: string;
  /** Stick to the top while scrolling. Default true. */
  sticky?: boolean;
  /** Router link component for nav links (e.g. Next.js Link). Default `a`. */
  linkAs?: LinkComponent;
  /** Labels for the mobile menu toggle. */
  menuLabels?: { open: string; close: string };
}

/**
 * Site header for marketing and public pages: wordmark, primary navigation with
 * disclosure menus, and actions. Under 768px the navigation collapses into a
 * menu button (`aria-expanded`), closed with Escape.
 */
export function SiteHeader({
  brand,
  nav = [],
  actions,
  navLabel = 'Main',
  sticky = true,
  linkAs,
  menuLabels = { open: 'Open menu', close: 'Close menu' },
  className,
  ...rest
}: SiteHeaderProps) {
  const [open, setOpen] = useState(false);
  const panelId = `mn-site-menu-${useId()}`;
  const toggleRef = useRef<HTMLButtonElement>(null);

  useEffect(() => {
    if (!open) return;
    const onKey = (e: KeyboardEvent) => {
      if (e.key === 'Escape') {
        setOpen(false);
        toggleRef.current?.focus();
      }
    };
    document.addEventListener('keydown', onKey);
    return () => document.removeEventListener('keydown', onKey);
  }, [open]);

  return (
    <header {...rest} className={cx('mn-site-header', className)} data-sticky={sticky || undefined}>
      <div className="mn-site-header__inner">
        <div className="mn-site-header__brand">{brand}</div>
        {nav.length ? (
          <nav aria-label={navLabel} className="mn-site-header__nav">
            <ul className="mn-site-header__list">
              {nav.map((item) =>
                isGroup(item) ? (
                  <li key={item.label}>
                    <PopoverPrimitive.Root>
                      <PopoverPrimitive.Trigger className="mn-site-header__link mn-site-header__trigger">
                        {item.label}
                        <ChevronDownGlyph className="mn-site-header__chevron" />
                      </PopoverPrimitive.Trigger>
                      <PopoverPrimitive.Portal>
                        <PopoverPrimitive.Content
                          className="mn-site-menu"
                          align="start"
                          sideOffset={10}
                          aria-label={item.label}
                        >
                          <ul className="mn-site-menu__list">
                            {item.items.map((link) => (
                              <li key={link.href}>
                                <SiteLinkView
                                  link={link}
                                  as={linkAs}
                                  className="mn-site-menu__item"
                                >
                                  <span className="mn-site-menu__label">{link.label}</span>
                                  {link.description ? (
                                    <span className="mn-site-menu__description">
                                      {link.description}
                                    </span>
                                  ) : null}
                                </SiteLinkView>
                              </li>
                            ))}
                          </ul>
                        </PopoverPrimitive.Content>
                      </PopoverPrimitive.Portal>
                    </PopoverPrimitive.Root>
                  </li>
                ) : (
                  <li key={item.href}>
                    <SiteLinkView link={item} as={linkAs} className="mn-site-header__link" />
                  </li>
                ),
              )}
            </ul>
          </nav>
        ) : null}
        <div className="mn-site-header__end">
          {actions ? <div className="mn-site-header__actions">{actions}</div> : null}
          {nav.length ? (
            <button
              ref={toggleRef}
              type="button"
              className="mn-site-header__toggle"
              aria-expanded={open}
              aria-controls={panelId}
              aria-label={open ? menuLabels.close : menuLabels.open}
              onClick={() => setOpen((o) => !o)}
            >
              {open ? <CloseGlyph /> : <MenuGlyph />}
            </button>
          ) : null}
        </div>
      </div>
      {open ? (
        <nav id={panelId} aria-label={navLabel} className="mn-site-header__panel">
          <ul className="mn-site-header__panel-list">
            {nav.map((item) =>
              isGroup(item) ? (
                <li key={item.label} className="mn-site-header__panel-group">
                  <p className="mn-site-header__panel-heading">{item.label}</p>
                  <ul>
                    {item.items.map((link) => (
                      <li key={link.href}>
                        <SiteLinkView
                          link={link}
                          as={linkAs}
                          className="mn-site-header__panel-link"
                        />
                      </li>
                    ))}
                  </ul>
                </li>
              ) : (
                <li key={item.href}>
                  <SiteLinkView link={item} as={linkAs} className="mn-site-header__panel-link" />
                </li>
              ),
            )}
          </ul>
          {actions ? <div className="mn-site-header__panel-actions">{actions}</div> : null}
        </nav>
      ) : null}
    </header>
  );
}
