import { useState } from 'react';
import { Button } from '@rookcom/monarch-react';
import {
  AiEvalReview,
  type AiEvalCriterion,
  type AiEvalItem,
  type AiEvalItemReview,
} from '@rookcom/monarch-react/ai';
import type { ComponentDocs } from '../../../internal/component-docs.js';

const ITEMS: AiEvalItem[] = [
  {
    id: 'q1',
    title: 'Summarise a meeting',
    prompt: 'Summarise the attached meeting notes in three bullet points.',
    output:
      '- The launch moves to **next Tuesday**.\n- Design owns the final copy review.\n- Support needs the FAQ by Friday.',
    reference: 'Launch moved to Tuesday; design reviews copy; support needs the FAQ by Friday.',
    meta: 'Model A · 12 Oct',
  },
  {
    id: 'q2',
    title: 'Answer a policy question',
    prompt: 'Can I expense a train ticket for a client visit?',
    output:
      'Yes. Travel for client visits is reimbursable with a receipt, up to the standard fare.',
    meta: 'Model A · 12 Oct',
  },
  {
    id: 'q3',
    title: 'Draft a reply',
    prompt: 'Draft a short, polite reply declining the invitation.',
    output:
      'Thank you for the invitation. Unfortunately I can’t join this time, but I hope it goes well.',
    meta: 'Model B · 13 Oct',
  },
];

const RUBRIC: AiEvalCriterion[] = [
  { id: 'accuracy', label: 'Accuracy', description: 'Facts match the source or reference.' },
  { id: 'completeness', label: 'Completeness', description: 'Covers everything asked.' },
  { id: 'tone', label: 'Tone', scale: 3 },
];

function Demo() {
  const [reviews, setReviews] = useState<Record<string, AiEvalItemReview>>({});
  const done = Object.values(reviews).filter((r) => r.verdict).length;
  return (
    <div style={{ width: '100%' }}>
      <AiEvalReview
        items={ITEMS}
        rubric={RUBRIC}
        reviews={reviews}
        onReviewChange={(_, __, all) => setReviews(all)}
        footer={
          <div>
            <Button variant="primary" disabled={done < ITEMS.length}>
              Submit reviews
            </Button>
          </div>
        }
      />
    </div>
  );
}

export default {
  id: 'ai-eval-review',
  title: 'AiEvalReview',
  summary: 'A review queue for AI outputs with rubric scoring, notes and pass / fail.',
  sections: {
    purpose: {
      summary:
        'Teams that ship AI features need people to grade outputs consistently. AiEvalReview puts a queue of outputs beside a review form: prompt, output and expected answer, a 1–5 rubric per criterion, notes and a pass / fail verdict, with fast keyboard movement between items.',
      useWhen: [
        'Human evaluation of model or prompt changes.',
        'Spot-checking AI outputs before a feature launches or a model is switched.',
      ],
      avoidWhen: [
        'End-user feedback on a single reply (use AiFeedback).',
        'Reviewing AI drafts before sending them (use AiDraftQueue).',
      ],
    },
    designPrinciples: {
      items: [
        'One item at a time, with the queue always visible so progress is clear.',
        'The rubric is explicit: every criterion has a label, an optional description and a fixed scale.',
        'Fast for experts: J / K and P / F shortcuts, without breaking typing in notes.',
        'The output is marked AI-generated and rendered as safe Markdown.',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: [
        'The ref goes to the root `<section>`.',
        '`reviews` is a record of item id → `{ scores, notes, verdict }`; `onReviewChange` receives the item’s review and the whole record, so persisting is one call.',
        'A string `output` renders through AiMarkdown; pass a node for anything else.',
      ],
    },
    typedProperties: { source: 'typescript' },
    variants: {
      items: [],
      notes: [
        'No visual variants. `advanceOnVerdict` moves to the next item after pass / fail; `shortcuts={false}` turns off single-key shortcuts.',
      ],
    },
    states: {
      items: [
        {
          name: 'Current item',
          attribute: 'aria-current="true"',
          description: 'The queue entry being reviewed.',
        },
        {
          name: 'Verdict',
          attribute: 'data-verdict="pass | fail | pending"',
          description: 'Shown on each queue entry with an icon and text.',
        },
        { name: 'Empty', description: '“Nothing to review.” when there are no items.' },
      ],
    },
    interaction: {
      items: [
        'Choosing a queue entry shows it; Previous and Next move one item and focus its heading.',
        'Scores are radio choices per criterion; the total updates as you score.',
        'Notes and verdict are saved per item through `onReviewChange`.',
      ],
    },
    keyboard: {
      items: [
        {
          keys: 'Arrow Up / Down, Home / End',
          action: 'Move through the queue; the item follows focus.',
        },
        { keys: 'J / K', action: 'Next / previous item (outside text fields).' },
        { keys: 'P / F', action: 'Pass / fail the current item (outside text fields).' },
        {
          keys: 'Arrow keys in a score group',
          action: 'Change the score (native radio behaviour).',
        },
      ],
    },
    accessibility: {
      role: 'region named “Review AI outputs”',
      name: '`messages.label`.',
      items: [
        'The queue is a list of buttons; the current one has `aria-current="true"` and every entry states its verdict as text.',
        'Each criterion is a fieldset with a legend; each score is named “3 of 5”.',
        'The output section carries an “AI-generated” badge.',
        'Focus moves to the new item’s heading after Previous, Next and J / K.',
      ],
    },
    responsive: {
      items: [
        'From 768px: queue column (sticky) beside the item.',
        'Below 768px: the queue stacks above the item with its own scroll; nothing scrolls horizontally at 390px.',
        'Score choices and queue entries are 44px on coarse pointers; the shortcut hint hides there.',
      ],
    },
    theme: {
      items: [
        '`--mn-color-ai-*` for the current entry, the output border and selected scores; success and danger tokens for pass and fail.',
        'Forced colours: borders in CanvasText, selections in Highlight.',
      ],
    },
    examples: {
      items: [{ title: 'Review queue with a submit footer', stack: true, render: () => <Demo /> }],
    },
    doDont: {
      do: [
        'Persist every `onReviewChange` so no review is lost.',
        'Describe each criterion so reviewers score the same way.',
        'Show the reference answer when there is one.',
      ],
      dont: [
        'Do not show which model produced an output when reviewers should judge blind.',
        'Do not use more than five points per criterion.',
      ],
    },
    related: {
      items: [
        { id: 'ai-feedback', reason: 'Thumbs feedback from end users.' },
        { id: 'ai-score', reason: 'Showing a single computed score.' },
        { id: 'ai-markdown', reason: 'Renders string outputs.' },
      ],
    },
    dependencies: {
      packages: ['react'],
      components: [
        'ai-generated-badge',
        'ai-markdown',
        'button',
        'form-field',
        'kbd',
        'text-field',
      ],
    },
    testing: {
      unit: [
        'Queue, current item, Markdown output, reference and rubric groups with descriptions.',
        'Scores, notes and verdict update the review, total and queue status.',
        'Previous / Next move and focus the heading; reviews are kept per item.',
        'Arrow keys, Home and End move through the queue.',
        'J / K / P / F shortcuts work outside text fields only; advanceOnVerdict.',
        'Starts at the first unreviewed item; controlled reviews; empty state.',
      ],
      axe: true,
      ssr: true,
    },
  },
} satisfies ComponentDocs;
