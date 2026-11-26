/**
 * Public components shipped by @rookcom/monarch-react. A local file with one of
 * these stems under components/ui (or primitives/common) is a duplicate.
 *
 * Generated: the list comes from bundled-data.ts (`npm run registry:conformance`),
 * which reads the component exports recorded from the react manifest, so it can
 * never fall behind the package. Rook-specific exports are left out.
 */
import { BUNDLED_COMPONENTS } from './bundled-data.js';

/** File stems that name a Monarch component in a shorter, older form. */
const LEGACY_STEMS = [
  { name: 'SettingsSection', stem: 'settings' },
  { name: 'ErrorState', stem: 'states' },
] as const;

export const MONARCH_COMPONENTS: readonly { readonly name: string; readonly stem: string }[] = [
  ...BUNDLED_COMPONENTS,
  ...LEGACY_STEMS,
];

export const MONARCH_COMPONENT_STEMS: ReadonlyMap<string, string> = new Map(
  MONARCH_COMPONENTS.map((c) => [c.stem, c.name]),
);
