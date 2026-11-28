/**
 * Vite plugin that turns the native packages (`platforms/android`,
 * `platforms/ios`) into docs-site data. Everything the site says about
 * Monarch for Android and Monarch for iOS is read here, at build time, from
 * the files those packages own, so the pages cannot drift from them.
 *
 * - `virtual:monarch-native-nav`: which native guides exist. Part of the entry
 *   chunk (the sidebar and the route table need it); a few bytes.
 * - `virtual:monarch-native-facts`: versions, requirements and counts per
 *   platform, read from the version catalog, the Swift manifest, the
 *   changelogs, the inventories and `platforms/parity.json`.
 * - `virtual:monarch-native-catalogue/<platform>`: one trimmed row per
 *   inventory entry and per planned component, for the catalogue page.
 * - `virtual:monarch-native-component/<id>`: both platforms' inventory entry
 *   (or the plan, when there is no entry yet) and preview images for one web
 *   component id. One small chunk per id; each `<id>.docs.tsx` gets a
 *   `loadNative` export that imports it, so only that component's page loads it.
 * - `virtual:monarch-native-platform/<platform>`: sections of the platform's
 *   README rendered to HTML, the inventory entries that have no web page
 *   (theme, icon) and a few compiled examples, for the landing page.
 * - `virtual:monarch-native-guide/<id>` and
 *   `virtual:monarch-native-changelog/<platform>`: a guide or changelog
 *   rendered to HTML, or `null` when the file does not exist.
 *
 * A missing inventory, guide, changelog or README section never fails the
 * build: the pages say less. An inventory file that is not in the shape of
 * `platforms/ios/inventory/schema.json` is skipped with a warning.
 */
import { existsSync, readFileSync, readdirSync } from 'node:fs';
import path from 'node:path';
import type { Plugin } from 'vite';
import { renderBody, renderGuide, slug, type RenderedGuide } from './guides.js';

export const PLATFORMS = ['android', 'ios'] as const;
export type Platform = (typeof PLATFORMS)[number];

export type Coverage = 'build' | 'style' | 'native' | 'compose' | 'web-only';
export type Status = 'available' | 'experimental' | 'planned' | 'deprecated';
const COVERAGE = new Set<string>(['build', 'style', 'native', 'compose', 'web-only']);
const STATUS = new Set<string>(['available', 'experimental', 'planned', 'deprecated']);

/** One inventory file (`platforms/<platform>/inventory/<id>.json`), as its schema defines it. */
export interface InventoryEntry {
  id: string;
  name: string | null;
  title: string;
  family: string;
  coverage: Coverage;
  wrapsNative: string | null;
  status: Status;
  since: string;
  summary: string;
  whenToUse: string;
  whenNotToUse: string;
  api: string[];
  options: { name: string; type: string; default: string; description: string }[];
  states: { name: string; description: string }[];
  appearance: string;
  platformNotes: string;
  /**
   * Written exceptions to the cross-platform parity rule (CROSS-PLATFORM.md section 8): where this platform
   * differs from the web on purpose, why, and the platform convention it follows.
   */
  parityExceptions: { rule: string; reason: string; convention: string }[];
  accessibility: string[];
  edgeCases: string[];
  tests: string[];
  examples: { title: string; code: string; setup?: string; imports?: string[] }[];
  dependsOn: string[];
  /**
   * In the patterns layer (`"layer": "patterns"`): an experimental domain
   * composite, shown with a "Pattern (experimental)" badge and in its own
   * catalogue group (ADR 0017).
   */
  pattern: boolean;
}

interface ParityPlatform {
  coverage: Coverage;
  name?: string | null;
  uses?: string | null;
  note?: string | null;
}
interface ParityEntry {
  id: string;
  family: string;
  title: string;
  android: ParityPlatform;
  ios: ParityPlatform;
  wave?: number;
}

