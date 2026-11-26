import { WorldMap } from '@rookcom/monarch-react/charts';
import type { ComponentDocs } from '../../../internal/component-docs.js';

const byCountry = [
  { country: 'US', value: 1240, details: [{ label: 'Users', value: 3820 }] },
  { country: 'GB', value: 410, details: [{ label: 'Users', value: 1190 }] },
  { country: 'DE', value: 380, details: [{ label: 'Users', value: 1020 }] },
  { country: 'IN', value: 350, details: [{ label: 'Users', value: 1460 }] },
  { country: 'BR', value: 210, details: [{ label: 'Users', value: 540 }] },
  { country: 'FR', value: 190, details: [{ label: 'Users', value: 470 }] },
  { country: 'JP', value: 120, details: [{ label: 'Users', value: 300 }] },
  { country: 'AU', value: 95, details: [{ label: 'Users', value: 210 }] },
  { country: 'ZA', value: 40, details: [{ label: 'Users', value: 88 }] },
  { country: 'SG', value: 60, details: [{ label: 'Users', value: 150 }] },
];

export default {
  id: 'world-map',
  title: 'WorldMap',
  summary:
    'Values by country on a simplified world outline — a choropleth or bubbles — in ChartFrame, with a legend, tiles for non-country values, keyboard inspection and a data table.',
  sections: {
    purpose: {
      summary:
        'Shows where something happens — accounts, visitors, domains by country — with the ranking and every value also available as text. Needed by: Atlas (domain footprint), Links (analytics by country).',
      useWhen: [
        'Values for many countries where geography helps people read the spread.',
        'A dashboard card that pairs the map with its ranked table ("View as table").',
      ],
      avoidWhen: [
        'A few countries, or when the ranking is the point: use BarList.',
        'Precise locations, routes or zooming into regions: use a mapping service.',
        'Choosing a country: use the country fields in LocationFields.',
      ],
    },
    designPrinciples: {
      items: [
        'No map library and no network request: a simplified outline (Natural Earth 1:110m, public domain, about 35 KB) ships as inline SVG paths keyed by ISO 3166-1 alpha-2 code, with centroids from 1:50m for small countries.',
        'The outline is simplified for showing values, not for navigation, and takes no position on disputed borders.',
        'One palette colour in four steps (shared with Heatmap); countries without a value stay muted. The bubble variant sizes circles by area on a neutral map.',
        'Values that belong to no country ("Global", "EU") are tiles under the map, and codes the outline does not have are listed in a note, so nothing silently disappears.',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: [
        'Takes every ChartFrame prop except `children`, `legend`, `table` and `height`; the ref goes to the `<figure>`.',
        'Exported from `@rookcom/monarch-react/charts`, with the outline data `WORLD_MAP_SHAPES`, `WORLD_MAP_CENTROIDS` and `WORLD_MAP_VIEWBOX` (equirectangular, 1000 × 394, latitude 84° to −58°).',
      ],
    },
    typedProperties: {
      source: 'typescript',
      notes: [
        'Country codes are ISO 3166-1 alpha-2, case-insensitive; Kosovo is `XK`. Northern Cyprus and Somaliland are drawn as part of `CY` and `SO`. Countries too small for the outline (Singapore, Malta…) get a dot at their centroid.',
        'The data was generated once from world-atlas@2 `countries-110m.json` / `countries-50m.json` with topojson-client and i18n-iso-countries (projection, Douglas–Peucker at 0.5 units, rounding); the method is in the header of `world-map.data.ts`.',
        'Migrating from the app copy: Atlas `WorldMap` `data: { country, count }` → `data: { country, value }` with `variant="bubble"`; its `GLOBAL` and `EU` buckets → `extras`; the unplaced count is shown automatically; `worldMap.data.ts` and the generator script can be deleted. Links `CountryWorkspaceMap` resolves country names to ISO codes in the app (as today), passes `value: workspaces` and `details: [{ label: "Users", value: users }]`, and drops react-simple-maps and the CDN fetch of the outline.',
      ],
    },
    variants: {
      items: [
        {
          prop: 'variant',
          values: ['choropleth', 'bubble'],
          description:
            'Fill countries by step, or circles sized by value. Exposed as `data-variant`.',
        },
        {
          prop: 'color',
          values: ['1', '2', '3', '4', '5', 'neutral'],
          description: 'Palette slot of the ramp or bubbles. Default 1.',
        },
        {
          prop: 'status',
          values: ['ready', 'loading', 'empty', 'error'],
          description: 'ChartFrame states, exposed as `data-status` on the figure.',
        },
      ],
    },
    states: {
      items: [
        {
          name: 'Level',
          attribute: 'data-level="0"…"4"',
          description: "A country's step (choropleth).",
        },
        {
          name: 'Inspected',
          attribute: 'data-active',
          description: 'Outlined country or bubble with its tooltip.',
        },
        {
          name: 'Selectable',
          attribute: 'data-selectable',
          description: 'Set when `onCountrySelect` is given: countries with values show a pointer.',
        },
        {
          name: 'Empty',
          attribute: 'data-status="empty"',
          description: 'Automatically when there are no values or extras.',
        },
      ],
    },
    interaction: {
      items: [
        'Hovering or tapping a country with a value outlines it and shows its name, value and details.',
        "Click, Enter or Space calls `onCountrySelect` with the country's datum, for drill-in.",
        'Countries without a value do not react.',
      ],
    },
    keyboard: {
      items: [
        { keys: 'Tab', action: 'Focuses the map and inspects the country with the largest value.' },
        {
          keys: 'Arrow keys',
          action: 'Next or previous country, in order of value (largest first).',
        },
        { keys: 'Home / End', action: 'Largest or smallest value.' },
        { keys: 'PageUp / PageDown', action: 'Move a tenth of the list.' },
        { keys: 'Enter / Space', action: 'Select the inspected country.' },
        { keys: 'Escape', action: 'Hide the tooltip.' },
      ],
      notes: [
        'Keyboard order follows the values rather than geography, so the most important countries come first.',
      ],
    },
    accessibility: {
      role: 'figure; the map is role="group" with aria-roledescription "map"',
      name: 'The figure is labelled by `title`; the map by the generated summary ("10 countries. Highest: United States 1,240, United Kingdom 410, Germany 380.") or `summary`.',
      items: [
        'Keyboard inspection is announced in a polite live region ("Germany: 380, Users 1020").',
        'The data table lists every country, its details and the extras; it is always in the page and visible through "View as table".',
        'Country names come from `Intl.DisplayNames` in `locale` (or `countryName`), so they are translated.',
        'Every word is replaceable through `labels`.',
      ],
    },
    responsive: {
      items: [
        'The map scales with its container (`viewBox`, width 100%), so it fits 390px with no horizontal scrolling; extras wrap under it.',
      ],
    },
    theme: {
      items: [
        'Fills mix `--mn-color-data-*` into `--mn-color-bg-muted` (the Heatmap ramp); land without values is `--mn-color-bg-muted`, borders `--mn-color-bg-surface`.',
        'Forced colours keep the fills with CanvasText borders, draw land without values as Canvas and outline the inspected country in Highlight.',
        'The only transition is removed under reduced motion.',
      ],
    },
    examples: {
      items: [
        {
          title: 'Choropleth with details and extras',
          stack: true,
          render: () => (
            <WorldMap
              title="Workspaces by country"
              description="Last 30 days"
              data={byCountry}
              extras={[{ key: 'global', label: 'Global', value: 75 }]}
              locale="en-US"
              style={{ width: '100%', maxWidth: '44rem' }}
            />
          ),
        },
        {
          title: 'Bubbles',
          stack: true,
          render: () => (
            <WorldMap
              title="Domains by country"
              variant="bubble"
              color={2}
              data={byCountry.map(({ country, value }) => ({ country, value }))}
              extras={[
                { key: 'global', label: 'Global', value: 320 },
                { key: 'eu', label: 'EU', value: 18 },
              ]}
              locale="en-US"
              style={{ width: '100%', maxWidth: '44rem' }}
            />
          ),
        },
      ],
    },
    doDont: {
      do: [
        'Pass ISO alpha-2 codes; resolve names or TLDs to codes in the app.',
        'Put values that belong to no country in `extras` instead of dropping them.',
        'Pass `locale` so country names and numbers match the page.',
      ],
      dont: [
        'Do not use the map to rank a few countries: BarList is easier to read.',
        'Do not present the outline as authoritative borders.',
        'Do not use status colours for the ramp.',
      ],
    },
    related: {
      items: [
        { id: 'heatmap', reason: 'The same sequential scale for two categories.' },
        { id: 'bar-list', reason: 'Ranked values by country.' },
        { id: 'location-fields', reason: 'Picking a country and region.' },
        { id: 'chart-frame', reason: 'The container, states and table every chart shares.' },
      ],
    },
    dependencies: { packages: ['react'], components: ['chart-frame', 'heatmap'] },
    testing: {
      unit: [
        'Outline data keyed by alpha-2 codes, with centroids for small countries.',
        'Levels, dots for small countries, unplaced note, legend and summary.',
        'Table with details and extras; extras tiles.',
        'Keyboard by value, End, Enter selection with the outline, Escape; live region.',
        'Hover tooltip with details, click selection, no reaction without a value.',
        'Bubble variant sizes; custom names, thresholds, colour and words; controlled country; empty state; SSR.',
      ],
      axe: true,
      ssr: true,
    },
  },
} satisfies ComponentDocs;
