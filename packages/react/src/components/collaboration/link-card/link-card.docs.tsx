import { useState } from 'react';
import { Button } from '@rookcom/monarch-react';
import {
  LinkCard,
  LinkCardList,
  LinkCardSkeleton,
  type LinkCardPreview,
} from '@rookcom/monarch-react/collaboration';
import type { ComponentDocs } from '../../../internal/component-docs.js';

/** A neutral inline picture, so the examples need no network. */
const PICTURE =
  'data:image/svg+xml,' +
  encodeURIComponent(
    '<svg xmlns="http://www.w3.org/2000/svg" width="1200" height="630"><rect width="1200" height="630" fill="gainsboro"/><circle cx="930" cy="200" r="110" fill="silver"/><path d="M0 630 L380 300 L640 520 L860 380 L1200 630Z" fill="darkgray"/></svg>',
  );

const ARTICLE: LinkCardPreview = {
  url: 'https://example.com/guides/first-week',
  title: 'Your first week: a practical guide',
  description: 'What to set up, who to meet and where to find things in your first five days.',
  siteName: 'Example Guides',
  image: { src: PICTURE, width: 1200, height: 630 },
};

const VIDEO: LinkCardPreview = {
  url: 'https://video.example.org/watch/intro',
  title: 'Product walkthrough in four minutes',
  siteName: 'Example Video',
  image: { src: PICTURE, width: 1280, height: 720 },
  video: true,
};

const PREVIEWS: LinkCardPreview[] = [ARTICLE, VIDEO];

function DismissDemo() {
  const [previews, setPreviews] = useState(PREVIEWS);
  return (
    <div style={{ display: 'grid', gap: 'var(--mn-space-2)', width: '100%' }}>
      <LinkCardList
        previews={previews}
        onDismiss={(url) => setPreviews((list) => list.filter((p) => p.url !== url))}
      />
      {previews.length < PREVIEWS.length ? (
        <Button size="sm" variant="secondary" onClick={() => setPreviews(PREVIEWS)}>
          Bring the previews back
        </Button>
      ) : null}
    </div>
  );
}

