/**
 * Home page showcase: a composed grid of cards built only from live Monarch
 * components. The content is neutral demo data (fictional people, no real
 * companies), and every date is relative to the visitor's today, so the
 * calendar and the messages always read as current.
 */
import { useMemo, useState } from 'react';
import {
  Badge,
  Button,
  Card,
  CardContent,
  CardDescription,
  CardFooter,
  CardHeader,
  CardTitle,
  Field,
  FieldDescription,
  FieldLabel,
  Input,
  SegmentedControl,
  StatCard,
  StatCardGroup,
  Switch,
} from '@rookcom/monarch-react';
import { BarChart, Sparkline } from '@rookcom/monarch-react/charts';
import {
  AgendaList,
  DatePicker,
  MiniMonth,
  type CalendarEvent,
  type MiniMonthDayInfo,
} from '@rookcom/monarch-react/scheduling';
import { MessageRow, type MessageReaction } from '@rookcom/monarch-react/collaboration';

const LOCALE = 'en-US';

const pad = (n: number) => String(n).padStart(2, '0');
const isoDay = (d: Date) => `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`;
const addDays = (d: Date, n: number) => new Date(d.getFullYear(), d.getMonth(), d.getDate() + n);
const at = (d: Date, h: number, m = 0) =>
  new Date(d.getFullYear(), d.getMonth(), d.getDate(), h, m).toISOString();

const SIGNUPS = [
  { month: 'Jun', web: 320, mobile: 180 },
  { month: 'Jul', web: 380, mobile: 210 },
  { month: 'Aug', web: 360, mobile: 260 },
  { month: 'Sep', web: 450, mobile: 300 },
  { month: 'Oct', web: 520, mobile: 340 },
  { month: 'Nov', web: 610, mobile: 390 },
];

function SharingCard() {
  const [access, setAccess] = useState('team');
  const [comments, setComments] = useState(true);
  const [digest, setDigest] = useState(false);
  return (
    <Card className="docs-show__card" data-area="controls">
      <CardHeader>
        <CardTitle>Sharing</CardTitle>
        <CardDescription>Choose who can open this project.</CardDescription>
      </CardHeader>
      <CardContent className="docs-show__stack">
        <SegmentedControl
          aria-label="Access"
          value={access}
          onValueChange={setAccess}
          options={[
            { value: 'private', label: 'Private' },
            { value: 'team', label: 'Team' },
            { value: 'public', label: 'Public' },
          ]}
        />
        <Field>
          <FieldLabel>Invite people</FieldLabel>
          <Input type="email" placeholder="name@example.com" />
        </Field>
        <div className="docs-show__switches">
          <Switch label="Allow comments" checked={comments} onCheckedChange={setComments} />
          <Switch label="Weekly summary" checked={digest} onCheckedChange={setDigest} />
        </div>
        <div className="docs-show__row">
          <Badge tone="accent">New</Badge>
          <Badge tone="success">Live</Badge>
          <Badge tone="warning">In review</Badge>
          <Badge>Draft</Badge>
        </div>
      </CardContent>
      <CardFooter className="docs-show__actions">
        <Button variant="ghost">Cancel</Button>
        <Button variant="primary">Save</Button>
      </CardFooter>
    </Card>
  );
}

function StatsCard() {
  return (
    <StatCardGroup
      className="docs-show__stats"
      data-area="stats"
      label="This month"
      columns={3}
      minItemWidth="10rem"
    >
      <StatCard
        label="Revenue"
        value={84_200}
        format="currency"
        currency="USD"
        notation="compact"
        locale={LOCALE}
        change={{ value: 0.124, locale: LOCALE }}
        caption="vs last month"
      />
      <StatCard
        label="Active members"
        value={1_942}
        format="number"
        locale={LOCALE}
        change={{ current: 1_942, previous: 1_810, locale: LOCALE }}
        caption="vs last month"
        sparkline={
          <Sparkline values={[1610, 1680, 1720, 1790, 1810, 1880, 1942]} responsive height={28} />
        }
      />
      <StatCard
        label="First reply"
        value={1.8}
        unit="h"
        format="number"
        locale={LOCALE}
        change={{ current: 1.8, previous: 2.6, goodDirection: 'down', locale: LOCALE }}
        caption="vs last month"
      />
    </StatCardGroup>
  );
}

function ChartCard() {
  return (
    <Card className="docs-show__card" data-area="chart">
      <CardContent className="docs-show__chart">
        <BarChart
          title="New sign-ups"
          description="By month, web and mobile"
          headingLevel={3}
          data={SIGNUPS}
          categoryKey="month"
          categoryLabel="Month"
          series={[
            { key: 'web', label: 'Web' },
            { key: 'mobile', label: 'Mobile' },
          ]}
          locale={LOCALE}
          height={220}
        />
      </CardContent>
    </Card>
  );
}

