import { DnsRecordTable, DomainProgress } from '@rookcom/monarch-react/patterns';
import type { ComponentDocs } from '../../../internal/component-docs.js';

const box = {
  width: '100%',
  maxWidth: '36rem',
  display: 'grid',
  gap: 'var(--mn-space-4)',
} as const;
const checkedAt = '2026-01-10T11:58:00Z';
const now = '2026-01-10T12:00:00Z';

export default {
  id: 'domain-progress',
  title: 'DomainProgress',
  summary: 'Stage-by-stage progress for connecting a custom domain: DNS, HTTPS, live.',
  sections: {
    purpose: {
      summary:
        'Tells the user where their custom domain is: DNS record found, HTTPS certificate issued, live. It shows the problems the product found, what public DNS shows right now, and that checking continues after they leave. Needed by: Tickets, Calendar, Recruit, Base, Pulse (the hand-copied `DomainProgress` in each).',
      useWhen: [
        'A product serves pages on a customer’s own hostname and re-checks DNS and HTTPS until it is live.',
      ],
      avoidWhen: [
        'A generic multi-step form: use a stepper.',
        'Only a list of records to add: use DnsRecordTable on its own.',
      ],
    },
    designPrinciples: {
      items: [
        'Three steps people understand, not the product’s internal states.',
        'Waiting is not failing: `info` problems are shown quietly, `blocking` ones as warnings on their step.',
        'Says what DNS shows now, so the user can compare it with what they typed.',
        'Reassures that checking continues on its own, and how they will be told.',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: [
        'Also exports `domainStepStates(stage, problems)`, `formatDnsSnapshot(found)` and `domainPollMs(stage, recheckAfterSec)` for polling.',
        'No data fetching: the product polls and passes the latest result in.',
      ],
    },
    typedProperties: {
      source: 'typescript',
      notes: [
        'Migrating from the app copy (`src/components/domains/DomainProgress.tsx`): `data.stage ?? STAGE_FOR[data.status]` becomes `stage` (map `pending_dns`→`dns`, `pending_verification`→`ownership`, `provisioning`→`certificate`, `active`→`live`); `data.problems` becomes `problems`, with the old INFO codes (`VERCEL_DNS_PENDING`, `CERTIFICATE_PENDING`, `DNS_LOOKUP_FAILED`, `PROBE_TRANSIENT`) passed as `severity: "info"` and `CAA_BLOCKS` as `step: "certificate"`; `data.dnsFound` → `dnsFound`; `data.checkedAt` → `checkedAt`; `data.recheckAfterSec !== null` → `rechecking`; `data.notify` `in_app` → `notify="in-app"`; `noun` → `messages.liveDetail`.',
        '`domainPollMs(data)` becomes `domainPollMs(stage, data.recheckAfterSec)`. `DnsProviderGuide` and `recordPurposeLabel` moved to DnsRecordTable.',
      ],
    },
    variants: {
      items: [],
      notes: ['No visual variants; the stage and problems drive the look.'],
    },
    states: {
      items: [
        { name: 'Done', attribute: 'data-state="done"', description: 'Green dot with a check.' },
        {
          name: 'Current',
          attribute: 'data-state="current", aria-current="step"',
          description: 'Accent dot with a spinner.',
        },
        {
          name: 'Needs attention',
          attribute: 'data-state="error"',
          description: 'Warning dot; the blocking problems are listed below.',
        },
        {
          name: 'Not started',
          attribute: 'data-state="todo"',
          description: 'Empty ring, muted title.',
        },
        {
          name: 'Stage',
          attribute: 'data-stage',
          description:
            'The root carries the stage (`dns`, `ownership`, `certificate`, `page`, `live`).',
        },
      ],
    },
    interaction: {
      items: [],
      notes: ['Not interactive. The "checked" time updates by itself.'],
    },
    keyboard: {
      items: [],
      notes: ['Not interactive: nothing in it takes focus.'],
    },
    accessibility: {
      role: 'list',
      name: '`messages.listLabel(host)`: "Progress for book.example.com".',
      items: [
        'Each step’s state is in its text ("(in progress)"), not only in the dot colour.',
        'The current step has `aria-current="step"`.',
        'A polite status announces the active step when it changes ("Step 2 of 3: HTTPS certificate, in progress.").',
        'Problems use InlineNotice live regions; the ticking "checked" time is not announced.',
      ],
    },
    responsive: {
      items: ['A single column at every width; long hostnames wrap rather than overflow at 390px.'],
    },
    theme: {
      items: [
        'Step dots use the success, accent and warning solid tokens with their on-solid text.',
        'Forced colours: dots use `CanvasText`, current and error steps a `Highlight` ring.',
        'Reduced motion: dot colour transitions are off; the spinner slows down.',
      ],
    },
    examples: {
      items: [
        {
          title: 'Issuing the certificate',
          stack: true,
          render: () => (
            <div style={box}>
              <DomainProgress
                host="book.example.com"
                stage="certificate"
                dnsFound={{ cnames: ['domains.example.net'] }}
                checkedAt={checkedAt}
                now={now}
                notify="email"
              />
            </div>
          ),
        },
        {
          title: 'A problem to fix, with the records',
          stack: true,
          render: () => (
            <div style={box}>
              <DomainProgress
                host="book.example.com"
                stage="dns"
                problems={[
                  {
                    code: 'WRONG_TARGET',
                    message:
                      'The CNAME points somewhere else. Change its value to domains.example.net.',
                  },
                ]}
                dnsFound={{ cnames: ['old-host.example.org'] }}
                checkedAt={checkedAt}
                now={now}
              />
              <DnsRecordTable
                zone="example.com"
                records={[
                  {
                    type: 'CNAME',
                    name: 'book.example.com',
                    value: 'domains.example.net',
                    purpose: 'routing',
                    ok: false,
                  },
                ]}
              />
            </div>
          ),
        },
        {
          title: 'Live',
          stack: true,
          render: () => (
            <div style={box}>
              <DomainProgress host="book.example.com" stage="live" />
            </div>
          ),
        },
      ],
    },
    doDont: {
      do: [
        'Poll with `domainPollMs` while the stage is not `live`, and pass each result in.',
        'Mark normal waits as `severity: "info"`.',
        'Show DnsRecordTable under it while DNS is not found.',
      ],
      dont: [
        'Do not show internal status codes as messages.',
        'Do not promise an email in `notify` unless the product sends one.',
      ],
    },
    related: {
      items: [
        { id: 'dns-record-table', reason: 'The records the user adds, and the provider guide.' },
        { id: 'inline-notice', reason: 'How problems are shown.' },
        { id: 'relative-time', reason: 'The "checked" time.' },
      ],
    },
    dependencies: {
      packages: ['react'],
      components: ['relative-time', 'inline-notice', 'spinner'],
    },
    testing: {
      unit: [
        'Step states for each stage; blocking problems mark their step, info problems do not.',
        'DNS snapshot formatting and the poll interval bounds.',
        'Accessible step text, `aria-current`, the status announcement and the DNS line.',
        'Blocking versus info problems; details hidden once live.',
        'Translated messages.',
      ],
      axe: true,
      ssr: true,
    },
  },
} satisfies ComponentDocs;
