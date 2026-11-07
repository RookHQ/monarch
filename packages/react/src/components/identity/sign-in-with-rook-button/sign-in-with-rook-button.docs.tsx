import { SignInWithRookButton } from '@rookcom/monarch-react/patterns';
import type { ComponentDocs } from '../../../internal/component-docs.js';

function Surfaces() {
  return (
    <div style={{ display: 'flex', flexWrap: 'wrap', gap: 16 }}>
      <div data-mn-theme="light" style={{ padding: 16, background: 'var(--mn-color-bg-surface)' }}>
        <SignInWithRookButton variant="light" />
      </div>
      <div data-mn-theme="dark" style={{ padding: 16, background: 'var(--mn-color-bg-canvas)' }}>
        <SignInWithRookButton variant="dark" />
      </div>
      <div style={{ padding: 16 }}>
        <SignInWithRookButton variant="neutral" />
      </div>
    </div>
  );
}

export default {
  id: 'sign-in-with-rook-button',
  title: 'SignInWithRookButton',
  summary: 'The "Sign in with Rook ID" button, as a button or a link, in three fixed looks.',
  sections: {
    purpose: {
      summary:
        'Sites that offer Rook ID sign-in need one recognisable button that looks the same everywhere. SignInWithRookButton renders it as a native button or a link, in light, dark and neutral looks and three sizes, with an optional mark and a loading state. Needed by: Rook Developers (identity docs and the button page), and any app or example that starts a Rook ID sign-in.',
      useWhen: [
        'A sign-in page or dialog that offers Rook ID.',
        'A docs page or example integration showing the official button.',
      ],
      avoidWhen: [
        'Any other primary action: use Button.',
        'Signing in with a password or code on the same page: use a form with Button.',
      ],
    },
    designPrinciples: {
      items: [
        'Recognisable everywhere: `light` and `dark` set `data-mn-theme` on the button, so its neutral tokens resolve to that theme whatever the page uses. No hex colours are involved.',
        'The label names the provider ("Sign in with Rook ID"), never a bare "Sign in".',
        'The mark is decorative because the label already says Rook ID.',
        'A link when the flow starts with a plain GET, a button when the page runs code first.',
      ],
    },
    apiReference: { source: 'typescript' },
    typedProperties: {
      source: 'typescript',
      notes: [
        'Pass `href` to render an `<a>`; otherwise it is a `<button type="button">`. Other props go to that element.',
        'Migrating from the app copy (Rook Developers `SignInWithRookIdButton`): `variant` and `size` keep their values; `markSrc` keeps its meaning but has no default, so pass your own copy of the mark. The fixed `ROOK_ID_BUTTON_CSS` is replaced by the stylesheet and theme tokens; the copy-paste HTML snippet (`rookIdButtonHtml`) for non-React sites stays in the product.',
      ],
    },
    variants: {
      items: [
        {
          prop: 'variant',
          values: ['light', 'dark', 'neutral', 'auto'],
          description:
            '`light` on light pages, `dark` on dark pages, `neutral` (borderless grey) on either; `auto` follows the page theme.',
        },
        { prop: 'size', values: ['sm', 'md', 'lg'], description: '32, 40 and 48px tall.' },
        { prop: 'block', values: ['false', 'true'], description: 'Fill the container width.' },
      ],
    },
    states: {
      items: [
        {
          name: 'Variant and size',
          attribute: 'data-variant, data-size',
          description: 'The look.',
        },
        {
          name: 'Pinned theme',
          attribute: 'data-mn-theme',
          description: '`light` or `dark`; absent for `auto`.',
        },
        {
          name: 'Loading',
          attribute: 'data-loading, aria-busy',
          description: 'A spinner replaces the mark and clicks are ignored.',
        },
        {
          name: 'Disabled',
          attribute: 'disabled',
          description: 'Dimmed, not clickable (button only).',
        },
      ],
    },
    interaction: {
      items: [
        'Clicking runs `onClick`, or follows `href` for a link.',
        'While `loading`, clicks do nothing, so a sign-in cannot start twice.',
      ],
    },
    keyboard: {
      items: [
        { keys: 'Tab', action: 'Focuses the button or link.' },
        { keys: 'Enter', action: 'Activates it.' },
        { keys: 'Space', action: 'Activates it (button only).' },
      ],
    },
    accessibility: {
      role: 'button, or link with `href`',
      name: 'The label ("Sign in with Rook ID").',
      items: [
        'The mark is hidden from assistive technology.',
        '`aria-busy` while loading; a loading link also sets `aria-disabled`.',
        'Visible focus ring from the focus-ring token.',
      ],
    },
    responsive: {
      items: [
        'Never wider than its container; a long translated label truncates with an ellipsis.',
        'At least 44px tall on touch screens; use `block` for full-width mobile layouts.',
      ],
    },
    theme: {
      items: [
        'Colours come from the neutral surface, text and border tokens of the pinned theme.',
        'More-contrast themes keep their stronger borders because the same tokens are used.',
        'In forced-colors mode it uses ButtonFace and ButtonText with a system border.',
      ],
    },
    examples: {
      items: [
        { title: 'On light, dark and any surface', render: () => <Surfaces /> },
        {
          title: 'Sizes',
          render: () => (
            <>
              <SignInWithRookButton size="sm" />
              <SignInWithRookButton size="md" />
              <SignInWithRookButton size="lg" />
            </>
          ),
        },
        {
          title: 'As a link, and loading',
          render: () => (
            <>
              <SignInWithRookButton href="#sign-in" />
              <SignInWithRookButton loading />
            </>
          ),
        },
      ],
    },
    doDont: {
      do: [
        'Host your own copy of the mark and pass it as `markSrc` or `mark`.',
        'Pick `light` or `dark` to match the surface behind the button.',
        'Set `loading` while the sign-in redirect is being prepared.',
      ],
      dont: [
        'Do not restyle it with product colours; it must look the same on every site.',
        'Do not change the label to something that hides the provider.',
      ],
    },
    related: {
      items: [
        { id: 'button', reason: 'Every other action.' },
        { id: 'connected-apps-list', reason: 'Where people see the apps they signed in to.' },
      ],
    },
    dependencies: {
      packages: ['react'],
      components: ['spinner'],
      notes: [
        'Relies on the theme blocks for `[data-mn-theme="light"]` and `[data-mn-theme="dark"]` in the tokens stylesheet.',
      ],
    },
    testing: {
      unit: [
        'A named button with type="button" that pins its theme; Enter activates; axe passes.',
        'Variants map to data-mn-theme; sizes to data-size.',
        'Renders a named link with href; axe passes.',
        'Blocks clicks while loading and sets aria-busy.',
        'Custom mark element is decorative.',
        'Renders on the server.',
      ],
      axe: true,
      ssr: true,
    },
  },
} satisfies ComponentDocs;
