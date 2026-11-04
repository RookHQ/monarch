'use client';

import { Button, IconButton } from '../../../components/actions/button/index.js';
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
} from '../../../components/overlays/dropdown-menu/index.js';
import { MoreGlyph } from '../../../internal/icons.js';
import type { ReactNode } from 'react';

/** One secondary page action. Shown as a button on wide screens, in the overflow menu otherwise. */
export interface PageHeaderAction {
  /** Stable key. */
  id: string;
  /** Visible label, also the menu item text. */
  label: string;
  /** Decorative icon before the label. */
  icon?: ReactNode;
  /** Called when the action is chosen (button click or menu selection). Needs a client component. */
  onSelect?: () => void;
  /** Makes the action a link. Works from server components. */
  href?: string;
  disabled?: boolean;
  /** `danger` for destructive actions. Pair with a confirmation step. */
  tone?: 'default' | 'danger';
}

interface PageHeaderActionBarProps {
  actions: PageHeaderAction[];
  maxVisible: number;
  moreLabel: string;
}

/**
 * Secondary actions: buttons for the first `maxVisible`, a menu for the rest.
 * Below 640px the buttons hide and every action is in the menu. The switch is
 * CSS only (`data-wide-inline` items hide on wide screens), so the server and
 * client render the same markup.
 */
export function PageHeaderActionBar({ actions, maxVisible, moreLabel }: PageHeaderActionBarProps) {
  const visible = Math.max(0, Math.min(maxVisible, actions.length));
  const overflow = actions.length > visible;
  return (
    <>
      {actions.slice(0, visible).map((action) =>
        action.href && !action.disabled ? (
          <Button
            key={action.id}
            asChild
            variant={action.tone === 'danger' ? 'danger-subtle' : 'secondary'}
            leadingIcon={action.icon}
            className="mn-page-header__action"
          >
            <a href={action.href} onClick={action.onSelect}>
              {action.label}
            </a>
          </Button>
        ) : (
          <Button
            key={action.id}
            variant={action.tone === 'danger' ? 'danger-subtle' : 'secondary'}
            leadingIcon={action.icon}
            disabled={action.disabled}
            onClick={action.onSelect}
            className="mn-page-header__action"
          >
            {action.label}
          </Button>
        ),
      )}
      <DropdownMenu>
        <DropdownMenuTrigger asChild>
          <IconButton
            label={moreLabel}
            icon={<MoreGlyph />}
            variant="secondary"
            className="mn-page-header__more"
            data-overflow={overflow || undefined}
          />
        </DropdownMenuTrigger>
        <DropdownMenuContent align="end">
          {actions.map((action, index) => {
            const inlineOnWide = index < visible;
            const common = {
              tone: action.tone ?? 'default',
              disabled: action.disabled ?? false,
              'data-wide-inline': inlineOnWide || undefined,
              className: 'mn-page-header__menu-item',
            } as const;
            return action.href && !action.disabled ? (
              <DropdownMenuItem key={action.id} {...common} asChild>
                <a href={action.href} onClick={action.onSelect}>
                  {action.label}
                </a>
              </DropdownMenuItem>
            ) : (
              <DropdownMenuItem
                key={action.id}
                {...common}
                icon={action.icon}
                {...(action.onSelect ? { onSelect: action.onSelect } : {})}
              >
                {action.label}
              </DropdownMenuItem>
            );
          })}
        </DropdownMenuContent>
      </DropdownMenu>
    </>
  );
}
