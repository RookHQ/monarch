/** Example-only line icons (20px, currentColor). Apps keep using their icon set. */
import type { SVGProps } from 'react';

const base = {
  width: 20,
  height: 20,
  viewBox: '0 0 20 20',
  fill: 'none',
  stroke: 'currentColor',
  strokeWidth: 1.6,
  strokeLinecap: 'round',
  strokeLinejoin: 'round',
  'aria-hidden': true,
} as const;

const make = (d: string) =>
  function Icon(props: SVGProps<SVGSVGElement>) {
    return (
      <svg {...base} {...props}>
        <path d={d} />
      </svg>
    );
  };

export const IconPages = make('M6 3h6l4 4v10H6zM12 3v4h4M8.5 11h5M8.5 14h3');
export const IconSlash = make('M12.5 3.5 7.5 16.5M4 6h3M4 14h3');
export const IconSearch = make('M9 15a6 6 0 1 0 0-12 6 6 0 0 0 0 12zM17 17l-3.5-3.5');
export const IconShare = make(
  'M8 11.5 12 8.5M8 8.5 12 11.5M5.5 12.5a2.5 2.5 0 1 0 0-5 2.5 2.5 0 0 0 0 5zM14.5 7.5a2.5 2.5 0 1 0 0-5 2.5 2.5 0 0 0 0 5zM14.5 17.5a2.5 2.5 0 1 0 0-5 2.5 2.5 0 0 0 0 5z',
);
export const IconHistory = make('M3.5 10a6.5 6.5 0 1 0 2-4.7M3.5 3.5v3h3M10 6.5V10l2.5 1.5');
/** Neutral AI icon (same geometry as lucide `Bot`). Rook rule: never a sparkle for AI. */
export function IconBot(props: SVGProps<SVGSVGElement>) {
  return (
    <svg {...base} {...props}>
      <path d="M10 6.5V3.5H7" />
      <rect x="3.5" y="6.5" width="13" height="10" rx="2" />
      <path d="M1.75 11.5h1.75M16.5 11.5h1.75M12.5 10.5v2M7.5 10.5v2" />
    </svg>
  );
}
