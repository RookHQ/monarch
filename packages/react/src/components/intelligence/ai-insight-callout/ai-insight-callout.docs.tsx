import { useState } from 'react';
import { AiInsightCallout } from '@rookcom/monarch-react/ai';
import { Button } from '@rookcom/monarch-react';
import type { ComponentDocs } from '../../../internal/component-docs.js';

function DismissDemo() {
  const [shown, setShown] = useState(true);
  const restoreRef = (el: HTMLButtonElement | null) => {
    if (el && !shown) el.focus();
  };
  return shown ? (
    <AiInsightCallout
      severity="warning"
      origin="ai"
      title="Renewal at risk"
      why="Weekly active users fell from 48 to 29 over the last 30 days."
      evidence={
        <ul style={{ margin: 0, paddingInlineStart: 'var(--mn-space-4)' }}>
          <li>Logins: 312 → 190 (30 days)</li>
          <li>Support tickets: 2 → 7 (30 days)</li>
        </ul>
      }
      action={{ label: 'Plan a check-in', onClick: () => {} }}
      onDismiss={() => setShown(false)}
    />
  ) : (
    <Button ref={restoreRef} size="sm" onClick={() => setShown(true)}>
      Show the insight again
    </Button>
  );
}

export default {
  id: 'ai-insight-callout',
  title: 'AiInsightCallout',
  summary: 'A proactive insight with its reason, evidence and origin (AI or rule) shown honestly.',
  sections: {
    purpose: {
      summary:
        'Surfaces something worth attention on a record or page (a risk, an opportunity, an anomaly) with why it was raised, the evidence behind it and one follow-up action. It always says whether a model or a fixed rule produced it.',
      useWhen: [
        'A record page or dashboard should point out a risk or opportunity proactively.',
        'Insights come from a mix of deterministic rules and model output and people need to tell them apart.',
        'You can explain why the insight was raised in one or two sentences.',
      ],
      avoidWhen: [
        'Reporting the result of something the person did: use Alert or a toast.',
        'Several next steps to choose from: use AiSuggestedActions.',
        'A summary of the whole record: use AiSummaryCard.',
      ],
    },
    designPrinciples: {
      items: [
        'Honest origin: "AI insight" with the Bot glyph and AI colour only when a model produced it; "Rule-based" with a checklist glyph in neutral colours otherwise. Never dress a rule up as AI, or AI as a rule.',
        'Severity is text and icon first, colour second: a hidden "Warning:" prefix, a distinct icon per severity and a tinted surface.',
        'Always explain why. An insight without a reason is noise.',
        'Evidence is collapsed by default so the callout stays short.',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: [
        'Native `<div>` attributes pass through and the ref is forwarded to the root.',
        '`messages.severity` and `messages.origin` merge per key; `AI_INSIGHT_CALLOUT_MESSAGES` holds the English defaults.',
        'The severity and origin colours never mix: severity tints the callout, origin only styles its chip.',
      ],
    },
    typedProperties: { source: 'typescript' },
    variants: {
      items: [
        {
          prop: 'severity',
          values: ['info', 'warning', 'critical', 'positive'],
          description:
            'Icon, hidden prefix and tone: info (blue), warning (amber), critical (red), positive (green). Exposed as `data-severity`.',
        },
        {
          prop: 'origin',
          values: ['rule', 'ai'],
          description:
            '`ai` shows "AI insight" with the Bot glyph in the AI colour; `rule` shows "Rule-based" in neutral colours. Exposed as `data-origin`.',
        },
      ],
    },
    states: {
      items: [
        { name: 'Default', description: 'Title, origin chip and reason.' },
        {
          name: 'Evidence open',
          attribute: 'open on the details element',
          description: 'The evidence disclosure is expanded.',
        },
        {
          name: 'Dismissible',
          description: 'With `onDismiss`, a close button sits on the trailing edge.',
        },
      ],
    },
    interaction: {
      items: [
        'The action button calls `action.onClick`; the callout stays until the app removes it.',
        'Dismiss calls `onDismiss`; the app removes the callout and moves focus somewhere sensible (the next insight, or the section heading).',
        'Show evidence toggles the native disclosure.',
      ],
    },
    keyboard: {
      items: [
        { keys: 'Tab / Shift+Tab', action: 'Moves through Show evidence, the action and Dismiss.' },
        { keys: 'Enter / Space', action: 'Activates the focused control or toggles the evidence.' },
      ],
      notes: ['The callout never takes focus when it appears.'],
    },
    accessibility: {
      role: 'group',
      name: 'The severity prefix and title, e.g. "Warning: Renewal at risk" (aria-labelledby)',
      items: [
        'The reason is the group’s accessible description (aria-describedby).',
        'The origin chip is visible text ("AI insight" or "Rule-based"), so screen reader users get the same provenance as everyone else.',
        'Icons are decorative; severity is carried by the hidden prefix.',
        'Evidence uses native `<details>`/`<summary>`.',
        'Not a live region: insights shown on page load are not announced. Wrap it in your own polite region if one appears in response to an action.',
        'Set `headingLevel` to make the title a heading when callouts form part of the page outline.',
      ],
    },
    responsive: {
      items: [
        'The origin chip wraps under the title on narrow widths; long text and evidence wrap, so there is no horizontal scroll at 390px.',
        'On coarse pointers the action, Show evidence and Dismiss are at least 44px.',
      ],
    },
    theme: {
      items: [
        'Severity uses the status tokens: `--mn-color-{info,warning,danger,success}-{subtle,strong,border}` for surface, icon and the thicker start border.',
        'The AI chip uses `--mn-color-ai-subtle`, `--mn-color-ai-strong` and `--mn-color-ai-border`; the rule chip uses `--mn-color-bg-surface` and `--mn-color-text-muted`.',
        'In forced colours borders and the icon use CanvasText.',
      ],
    },
    examples: {
      items: [
        {
          title: 'AI insight with evidence, action and dismiss',
          stack: true,
          render: () => <DismissDemo />,
        },
        {
          title: 'Rule-based insight',
          stack: true,
          render: () => (
            <AiInsightCallout
              severity="critical"
              origin="rule"
              title="Invoice overdue"
              why="Invoice 1042 is 31 days past its due date. The rule flags anything over 30 days."
              action={{ label: 'Send a reminder', onClick: () => {} }}
            />
          ),
        },
        {
          title: 'Every severity',
          stack: true,
          render: () => (
            <>
              <AiInsightCallout
                severity="info"
                origin="ai"
                title="Busiest day is Tuesday"
                why="Most orders over the last 8 weeks arrived on Tuesdays."
              />
              <AiInsightCallout
                severity="warning"
                origin="rule"
                title="Contract ends in 21 days"
                why="No renewal has been recorded."
              />
              <AiInsightCallout
                severity="critical"
                origin="ai"
                title="Unusual refund volume"
                why="Refunds this week are four times the weekly average."
              />
              <AiInsightCallout
                severity="positive"
                origin="rule"
                title="Target reached"
                why="Quarterly bookings passed the target set for this team."
              />
            </>
          ),
        },
      ],
    },
    doDont: {
      do: [
        'Set `origin` from where the insight really came from.',
        'Write `why` with the numbers behind it.',
        'Offer at most one action; link to the record for more.',
      ],
      dont: [
        'Do not mark a rule-based insight as AI to make it look smarter, or hide that a model produced one.',
        'Do not stack more than three callouts in one place; rank them and show the rest on demand.',
        'Do not use `critical` for anything that does not need action soon.',
      ],
    },
    related: {
      items: [
        { id: 'alert', reason: 'Status messages about the page or an action, not insights.' },
        { id: 'ai-suggested-actions', reason: 'Several next steps to choose from.' },
        { id: 'ai-summary-card', reason: 'A summary of the whole record.' },
      ],
    },
    dependencies: { packages: ['react'], components: ['button'] },
    testing: {
      unit: [
        'Group named by the severity prefix and title, described by the reason; props and ref forwarded.',
        'AI insights show the AI chip and Bot glyph; rule-based insights show "Rule-based" and no AI text.',
        'Hidden severity text for every severity.',
        'Evidence collapsed by default; action and dismiss run from the keyboard.',
        'Heading level, no buttons without handlers, and message overrides.',
      ],
      axe: true,
      ssr: true,
    },
  },
} satisfies ComponentDocs;
