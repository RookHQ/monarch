import { Button, Field, FieldLabel, Input } from '@rookcom/monarch-react';
import { AuthPage } from '@rookcom/monarch-react/patterns';
import type { ComponentDocs } from '../../../internal/component-docs.js';

const sampleLinks = [
  { label: 'Terms', href: '#terms' },
  { label: 'Privacy', href: '#privacy' },
  { label: 'Help', href: '#help' },
];

function SampleForm({ submit }: { submit: string }) {
  return (
    <form onSubmit={(e) => e.preventDefault()} style={{ display: 'grid', gap: 16 }}>
      <Field>
        <FieldLabel>Email</FieldLabel>
        <Input type="email" autoComplete="email" />
      </Field>
      <Button type="submit" variant="primary" block>
        {submit}
      </Button>
    </form>
  );
}

export default {
  id: 'auth-page',
  title: 'AuthPage',
  summary:
    'The centred layout for sign-in, sign-up and reset screens: brand, heading, the product’s form, footer and legal links.',
  sections: {
    purpose: {
      summary:
        'Gives every sign-in, sign-up, password-reset and verification screen the same frame, so the product only writes the form. Needed by: Rook ID (`rookid/app/auth/page.tsx`, `rookid/app/auth/signup/page.tsx`, `rookid/app/auth/verify`), Tickets help-centre sign-in (`rooktickets-frontend/src/app/support/[slug]/(site)/signin/page.tsx`), and the redirect screens in each app’s `src/app/(auth)/login/page.tsx` (Base, Atlas, People, Stride, Pulse and others).',
      useWhen: [
        'Sign in, create account, reset password, verify email, accept invitation.',
        'The short "Redirecting to sign in…" screen while handing over to Rook ID.',
      ],
      avoidWhen: [
        'Settings for an account that is already signed in: use SettingsPage.',
        'Sign-in inside a dialog over the app: use Dialog with the form.',
      ],
    },
    designPrinciples: {
      items: [
        'Layout only: AuthPage holds no form state, validation or calls; the product owns the form.',
        'One task per screen, one heading per screen.',
        'The side panel is decoration and context only; it is hidden on small screens, so nothing needed to sign in may live there.',
        'Links from data are checked; unsafe brand or footer URLs are dropped.',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: [
        'The ref and native attributes go to the root `<div>`.',
        '`defaultAuthPageMessages` holds the English defaults.',
      ],
    },
    typedProperties: {
      source: 'typescript',
      notes: [
        'Migrating from the Rook ID screens (shadcn/ui `Button`, `Input`, `Label`): the outer `min-h-screen flex` wrapper and the `lg:w-[480px]` brand panel become AuthPage with `aside`; `Label` + `Input` pairs become `Field` + `FieldLabel` + `Input` inside `children`; the "Already have an account?" line becomes `footer`.',
        'Migrating the app login redirect screens: keep the redirect effect, render `<AuthPage title="Signing you in" description="…">` with a Spinner as `children`.',
      ],
    },
    variants: {
      items: [
        {
          prop: 'variant',
          values: ['card', 'plain'],
          description: 'Form on a raised card, or straight on the page background.',
        },
        {
          prop: 'asidePosition',
          values: ['start', 'end'],
          description: 'Side of the optional panel on wide screens.',
        },
        {
          prop: 'headingLevel',
          values: ['h1', 'h2'],
          description: '`h2` when the screen is embedded under another heading.',
        },
      ],
    },
    states: {
      items: [
        { name: 'Variant', attribute: 'data-variant', description: '`card` or `plain`.' },
        {
          name: 'With side panel',
          attribute: 'data-has-aside',
          description: 'Two columns from 1024px.',
        },
        {
          name: 'Panel side',
          attribute: 'data-aside-position',
          description: '`start` or `end`, set only with a panel.',
        },
        {
          name: 'Notice',
          description: '`notice` shows above the form (session expired, last error).',
        },
      ],
    },
    interaction: {
      items: [
        'Nothing in the layout is interactive except the brand link and footer links.',
        'Form behaviour, submission and errors belong to the product’s form in `children`.',
      ],
    },
    keyboard: {
      items: [
        {
          keys: 'Tab',
          action: 'Brand link, then the form fields and buttons, then the footer and legal links.',
        },
        { keys: 'Enter', action: 'Follows a focused link or submits the form.' },
      ],
    },
    accessibility: {
      role: 'region (section) + navigation',
      name: 'The card is named by the title; the links by `messages.linksLabel` ("Legal and help").',
      items: [
        'The title is the page’s h1 by default.',
        'Give `brandLabel` when the brand is an image without alt text.',
        'Links that open a new tab say so to screen readers.',
        'Put error notices in an Alert so they are announced.',
      ],
    },
    responsive: {
      items: [
        'The column is at most 26rem wide and fills the width at 390px with a 16px gutter.',
        'Below 480px the form starts near the top so the keyboard does not cover it, and the card padding tightens.',
        'The side panel hides below 1024px.',
        'Footer links are 44px tall on coarse pointers.',
      ],
    },
    theme: {
      items: [
        'Canvas, surface, border and accent tokens; the side panel uses the product accent with its on-solid text colour.',
        'Logical properties, so the panel flips in RTL.',
        'Forced-colours mode keeps the card border and Highlight focus outlines.',
      ],
    },
    examples: {
      items: [
        {
          title: 'Sign in',
          render: () => (
            <AuthPage
              style={{ minHeight: 0 }}
              brand={<strong>Product</strong>}
              brandHref="#home"
              title="Sign in"
              headingLevel="h2"
              description="Use your work email to continue."
              footer={<span>No account yet? Create one.</span>}
              links={sampleLinks}
            >
              <SampleForm submit="Continue" />
            </AuthPage>
          ),
        },
        {
          title: 'Sign up with a side panel (wide screens)',
          render: () => (
            <AuthPage
              style={{ minHeight: 0 }}
              brand={<strong>Product</strong>}
              title="Create your account"
              headingLevel="h2"
              aside={<p>A short product message or illustration goes here.</p>}
              variant="plain"
            >
              <SampleForm submit="Create account" />
            </AuthPage>
          ),
        },
      ],
    },
    doDont: {
      do: [
        'Keep one form and one primary button per screen.',
        'Show the last error in `notice` with an Alert.',
        'Link terms and privacy in `links`.',
      ],
      dont: [
        'Do not put required fields or the only sign-in option in `aside`.',
        'Do not put marketing claims or unsourced numbers in the side panel.',
        'Do not handle passwords or tokens in AuthPage; it is layout only.',
      ],
    },
    related: {
      items: [
        { id: 'form-field', reason: 'Fields inside the form.' },
        { id: 'button', reason: 'The submit button.' },
        { id: 'alert', reason: 'Notices above the form.' },
        { id: 'settings-page', reason: 'Account settings once signed in.' },
      ],
    },
    dependencies: { packages: ['react'], components: [] },
    testing: {
      unit: [
        'Brand link, card named by the title, description, form and footer; axe clean.',
        'Safe footer links in a named nav; unsafe links dropped; new-tab text; axe clean.',
        'Tab order: brand, form, links.',
        'Unsafe brand link ignored; notice, side panel, plain variant and panel position.',
        'Translated labels and ref forwarding.',
        'Server rendering.',
      ],
      axe: true,
      ssr: true,
    },
  },
} satisfies ComponentDocs;
