import { version } from 'react';

/**
 * `inert` as a prop that works on React 18 and 19. React 19 treats it as a
 * boolean attribute; React 18 passes unknown attributes through as strings and
 * needs an empty string. Spread the result onto the element.
 */
export function inertProps(): Record<string, unknown> {
  return Number.parseInt(version, 10) >= 19 ? { inert: true } : { inert: '' };
}
