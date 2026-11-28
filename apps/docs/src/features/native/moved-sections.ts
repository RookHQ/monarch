/**
 * Sections that moved to another page, by the page they were on and their
 * id. `/ios` and `/android` were one long page each; they are now landing
 * pages, and their sections are on `/<platform>/get-started` and
 * `/<platform>/guide` under the same ids. An old link such as
 * `/ios/#installation` is sent to the section's new page by the landing page
 * (platform-landing.tsx), so the entry chunk does not carry the list.
 */
const GET_STARTED_SECTIONS = [
  'requirements',
  'installation',
  'getting-started',
  'theme-setup',
  'tokens',
];
const GUIDE_SECTIONS = [
  'foundation',
  'philosophy',
  'components',
  'previews',
  'examples',
  'accessibility',
  'ipad',
  'adaptive-layouts',
  'limitations',
  'reference',
  'releases',
  'platform-terms',
  'elsewhere',
];
const movedSections = (platform: string): Record<string, string> => ({
  ...Object.fromEntries(GET_STARTED_SECTIONS.map((id) => [id, `/${platform}/get-started`])),
  ...Object.fromEntries(GUIDE_SECTIONS.map((id) => [id, `/${platform}/guide`])),
  // The reference entries, `#reference-theme` and so on.
  'reference-*': `/${platform}/guide`,
});
export const MOVED_SECTIONS: Readonly<Record<string, Readonly<Record<string, string>>>> = {
  ios: movedSections('ios'),
  android: movedSections('android'),
};

/** The page a section of the old one-page `/<platform>/` moved to, or null. */
export function movedSection(platform: string, anchor: string | null): string | null {
  const moved = MOVED_SECTIONS[platform];
  if (!moved || !anchor) return null;
  if (moved[anchor]) return moved[anchor];
  return anchor.startsWith('reference-') ? (moved['reference-*'] ?? null) : null;
}