function FormCard({ today }: { today: string }) {
  const [start, setStart] = useState<string | null>(null);
  return (
    <Card className="docs-show__card" data-area="form">
      <CardHeader>
        <CardTitle>Invite a teammate</CardTitle>
        <CardDescription>They will get an email with a link to join.</CardDescription>
      </CardHeader>
      <CardContent className="docs-show__stack">
        <Field>
          <FieldLabel>Full name</FieldLabel>
          <Input placeholder="Alex Morgan" autoComplete="off" />
        </Field>
        <Field>
          <FieldLabel>Email</FieldLabel>
          <Input type="email" placeholder="alex@example.com" autoComplete="off" />
        </Field>
        <Field>
          <FieldLabel>Start date</FieldLabel>
          <DatePicker
            locale={LOCALE}
            today={today}
            min={today}
            value={start}
            onValueChange={setStart}
          />
          <FieldDescription>Type a date or pick one.</FieldDescription>
        </Field>
      </CardContent>
      <CardFooter className="docs-show__actions">
        <Button variant="primary">Send invite</Button>
      </CardFooter>
    </Card>
  );
}

function ScheduleCard({ now }: { now: Date }) {
  const today = isoDay(now);
  const [day, setDay] = useState<string | null>(today);
  const { events, dayInfo } = useMemo(() => {
    const tomorrow = addDays(now, 1);
    const list: CalendarEvent[] = [
      { id: 'focus', title: 'Focus time', start: at(now, 9), end: at(now, 11) },
      {
        id: 'review',
        title: 'Design review',
        start: at(now, 14),
        end: at(now, 15),
        detail: 'Video call',
      },
      { id: 'kickoff', title: 'Project kickoff', start: at(tomorrow, 10), end: at(tomorrow, 11) },
      {
        id: 'lunch',
        title: 'Team lunch',
        start: at(tomorrow, 12, 30),
        end: at(tomorrow, 13, 30),
        status: 'tentative',
      },
    ];
    const info: Record<string, MiniMonthDayInfo> = {
      [today]: { dots: ['accent', 'info'], description: '2 events' },
      [isoDay(tomorrow)]: { dots: ['accent', 'success'], description: '2 events' },
      [isoDay(addDays(now, 6))]: { dots: ['warning'], description: 'Deadline' },
    };
    return { events: list, dayInfo: info };
  }, [now, today]);
  return (
    <Card className="docs-show__card" data-area="agenda">
      <CardHeader>
        <CardTitle>Schedule</CardTitle>
      </CardHeader>
      <CardContent className="docs-show__stack">
        <MiniMonth
          locale={LOCALE}
          today={today}
          value={day}
          onValueChange={setDay}
          getDayInfo={(d) => dayInfo[d]}
          aria-label="Choose a day"
        />
        <AgendaList events={events} locale={LOCALE} today={today} headingLevel={4} />
      </CardContent>
    </Card>
  );
}

function MessagesCard({ now }: { now: Date }) {
  const [reactions, setReactions] = useState<MessageReaction[]>([
    { emoji: '👍', label: 'thumbs up', count: 3, reacted: true, title: 'You and 2 others' },
    { emoji: '🎉', label: 'party popper', count: 2 },
  ]);
  const toggle = (emoji: string) =>
    setReactions((list) =>
      list.map((r) =>
        r.emoji === emoji
          ? { ...r, reacted: !r.reacted, count: r.count + (r.reacted ? -1 : 1) }
          : r,
      ),
    );
  const minutesAgo = (m: number) => new Date(now.getTime() - m * 60_000).toISOString();
  return (
    <Card className="docs-show__card" data-area="message">
      <CardHeader>
        <CardTitle>Messages</CardTitle>
      </CardHeader>
      <CardContent className="docs-show__messages">
        <MessageRow
          author={{ name: 'Maya Chen' }}
          time={minutesAgo(18)}
          now={now.toISOString()}
          locale={LOCALE}
          reactions={reactions}
          onReactionToggle={toggle}
        >
          <p>The new onboarding flow is live. It feels so much calmer.</p>
        </MessageRow>
        <MessageRow
          author={{ name: 'Sam Rivera' }}
          time={minutesAgo(4)}
          now={now.toISOString()}
          locale={LOCALE}
        >
          <p>Agreed. Dark mode looks great on my phone, too.</p>
        </MessageRow>
      </CardContent>
    </Card>
  );
}

/** The showcase grid. Dates follow the visitor's device, read once per visit. */
export function Showcase() {
  const now = useMemo(() => new Date(), []);
  return (
    <div className="docs-show">
      <SharingCard />
      <StatsCard />
      <ChartCard />
      <FormCard today={isoDay(now)} />
      <ScheduleCard now={now} />
      <MessagesCard now={now} />
    </div>
  );
}
