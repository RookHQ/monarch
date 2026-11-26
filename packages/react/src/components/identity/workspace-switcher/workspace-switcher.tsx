'use client';

import { forwardRef, useMemo, useState } from 'react';
import type { ComboboxOption } from '../../forms/combobox/index.js';
import { EntityPicker } from '../../forms/entity-picker/index.js';
import { Avatar } from '../avatar/index.js';
import { cx } from '../../../internal/cx.js';
import { ChevronsGlyph, PlusGlyph, SettingsGlyph } from '../../../internal/shell/icons.js';
import { TipButton } from '../../../internal/shell/tip-button.js';

export interface WorkspaceOption {
  /** Stable id. */
  id: string;
  name: string;
  /** Short handle shown as `@handle` and matched by search. */
  handle?: string | undefined;
  avatarUrl?: string | null | undefined;
  /** Extra words search matches but never shows (old names). */
  keywords?: string | undefined;
}

export interface WorkspaceSwitcherMessages {
  /** Names the trigger ("Workspace: Acme") and the popover. Default "Workspace". */
  label: string;
  /** Trigger text when no workspace is chosen. */
  placeholder: string;
  search: string;
  empty: string;
  loading: string;
  create: string;
  manage: string;
  newTab: string;
}

const DEFAULT_MESSAGES: WorkspaceSwitcherMessages = {
  label: 'Workspace',
  placeholder: 'Choose a workspace',
  search: 'Search workspaces…',
  empty: 'No workspaces match.',
  loading: 'Loading workspaces…',
  create: 'Create workspace',
  manage: 'All workspaces',
  newTab: 'opens in a new tab',
};

export interface WorkspaceSwitcherProps {
  /** The workspaces this person belongs to. */
  workspaces: WorkspaceOption[];
  /** The current workspace id (controlled). */
  value?: string | null | undefined;
  defaultValue?: string | null | undefined;
  /** Called with the chosen workspace id. The app navigates (and keeps the sub-page if it wants). */
  onValueChange?: ((workspaceId: string) => void) | undefined;
  /** "Create workspace": an action (the popover closes first)… */
  onCreate?: (() => void) | undefined;
  /** …or a link. */
  createHref?: string | undefined;
  /** "All workspaces" link (usually Rook ID). */
  manageHref?: string | undefined;
  /** Open `manageHref` in a new tab. Default true. */
  manageExternal?: boolean | undefined;
  loading?: boolean | undefined;
  /** Avatar-only trigger for a collapsed rail; the name moves into a tooltip. */
  collapsed?: boolean | undefined;
  align?: 'start' | 'center' | 'end' | undefined;
  open?: boolean | undefined;
  onOpenChange?: ((open: boolean) => void) | undefined;
  className?: string | undefined;
  messages?: Partial<WorkspaceSwitcherMessages> | undefined;
}

/**
 * Switches between the workspaces a person belongs to: an avatar trigger
 * (name and handle when there is room), a searchable popover with the current
 * workspace checked, and Create / All workspaces actions. Keyboard: the
 * listbox model of EntityPicker (arrows, Home/End, type to search, Enter,
 * Escape returns focus). The app supplies the workspaces and does the switch.
 */
export const WorkspaceSwitcher = forwardRef<HTMLButtonElement, WorkspaceSwitcherProps>(
  function WorkspaceSwitcher(
    {
      workspaces,
      value: valueProp,
      defaultValue = null,
      onValueChange,
      onCreate,
      createHref,
      manageHref,
      manageExternal = true,
      loading = false,
      collapsed = false,
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
    const value = valueProp !== undefined ? valueProp : inner;
    const current = value ? workspaces.find((w) => w.id === value) : undefined;

    const options: ComboboxOption[] = workspaces.map((w) => ({
      value: w.id,
      label: w.name,
      ...(w.handle ? { hint: `@${w.handle}` } : {}),
      ...(w.keywords ? { keywords: w.keywords } : {}),
      media: <Avatar name={w.name} src={w.avatarUrl ?? null} shape="square" size={24} decorative />,
    }));
    const name = current?.name ?? messages.placeholder;
    const choose = (next: string | null) => {
      // Picking the current workspace again changes nothing; there is no "none".
      if (!next || next === value) return;
      if (valueProp === undefined) setInner(next);
      onValueChange?.(next);
    };
    const showFooter = Boolean(onCreate || createHref || manageHref);

    return (
      <EntityPicker
        aria-label={messages.label}
        options={options}
        value={current ? current.id : null}
        onValueChange={choose}
        loading={loading}
        emptyMessage={messages.empty}
        searchPlaceholder={messages.search}
        messages={{ loading: messages.loading }}
        align={align}
        {...(open !== undefined ? { open } : {})}
        {...(onOpenChange ? { onOpenChange } : {})}
        contentClassName="mn-workspace-switcher__content"
        trigger={
          <TipButton
            ref={ref}
            className={cx('mn-workspace-switcher', className)}
            data-collapsed={collapsed ? '' : undefined}
            // Named by its text (kept for screen readers when collapsed), never by an
            // aria-label, so the visible text is always part of the name.
            tip={collapsed ? `${messages.label}: ${name}` : undefined}
          >
            <Avatar
              name={current?.name ?? messages.label}
              src={current?.avatarUrl ?? null}
              shape="square"
              size={collapsed ? 32 : 28}
              decorative
              {...(current ? {} : { fallback: 'icon' as const })}
            />
            <span className="mn-workspace-switcher__text">
              <span className="mn-visually-hidden">{messages.label}: </span>
              <span className="mn-workspace-switcher__name">{name}</span>{' '}
              {current?.handle ? (
                <span className="mn-workspace-switcher__handle">@{current.handle}</span>
              ) : null}
            </span>
            <ChevronsGlyph className="mn-workspace-switcher__chevron" />
          </TipButton>
        }
        footer={
          showFooter
            ? ({ close }) => (
                <div className="mn-workspace-switcher__footer">
                  {onCreate ? (
                    <button
                      type="button"
                      className="mn-workspace-switcher__action"
                      onClick={() => {
                        close();
                        onCreate();
                      }}
                    >
                      <PlusGlyph />
                      {messages.create}
                    </button>
                  ) : createHref ? (
                    <a className="mn-workspace-switcher__action" href={createHref}>
                      <PlusGlyph />
                      {messages.create}
                    </a>
                  ) : null}
                  {manageHref ? (
                    <a
                      className="mn-workspace-switcher__action"
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
