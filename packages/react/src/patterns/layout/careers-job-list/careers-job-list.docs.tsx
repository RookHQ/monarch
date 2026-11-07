import { CareersJobList, type CareerJob } from '@rookcom/monarch-react/marketing';
import type { ComponentDocs } from '../../../internal/component-docs.js';

const jobs: CareerJob[] = [
  {
    id: 'j1',
    title: 'Product Designer',
    department: 'Design',
    location: 'Remote',
    employmentType: 'Full time',
    experience: '3+ years',
    salary: 'USD 90,000 – 120,000',
    summary: 'Shape how teams plan and track their work, from research to shipped flows.',
    responsibilities: ['Design new flows end to end', 'Run research sessions with customers'],
    qualifications: ['Three years of product design', 'A portfolio of shipped work'],
    mustHaveSkills: ['Prototyping', 'User research'],
    niceToHaveSkills: ['Motion design'],
    benefits: ['Learning budget', 'Flexible hours'],
    applyHref: '#apply-designer',
  },
  {
    id: 'j2',
    title: 'Support Specialist',
    department: 'Customer support',
    location: 'Lisbon',
    employmentType: 'Part time',
    summary: 'Help customers get the most out of the product.',
    mustHaveSkills: ['Written English', 'Patience'],
  },
  {
    id: 'j3',
    title: 'Backend Engineer',
    department: 'Engineering',
    location: 'Hybrid · Berlin',
    employmentType: 'Full time',
    summary: 'Build the APIs that power scheduling and billing.',
    mustHaveSkills: ['TypeScript', 'PostgreSQL'],
    applyHref: '#apply-backend',
  },
];

