import { Favicon } from '@rookcom/monarch-react';
import type { ComponentDocs } from '../../../internal/component-docs.js';

const SITES = ['example.com', 'docs.example.org', 'northwind.test', 'contoso.test'];

const DOT =
  'data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNk+M9QDwADhgGAWjR9awAAAABJRU5ErkJggg==';

export default {
  id: 'favicon',
  title: 'Favicon',
  summary: 'A site icon at a fixed size, with a letter tile when there is no safe icon.',
  sections: {
    purpose: {
      summary:
        'Shows which website a row, result or link belongs to. Only safe image URLs are loaded; anything else, and any image that fails, becomes a letter tile of the same size. Needed by: Ranker (sites, rivals, ranking results).',
      useWhen: [
        'A list of URLs or domains where a small icon makes rows easier to scan.',
        'A site picker or result row that already writes the site name next to the icon.',
      ],
      avoidWhen: [
        'Showing a person or workspace: use Avatar.',
        'Showing a Rook product: use the product mark from the shell.',
      ],
    },
    designPrinciples: {
      items: [
        'Never a broken image: unsafe sources and load failures fall back to a letter tile at the same size, so rows do not shift.',
        'Monarch never calls a third-party icon service on its own. Pass `src`, or `resolveSrc` to build URLs from your own proxy.',
        'Decorative by default, because the site name is written beside it; give `label` when the icon stands alone.',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: [
        'Renders a `<span>` (ref) holding an `<img>` or the letter. Other span attributes pass through.',
        '`isSafeFaviconSrc(src)`, `faviconHostname(urlOrHost)` and `faviconLetter(name)` are exported for products that build their own rows.',
      ],
    },
    typedProperties: {
      source: 'typescript',
      notes: [
        'Migrating from the app copy (Ranker `Favicon`): `url` → `domain`; `size` is unchanged; `className` is unchanged. The app built a Google favicon URL itself; pass the same function as `resolveSrc={(host, px) => …}` if the product keeps that service. The globe fallback is now a letter tile.',
      ],
    },
    variants: {
      items: [
        {
          prop: 'shape',
          values: ['rounded', 'circle', 'square'],
          description:
            'Corner shape of the image and the letter tile (`data-shape`). Default `rounded`.',
        },
        {
          prop: 'size',
          values: ['number (px)'],
          description:
            'Rendered size; the image URL from `resolveSrc` is asked for at twice the size. Default 16.',
        },
      ],
    },
    states: {
      items: [
        {
          name: 'Image',
          attribute: 'data-state="image"',
          description: 'A safe icon URL is loading or loaded.',
        },
        {
          name: 'Fallback',
          attribute: 'data-state="fallback"',
          description:
            'No safe URL, or the image failed: a muted tile with the first letter or digit of `name` or the host name.',
        },
      ],
    },
    interaction: {
      items: [],
      notes: ['Not interactive. Wrap it in a link or button when the row is.'],
    },
    keyboard: { items: [], notes: ['Not focusable.'] },
    accessibility: {
      role: 'none (decorative) or img',
      name: '`label`, when given; otherwise hidden with `aria-hidden` and `alt=""`.',
      items: [
        'The image is requested with `referrerPolicy="no-referrer"` and lazy loading.',
        'Images that failed before hydration are detected after mount and replaced by the letter.',
      ],
    },
    responsive: {
      items: ['Fixed size; it never stretches or shrinks inside flex rows (`flex-shrink: 0`).'],
    },
    theme: {
      items: [
        'The letter tile uses `--mn-color-bg-muted`, `--mn-color-border-subtle` and `--mn-color-text-muted`, so it follows every theme.',
        'Forced colours: the tile uses Canvas and CanvasText.',
      ],
    },
    examples: {
      items: [
        {
          title: 'Letter fallbacks in a list',
          stack: true,
          render: () => (
            <ul style={{ listStyle: 'none', margin: 0, padding: 0, display: 'grid', gap: 8 }}>
              {SITES.map((site) => (
                <li key={site} style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
                  <Favicon domain={site} />
                  {site}
                </li>
              ))}
            </ul>
          ),
        },
        {
          title: 'Sizes and shapes',
          render: () => (
            <>
              <Favicon domain="example.com" size={16} />
              <Favicon domain="example.com" size={24} shape="circle" />
              <Favicon domain="example.com" size={32} shape="square" />
              <Favicon domain="example.com" size={32} src={DOT} label="Example" />
            </>
          ),
        },
        {
          title: 'Icons from your own proxy',
          render: () => (
            <Favicon
              domain="https://example.org/pricing"
              size={28}
              resolveSrc={(host, px) => `/icons/${encodeURIComponent(host)}?size=${px}`}
            />
          ),
        },
      ],
    },
    doDont: {
      do: [
        'Write the site name next to the icon.',
        'Serve icons through your own proxy with `resolveSrc` if you need real icons for any domain.',
      ],
      dont: [
        'Do not pass user-supplied SVG or `data:` URLs expecting them to load; only raster `data:image` URLs are accepted.',
        'Do not use it as the only way to tell sites apart.',
      ],
    },
    related: {
      items: [
        { id: 'avatar', reason: 'People and workspaces.' },
        { id: 'link-card', reason: 'A link preview that includes a site icon.' },
      ],
    },
    dependencies: { packages: ['react'], components: [] },
    testing: {
      unit: [
        'Decorative by default; an `img` role and name with `label`.',
        'Builds the URL with `resolveSrc` at twice the size.',
        'Falls back to the letter when the image fails.',
        'Rejects `javascript:`, SVG and HTML data URLs, `blob:` and other schemes.',
        'Helpers: hostname, letter and safe-source checks.',
      ],
      axe: true,
      ssr: true,
    },
  },
} satisfies ComponentDocs;
