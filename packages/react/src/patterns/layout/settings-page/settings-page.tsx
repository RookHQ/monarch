'use client';

import {
  forwardRef,
  useEffect,
  useRef,
  useState,
  type HTMLAttributes,
  type ReactNode,
  type Ref,
} from 'react';
import { cx } from '../../../internal/cx.js';
import { isSafeUrl } from '../../../internal/sanitize.js';
import { Button } from '../../../components/actions/button/index.js';
import { VisuallyHidden } from '../../../components/layout/visually-hidden/index.js';
import {
  UnsavedChangesGuard,
  type UnsavedChangesGuardHandle,
  type UnsavedChangesMessages,
} from '../../../components/overlays/unsaved-changes-guard/index.js';
import { DangerZone } from '../danger-zone/index.js';

/** One entry in the settings navigation. */
export interface SettingsPageNavItem {
  /** Id of the section on this page (its element `id`), or a key for `href` items. */
  id: string;
  /** Link text. */
  label: ReactNode;
  /**
   * A separate settings page to link to instead of a section on this one.
   * Unsafe URLs fall back to `#id`.
   */
  href?: string | undefined;
  /** Decorative icon before the label. */
  icon?: ReactNode;
}

/** Visible and assistive-technology text. English defaults. */
export interface SettingsPageMessages {
  /** Accessible name of the section navigation. */
  navLabel: string;
  /** Nav label and default heading of the danger zone. */
  dangerZone: string;
  /** Text in the save bar while there are unsaved changes. */
  unsaved: string;
  /** Accessible name of the save bar region. */
  saveBarLabel: string;
  save: string;
  discard: string;
}

export const defaultSettingsPageMessages: SettingsPageMessages = {
  navLabel: 'Settings sections',
  dangerZone: 'Danger zone',
  unsaved: 'You have unsaved changes.',
  saveBarLabel: 'Unsaved changes',
  save: 'Save changes',
  discard: 'Discard',
};

export interface SettingsPageProps extends Omit<HTMLAttributes<HTMLDivElement>, 'title'> {
  /** Page title, such as "Workspace settings". */
  title: ReactNode;
  /** One line under the title. */
  description?: ReactNode;
  /** Actions beside the title (a link to docs, an export button). */
  actions?: ReactNode;
  /** Heading element of the title. `h1` (default) for a page; `h2` inside another page. */
  headingLevel?: 'h1' | 'h2' | undefined;
  /** Replaces the built-in title block, for example with a PageHeader. */
  header?: ReactNode;
  /**
   * Section navigation. Each item links to the element with its `id`
   * (normally a SettingsSection) or to `href`. Leave it out for a short page.
   */
  nav?: readonly SettingsPageNavItem[] | undefined;
  /** The highlighted nav item (controlled). */
  activeSection?: string | undefined;
  /** Called when a nav item is chosen. */
  onActiveSectionChange?: ((id: string) => void) | undefined;
  /** The sections: SettingsSection elements whose `id`s match `nav`. */
  children?: ReactNode;
  /**
   * DangerZoneItem rows. When given, they are wrapped in a DangerZone at the
   * end of the page and a "Danger zone" link is added to the navigation.
   */
  dangerZone?: ReactNode;
  /** Element id of the danger zone. Default `danger-zone`. */
  dangerZoneId?: string | undefined;
  /** Heading of the danger zone. Default `messages.dangerZone`. */
  dangerZoneHeading?: ReactNode;
  /** One or two sentences under the danger zone heading. */
  dangerZoneDescription?: ReactNode;
  /**
   * The page's draft differs from the saved settings. Shows the save bar
   * and guards leaving the page (tab close, reload and, with
   * `interceptLinks`, link clicks).
   */
  dirty?: boolean | undefined;
  /**
   * Saves the whole page. Shows Save in the save bar and "Save and leave" in
   * the leave dialog. A returned promise keeps that dialog pending.
   */
  onSave?: (() => unknown) | undefined;
  /** Discards the draft. Shows Discard in the save bar. */
  onDiscard?: (() => void) | undefined;
  /** A save is in flight: Save shows a spinner and Discard is disabled. */
  saving?: boolean | undefined;
  /** Also ask before same-origin link clicks leave the page. Default `true`. */
  interceptLinks?: boolean | undefined;
  /** Ref to the guard, whose `guard(proceed)` wraps the app's own navigation. */
  guardRef?: Ref<UnsavedChangesGuardHandle> | undefined;
  /** Text of the leave dialog. */
  guardMessages?: Partial<UnsavedChangesMessages> | undefined;
  messages?: Partial<SettingsPageMessages> | undefined;
}

/**
 * A full settings page: title, a section navigation that becomes a
 * scrolling strip on phones, the product's SettingsSections, an optional
 * danger zone at the end, and a page-level save bar with an unsaved-changes
 * guard while the draft differs from what is saved.
 */
