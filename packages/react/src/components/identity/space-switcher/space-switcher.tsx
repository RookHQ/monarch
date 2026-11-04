'use client';

import { forwardRef, useMemo, useState, type ReactNode } from 'react';
import type { ComboboxOption } from '../../forms/combobox/index.js';
import { EntityPicker } from '../../forms/entity-picker/index.js';
import { cx } from '../../../internal/cx.js';
import {
  ChevronsGlyph,
  LayersGlyph,
  PlusGlyph,
  SettingsGlyph,
} from '../../../internal/shell/icons.js';
import { TipButton } from '../../../internal/shell/tip-button.js';

export interface SpaceOption {
  /** Stable id (the Rook ID Space id). */
  id: string;
  name: string;
  /** Secondary text in the list, such as the member count. */
  description?: string | undefined;
  /** Leading visual. Decorative. */
  icon?: ReactNode;
}

export interface SpaceSwitcherMessages {
  /** Names the trigger ("Space: Marketing") and the popover. Default "Space". */
  label: string;
  /** The all-spaces choice. Default "All spaces". */
  all: string;
  search: string;
  empty: string;
  loading: string;
  manage: string;
  create: string;
  /** Appended to a link that opens in a new tab. */
  newTab: string;
}

const DEFAULT_MESSAGES: SpaceSwitcherMessages = {
  label: 'Space',
  all: 'All spaces',
  search: 'Search spaces…',
  empty: 'No spaces match.',
  loading: 'Loading spaces…',
  manage: 'Manage Spaces',
  create: 'New Space',
  newTab: 'opens in a new tab',
};

export interface SpaceSwitcherProps {
  /** The Spaces of the current workspace that this person can open. */
  spaces: SpaceOption[];
  /** The chosen Space id; `null` is "All spaces" (controlled). */
  value?: string | null | undefined;
  defaultValue?: string | null | undefined;
  onValueChange?: ((spaceId: string | null) => void) | undefined;
  /** Offer "All spaces" first. Default true. Without it a Space must stay chosen. */
  allowAll?: boolean | undefined;
  /** "Manage Spaces" link (usually Rook ID). */
  manageHref?: string | undefined;
  /** Open `manageHref` in a new tab. Default true. */
  manageExternal?: boolean | undefined;
  /** "New Space": a link… */
  createHref?: string | undefined;
  /** …or an action (the popover closes first). */
  onCreate?: (() => void) | undefined;
  /** Spaces are still loading: the list says so. */
  loading?: boolean | undefined;
  /** Icon-only trigger for a collapsed rail; the name moves into a tooltip. */
  collapsed?: boolean | undefined;
  /** Render nothing when there are no Spaces and nothing is loading. Default true. */
  hideWhenEmpty?: boolean | undefined;
  /** Popover alignment. Default `start`. */
  align?: 'start' | 'center' | 'end' | undefined;
  /** Open (controlled). */
  open?: boolean | undefined;
  onOpenChange?: ((open: boolean) => void) | undefined;
  className?: string | undefined;
  /** Interface text, for translation. */
  messages?: Partial<SpaceSwitcherMessages> | undefined;
}

/**
 * Chooses the Space (a department-like slice of a workspace) the app shows:
 * "All spaces" or one Space. A popover with search, the current Space checked,
 * and New / Manage links; the list is a listbox with arrow keys, Home/End and
 * type-to-search (EntityPicker). The product supplies the Spaces and stores
 * the choice; the switcher never fetches.
 */
export const SpaceSwitcher = forwardRef<HTMLButtonElement, SpaceSwitcherProps>(
  function SpaceSwitcher(
    {
      spaces,
      value: valueProp,
      defaultValue = null,
      onValueChange,
      allowAll = true,
      manageHref,
      manageExternal = true,
      createHref,
      onCreate,
      loading = false,
      collapsed = false,
      hideWhenEmpty = true,
      align = 'start',
      open,
      onOpenChange,
      className,
      messages: messagesProp,
    },
    ref,
  ) {
    const messages = useMemo(() => ({ ...DEFAULT_MESSAGES, ...messagesProp }), [messagesProp]);
    const [inner, setInner] = useState<string | null>(defaultValue);
    const raw = valueProp !== undefined ? valueProp : inner;
    // A chosen Space that no longer exists (deleted, access removed) reads as "All spaces".
    const current = raw ? spaces.find((s) => s.id === raw) : undefined;
    const value = current ? current.id : null;

    if (hideWhenEmpty && spaces.length === 0 && !loading) return null;

    const options: ComboboxOption[] = spaces.map((s) => ({
      value: s.id,
      label: s.name,
      ...(s.description ? { description: s.description } : {}),
      ...(s.icon ? { media: s.icon } : {}),
    }));
    const name = current?.name ?? messages.all;
    const choose = (next: string | null) => {
      if (next === null && !allowAll) return;
      if (valueProp === undefined) setInner(next);
      onValueChange?.(next);
    };
    const showFooter = Boolean(manageHref || createHref || onCreate);

    return (
      <EntityPicker
        aria-label={messages.label}
        options={options}
        value={value}
        onValueChange={choose}
        loading={loading}
        emptyMessage={messages.empty}
        searchPlaceholder={messages.search}
        messages={{ loading: messages.loading }}
        align={align}
        {...(allowAll ? { clearOption: { label: messages.all, media: <LayersGlyph /> } } : {})}
        {...(open !== undefined ? { open } : {})}
        {...(onOpenChange ? { onOpenChange } : {})}
        contentClassName="mn-space-switcher__content"
        trigger={
          <TipButton
            ref={ref}
            className={cx('mn-space-switcher', className)}
            data-collapsed={collapsed ? '' : undefined}
            data-all={current ? undefined : ''}
            // Named by its text (kept for screen readers when collapsed), never by an
            // aria-label, so the visible text is always part of the name.
            tip={collapsed ? `${messages.label}: ${name}` : undefined}
          >
            <span className="mn-space-switcher__icon" aria-hidden>
              {current?.icon ?? <LayersGlyph />}
            </span>
            <span className="mn-space-switcher__text">
              <span className="mn-space-switcher__caption" aria-hidden>
                {messages.label}
              </span>
              <span className="mn-visually-hidden">{messages.label}: </span>
              <span className="mn-space-switcher__name">{name}</span>
            </span>
            <ChevronsGlyph className="mn-space-switcher__chevron" />
          </TipButton>
        }
        footer={
          showFooter
            ? ({ close }) => (
                <div className="mn-space-switcher__footer">
                  {onCreate ? (
                    <button
                      type="button"
                      className="mn-space-switcher__action"
                      onClick={() => {
                        close();
                        onCreate();
                      }}
                    >
                      <PlusGlyph />
                      {messages.create}
                    </button>
                  ) : createHref ? (
                    <a className="mn-space-switcher__action" href={createHref}>
                      <PlusGlyph />
                      {messages.create}
                    </a>
                  ) : null}
                  {manageHref ? (
                    <a
                      className="mn-space-switcher__action"
                      href={manageHref}
                      {...(manageExternal ? { target: '_blank', rel: 'noopener noreferrer' } : {})}
                    >
                      <SettingsGlyph />
                      {messages.manage}
                      {manageExternal ? (
                        <span className="mn-visually-hidden"> ({messages.newTab})</span>
                      ) : null}
                    </a>
                  ) : null}
                </div>
              )
            : undefined
        }
      />
    );
  },
);