export default {
  id: 'careers-job-list',
  title: 'CareersJobList',
  summary:
    'The open-roles section of a public careers page: job cards and a details dialog with Apply.',
  sections: {
    purpose: {
      summary:
        'A company’s careers page lists its open roles; candidates scan the cards, open one for the details and apply. CareersJobList is that section, used on every careers page template. Needed by: Recruit (public careers pages on the path route and on custom domains).',
      useWhen: [
        'A public careers page or a careers block on a marketing site.',
        'Any public list of openings where details open in place and Apply goes to a form or email.',
      ],
      avoidWhen: [
        'The recruiter’s own job list inside the app: use DataTable.',
        'The application form itself: build it with Field components or the app’s apply wizard.',
      ],
    },
    designPrinciples: {
      items: [
        'Data in: the app formats salaries and types and passes each job’s apply link; the list never fetches.',
        'Server-safe props: apply links are strings on each job, never functions, so a Server Component can render the page.',
        'There is always a way to apply: the hosted form, email, or at least a clear Close.',
        'The whole card is clickable, but the accessible control is the job title.',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: ['Forwards its ref to the root `<section>`. Built on Dialog, Button and Badge.'],
    },
    typedProperties: {
      source: 'typescript',
      notes: [
        '`openJobId` + `onOpenJobChange` control which job’s details are open, for `?job=` deep links.',
        'Migrating from the app copy (Recruit `app/p/[username]/PublicJobsGrid.tsx`, used by `components/careers/PublicCareerPageView.tsx`): map `PublicJob` to `{ id, title, department, location, employmentType: type.replace("_", " ").toLowerCase(), experience: experienceText, salary: formatSalary(job), summary: roleOverview, description, responsibilities, qualifications, mustHaveSkills, niceToHaveSkills: goodToHaveSkills, benefits, applyHref: applyBasePath !== undefined ? `${applyBasePath}/jobs/${id}/apply` : null }`. `workspaceName` → `organizationName`, `contactEmail` stays, `columns` stays. The "Open roles" heading and count in each template move into the component (use `hideHeader` to keep your own). `brand` / `accent` colours become the product theme instead of inline styles. The page templates (hero, about, social links, "Powered by" footer) stay in Recruit; the marketing mockup `components/marketing/mockups/CareersPage.tsx` is illustration only.',
      ],
    },
    variants: {
      items: [
        {
          prop: 'columns',
          values: ['1', '2', '3'],
          description: 'Cards per row on wide screens (1 below 40rem).',
        },
        {
          prop: 'headingAs',
          values: ['h2', 'h3'],
          description: 'Section heading level; job titles are one level below.',
        },
        {
          prop: 'hideHeader',
          values: ['false', 'true'],
          description: 'Drop the heading and count when the page has its own.',
        },
      ],
    },
    states: {
      items: [
        { name: 'Empty', description: 'No jobs: a dashed box says there are no open roles.' },
        {
          name: 'Details open',
          attribute: 'aria-haspopup="dialog"',
          description: 'The job’s dialog is showing.',
        },
        {
          name: 'Apply',
          description:
            'Apply now (with Email instead), Apply by email, or Close when neither is set.',
        },
        { name: 'Columns', attribute: 'data-columns', description: 'The grid width.' },
      ],
    },
    interaction: {
      items: [
        'Clicking anywhere on a card, or pressing Enter on its title, opens the details.',
        'Escape, the close button or an outside click closes the details and returns focus to the card.',
        'Apply follows the job’s `applyHref`; email links open the mail app with the subject filled in.',
      ],
    },
    keyboard: {
      items: [
        { keys: 'Tab', action: 'Move between job titles.' },
        { keys: 'Enter / Space', action: 'Open a job’s details.' },
        { keys: 'Escape', action: 'Close the details and return to the card.' },
      ],
    },
    accessibility: {
      role: 'region with a list of job cards and a modal dialog',
      name: 'The region is named by "Open roles"; each title button reads "<title>, View details"; the dialog is named by the job title.',
      items: [
        'Each card is an `article` with its title as a heading, so screen-reader users can jump between jobs.',
        'Detail sections have headings; skills are lists of badges.',
        'Focus moves into the dialog and back to the card that opened it.',
      ],
    },
    responsive: {
      items: [
        'One column below 40rem, two up to 64rem, then `columns`. No horizontal scroll at 390px.',
        'The dialog body scrolls while the Apply footer stays visible.',
      ],
    },
    theme: {
      items: [
        'Surface cards with subtle borders and a medium shadow on hover; must-have skills use accent badges, so they follow the product brand.',
        'Forced colours outline cards in CanvasText and show titles as LinkText; reduced motion removes hover transitions.',
      ],
    },
    examples: {
      items: [
        {
          title: 'Two columns with apply links and email',
          stack: true,
          render: () => (
            <CareersJobList
              jobs={jobs}
              organizationName="Northwind"
              contactEmail="jobs@example.com"
            />
          ),
        },
        {
          title: 'Single column',
          stack: true,
          render: () => <CareersJobList jobs={jobs.slice(0, 2)} columns={1} />,
        },
        { title: 'No open roles', stack: true, render: () => <CareersJobList jobs={[]} /> },
      ],
    },
    doDont: {
      do: [
        'Format salary, type and location in the candidate’s language before passing them.',
        'Give every job an apply link or set `contactEmail`.',
      ],
      dont: [
        'Do not pass functions for apply links from a Server Component; build the string per job.',
        'Do not list closed or internal-only roles.',
      ],
    },
    related: {
      items: [
        { id: 'marketing-hero', reason: 'The careers page hero above the list.' },
        { id: 'marketing-section-blocks', reason: 'About and benefits sections around it.' },
        { id: 'dialog', reason: 'The details dialog.' },
      ],
    },
    dependencies: { packages: ['react'], components: ['dialog', 'button', 'badge'] },
    testing: {
      unit: [
        'Region, count, job headings, meta and skills list.',
        'Enter opens the named dialog with sections and apply links; Escape returns focus.',
        'Email fallback, then Close; controlled deep link; empty state; hidden header; translation.',
      ],
      axe: true,
      ssr: true,
    },
  },
} satisfies ComponentDocs;
