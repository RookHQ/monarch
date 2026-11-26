import { useState } from 'react';
import {
  Avatar,
  BackLink,
  Badge,
  Button,
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
  IconButton,
  InlineNotice,
  StatusBadge,
  TabsLink,
  TabsNav,
} from '@rookcom/monarch-react';
import { RecordHeader } from '@rookcom/monarch-react/patterns';
import type { ComponentDocs } from '../../../internal/component-docs.js';
import { MoreIcon } from '../../../internal/docs-demo.js';

const tickets = [
  'Printer offline on floor 3',
  'Invoice shows the wrong address',
  'Reset two-factor',
];

function TicketDemo() {
  const [index, setIndex] = useState(0);
  const [subjects, setSubjects] = useState(tickets);
  return (
    <RecordHeader
      headingLevel="h2"
      back={<BackLink href="#/components/record-header">Inbox</BackLink>}
      navigator={{
        position: { index, total: subjects.length },
        previousLabel: 'Previous ticket',
        nextLabel: 'Next ticket',
        ...(index > 0 ? { onPrevious: () => setIndex(index - 1) } : {}),
        ...(index < subjects.length - 1 ? { onNext: () => setIndex(index + 1) } : {}),
      }}
      title={subjects[index] ?? ''}
      titleLabel="Subject"
      onTitleSave={(next) => setSubjects(subjects.map((s, i) => (i === index ? next : s)))}
      status={<StatusBadge status="info" label="Open" />}
      badges={<Badge tone="warning">High</Badge>}
      subtitle={<span>#{1040 + index} · Opened 2 hours ago by Grace Hopper</span>}
    />
  );
}

export default {
  id: 'record-header',
  title: 'RecordHeader',
  summary:
    'The header of a record detail page: avatar, editable title, status, key facts, actions, prev/next, state banner and tabs.',
  sections: {
    purpose: {
      summary:
        'Heads the detail page of one record (a contact, a ticket, a project): a back link and previous/next navigation, an avatar or icon, the title (editable in place), status and badges, a subtitle, key facts, record actions, a state banner (archived, merged) and section tabs. The product supplies all data and callbacks.',
      useWhen: [
        'The detail page or detail pane of one record.',
        'People move through a list record by record (prev/next with a position).',
        'The record’s name can be renamed in place.',
      ],
      avoidWhen: [
        'A page that is not about one record (a list, settings, a dashboard): use PageHeader.',
        'Facts that are the main content of the page: use a DescriptionList in the body.',
      ],
    },
    designPrinciples: {
      items: [
        'Identity first: avatar, name and status read as one block.',
        'Key facts only: three to six facts that orient, not the whole record.',
        'State is visible: archived, merged or deleted records say so in the banner, under the identity.',
        'No business logic: the product decides which actions, facts and neighbours exist.',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: [
        'Renders a `<header>`, forwards its ref and passes native attributes through.',
        'With `onTitleSave` and a string `title`, the title is an InlineEditText inside a `role="heading"` element (a block editor may not sit in an `<h1>`). `titleEditProps` passes validate, maxLength and the rest through.',
        '`facts` render as a DescriptionList with `columns="auto"` (9rem minimum); `copyable` and empty placeholders work as there.',
        '`navigator` is `{ position?, onPrevious?, onNext?, previousLabel?, nextLabel?, formatPosition? }`; a missing handler disables its button. Keyboard shortcuts such as J/K belong to the product.',
        'Exported from `@rookcom/monarch-react/patterns`.',
      ],
    },
    typedProperties: { source: 'typescript' },
    variants: {
      items: [
        {
          prop: 'avatar / status / badges / subtitle / facts / actions / back / navigator / banner / tabs',
          values: ['omitted', 'provided'],
          description: 'Each part renders only when provided.',
        },
        {
          prop: 'headingLevel',
          values: ['h1', 'h2'],
          description:
            '`h1` (default) on a record page; `h2` inside an inbox detail pane under the page h1.',
        },
        {
          prop: 'onTitleSave',
          values: ['omitted', 'function'],
          description: 'Makes a text title editable in place. Exposed as `data-editable`.',
        },
      ],
    },
    states: {
      items: [
        {
          name: 'Static',
          description: 'Without `onTitleSave` and `navigator` the header has no state of its own.',
        },
        {
          name: 'Editing title',
          description:
            'InlineEditText states: editing, saving (spinner), error (kept open with the message).',
        },
        {
          name: 'First / last record',
          description: 'The previous or next button is disabled when its handler is omitted.',
        },
      ],
    },
    interaction: {
      items: [
        'Activating the title (when editable) opens the editor; Enter or blur saves, Escape cancels.',
        'Previous and next call `onPrevious` and `onNext`; the product navigates and updates `position`.',
        'Fact copy buttons copy their value.',
      ],
    },
    keyboard: {
      items: [
        {
          keys: 'Tab',
          action:
            'Back link, previous/next, title editor, actions, fact copy buttons, banner actions, tabs.',
        },
        { keys: 'Enter', action: 'On the title, starts editing; in the editor, saves.' },
        { keys: 'Escape', action: 'In the title editor, cancels and restores the title.' },
      ],
    },
    accessibility: {
      role: 'banner landmark only when it is not inside main, article or section (native <header> rules)',
      name: 'The title is the page or pane heading.',
      items: [
        'The title is an `h1` (or `h2`); an editable title keeps the heading role and level.',
        'Previous/next are icon buttons with required names; name the record type ("Next ticket").',
        'Put role="status" content in `banner` (InlineNotice) so state changes are announced.',
        'Give an actions menu a specific name: "More actions for Ada Lovelace".',
        'The live examples on this page render with `headingLevel="h2"` because the docs page has its own h1; in an app use the default h1.',
      ],
    },
    responsive: {
      items: [
        'The identity block and the actions wrap; actions take the full width under 480px.',
        'Facts fill as many 9rem columns as fit and become one column at 640px and below.',
        'Long titles wrap anywhere; the title steps down a size under 480px.',
        'Actions, navigator and tabs are hidden when printing.',
      ],
    },
    theme: {
      items: ['Text, borders and dividers use Monarch semantic tokens, so every theme applies.'],
    },
    examples: {
      items: [
        {
          title: 'Contact with facts, actions and tabs',
          stack: true,
          render: () => (
            <RecordHeader
              headingLevel="h2"
              back={<BackLink href="#/components/record-header">All contacts</BackLink>}
              avatar={<Avatar name="Ada Lovelace" size="lg" />}
              title="Ada Lovelace"
              status={<StatusBadge status="success" label="Active" />}
              badges={<Badge>Enterprise</Badge>}
              subtitle={<span>ada@example.com · Head of Engineering, Northfield</span>}
              facts={[
                { term: 'Owner', value: 'Grace Hopper' },
                { term: 'Lifecycle', value: 'Customer' },
                { term: 'Contact ID', value: 'c_1042', copyable: true },
                { term: 'Phone', value: null },
              ]}
              actions={
                <>
                  <Button>Email</Button>
                  <DropdownMenu>
                    <DropdownMenuTrigger asChild>
                      <IconButton
                        label="More actions for Ada Lovelace"
                        icon={<MoreIcon />}
                        variant="secondary"
                      />
                    </DropdownMenuTrigger>
                    <DropdownMenuContent align="end">
                      <DropdownMenuItem>Merge</DropdownMenuItem>
                      <DropdownMenuItem>Archive</DropdownMenuItem>
                      <DropdownMenuItem tone="danger">Delete contact</DropdownMenuItem>
                    </DropdownMenuContent>
                  </DropdownMenu>
                </>
              }
              tabs={
                <TabsNav aria-label="Contact sections">
                  <TabsLink href="#/components/record-header" active>
                    Overview
                  </TabsLink>
                  <TabsLink href="#/components/record-header">Activity</TabsLink>
                  <TabsLink href="#/components/record-header">Deals</TabsLink>
                </TabsNav>
              }
            />
          ),
        },
        {
          title: 'Ticket with editable subject and prev/next',
          stack: true,
          render: () => <TicketDemo />,
        },
        {
          title: 'Record state banner',
          stack: true,
          render: () => (
            <RecordHeader
              headingLevel="h2"
              title="Q3 renewal"
              status={<StatusBadge status="neutral" label="Archived" />}
              subtitle="Northfield · Opportunity"
              banner={
                <InlineNotice tone="neutral">
                  Archived on 2 May. It no longer appears in pipelines or reports.
                </InlineNotice>
              }
              actions={<Button>Unarchive</Button>}
            />
          ),
        },
      ],
    },
    doDont: {
      do: [
        'Keep facts to the few that orient someone on arrival.',
        'Name previous/next after the record type.',
        'Show archived, merged and deleted states in `banner`.',
      ],
      dont: [
        'Do not put a second h1 on a page that has a RecordHeader.',
        'Do not make a rich (non-text) title editable; `onTitleSave` needs a string title.',
        'Do not repeat every field of the record in `facts`.',
      ],
    },
    related: {
      items: [
        { id: 'page-header', reason: 'Headers for pages that are not one record.' },
        {
          id: 'description-list',
          reason: 'The facts row; use it in the body for the full record.',
        },
        { id: 'inline-edit-text', reason: 'The editable title.' },
        { id: 'split-inbox-layout', reason: 'Hosts a RecordHeader (h2) in its detail pane.' },
        { id: 'breadcrumb', reason: 'An alternative to BackLink in `back`.' },
      ],
    },
    dependencies: {
      packages: ['react'],
      components: ['button', 'description-list', 'inline-edit-text'],
    },
    testing: {
      unit: [
        'Renders back link, avatar, title (h1), status and badges, subtitle, facts (dl with copy and placeholder), actions, banner and tabs; ref forwarding.',
        'Prev/next: position text, custom labels, disabled first record, keyboard activation.',
        'Custom position format; no position for an empty list.',
        'Editable title keeps the heading role; Enter saves, Escape cancels; extra editor props apply.',
        'A non-text title stays a plain heading.',
        'Renders on the server.',
      ],
      axe: true,
      ssr: true,
      browser: [
        'Visual baselines: /__gallery/record-header',
        'Cross-browser axe: /components/record-header',
      ],
    },
  },
} satisfies ComponentDocs;
