import {
  Avatar,
  Badge,
  Button,
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from '@rookcom/monarch-react';
import {
  CTABand,
  FAQ,
  FeatureGrid,
  MarketingHero,
  MarketingSection,
  MarketingSectionHeader,
  ProductFrame,
  SiteFooter,
  SiteHeader,
  StatStrip,
} from '@rookcom/monarch-react/marketing';
import './example.css';
import { IconHistory, IconPages, IconSearch, IconShare, IconSlash, IconBot } from './icons.js';

/** Example wordmark: a product mark in the accent plus the product name. */
function Wordmark({ name }: { name: string }) {
  return (
    <a href="#/__marketing" className="mx-wordmark">
      <span className="mx-wordmark__mark" aria-hidden>
        {name.charAt(0)}
      </span>
      <span className="mx-wordmark__name">
        Rook <b>{name}</b>
      </span>
    </a>
  );
}

/** The illustrated editor, built from real Monarch components. */
function EditorIllustration() {
  return (
    <div className="mx-editor">
      <aside className="mx-editor__outline">
        <p className="mx-editor__doc">Launch plan</p>
        {['Overview', 'Goals', 'Requirements', 'Risks', 'Timeline'].map((s, i) => (
          <p key={s} className="mx-editor__item" data-active={i === 0 || undefined}>
            <span>0{i + 1}</span> {s}
          </p>
        ))}
      </aside>
      <div className="mx-editor__page">
        <div className="mx-editor__meta">
          <Badge tone="accent">In review</Badge>
          <span className="mx-editor__people">
            <Avatar name="Maya Iyer" size="xs" decorative />
            <Avatar name="Leo Park" size="xs" decorative />
            <Avatar name="Sara Okafor" size="xs" decorative />
          </span>
        </div>
        <p className="mx-editor__title">A clearer first run</p>
        <p className="mx-editor__lead">What we are shipping, who it is for, and why now.</p>
        <Table density="compact" className="mx-editor__table">
          <TableHeader>
            <TableRow>
              <TableHead>Decision</TableHead>
              <TableHead>Owner</TableHead>
              <TableHead>Status</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            <TableRow>
              <TableCell>Ship guided onboarding</TableCell>
              <TableCell>Product</TableCell>
              <TableCell>
                <Badge tone="success" size="sm">
                  Agreed
                </Badge>
              </TableCell>
            </TableRow>
            <TableRow>
              <TableCell>Export to PDF by default</TableCell>
              <TableCell>Design</TableCell>
              <TableCell>
                <Badge tone="warning" size="sm">
                  Open
                </Badge>
              </TableCell>
            </TableRow>
            <TableRow>
              <TableCell>Retire legacy templates</TableCell>
              <TableCell>Ops</TableCell>
              <TableCell>
                <Badge size="sm">Later</Badge>
              </TableCell>
            </TableRow>
          </TableBody>
        </Table>
      </div>
      <aside className="mx-editor__assistant">
        <p className="mx-editor__assistant-title">
          <IconBot width={16} height={16} /> Assistant
        </p>
        <p className="mx-editor__bubble">Summarise the open decisions</p>
        <p className="mx-editor__answer">
          One decision is still open: whether PDF becomes the default export. Design owns it.
        </p>
      </aside>
    </div>
  );
}

function ShareIllustration() {
  return (
    <div className="mx-share">
      <p className="mx-share__title">Share “Launch plan”</p>
      <div className="mx-share__row">
        <span className="mx-share__field">Anyone with the link can view</span>
        <Button size="sm" variant="primary">
          Copy link
        </Button>
      </div>
      <div className="mx-share__grid">
        <span>Expires</span>
        <b>In 30 days</b>
        <span>Password</span>
        <b>On</b>
        <span>Views</span>
        <b>128</b>
      </div>
    </div>
  );
}

const nav = [
  {
    label: 'Product',
    items: [
      { label: 'Editor', href: '#/__marketing', description: 'Pages, blocks and a slash menu' },
      { label: 'Search', href: '#/__marketing', description: 'Find any page by meaning' },
      { label: 'Sharing', href: '#/__marketing', description: 'Controlled links and exports' },
    ],
  },
  {
    label: 'Solutions',
    items: [
      { label: 'Product teams', href: '#/__marketing', description: 'Specs and decision logs' },
      { label: 'Operations', href: '#/__marketing', description: 'Runbooks that stay current' },
    ],
  },
  { label: 'Templates', href: '#/__marketing' },
  { label: 'Pricing', href: '#/__marketing' },
];

/**
 * A complete marketing page composed only from Monarch. It is both the
 * reference composition for Rook product sites and a visual-regression surface.
 */
export function MarketingExample({ productName }: { productName: string }) {
  return (
    <div className="mx-page" data-testid="marketing">
      <SiteHeader
        brand={<Wordmark name={productName} />}
        nav={nav}
        actions={
          <>
            <Button variant="ghost" size="sm">
              Log in
            </Button>
            <Button variant="primary" size="sm">
              Start free
            </Button>
          </>
        }
      />
      <main id="main">
        <MarketingHero
          eyebrow={`Rook ${productName}`}
          title={
            <>
              Write it once. <span className="mx-nowrap">Find it whenever.</span>
            </>
          }
          lead="Structured documents for teams whose specs, decisions and runbooks need to stay connected, searchable and current."
          actions={
            <>
              <Button variant="primary">Start writing</Button>
              <Button variant="secondary">See how it works</Button>
            </>
          }
          note="Free for teams of up to 10. No card needed."
          media={
            <ProductFrame
              label="Editor showing a launch plan with an outline, a decisions table and the Assistant panel"
              title="Launch plan"
              backdrop
            >
              <EditorIllustration />
            </ProductFrame>
          }
        />

        <MarketingSection spacing="lg" aria-labelledby="mx-features">
          <MarketingSectionHeader
            titleId="mx-features"
            eyebrow="The editor"
            title="Every idea gets the right shape."
            description="Type a slash and pick a block. Structure stays in the document, so it survives exports, sharing and search."
          />
          <FeatureGrid
            columns={3}
            items={[
              {
                icon: <IconPages />,
                title: 'Multi-page documents',
                description:
                  'Long work splits into pages with their own titles, and an outline that follows your headings.',
              },
              {
                icon: <IconSlash />,
                title: 'Slash commands',
                description:
                  'Tables, task lists, callouts and code blocks are one keystroke away, and stay editable.',
              },
              {
                icon: <IconSearch />,
                title: 'Search by meaning',
                description:
                  'Find the page that answers the question, even when it uses different words.',
              },
              {
                icon: <IconShare />,
                title: 'Controlled sharing',
                description:
                  'Links with expiry, passwords and view counts. Revoke any link at any time.',
              },
              {
                icon: <IconHistory />,
                title: 'Version history',
                description:
                  'Named snapshots and line-level changes. Restore safely, without losing newer work.',
              },
              {
                icon: <IconBot />,
                title: 'Assistant in context',
                description:
                  'Ask about the page you are on. Answers cite the document, not the internet.',
              },
            ]}
          />
        </MarketingSection>

        <MarketingSection tone="subtle" spacing="lg" aria-labelledby="mx-share">
          <div className="mx-split">
            <MarketingSectionHeader
              titleId="mx-share"
              eyebrow="Sharing"
              title="Share the work, not the workspace."
              description="Send a read-only page to anyone outside the team. You decide how long it lives, who can open it, and you can see when it is read."
              actions={<Button variant="secondary">Learn about sharing</Button>}
            />
            <ProductFrame
              label="Share dialog with a copyable link, a 30-day expiry, password on, and 128 views"
              chrome="browser"
              title="dossier.rookhq.com/share/launch-plan"
            >
              <ShareIllustration />
            </ProductFrame>
          </div>
        </MarketingSection>

        <MarketingSection tone="inverse" spacing="lg" aria-labelledby="mx-numbers">
          <MarketingSectionHeader
            titleId="mx-numbers"
            eyebrow="Built for teams that write things down"
            title="One place for the documents that run the company."
            description="Every format a team needs, in every language the team speaks."
          />
          <StatStrip
            items={[
              { value: '24', label: 'Editor blocks' },
              { value: '3', label: 'Export formats', description: 'HTML, Markdown and PDF' },
              { value: '13', label: 'Interface languages' },
              { value: '1', label: 'Rook ID for every app' },
            ]}
          />
        </MarketingSection>

        <MarketingSection spacing="lg" width="content" aria-labelledby="mx-faq">
          <div className="mx-faq">
            <MarketingSectionHeader
              titleId="mx-faq"
              eyebrow="Questions"
              title="Answers before you sign up."
            />
            <FAQ
              defaultOpen={0}
              items={[
                {
                  question: 'Can people outside my team read a document?',
                  answer:
                    'Yes. Create a share link for a read-only copy. Links can expire, need a password and be revoked at any time.',
                },
                {
                  question: 'Which formats can I export?',
                  answer:
                    'HTML, Markdown and a styled PDF, all from the same source, so the structure survives.',
                },
                {
                  question: 'Does the Assistant read my whole workspace?',
                  answer:
                    'It works with the page you are on. It can insert text at your cursor or improve a selection you choose.',
                },
                {
                  question: 'Do I need a separate account?',
                  answer: 'No. One Rook ID signs you in to every Rook app.',
                },
              ]}
            />
          </div>
        </MarketingSection>

        <MarketingSection spacing="sm" width="wide" className="mx-cta-section">
          <CTABand
            title="Start with a blank page, or a structure already made."
            description="Templates for specs, runbooks and reviews. Change anything."
            actions={
              <>
                <Button variant="primary">Start writing</Button>
                <Button variant="secondary">Browse templates</Button>
              </>
            }
          />
        </MarketingSection>
      </main>
      <SiteFooter
        brand={<Wordmark name={productName} />}
        description="Structured documents for teams. Part of Rook."
        columns={[
          {
            title: 'Product',
            links: [
              { label: 'Editor', href: '#/__marketing' },
              { label: 'Search', href: '#/__marketing' },
              { label: 'Sharing', href: '#/__marketing' },
              { label: 'Templates', href: '#/__marketing' },
            ],
          },
          {
            title: 'Solutions',
            links: [
              { label: 'Product teams', href: '#/__marketing' },
              { label: 'Operations', href: '#/__marketing' },
            ],
          },
          {
            title: 'Resources',
            links: [
              { label: 'Documentation', href: '#/__marketing' },
              { label: 'Status', href: '#/__marketing' },
            ],
          },
          {
            title: 'Company',
            links: [
              { label: 'About Rook', href: '#/__marketing' },
              { label: 'Careers', href: '#/__marketing' },
            ],
          },
        ]}
        legal="© 2026 Rook"
        legalLinks={[
          { label: 'Terms', href: '#/__marketing' },
          { label: 'Privacy', href: '#/__marketing' },
        ]}
      />
    </div>
  );
}