/** The plan for a component on one platform, from `platforms/parity.json`. */
export interface NativePlan {
  coverage: Coverage;
  name: string | null;
  uses: string | null;
  note: string | null;
  wave: number | null;
}

export interface PreviewImage {
  src: string;
  width: number;
  height: number;
}
export type Previews = Partial<Record<'light' | 'dark', PreviewImage>>;

/** What a component page shows for one platform. */
export interface NativePlatformDocs {
  entry: Omit<InventoryEntry, 'id' | 'title' | 'family'> | null;
  plan: NativePlan | null;
  previews: Previews;
}
export type NativeComponentDocs = Record<Platform, NativePlatformDocs>;

/** One row of a platform's catalogue. */
export interface CatalogueRow {
  id: string;
  title: string;
  family: string;
  coverage: Coverage;
  /** The inventory's status, or `planned` when only the plan has the component. */
  status: Status;
  /** The Monarch type, when Monarch ships one. */
  name: string | null;
  /** The platform API it stands on. */
  uses: string | null;
  /** In the inventory (shipped documentation), not only in the plan. */
  entry: boolean;
  /** Has a page on this site (`/components/<id>/`). */
  web: boolean;
  /** In the patterns layer (ADR 0017). */
  pattern?: true;
  summary?: string;
  since?: string;
}

const NAV_ID = 'virtual:monarch-native-nav';
const FACTS_ID = 'virtual:monarch-native-facts';
const CATALOGUE_ID = 'virtual:monarch-native-catalogue/';
const COMPONENT_ID = 'virtual:monarch-native-component/';
const PLATFORM_ID = 'virtual:monarch-native-platform/';
const GUIDE_ID = 'virtual:monarch-native-guide/';
const CHANGELOG_ID = 'virtual:monarch-native-changelog/';
const DOCS_FILE = /[\\/]packages[\\/]react[\\/]src[\\/].+[\\/]([^\\/]+)\.docs\.tsx$/;

/** Native guides rendered as pages, by route id (`/native/<id>/`). */
export const NATIVE_GUIDES: Readonly<Record<string, string>> = {
  'adaptive-layout': 'docs/guides/native-adaptive-layout.md',
  accessibility: 'docs/guides/native-accessibility-and-testing.md',
  compatibility: 'docs/architecture/NATIVE-COMPATIBILITY.md',
  contributing: 'docs/guides/native-contributing.md',
};

/** README sections each landing page shows, by heading slug. */
const README_SECTIONS: Readonly<Record<Platform, readonly string[]>> = {
  android: [
    'modules',
    'requirements',
    'install-from-source',
    'getting-started',
    'theme-setup',
    'how-monarch-maps-to-material-3',
    'tokens',
    'accessibility',
    'public-api-policy',
  ],
  ios: [
    'requirements',
    'installing',
    'getting-started',
    'theme-setup',
    'token-usage',
    'accessibility',
    'experimental-api',
    'known-limitations',
  ],
};

/** Commands the landing pages quote. Each is shown only while the README still has it. */
const README_COMMANDS: Readonly<Record<Platform, Readonly<Record<string, string>>>> = {
  android: {
    sampleInstall: './gradlew :sample:installDebug',
    sampleBuild: './gradlew :sample:assembleDebug',
    verify: './gradlew verifyMonarch',
  },
  ios: {
    verify: 'Scripts/verify.sh',
    sampleGenerate: 'xcodegen generate --spec Sample/project.yml',
  },
};

/** The landing page's install snippet: a code block of a README section (0 = first, -1 = last). */
const LANDING_INSTALL: Readonly<Record<Platform, { section: string; block: number }>> = {
  ios: { section: 'installing', block: 0 },
  android: { section: 'install-from-source', block: -1 },
};

/** The compiled inventory examples the landing page quotes: entry id and example index. */
const LANDING_CODE: Readonly<Record<Platform, readonly (readonly [id: string, index: number])[]>> =
  {
    ios: [
      ['button', 0],
      ['text-field', 0],
    ],
    android: [
      ['button', 0],
      ['form-field', 1],
    ],
  };

