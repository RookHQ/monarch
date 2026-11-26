import { Button, ErrorPage, Link } from '@rookcom/monarch-react';
import type { ComponentDocs } from '../../../internal/component-docs.js';

export default {
  id: 'error-page',
  title: 'ErrorPage',
  summary: 'A whole page failed: not found, forbidden, server error, offline or maintenance.',
  sections: {
    purpose: {
      summary:
        'The page shown when a route cannot render: a 404, a 403, a server error, no connection or planned maintenance. It works on its own, without the app shell, so it can back root not-found and error routes.',
      useWhen: [
        'A route does not exist or a record behind it is gone (`not-found`).',
        'The person may not open the page (`forbidden`).',
        'The page crashed or the server failed (`server`, the default).',
        'The app cannot reach the network (`offline`).',
        'The service is down on purpose (`maintenance`).',
      ],
      avoidWhen: [
        'Only one region of the page failed: use ErrorState or InlineErrorRetry.',
        'The page loaded and is simply empty: use EmptyState.',
      ],
    },
    designPrinciples: {
      items: [
        'The product supplies every word: title, description, actions and the support contact. The component supplies the layout, icon and numeral.',
        'Never show a raw error message, stack, vendor or technology name; offer the next step instead.',
        'A large, faint numeral (404, 403, 500) stands in for an illustration, so no artwork is needed; connectivity and maintenance have none.',
        'One primary action (retry, or the product’s way home) and at most one secondary.',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: [
        'The ref is forwarded to the root element (`main`, `section` or `div`). Native attributes pass through.',
        'Actions are the product’s own elements: pass Buttons, or Button `asChild` around your router’s link.',
        'Use `errorKindFromStatus` (exported with ErrorState) to pick a kind from an HTTP status; `maintenance` is always the product’s decision.',
      ],
    },
    typedProperties: { source: 'typescript' },
    variants: {
      items: [
        {
          prop: 'kind',
          values: ['not-found', 'forbidden', 'server', 'offline', 'maintenance'],
          description:
            'Default icon, icon tint and numeral (404, 403 and 500; none for offline and maintenance). Exposed as `data-kind`.',
        },
        {
          prop: 'fullScreen',
          values: ['true', 'false'],
          description:
            '`true` (default) fills the viewport with the canvas background and its own font, for pages without the app shell; `false` sits inside the shell’s content area. `true` is exposed as `data-full-screen`.',
        },
        {
          prop: 'as',
          values: ['main', 'section', 'div'],
          description:
            'Root element. Default `main` when full screen, `section` otherwise, so it never nests a second main landmark.',
        },
        {
          prop: 'numeral',
          values: ['string', 'false'],
          description: 'Overrides the kind’s numeral (e.g. "502"), or `false` to hide it.',
        },
      ],
    },
    states: {
      items: [
        {
          name: 'Retrying',
          attribute: 'aria-busy="true" on the retry button',
          description: '`retrying` shows the Button loading state and blocks repeat clicks.',
        },
        {
          name: 'Waiting for connection',
          attribute: 'data-kind="offline"',
          description:
            'With `onRetry` and `retryWhenOnline` (default), the page retries once when the browser fires `online`.',
        },
      ],
    },
    interaction: {
      items: [
        'The retry button (when `onRetry` is set) comes first and calls `onRetry`.',
        'Offline pages retry automatically once when the connection returns; set `retryWhenOnline={false}` to opt out.',
        '`focusOnMount` moves focus to the title after a client-side navigation, so screen readers start at the error.',
        'The request-id copy button copies the id and announces the result.',
      ],
    },
    keyboard: {
      items: [
        {
          keys: 'Tab / Shift+Tab',
          action: 'Moves through retry, the product’s actions, the copy button and support links.',
        },
        { keys: 'Enter / Space', action: 'Activates the focused action.' },
      ],
      notes: ['The title is focusable only by script (`tabIndex=-1`), for `focusOnMount`.'],
    },
    accessibility: {
      role: 'main (full screen) or region, labelled by the title',
      name: 'From the title, through `aria-labelledby` with a generated id, so two pages never share an id.',
      items: [
        'The title is an h1 by default (`headingLevel="h2"` inside a shell that already has an h1).',
        'The numeral is drawn by CSS (`::before` from `data-numeral`) and, like the icon, `aria-hidden`; say the same thing in the title ("Page not found", not "404").',
        'No live region: the page is the content. Use `focusOnMount` after client-side navigation.',
        'Verified with axe in jsdom and in Chromium, Firefox and WebKit on /components/error-page.',
      ],
    },
    responsive: {
      items: [
        'Content is centred in a 30rem column; the numeral scales with the viewport (`clamp`).',
        'Below 30rem the title steps down a size and actions stack full width.',
        'Full screen uses `100dvh`, so mobile browser toolbars do not cut it off.',
      ],
    },
    theme: {
      items: [
        '`not-found` uses the accent tile; `server` danger; `offline` warning; `forbidden` and `maintenance` the muted surface.',
        'Full screen sets `--mn-color-bg-canvas`, `--mn-color-text-default` and `--mn-font-sans` itself, so it looks right without the app shell.',
        'Colours are Monarch semantic tokens only, so light, dark and more-contrast themes apply automatically.',
      ],
    },
    examples: {
      items: [
        {
          title: 'Not found, inside the shell',
          stack: true,
          render: () => (
            <ErrorPage
              kind="not-found"
              fullScreen={false}
              headingLevel="h2"
              title="Page not found"
              description="The link may be broken, or the page may have moved."
              actions={
                <>
                  <Button variant="primary" asChild>
                    <a href="#/">Go to dashboard</a>
                  </Button>
                  <Button>Go back</Button>
                </>
              }
            />
          ),
        },
        {
          title: 'Server error with support',
          stack: true,
          render: () => (
            <ErrorPage
              kind="server"
              fullScreen={false}
              headingLevel="h2"
              title="Something went wrong"
              description="We couldn't open this page. Try again in a moment."
              onRetry={() => {}}
              requestId="req_5c81e0a4"
              support={
                <>
                  Still stuck? <Link href="mailto:support@example.com">Contact support</Link>
                </>
              }
            />
          ),
        },
        {
          title: 'Offline and maintenance, without the shell',
          stack: true,
          render: () => (
            <>
              <ErrorPage
                as="div"
                kind="offline"
                headingLevel="h2"
                style={{ minHeight: '24rem' }}
                brand={<strong>Acme</strong>}
                title="You're offline"
                description="Check your connection. We'll try again when you're back online."
                onRetry={() => {}}
              />
              <ErrorPage
                as="div"
                kind="maintenance"
                headingLevel="h2"
                style={{ minHeight: '24rem' }}
                brand={<strong>Acme</strong>}
                title="Down for maintenance"
                description="We're making some improvements and will be back shortly."
              />
            </>
          ),
        },
      ],
    },
    doDont: {
      do: [
        'Write a title that says what happened without the code ("Page not found").',
        'Give a way forward: retry, home, or back.',
        'Pass your support contact through `support` and the request id when you have one.',
        'Use `fullScreen={false}` when the shell can still render around the error.',
      ],
      dont: [
        'Do not show raw error messages, stack traces or vendor names.',
        'Do not offer retry on a 404 or 403.',
        'Do not render a full-screen ErrorPage inside another `main`; set `as="section"` or `fullScreen={false}`.',
      ],
    },
    related: {
      items: [
        { id: 'error-state', reason: 'One region failed, not the whole page.' },
        { id: 'inline-error-retry', reason: 'A small section failed inside a working page.' },
        { id: 'empty-state', reason: 'The page works but has nothing to show.' },
      ],
    },
    dependencies: { packages: ['react'], components: ['button', 'copy-button', 'error-state'] },
    testing: {
      unit: [
        'Full screen renders a main landmark labelled by its h1, with an aria-hidden numeral.',
        'Numerals default by kind and can be overridden or hidden.',
        'Inside a shell it renders a labelled section, never a second main; each instance has its own title id.',
        'Retry comes first, is keyboard operable and shows a busy state.',
        'Offline retries once on the `online` event, and not when `retryWhenOnline` is false.',
        '`focusOnMount` focuses the title; brand, support and request id render; the ref is forwarded.',
        '`as="div"` drops `aria-labelledby` (not allowed on a generic element).',
      ],
      axe: true,
      ssr: true,
      browser: ['Visual baselines: /__gallery', 'Cross-browser axe: /components/error-page'],
    },
  },
} satisfies ComponentDocs;
