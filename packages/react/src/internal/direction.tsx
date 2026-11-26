'use client';

/**
 * One source of truth for reading direction. `DirectionProvider` sets it for
 * a subtree (and for the Radix primitives inside); without a provider the
 * direction comes from the DOM at the moment it is needed: the nearest `dir`
 * attribute, then the computed `direction`, then `ltr`.
 */
import { createContext, useCallback, useContext, type ReactNode } from 'react';
import { DirectionProvider as RadixDirectionProvider } from '@radix-ui/react-direction';

export type Direction = 'ltr' | 'rtl';

const DirectionContext = createContext<Direction | undefined>(undefined);

export interface DirectionProviderProps {
  /** Reading direction of everything below. */
  dir: Direction;
  children?: ReactNode;
}

/**
 * Sets the reading direction for Monarch components and the Radix
 * primitives they use (menus, tabs, roving focus). Still set `dir` on your
 * `<html>` (or a wrapping element) so CSS logical properties follow.
 */
export function DirectionProvider({ dir, children }: DirectionProviderProps) {
  return (
    <DirectionContext.Provider value={dir}>
      <RadixDirectionProvider dir={dir}>{children}</RadixDirectionProvider>
    </DirectionContext.Provider>
  );
}

/** The direction of `el` from the DOM: nearest `dir` attribute, computed style, else `ltr`. */
export function elementDirection(el: Element | null | undefined): Direction {
  if (!el) return 'ltr';
  const attr = el.closest('[dir]')?.getAttribute('dir')?.toLowerCase();
  if (attr === 'rtl' || attr === 'ltr') return attr;
  if (typeof getComputedStyle === 'function' && getComputedStyle(el).direction === 'rtl') {
    return 'rtl';
  }
  return 'ltr';
}

/** The direction set by the nearest `DirectionProvider`, or `undefined` when there is none. */
export function useDirectionContext(): Direction | undefined {
  return useContext(DirectionContext);
}

/**
 * Returns a resolver for the reading direction: an explicit `dir` (a prop)
 * wins, then the nearest `DirectionProvider`, then the DOM around the element
 * passed in. Call the resolver in event handlers, where the DOM is current.
 */
export function useDirection(dir?: Direction | undefined): (el?: Element | null) => Direction {
  const context = useContext(DirectionContext);
  const fixed = dir ?? context;
  return useCallback((el?: Element | null) => fixed ?? elementDirection(el), [fixed]);
}