export const SettingsPage = forwardRef<HTMLDivElement, SettingsPageProps>(function SettingsPage(
  {
    title,
    description,
    actions,
    headingLevel: Heading = 'h1',
    header,
    nav,
    activeSection,
    onActiveSectionChange,
    children,
    dangerZone,
    dangerZoneId = 'danger-zone',
    dangerZoneHeading,
    dangerZoneDescription,
    dirty = false,
    onSave,
    onDiscard,
    saving = false,
    interceptLinks = true,
    guardRef,
    guardMessages,
    messages,
    className,
    ...rest
  },
  ref,
) {
  const m = { ...defaultSettingsPageMessages, ...messages };
  const items: SettingsPageNavItem[] = [
    ...(nav ?? []),
    ...(nav && dangerZone ? [{ id: dangerZoneId, label: m.dangerZone }] : []),
  ];
  const [activeState, setActiveState] = useState<string | undefined>(items[0]?.id);
  const active = activeSection ?? activeState;

  // Start on the section named in the URL hash, after hydration.
  useEffect(() => {
    if (activeSection !== undefined) return;
    const hash = window.location.hash.slice(1);
    if (hash && items.some((item) => item.id === hash)) setActiveState(hash);
    // Only on mount: later changes come from clicks.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const choose = (id: string) => {
    if (activeSection === undefined) setActiveState(id);
    onActiveSectionChange?.(id);
  };

  const hasSaveBar = dirty && (onSave !== undefined || onDiscard !== undefined);

  // Save and Discard remove the bar they sit in. Without this, focus falls to
  // <body> and the next Tab starts from the top of the page. `focusInBar` is set
  // when one of them is pressed.
  const contentRef = useRef<HTMLDivElement>(null);
  const focusInBar = useRef(false);
  useEffect(() => {
    if (hasSaveBar || !focusInBar.current) return;
    focusInBar.current = false;
    const active = document.activeElement;
    if (active && active !== document.body) return;
    const content = contentRef.current;
    if (!content) return;
    if (!content.hasAttribute('tabindex')) content.setAttribute('tabindex', '-1');
    content.focus({ preventScroll: true });
  }, [hasSaveBar]);

  const discard = () => {
    focusInBar.current = true;
    onDiscard?.();
  };
  const save = () => {
    if (!onSave) return;
    focusInBar.current = true;
    // A rejected save is the product's to report; it must not become an unhandled rejection.
    const result = onSave();
    if (result && typeof (result as PromiseLike<unknown>).then === 'function')
      (result as PromiseLike<unknown>).then(undefined, () => {});
  };

  return (
    <div
      ref={ref}
      {...rest}
      className={cx('mn-settings-page', className)}
      data-has-nav={items.length > 0 || undefined}
      data-dirty={dirty || undefined}
    >
      {header ?? (
        <header className="mn-settings-page__header">
          <div className="mn-settings-page__heading">
            <Heading className="mn-settings-page__title">{title}</Heading>
            {description ? <p className="mn-settings-page__description">{description}</p> : null}
          </div>
          {actions ? <div className="mn-settings-page__actions">{actions}</div> : null}
        </header>
      )}

      <div className="mn-settings-page__layout">
        {items.length > 0 ? (
          <nav className="mn-settings-page__nav" aria-label={m.navLabel}>
            <ul className="mn-settings-page__nav-list">
              {items.map((item) => {
                const external = item.href !== undefined && isSafeUrl(item.href);
                const current = active === item.id;
                return (
                  <li key={item.id}>
                    <a
                      className="mn-settings-page__nav-link"
                      href={external ? item.href : `#${item.id}`}
                      aria-current={current ? (external ? 'page' : 'location') : undefined}
                      data-danger={item.id === dangerZoneId && dangerZone ? '' : undefined}
                      onClick={() => choose(item.id)}
                    >
                      {item.icon ? (
                        <span className="mn-settings-page__nav-icon" aria-hidden>
                          {item.icon}
                        </span>
                      ) : null}
                      <span>{item.label}</span>
                    </a>
                  </li>
                );
              })}
            </ul>
          </nav>
        ) : null}

        <div ref={contentRef} className="mn-settings-page__content">
          {children}
          {dangerZone ? (
            <DangerZone
              id={dangerZoneId}
              heading={dangerZoneHeading ?? m.dangerZone}
              description={dangerZoneDescription}
              className="mn-settings-page__danger"
            >
              {dangerZone}
            </DangerZone>
          ) : null}
        </div>
      </div>

      <VisuallyHidden role="status" aria-live="polite">
        {dirty ? m.unsaved : ''}
      </VisuallyHidden>
      {hasSaveBar ? (
        <div className="mn-settings-page__save-bar" role="region" aria-label={m.saveBarLabel}>
          <p className="mn-settings-page__save-text">{m.unsaved}</p>
          <div className="mn-settings-page__save-actions">
            {onDiscard ? (
              <Button disabled={saving} onClick={discard}>
                {m.discard}
              </Button>
            ) : null}
            {onSave ? (
              <Button variant="primary" loading={saving} onClick={save}>
                {m.save}
              </Button>
            ) : null}
          </div>
        </div>
      ) : null}

      <UnsavedChangesGuard
        ref={guardRef}
        when={dirty}
        interceptLinks={interceptLinks}
        {...(onSave !== undefined && { onSave })}
        {...(guardMessages !== undefined && { messages: guardMessages })}
      />
    </div>
  );
});
