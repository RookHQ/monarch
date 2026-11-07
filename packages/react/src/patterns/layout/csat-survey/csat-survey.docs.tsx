import { useState } from 'react';
import { CSATSurvey, type CSATRating } from '@rookcom/monarch-react/marketing';
import type { ComponentDocs } from '../../../internal/component-docs.js';

const wait = (ms: number) => new Promise((r) => setTimeout(r, ms));

function Instant() {
  const [rating, setRating] = useState<CSATRating | null>(null);
  return (
    <CSATSurvey
      headingAs="h2"
      context="Request #1042 · Billing question"
      rating={rating}
      onRate={async (next) => {
        await wait(500);
        setRating(next);
      }}
      onComment={() => wait(500)}
    />
  );
}

function Widget() {
  return (
    <div style={{ maxWidth: '22rem' }}>
      <CSATSurvey
        headingAs="h3"
        mode="form"
        scale="five"
        messages={{ question: 'How was this conversation?' }}
        onRate={() => wait(500)}
      />
    </div>
  );
}

export default {
  id: 'csat-survey',
  title: 'CSATSurvey',
  summary: 'A satisfaction survey: Good / Bad or 1–5, an optional comment, and every saving state.',
  sections: {
    purpose: {
      summary:
        'Asks a customer how a support conversation went, records the answer and lets them add a comment. Works as a one-click page from an email (the rating saves at once) or as a short form at the end of a chat. Needed by: Tickets (public CSAT page), the support chat widget (rating after a resolved conversation).',
      useWhen: [
        'After a ticket or chat is resolved, on a public survey page or inside the widget.',
        'A one-question rating where the comment is optional.',
      ],
      avoidWhen: [
        'Multi-question surveys: build a form with Field and RatingPicker.',
        'Rating a product or an article inline: use RatingPicker or AIFeedback-style thumbs.',
      ],
    },
    designPrinciples: {
      items: [
        'One click is enough: in `instant` mode the rating saves when chosen; the comment is extra.',
        'Optimistic with rollback: the choice shows at once and reverts if saving fails, with the reason.',
        'Data in, answer out: the app loads the survey, stores the answer and decides when it is locked.',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: [
        'Forwards its ref to the root `<section>`. Built on Button, Field, Textarea, RatingPicker, Alert and LoadingState.',
        '`CSATRating` is `"good" | "bad"` on the two-choice scale, or 1–5 on the five-point scale.',
      ],
    },
    typedProperties: {
      source: 'typescript',
      notes: [
        '`onRate` and `onComment` may return a promise: the survey shows progress, a success note, or the rejection’s message.',
        'Migrating from the app copies: Tickets `components/portal/Feedback.tsx` → `<CSATSurvey rating={survey.rating?.toLowerCase()} comment={survey.comment} locked={survey.canChange === false} context={`Request #${number} · ${subject}`} messages={{ question: survey.question }} onRate={(r, c) => portalApi.rate(slug, token, r.toUpperCase(), c || null)} onComment={(c, r) => portalApi.rate(…)} />`; the one-click `?rating=` effect stays in the app and calls the same save; `q.isLoading` → `loading`; a 404 → `error`. The support chat widget `Csat` → `mode="form"`, `headingAs="h2"`, `submitted={!!conv.rating}`, `onRate={(r, c) => client.rate(conv.id, r.toUpperCase(), c || undefined)}`.',
      ],
    },
    variants: {
      items: [
        {
          prop: 'scale',
          values: ['good-bad', 'five'],
          description: 'Two large thumbs buttons, or a labelled 1–5 scale.',
        },
        {
          prop: 'mode',
          values: ['instant', 'form'],
          description:
            'Save the rating when chosen and the comment separately, or both with one Send button.',
        },
        {
          prop: 'headingAs',
          values: ['h1', 'h2', 'h3'],
          description: 'The question’s heading level.',
        },
      ],
    },
    states: {
      items: [
        { name: 'Unanswered', description: 'A hint asks for a choice first.' },
        {
          name: 'Saving',
          attribute: 'aria-busy="true"',
          description: 'Choices and Send are disabled; "Saving your answer…" is announced.',
        },
        {
          name: 'Saved / failed',
          attribute: 'role="status"',
          description: 'A success or danger note after each save.',
        },
        {
          name: 'Locked',
          attribute: 'data-locked',
          description: 'The answer can no longer change: choices disabled, a note, no comment box.',
        },
        { name: 'Submitted', description: 'Only the thank-you message.' },
        {
          name: 'Loading / error',
          description: 'A spinner, or a heading and the reason the survey cannot load.',
        },
      ],
    },
    interaction: {
      items: [
        'Choosing Good or Bad (or a number) selects it; in `instant` mode it saves at once and the comment box appears.',
        'Save comment (instant) or Send feedback (form) sends the trimmed comment; an empty comment is sent as an empty string.',
        'The comment is limited to `commentMaxLength` characters, with a count below it.',
      ],
    },
    keyboard: {
      items: [
        { keys: 'Tab', action: 'Move between the choices, the comment and the button.' },
        { keys: 'Enter / Space', action: 'Choose Good or Bad.' },
        { keys: '← / → / ↑ / ↓', action: 'On the five-point scale: move and choose.' },
      ],
    },
    accessibility: {
      role: 'region named by the question',
      name: 'The question heading; the choices are a group named "Your rating".',
      items: [
        'Good and Bad are toggle buttons with `aria-pressed`; the five-point scale is RatingPicker’s radio group.',
        'Progress, success and failure are announced through status regions.',
        'The comment is a labelled textarea with its character count as its description.',
      ],
    },
    responsive: {
      items: [
        'At most 32rem wide and centred; padding tightens below 30rem.',
        'Good and Bad stay side by side at 390px, each at least 80px tall.',
      ],
    },
    theme: {
      items: [
        'The chosen answer fills with `--mn-color-accent-solid`, so it takes the product’s brand.',
        'Forced colours use Highlight for the chosen answer and GrayText when locked; reduced motion removes transitions.',
      ],
    },
    examples: {
      items: [
        { title: 'One-click page with a comment', render: () => <Instant /> },
        { title: 'Five-point form in a widget', render: () => <Widget /> },
        {
          title: 'Locked',
          render: () => (
            <CSATSurvey headingAs="h2" rating="good" comment="Quick and friendly." locked />
          ),
        },
      ],
    },
    doDont: {
      do: [
        'Accept the rating from the email link straight away, then show this page to add a comment.',
        'Keep the question about the support received, not the product.',
      ],
      dont: ['Do not require a comment.', 'Do not ask again after the answer is locked.'],
    },
    related: {
      items: [
        { id: 'rating-picker', reason: 'The five-point scale.' },
        { id: 'help-center-home', reason: 'The other public support page.' },
        { id: 'support-panel', reason: 'Where a chat ends and the survey can appear.' },
      ],
    },
    dependencies: {
      packages: ['react'],
      components: ['button', 'form-field', 'text-field', 'rating-picker', 'alert', 'loading-state'],
    },
    testing: {
      unit: [
        'Region named by the question; Good / Bad group; first-choice hint.',
        'Instant: saves at once with progress, then the comment.',
        'Failed save rolls back and shows the reason.',
        'Form mode with the five-point scale sends both, then thanks.',
        'Locked; loading, load error and submitted; prefilled comment; translation.',
      ],
      axe: true,
      ssr: true,
    },
  },
} satisfies ComponentDocs;
