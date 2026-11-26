'use client';

import { createContext, useContext } from 'react';
import type { ShellLinkComponent } from '../../../internal/shell/link.js';

/** What a RookBar panel is told about where it renders. */
export interface RookBarPanelContext {
  /** `desktop`: the wide panel under the bar. `mobile`: inside a section of the menu sheet. */
  layout: 'desktop' | 'mobile';
  /** Close every menu (call after a link is followed). */
  close: () => void;
  /** The bar's link component, for internal links. */
  linkComponent?: ShellLinkComponent | undefined;
}

const PanelContext = createContext<RookBarPanelContext>({ layout: 'desktop', close: () => {} });

export const RookBarPanelProvider = PanelContext.Provider;

/** Reads the panel context inside a RookBar menu (layout and `close`). */
export function useRookBarPanel(): RookBarPanelContext {
  return useContext(PanelContext);
}
