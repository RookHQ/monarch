import { StatusBadge } from '@rookcom/monarch-react';
import { PayslipView, type PayslipLine } from '@rookcom/monarch-react/patterns';
import type { ComponentDocs } from '../../../internal/component-docs.js';

const lines: PayslipLine[] = [
  { name: 'Basic', kind: 'earning', amount: '4200.00', note: '22 of 22 working days' },
  { name: 'Housing allowance', kind: 'earning', amount: '1300.00' },
  { name: 'Overtime', kind: 'earning', amount: '180.00', note: '6 hours at 1.5×' },
  { name: 'Income tax', kind: 'deduction', amount: '820.00' },
  { name: 'Pension (employee)', kind: 'deduction', amount: '275.00' },
  { name: 'Travel', kind: 'reimbursement', amount: '64.50' },
  { name: 'Pension (employer)', kind: 'employer', amount: '275.00' },
];

const details = [
  { label: 'Employee', value: 'Jordan Park' },
  { label: 'Employee ID', value: 'E-1042' },
  { label: 'Department', value: 'Operations' },
  { label: 'Pay period', value: '1 Mar – 31 Mar 2026' },
  { label: 'Pay date', value: '31 Mar 2026' },
  { label: 'Paid days', value: '22 of 22' },
];

export default {
  id: 'payslip-view',
  title: 'PayslipView',
  summary:
    'An on-screen payslip: details, grouped pay lines, gross-to-net summary and year-to-date figures.',
  sections: {
    purpose: {
      summary:
        'Shows a payslip people can read on any screen, with the same numbers as the downloadable PDF: who paid, the period, each earning and deduction, and net pay. Needed by: People (My pay, payroll runs, final settlement).',
      useWhen: [
        'An employee views their payslip or a payroll admin checks one in a run.',
        'Supplementary, off-cycle or final-settlement statements that share the payslip layout.',
      ],
      avoidWhen: [
        'Editing amounts: payroll calculates them; use LineItemsEditor only for typed invoice rows.',
        'A one-figure “latest net pay” card: use StatCard with Money.',
      ],
    },
    designPrinciples: {
      items: [
        'Display only. Totals come from payroll and are never recalculated in the browser, so the screen cannot disagree with the PDF.',
        'Net pay is the most prominent figure; everything else supports it.',
        'Every label can be replaced, so a payslip can be shown in the language it was issued in.',
        'No product data model: details are label/value pairs and lines are name, kind and amount.',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: [
        'The ref and native attributes go to the root `<article>`, named by the title. Pass `lang` for a payslip in another language.',
        'Groups render in the order earnings, deductions, reimbursements, employer contributions; empty groups are left out.',
      ],
    },
    typedProperties: {
      source: 'typescript',
      notes: [
        'Amounts accept numbers, bigints or exact decimal strings; set `minorUnits` when they are integer cents or paise.',
        'Migrating from the app copy (People `components/payroll/PayslipView.tsx`): `p.organisation.name` → `issuer`; the kind label (“Payslip”, “Final settlement”) → `title`; period and number → `subtitle`; the employee/period grid → `details` as `{ label, value }`; `p.lines` → `lines` with `kind` lower-cased (`EARNING` → `earning`, `EMPLOYER_CONTRIBUTION` → `employer`); `p.gross/deductions/reimbursements/net` → `totals`; `p.ytd` → `yearToDate` items plus `yearToDateTitle`; `p.labels` → `messages`; `p.language === "hi"` → `lang="hi"`; `onDownload` and `downloading` are unchanged.',
      ],
    },
    variants: {
      items: [
        {
          prop: 'hideEmployerContributions',
          values: ['false', 'true'],
          description: 'Leave out the employer contributions group.',
        },
        {
          prop: 'headingLevel',
          values: ['2', '3', '4'],
          description: 'Title heading level; group headings use the next level.',
        },
      ],
    },
    states: {
      items: [
        { name: 'No lines', description: 'The `noLines` message replaces the groups.' },
        {
          name: 'Downloading',
          attribute: 'aria-busy (button)',
          description: '`downloading` shows a spinner in Download PDF and blocks repeat clicks.',
        },
        {
          name: 'Group',
          attribute: 'data-kind',
          description: 'Each group section carries its kind for product styling.',
        },
      ],
    },
    interaction: {
      items: [
        'Download PDF calls `onDownload`; the product fetches or generates the file.',
        'Everything else is read-only. The actions are hidden when printing.',
      ],
    },
    keyboard: {
      items: [
        { keys: 'Tab, Enter / Space', action: 'Reach and press Download PDF and any `actions`.' },
      ],
      notes: ['The payslip content itself is not interactive.'],
    },
    accessibility: {
      role: 'article',
      name: 'The title, via aria-labelledby.',
      items: [
        'Each group is a region named by its heading, so screen reader users can jump between Earnings and Deductions.',
        'Lines are lists; the summary and year-to-date blocks are description lists pairing each label with its amount.',
        'Amounts use Money, so compact or empty values are read correctly.',
      ],
    },
    responsive: {
      items: [
        'The details grid drops to one column below 640px; long line names wrap and amounts never do.',
        'Year-to-date tiles fit as many 9rem columns as the width allows, one per row on a 390px phone.',
        'Download PDF is 44px tall on coarse pointers.',
      ],
    },
    theme: {
      items: [
        'Line lists and tiles use `--mn-color-border-subtle`; the summary sits on `--mn-color-bg-subtle`.',
        'Secondary text uses `--mn-color-text-muted`; net pay uses `--mn-color-text-default` at `--mn-text-xl`.',
        'Forced-colours mode keeps every border as a system colour.',
      ],
    },
    examples: {
      items: [
        {
          title: 'Monthly payslip',
          stack: true,
          render: () => (
            <PayslipView
              issuer="Example Co."
              title="Payslip"
              subtitle="March 2026 · PS-2026-03-0042"
              status={<StatusBadge status="success" label="Paid" />}
              details={details}
              lines={lines}
              totals={{
                gross: '5680.00',
                deductions: '1095.00',
                reimbursements: '64.50',
                net: '4649.50',
              }}
              yearToDate={[
                { label: 'Gross', amount: '17040.00' },
                { label: 'Deductions', amount: '3285.00' },
                { label: 'Net', amount: '13755.00' },
                { label: 'Tax withheld', amount: '2460.00' },
              ]}
              yearToDateTitle="Year to date (from January)"
              currency="USD"
              locale="en-US"
              onDownload={() => {}}
            />
          ),
        },
        {
          title: 'Employee copy without employer contributions, minor units',
          stack: true,
          render: () => (
            <PayslipView
              title="Final settlement"
              subtitle="Last working day 15 Feb 2026"
              lines={[
                { name: 'Salary to last day', kind: 'earning', amount: 6250000 },
                { name: 'Leave encashment', kind: 'earning', amount: 1800000 },
                { name: 'Notice recovery', kind: 'deduction', amount: 500000 },
                { name: 'Gratuity fund', kind: 'employer', amount: 120000 },
              ]}
              totals={{ gross: 8050000, deductions: 500000, net: 7550000 }}
              currency="INR"
              locale="en-IN"
              minorUnits
              hideEmployerContributions
            />
          ),
        },
      ],
    },
    doDont: {
      do: [
        'Pass the totals payroll calculated, exactly as on the PDF.',
        'Use `messages` and `lang` when the payslip was issued in another language.',
        'Explain unusual amounts with a line `note` (days paid, hours, rate).',
      ],
      dont: [
        'Do not compute net pay from the lines in the browser.',
        'Do not show negative signs on deductions; the group already says they are subtracted.',
        'Do not hide the issuer or period on a document people may forward.',
      ],
    },
    related: {
      items: [
        { id: 'money', reason: 'Formats each amount.' },
        { id: 'description-list', reason: 'Used for the details grid.' },
        { id: 'line-items-editor', reason: 'Editable priced rows for invoices.' },
        { id: 'receipt-preview', reason: 'Shows an uploaded expense receipt.' },
      ],
    },
    dependencies: {
      packages: ['react'],
      components: ['button', 'description-list', 'money'],
    },
    testing: {
      unit: [
        'Article named by the title; groups are named regions in fixed order with notes.',
        'Employer contributions can be hidden; heading levels follow `headingLevel`.',
        'Zero reimbursements are omitted; year-to-date tiles render with a custom title.',
        'Minor units format in the given locale (en-IN grouping).',
        'Download calls `onDownload` and shows the busy state.',
        'Translated labels via `messages`, `lang` and the empty message; axe and SSR.',
      ],
      axe: true,
      ssr: true,
      browser: ['Visual baselines: /__gallery', 'Cross-browser axe: /components/payslip-view'],
    },
  },
} satisfies ComponentDocs;