export default {
  id: 'link-card',
  title: 'LinkCard',
  summary: 'A preview of a shared link: picture, site, title and description, as one link.',
  sections: {
    purpose: {
      summary:
        'Shows what a link in a message or post points to before anyone opens it: the page’s picture, site name, title and description, all as one link. The data comes in already unfurled by the product’s server; the card never fetches. LinkCardList stacks the cards of one message and LinkCardSkeleton holds their space while a preview is still being fetched. Needed by: Chat (message link previews); also fits Base posts and Tickets replies.',
      useWhen: [
        'A message, post or comment contains a link the server has unfurled.',
        'People should see where a link leads without opening it.',
      ],
      avoidWhen: [
        'A link to a record inside the product with its own actions: build a product card and pass it as `footer`, or use a dedicated card.',
        'Embedding playable media: use a media player; this card only links out.',
        'A plain inline link in text: use an ordinary link.',
      ],
    },
    designPrinciples: {
      items: [
        'One link, one tab stop: picture, site, title and description are a single link named by its text.',
        'No layout jumps: the picture keeps its space while it loads, and a skeleton holds the space of a preview still on its way.',
        'Odd pictures never dominate: the picture ratio is kept between 1.2 and 2.2.',
        'Safe by default: only http(s) and mailto links and safe image sources render; anything else shows as plain text.',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: [
        '`LinkCard` takes one `LinkCardPreview` spread as props. `LinkCardList` takes `previews` and passes `size`, `onDismiss(url)` and `messages` to each card; `pending` adds a skeleton.',
        '`linkCardImageRatio(width, height)` is the clamped picture ratio. `LINK_CARD_MESSAGES` holds the English defaults.',
        'For images behind authentication, give `image.src` a URL the browser can load (for example a short-lived signed URL); the card does not attach credentials.',
      ],
    },
    typedProperties: {
      source: 'typescript',
      notes: [
        'Migrating from the app copy (Chat `chat/messages/LinkCards.tsx`): `LinkCards({ previews, compact, onHide })` → `LinkCardList({ previews, size: compact ? "sm" : "md", onDismiss })`. Each `LinkPreview` maps directly (`url`, `title`, `description`, `siteName`, `video`), with `image.url` → `image.src` (resolve it with the product’s `apiUrl` and authed-image hook first). `onHide()` now receives the URL. `LinkCardSkeleton({ compact })` → `LinkCardSkeleton({ size })`, or `LinkCardList pending`. `useLinkCardPending` and `cardWaitMs` stay in the product; pass their result as `pending`. `cardRatio` → `linkCardImageRatio`.',
      ],
    },
    variants: {
      items: [
        {
          prop: 'size',
          values: ['md', 'sm'],
          description:
            '`md` (default) is up to 27.5rem wide, for the main timeline; `sm` is up to 20rem with tighter padding, for threads and side panels.',
        },
      ],
    },
    states: {
      items: [
        {
          name: 'Picture loading',
          attribute: 'data-state="loading" on the media',
          description: 'The space is reserved at the picture’s ratio; the picture fades in.',
        },
        {
          name: 'Picture failed',
          description: 'The picture is removed and the card shows its text only.',
        },
        {
          name: 'Video',
          attribute: 'data-video on the root',
          description: 'A play mark on the picture and "Video" in the link name.',
        },
        {
          name: 'Unsafe link',
          attribute: 'data-inert on the body',
          description: 'Shown as text, not a link.',
        },
        { name: 'Pending', description: 'LinkCardSkeleton, hidden from assistive technology.' },
      ],
    },
    interaction: {
      items: [
        'Activating the card opens the link, in a new tab by default.',
        'With `onDismiss`, a remove button appears on hover or focus (always on touch screens).',
        '`linkProps` passes extra props to the link, such as an `onClick` for analytics.',
      ],
    },
    keyboard: {
      items: [
        { keys: 'Tab', action: 'Moves to the card’s link, then to its remove button.' },
        { keys: 'Enter', action: 'Opens the link, or removes the preview from the button.' },
      ],
    },
    accessibility: {
      role: 'link',
      name: 'Site name, title and description, plus "(opens in a new tab)" and "Video" where they apply',
      items: [
        'The picture is decorative (`alt=""`) unless `image.alt` is given; the favicon is decorative.',
        'The remove button is named "Remove the preview of …" (messages.dismiss).',
        'The skeleton is `aria-hidden`; announce the arrival of previews in the message, not the card.',
      ],
    },
    responsive: {
      items: [
        'Cards fill their container up to their maximum width, so they fit a 390px screen.',
        'Titles and descriptions clamp to two lines; long words break.',
        'On coarse pointers the remove button is always visible with a 44px target.',
      ],
    },
    theme: {
      items: [
        'Card: `--mn-color-bg-surface`, `--mn-color-border-default`, `--mn-radius-lg`; hover `--mn-color-bg-subtle`.',
        'Picture placeholder `--mn-color-bg-muted`; play mark `--mn-color-bg-inverse` and `--mn-color-text-inverse`.',
        'In forced colours the card and buttons get CanvasText borders; transitions stop under reduced motion.',
      ],
    },
    examples: {
      items: [
        { title: 'Link preview', render: () => <LinkCard {...ARTICLE} /> },
        { title: 'Video, small', render: () => <LinkCard {...VIDEO} size="sm" /> },
        {
          title: 'Text only',
          render: () => (
            <LinkCard
              url="https://example.com/changelog"
              title="Changelog"
              description="Every change, newest first."
            />
          ),
        },
        { title: 'Removable list', stack: true, render: () => <DismissDemo /> },
        { title: 'Loading', render: () => <LinkCardSkeleton /> },
      ],
    },
    doDont: {
      do: [
        'Unfurl on the server and pass the result; keep the fetching and caching in the product.',
        'Let the author of a message remove its previews.',
        'Use `size="sm"` in threads and narrow panels.',
      ],
      dont: [
        'Do not put buttons inside the link; use `footer` for extra actions.',
        'Do not show more than three previews for one message.',
        'Do not hot-link pictures from third-party sites; serve them through the product.',
      ],
    },
    related: {
      items: [
        { id: 'message-row', reason: 'The message the cards sit under.' },
        { id: 'favicon', reason: 'The site icon.' },
        { id: 'skeleton', reason: 'The loading placeholder.' },
      ],
    },
    dependencies: { packages: ['react'], components: ['favicon', 'skeleton'] },
    testing: {
      unit: [
        'Clamped picture ratio.',
        'One link with the right name, target and rel; site name.',
        'Picture loading, loaded, failed; video mark and name.',
        'Unsafe links and images; host name fallback; same-tab links.',
        'Remove button by keyboard; the list passes the URL; skeleton is hidden; nothing renders without previews.',
      ],
      axe: true,
      ssr: true,
    },
  },
} satisfies ComponentDocs;
