'use client';

import { forwardRef, useId, useState } from 'react';
import {
  LinkedDocsPanel,
  type LinkedDocsPanelMessages,
  type LinkedDocsPanelProps,
} from '../../../components/collaboration/linked-docs-panel/index.js';
import { cx } from '../../../internal/cx.js';
import { ChevronDownGlyph } from '../../../internal/icons.js';
import { DocGlyph } from '../../../internal/shell/panel-icons.js';

export interface DocsPanelMessages extends LinkedDocsPanelMessages {
  /** The collapsible toggle's text. Default "Docs". */
  toggle: string;
  /** Appended to the toggle when `count` is known. Default "3 linked". */
  toggleCount: (count: number) => string;
}

export interface DocsPanelProps extends Omit<LinkedDocsPanelProps, 'messages'> {
  /**
   * The frame around the panel: `card` (bordered surface, for a record page
   * column), `divider` (a rule below, for a stacked side panel) or `none`.
   * Default `card`.
   */
  frame?: 'card' | 'divider' | 'none' | undefined;
  /**
   * Start closed behind a "Docs" toggle and mount the panel only when opened,
   * so a long list of records does not load docs for each one.
   */
  collapsible?: boolean | undefined;
  /** Open (controlled), with `collapsible`. Load the items when it becomes true. */
  open?: boolean | undefined;
  /** Initial open state with `collapsible`. Default false. */
  defaultOpen?: boolean | undefined;
  onOpenChange?: ((open: boolean) => void) | undefined;
  /** Linked count shown on the closed toggle, when the app already knows it. */
  count?: number | undefined;
  /** Interface text, for translation. Includes every LinkedDocsPanel message. */
  messages?: Partial<DocsPanelMessages> | undefined;
}

/**
 * The record-page block for Rook Dossier docs and links: LinkedDocsPanel in a
 * card or divider frame, optionally behind a "Docs" disclosure that mounts the
 * panel only when opened. The app supplies the items and does the linking
 * (Dossier API, session refresh); this block never fetches.
 */
export const DocsPanel = forwardRef<HTMLElement, DocsPanelProps>(function DocsPanel(
  {
    frame = 'card',
    collapsible = false,
    open: openProp,
    defaultOpen = false,
    onOpenChange,
    count,
    messages: messagesProp,
    className,
    style,
    id,
    ...panelProps
  },
  ref,
) {
  const autoId = useId();
  const regionId = `${id ?? autoId}-docs`;
  const [openInner, setOpenInner] = useState(defaultOpen);
  const open = !collapsible || (openProp ?? openInner);
  const setOpen = (next: boolean) => {
    if (openProp === undefined) setOpenInner(next);
    onOpenChange?.(next);
  };
  const { toggle, toggleCount, ...panelMessages } = messagesProp ?? {};
  const toggleText = toggle ?? 'Docs';
  const countText =
    count !== undefined && count > 0
      ? (toggleCount ?? ((n: number) => `${n} linked`))(count)
      : null;

  return (
    <div
      className={cx('mn-docs-panel', className)}
      style={style}
      data-frame={frame}
      data-collapsible={collapsible ? '' : undefined}
      data-state={collapsible ? (open ? 'open' : 'closed') : undefined}
    >
      {collapsible ? (
        <button
          type="button"
          className="mn-docs-panel__toggle"
          aria-expanded={open}
          aria-controls={open ? regionId : undefined}
          onClick={() => setOpen(!open)}
        >
          <DocGlyph className="mn-docs-panel__toggle-icon" aria-hidden />
          <span>{toggleText}</span>
          {countText ? (
            <span className="mn-docs-panel__toggle-count">
              <span className="mn-visually-hidden">, </span>
              {countText}
            </span>
          ) : null}
          <ChevronDownGlyph className="mn-docs-panel__chevron" aria-hidden />
        </button>
      ) : null}
      {open ? (
        <LinkedDocsPanel
          ref={ref}
          {...panelProps}
          id={collapsible ? regionId : id}
          className="mn-docs-panel__panel"
          messages={panelMessages}
        />
      ) : null}
    </div>
  );
});
