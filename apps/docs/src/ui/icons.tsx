/**
 * Docs-site line icons (24px grid, currentColor, decorative). Apps use their own
 * icon set; these exist so the site has no icon dependency.
 */
import type { SVGProps } from 'react';

type IconProps = SVGProps<SVGSVGElement> & { size?: number };

function make(paths: string[]) {
  return function Icon({ size = 16, ...props }: IconProps) {
    return (
      <svg
        width={size}
        height={size}
        viewBox="0 0 24 24"
        fill="none"
        stroke="currentColor"
        strokeWidth={1.75}
        strokeLinecap="round"
        strokeLinejoin="round"
        aria-hidden
        focusable="false"
        {...props}
      >
        {paths.map((d) => (
          <path key={d} d={d} />
        ))}
      </svg>
    );
  };
}

export const SearchIcon = make(['M11 18a7 7 0 1 0 0-14 7 7 0 0 0 0 14z', 'm20 20-4-4']);
export const SunIcon = make([
  'M12 16a4 4 0 1 0 0-8 4 4 0 0 0 0 8z',
  'M12 2v2M12 20v2M4.93 4.93l1.41 1.41M17.66 17.66l1.41 1.41M2 12h2M20 12h2M6.34 17.66l-1.41 1.41M19.07 4.93l-1.41 1.41',
]);
export const MoonIcon = make(['M20.5 14.1A8.5 8.5 0 1 1 9.9 3.5a6.6 6.6 0 0 0 10.6 10.6z']);
export const MonitorIcon = make(['M4 4h16v11H4z', 'M8 20h8M12 15v5']);
export const SlidersIcon = make([
  'M4 6h9M17 6h3M4 12h3M11 12h9M4 18h11M19 18h1',
  'M15 4v4M9 10v4M17 16v4',
]);
export const MenuIcon = make(['M4 6h16M4 12h16M4 18h16']);
export const ChevronDownIcon = make(['m6 9 6 6 6-6']);
export const ArrowLeftIcon = make(['M19 12H5', 'm12 19-7-7 7-7']);
export const ArrowRightIcon = make(['M5 12h14', 'm12 5 7 7-7 7']);
export const CodeIcon = make(['m16 18 6-6-6-6', 'm8 6-6 6 6 6']);
export const RepoIcon = make(['M4 19.5V5a2 2 0 0 1 2-2h14v15H6.5a2.5 2.5 0 0 0 0 5H20', 'M9 7h6']);
export const PaletteIcon = make([
  'M12 21a9 9 0 1 1 9-9c0 2.5-2 3.5-3.5 3.5H15a2 2 0 0 0-1.5 3.3c.6.7.2 2.2-1.5 2.2z',
  'M7.5 11.5h.01M10.5 7.5h.01M15.5 8.5h.01',
]);
export const TypeIcon = make(['M4 7V5h16v2', 'M9 19h6', 'M12 5v14']);
export const GridIcon = make(['M4 4h7v7H4zM13 4h7v7h-7zM4 13h7v7H4zM13 13h7v7h-7z']);
export const ContrastIcon = make([
  'M12 21a9 9 0 1 0 0-18 9 9 0 0 0 0 18z',
  'M12 3v18',
  'M12 7h4M12 11h6M12 15h5',
]);
export const LayersIcon = make(['m12 3 9 5-9 5-9-5 9-5z', 'm3 13 9 5 9-5']);
export const ShieldIcon = make([
  'M12 3 4 6v6c0 4.5 3.4 8.2 8 9 4.6-.8 8-4.5 8-9V6l-8-3z',
  'm9 12 2 2 4-4',
]);
export const BookIcon = make([
  'M4 5a2 2 0 0 1 2-2h13v16H6a2 2 0 0 0-2 2V5z',
  'M4 19a2 2 0 0 1 2-2h13',
]);
export const ScaleIcon = make([
  'M12 3v18',
  'M5 7h14',
  'm5 7-3 7a3 3 0 0 0 6 0L5 7zM19 7l-3 7a3 3 0 0 0 6 0l-3-7z',
  'M8 21h8',
]);