/** The fenced code blocks of a Markdown body, with their info string. */
export function codeBlocks(md: string): { lang: string; code: string }[] {
  return [...md.matchAll(/^(```|~~~)([^\n]*)\n([\s\S]*?)^\1\s*$/gm)].map((m) => ({
    lang: (m[2] ?? '').trim(),
    code: (m[3] ?? '').replace(/\n$/, ''),
  }));
}

/** Entries whose first example the landing page shows, in order of preference. */
const EXAMPLE_IDS = ['button', 'text-field', 'alert', 'badge', 'card', 'checkbox'];
const EXAMPLE_COUNT = 3;

const text = (file: string): string | undefined =>
  existsSync(file) ? readFileSync(file, 'utf8') : undefined;

/** A JSON file, or undefined when it is missing or (mid-write) not valid JSON. */
function json(file: string): unknown {
  const source = text(file);
  if (source === undefined) return undefined;
  try {
    return JSON.parse(source) as unknown;
  } catch {
    return undefined;
  }
}

const isObject = (v: unknown): v is Record<string, unknown> =>
  typeof v === 'object' && v !== null && !Array.isArray(v);
const str = (v: unknown): v is string => typeof v === 'string' && v.length > 0;
const strings = (v: unknown): string[] => (Array.isArray(v) ? v.filter(str) : []);

/** An inventory entry in the schema's shape, or null for anything else (no adapter for older shapes). */
export function toEntry(raw: unknown): InventoryEntry | null {
  if (!isObject(raw)) return null;
  const { id, title, family, coverage, status, summary } = raw;
  if (!str(id) || !str(title) || !str(family) || !str(summary)) return null;
  if (!str(coverage) || !COVERAGE.has(coverage) || !str(status) || !STATUS.has(status)) return null;
  const s = (v: unknown) => (str(v) ? v : '');
  return {
    id,
    title,
    family,
    summary,
    coverage: coverage as Coverage,
    status: status as Status,
    name: str(raw.name) ? raw.name : null,
    wrapsNative: str(raw.wrapsNative) ? raw.wrapsNative : null,
    since: s(raw.since),
    whenToUse: s(raw.whenToUse),
    whenNotToUse: s(raw.whenNotToUse),
    appearance: s(raw.appearance),
    platformNotes: s(raw.platformNotes),
    parityExceptions: (Array.isArray(raw.parityExceptions) ? raw.parityExceptions : [])
      .filter(isObject)
      .filter((x) => str(x.reason))
      .map((x) => ({ rule: s(x.rule), reason: s(x.reason), convention: s(x.convention) })),
    api: strings(raw.api),
    accessibility: strings(raw.accessibility),
    edgeCases: strings(raw.edgeCases),
    tests: strings(raw.tests),
    dependsOn: strings(raw.dependsOn),
    pattern: raw.layer === 'patterns',
    options: (Array.isArray(raw.options) ? raw.options : []).filter(isObject).map((o) => ({
      name: s(o.name),
      type: s(o.type),
      default: s(o.default),
      description: s(o.description),
    })),
    states: (Array.isArray(raw.states) ? raw.states : [])
      .filter(isObject)
      .map((o) => ({ name: s(o.name), description: s(o.description) })),
    examples: (Array.isArray(raw.examples) ? raw.examples : [])
      .filter(isObject)
      .filter((e) => str(e.title) && str(e.code))
      .map((e) => ({
        title: e.title as string,
        code: e.code as string,
        ...(str(e.setup) ? { setup: e.setup } : {}),
        ...(strings(e.imports).length ? { imports: strings(e.imports) } : {}),
      })),
  };
}

export interface NativeData {
  /** The plan: every web component, in the order of `platforms/parity.json`. Empty when the file is missing. */
  parity: ParityEntry[];
  inventory: Record<Platform, InventoryEntry[]>;
  /** Inventory files that were skipped because they are not in the schema's shape. */
  skipped: string[];
}

/**
 * Reads the plan and both inventories. An inventory is the folder
 * `platforms/<platform>/inventory` (one file per id, `schema.json` aside), or
 * the generated roll-up `components.json` when the folder is missing.
 */
export function readNative(repoRoot: string): NativeData {
  const platforms = path.join(repoRoot, 'platforms');
  const parityFile = json(path.join(platforms, 'parity.json'));
  const parity = (
    isObject(parityFile) && Array.isArray(parityFile.components) ? parityFile.components : []
  ).filter(
    (c): c is ParityEntry =>
      isObject(c) &&
      str(c.id) &&
      str(c.title) &&
      str(c.family) &&
      isObject(c.android) &&
      isObject(c.ios),
  );
  const skipped: string[] = [];
  const inventory = Object.fromEntries(
    PLATFORMS.map((platform) => {
      const dir = path.join(platforms, platform, 'inventory');
      const raw: [string, unknown][] = existsSync(dir)
        ? readdirSync(dir)
            .filter((f) => f.endsWith('.json') && f !== 'schema.json')
            .sort()
            .map((f) => [`platforms/${platform}/inventory/${f}`, json(path.join(dir, f))])
        : ((roll) =>
            (Array.isArray(roll) ? roll : []).map((e, i): [string, unknown] => [
              `platforms/${platform}/components.json[${i}]`,
              e,
            ]))(json(path.join(platforms, platform, 'components.json')));
      const entries: InventoryEntry[] = [];
      const seen = new Set<string>();
      for (const [file, value] of raw) {
        const entry = toEntry(value);
        if (!entry || seen.has(entry.id)) skipped.push(file);
        else {
          seen.add(entry.id);
          entries.push(entry);
        }
      }
      return [platform, entries];
    }),
  ) as Record<Platform, InventoryEntry[]>;
  return { parity, inventory, skipped };
}

const planOf = (p: ParityEntry, platform: Platform): NativePlan | null => {
  const plan = p[platform];
  if (!str(plan.coverage) || !COVERAGE.has(plan.coverage)) return null;
  return {
    coverage: plan.coverage,
    name: str(plan.name) ? plan.name : null,
    uses: str(plan.uses) ? plan.uses : null,
    note: str(plan.note) ? plan.note : null,
    wave: typeof p.wave === 'number' ? p.wave : null,
  };
};

/** Width and height of a PNG file, from its header; null for anything else. */
export function pngSize(file: string): { width: number; height: number } | null {
  if (!existsSync(file)) return null;
  const head = readFileSync(file).subarray(0, 24);
  if (
    head.length < 24 ||
    head.readUInt32BE(0) !== 0x89504e47 ||
    head.toString('latin1', 12, 16) !== 'IHDR'
  )
    return null;
  const width = head.readUInt32BE(16);
  const height = head.readUInt32BE(20);
  return width > 0 && height > 0 ? { width, height } : null;
}

/**
 * Pixels per point (iOS) or dp (Android) in the published previews: iOS
 * previews are resampled to 2x (platforms/ios/Scripts/process-previews.py),
 * Android previews are captured at xxhdpi, 3x (PreviewCaptureTest).
 */
export const PREVIEW_DENSITY: Record<Platform, number> = { ios: 2, android: 3 };

/** The preview images that exist for an entry: `platforms/<platform>/previews/<id>.<theme>.png`. */
export function previewFiles(repoRoot: string, platform: Platform, id: string) {
  const out: { theme: 'light' | 'dark'; file: string; width: number; height: number }[] = [];
  for (const theme of ['light', 'dark'] as const) {
    const file = path.join(repoRoot, 'platforms', platform, 'previews', `${id}.${theme}.png`);
    const size = pngSize(file);
    if (size) {
      // Width and height in CSS pixels: the image is shown at the size it has on the device.
      const density = PREVIEW_DENSITY[platform];
      out.push({
        theme,
        file,
        width: Math.round(size.width / density),
        height: Math.round(size.height / density),
      });
    }
  }
  return out;
}

/**
 * Source of a module whose default export is `value`, with every preview
 * image imported as an asset, so the bundler hashes and ships it. `build`
 * gets the function that finds an entry's previews and registers the imports.
 */
function moduleWithPreviews(
  repoRoot: string,
  build: (previews: (platform: Platform, id: string) => Previews) => unknown,
): string {
  const imports: string[] = [];
  const value = build((platform, id) => {
    const previews: Previews = {};
    for (const p of previewFiles(repoRoot, platform, id)) {
      const name = `__preview${imports.length}__`;
      imports.push(`import ${name} from ${JSON.stringify(p.file)};`);
      previews[p.theme] = { src: name, width: p.width, height: p.height };
    }
    return previews;
  });
  const body = JSON.stringify(value).replace(/"(__preview\d+__)"/g, '$1');
  return `${imports.join('\n')}\nexport default ${body};\n`;
}

const trimmed = ({ id: _id, title: _title, family: _family, ...rest }: InventoryEntry) => rest;

/** Source of `virtual:monarch-native-component/<id>`. */
export function componentModule(data: NativeData, repoRoot: string, id: string): string {
  const plan = data.parity.find((p) => p.id === id);
  return moduleWithPreviews(repoRoot, (previews) =>
    Object.fromEntries(
      PLATFORMS.map((platform) => {
        const entry = data.inventory[platform].find((e) => e.id === id);
        return [
          platform,
          {
            entry: entry ? trimmed(entry) : null,
            plan: plan ? planOf(plan, platform) : null,
            previews: entry ? previews(platform, id) : {},
          } satisfies NativePlatformDocs,
        ];
      }),
    ),
  );
}

/** The rows of one platform's catalogue: inventory entries, then what is only planned. */
export function catalogueRows(
  data: NativeData,
  platform: Platform,
  webIds: ReadonlySet<string>,
): CatalogueRow[] {
  const entries = data.inventory[platform];
  const have = new Set(entries.map((e) => e.id));
  const rows: CatalogueRow[] = entries.map((e) => ({
    id: e.id,
    title: e.title,
    family: e.family,
    coverage: e.coverage,
    status: e.status,
    name: e.name,
    uses: e.wrapsNative,
    entry: true,
    web: webIds.has(e.id),
    summary: e.summary,
    ...(e.since ? { since: e.since } : {}),
    ...(e.pattern ? { pattern: true as const } : {}),
  }));
  for (const p of data.parity) {
    if (have.has(p.id)) continue;
    const plan = planOf(p, platform);
    if (!plan) continue;
    rows.push({
      id: p.id,
      title: p.title,
      family: p.family,
      coverage: plan.coverage,
      status: 'planned',
      name: plan.name,
      uses: plan.uses,
      entry: false,
      web: webIds.has(p.id),
    });
  }
  return rows;
}

const count = <T extends string>(values: readonly T[]) => {
  const out: Partial<Record<T, number>> = {};
  for (const v of values) out[v] = (out[v] ?? 0) + 1;
  return out;
};

/** The first version heading of a changelog: `## 0.1.0 (unreleased)`. */
export function latestRelease(changelog: string | undefined) {
  const versions = [...(changelog ?? '').matchAll(/^##\s+\[?(\d+\.\d+\.\d+[^\s\]]*)\]?(.*)$/gm)];
  const first = versions[0];
  return {
    version: first?.[1] ?? null,
    /** No version has been released: the newest is marked unreleased, or there is none. */
    unreleased: !first || /unreleased/i.test(first[2] ?? ''),
    versions: versions.length,
  };
}

/** `key = "value"` in the `[versions]` table of a Gradle version catalog. */
export function catalogVersions(toml: string | undefined): Record<string, string> {
  const table = /^\[versions\]\s*$([\s\S]*?)(?=^\[|(?![\s\S]))/m.exec(toml ?? '')?.[1] ?? '';
  return Object.fromEntries(
    [...table.matchAll(/^\s*([A-Za-z][\w-]*)\s*=\s*"([^"]+)"/gm)].map((m) => [m[1], m[2]]),
  );
}

/** The h3 headings under one h2 of a Markdown file (the topics of a guide's platform section). */
export function topicsUnder(md: string | undefined, heading: string): string[] {
  const out: string[] = [];
  let inside = false;
  let fenced = false;
  for (const line of (md ?? '').split('\n')) {
    if (/^(```|~~~)/.test(line)) fenced = !fenced;
    if (fenced) continue;
    const h2 = /^##\s+(.+)$/.exec(line);
    if (h2) inside = h2[1]?.trim() === heading;
    else if (inside) {
      const h3 = /^###\s+(.+)$/.exec(line);
      if (h3?.[1]) out.push(h3[1].trim());
    }
  }
  return out;
}

/** Versions, requirements and counts, per platform. Everything is read; nothing is typed in. */
export function nativeFacts(data: NativeData, repoRoot: string) {
  const at = (...parts: string[]) => path.join(repoRoot, 'platforms', ...parts);
  const webIds = new Set(data.parity.map((p) => p.id));
  const adaptive = text(path.join(repoRoot, NATIVE_GUIDES['adaptive-layout'] as string));
  const platform = (p: Platform) => {
    const entries = data.inventory[p];
    const covered = entries.filter((e) => webIds.has(e.id));
    const readme = text(at(p, 'README.md')) ?? '';
    return {
      ...latestRelease(text(at(p, 'CHANGELOG.md'))),
      /** Inventory entries, and how many of them are web components of the plan. */
      entries: entries.length,
      covered: covered.length,
      /** Web components in the plan (the catalogue of the web library). */
      total: data.parity.length,
      status: count(covered.map((e) => e.status)),
      coverage: count(covered.map((e) => e.coverage)),
      plan: count(data.parity.map((c) => c[p].coverage).filter((c) => COVERAGE.has(c))),
      adaptiveTopics: topicsUnder(adaptive, p === 'android' ? 'Android' : 'iOS'),
      commands: Object.fromEntries(
        Object.entries(README_COMMANDS[p]).map(([k, v]) => [k, readme.includes(v) ? v : null]),
      ),
    };
  };
  const versions = catalogVersions(text(at('android', 'gradle/libs.versions.toml')));
  const gradle =
    /gradle-([\d.]+)-/.exec(
      text(at('android', 'gradle/wrapper/gradle-wrapper.properties')) ?? '',
    )?.[1] ?? null;
  const manifest = text(at('ios', 'Package.swift')) ?? '';
  const iosVersion =
    /static let version = "([^"]+)"/.exec(
      text(at('ios', 'Sources/MonarchUI/MonarchUI.swift')) ?? '',
    )?.[1] ?? null;
  const android = platform('android');
  const ios = platform('ios');
  return {
    android: {
      ...android,
      version: versions.monarch ?? android.version,
      requirements: {
        minSdk: versions.minSdk ?? null,
        compileSdk: versions.compileSdk ?? null,
        targetSdk: versions.targetSdk ?? null,
        kotlin: versions.kotlin ?? null,
        agp: versions.agp ?? null,
        composeBom: versions.composeBom ?? null,
        jdk: versions.jdk ?? null,
        jvmTarget: versions.jvmTarget ?? null,
        gradle,
      },
    },
    ios: {
      ...ios,
      version: iosVersion ?? ios.version,
      requirements: {
        ios: /\.iOS\(\.v(\d+(?:_\d+)?)\)/.exec(manifest)?.[1]?.replace('_', '.') ?? null,
        swiftTools: /^\/\/\s*swift-tools-version:\s*([\d.]+)/m.exec(manifest)?.[1] ?? null,
        dependencies: /\.package\(/.test(manifest),
      },
    },
  };
}
export type NativeFacts = ReturnType<typeof nativeFacts>;

/** A Markdown file split at its h2 and h3 headings: slug of the heading → title and body. */
export function splitSections(md: string): Map<string, { title: string; body: string }> {
  const out = new Map<string, { title: string; body: string }>();
  let current: { title: string; lines: string[] } | null = null;
  let fenced = false;
  const close = () => {
    if (!current) return;
    const key = slug(current.title);
    if (key && !out.has(key))
      out.set(key, { title: current.title, body: current.lines.join('\n') });
  };
  for (const line of md.split('\n')) {
    if (/^(```|~~~)/.test(line)) fenced = !fenced;
    const heading = fenced ? null : /^#{2,3}\s+(.+)$/.exec(line);
    if (heading?.[1]) {
      close();
      current = { title: heading[1].trim(), lines: [] };
    } else current?.lines.push(line);
  }
  close();
  return out;
}

/** Source of `virtual:monarch-native-platform/<platform>`. */
export function platformModule(
  data: NativeData,
  repoRoot: string,
  platform: Platform,
  base: string,
): string {
  const source = `platforms/${platform}/README.md`;
  const all = splitSections(text(path.join(repoRoot, source)) ?? '');
  const sections: Record<string, string> = {};
  for (const key of README_SECTIONS[platform]) {
    const section = all.get(key);
    if (!section) continue;
    sections[key] = renderBody(section.body, {
      base,
      source,
      repoRoot,
      fragment: true,
      codeLabel: (n) => `${section.title}: code example ${n}`,
    });
  }
  const webIds = new Set(data.parity.map((p) => p.id));
  const entries = data.inventory[platform];
  const withExample = entries.filter((e) => webIds.has(e.id) && e.examples.length > 0);
  const picked = [
    ...EXAMPLE_IDS.flatMap((id) => withExample.filter((e) => e.id === id)),
    ...withExample.filter((e) => !EXAMPLE_IDS.includes(e.id)),
  ].slice(0, EXAMPLE_COUNT);
  return moduleWithPreviews(repoRoot, (previews) => ({
    sections,
    /** Entries with no web component page: documented on the landing page. */
    foundations: entries
      .filter((e) => !webIds.has(e.id))
      // The theme first: everything else depends on it.
      .sort((a, b) => Number(b.id === 'theme') - Number(a.id === 'theme'))
      .map((e) => ({
        id: e.id,
        title: e.title,
        docs: { entry: trimmed(e), plan: null, previews: previews(platform, e.id) },
      })),
    examples: picked.map((e) => ({
      id: e.id,
      title: e.title,
      name: e.name,
      example: e.examples[0],
    })),
    landing: {
      /** The number of inventory entries per catalogue group (the patterns layer is its own group). */
      groups: Object.entries(count(entries.map((e) => (e.pattern ? 'patterns' : e.family)))).map(
        ([group, n]) => ({ group, count: n ?? 0 }),
      ),
      install: ((spec) => {
        const blocks = codeBlocks(all.get(spec.section)?.body ?? '');
        return blocks.at(spec.block) ?? null;
      })(LANDING_INSTALL[platform]),
      code: LANDING_CODE[platform].flatMap(([id, index]) => {
        const entry = entries.find((e) => e.id === id);
        const example = entry?.examples[index];
        return entry && example ? [{ id, title: entry.title, example }] : [];
      }),
    },
    /** Up to six entries that have a preview image. */
    previews: entries
      .filter((e) => webIds.has(e.id))
      .map((e) => ({ id: e.id, title: e.title, previews: previews(platform, e.id) }))
      .filter((e) => Object.keys(e.previews).length > 0)
      .slice(0, 6),
  }));
}

/** A Markdown file of the repository rendered as a guide, or null when it does not exist. */
export function renderFile(repoRoot: string, source: string, base: string): RenderedGuide | null {
  const md = text(path.join(repoRoot, source));
  return md === undefined ? null : renderGuide(md, { base, source, repoRoot });
}

/** The native guides that exist, in the order of NATIVE_GUIDES. */
export const presentGuides = (repoRoot: string): string[] =>
  Object.entries(NATIVE_GUIDES)
    .filter(([, file]) => existsSync(path.join(repoRoot, file)))
    .map(([id]) => id);

const isPlatform = (v: string): v is Platform => (PLATFORMS as readonly string[]).includes(v);

export function monarchNative({ repoRoot }: { repoRoot: string }): Plugin {
  let base = '/';
  let cache: NativeData | null = null;
  let warned = false;
  const data = () => (cache ??= readNative(repoRoot));
  return {
    name: 'monarch-native',
    enforce: 'pre',
    configResolved(config) {
      base = config.base.endsWith('/') ? config.base : `${config.base}/`;
    },
    buildStart() {
      cache = null;
      warned = false;
    },
    resolveId(id) {
      return id.startsWith('virtual:monarch-native-') ? `\0${id}` : undefined;
    },
    load(resolved) {
      if (!resolved.startsWith('\0virtual:monarch-native-')) return undefined;
      const id = resolved.slice(1);
      const d = data();
      if (!warned && d.skipped.length) {
        warned = true;
        this.warn(
          `${d.skipped.length} inventory file(s) are not in the schema's shape and were skipped: ${d.skipped.slice(0, 5).join(', ')}${d.skipped.length > 5 ? ', …' : ''}`,
        );
      }
      for (const p of PLATFORMS)
        this.addWatchFile(path.join(repoRoot, 'platforms', p, 'README.md'));
      this.addWatchFile(path.join(repoRoot, 'platforms/parity.json'));
      const exported = (value: unknown) => `export default ${JSON.stringify(value)};`;
      if (id === NAV_ID) return `export const guides = ${JSON.stringify(presentGuides(repoRoot))};`;
      if (id === FACTS_ID) return exported(nativeFacts(d, repoRoot));
      if (id.startsWith(COMPONENT_ID))
        return componentModule(d, repoRoot, id.slice(COMPONENT_ID.length));
      const arg = (prefix: string) => (id.startsWith(prefix) ? id.slice(prefix.length) : null);
      const guide = arg(GUIDE_ID);
      if (guide !== null) {
        const file = NATIVE_GUIDES[guide];
        if (!file) return this.error(`${id}: no such native guide`);
        return exported(renderFile(repoRoot, file, base));
      }
      const platform = arg(CATALOGUE_ID) ?? arg(PLATFORM_ID) ?? arg(CHANGELOG_ID) ?? '';
      if (!isPlatform(platform)) return this.error(`${id}: unknown module`);
      if (id.startsWith(CHANGELOG_ID))
        return exported(renderFile(repoRoot, `platforms/${platform}/CHANGELOG.md`, base));
      if (id.startsWith(PLATFORM_ID)) return platformModule(d, repoRoot, platform, base);
      // The catalogue links a row to its component page when the site has one.
      const webIds = new Set(
        readdirSync(path.join(repoRoot, 'packages/react/src'), {
          recursive: true,
          encoding: 'utf8',
        })
          .filter((f) => f.endsWith('.docs.tsx'))
          .map((f) => path.basename(f, '.docs.tsx')),
      );
      return exported(catalogueRows(d, platform, webIds));
    },
    transform(code, id) {
      const docsId = DOCS_FILE.exec(id.split('?')[0] as string)?.[1];
      if (!docsId) return undefined;
      // Each docs file can load its own native documentation: a chunk of its
      // own, fetched by the component page and by nothing else.
      return {
        code: `${code}\nexport const loadNative = () => import(${JSON.stringify(COMPONENT_ID + docsId)});\n`,
        map: null,
      };
    },
  };
}
