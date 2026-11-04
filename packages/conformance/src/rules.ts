/**
 * Conformance rules. Each scanner is a pure function over one file's text, so
 * rules are easy to test with fixtures and cheap to run across 20+ repos.
 *
 * Scanning is lexical (regular expressions plus a small JSX tag reader), not a
 * full parse. Rules are written to prefer missing a case over inventing one.
 */
import type { Deprecation, RuleMeta, Severity } from './types.js';
import { MONARCH_COMPONENT_STEMS } from './monarch-components.js';

export const RULES = {
  'color/no-raw-palette': {
    id: 'color/no-raw-palette',
    severity: 'error',
    summary: 'Raw Tailwind palette colour or arbitrary colour class',
    help: 'Use a semantic colour (bg-mn-surface, text-mn-fg-muted, bg-mn-danger-subtle, or the legacy bg-primary / text-muted-foreground). Palette classes ignore theme, contrast mode and product accent.',
  },
  'color/no-hard-coded-color': {
    id: 'color/no-hard-coded-color',
    severity: 'error',
    summary: 'Hard-coded colour literal (hex, rgb(), hsl())',
    help: 'Reference a token: var(--mn-color-…) in CSS, or a semantic class. For charts use var(--mn-color-data-1…5).',
  },
  'typography/no-arbitrary-type': {
    id: 'typography/no-arbitrary-type',
    severity: 'warn',
    summary: 'Arbitrary font size, weight, family, line height or letter spacing',
    help: 'Use the Monarch type scale (text-2xs…text-5xl, font-medium, leading-snug, tracking-tight) and var(--mn-font-sans|mono).',
  },
  'spacing/off-grid': {
    id: 'spacing/off-grid',
    severity: 'warn',
    summary: 'Spacing value off the 4px grid',
    help: 'Use the spacing scale (p-1 = 4px, p-1.5 = 6px, p-2 = 8px …). Arbitrary values must be multiples of 2px or a var(--mn-space-…).',
  },
  'tokens/local-token-definitions': {
    id: 'tokens/local-token-definitions',
    severity: 'error',
    summary: 'Design tokens defined locally instead of imported from Monarch',
    help: 'Import @rookcom/monarch-tokens/css (plus /css/compat during migration) and set data-mn-product on <html>. Delete the local copy.',
  },
  'components/duplicated-primitive': {
    id: 'components/duplicated-primitive',
    severity: 'warn',
    summary: 'Local copy of a component Monarch provides',
    help: 'Import the component from @rookcom/monarch-react and delete the local file once call sites are migrated.',
  },
  'a11y/img-alt': {
    id: 'a11y/img-alt',
    severity: 'error',
    summary: '<img> without alt',
    help: 'Add alt text, or alt="" for decorative images (WCAG 1.1.1).',
  },
  'a11y/click-without-key': {
    id: 'a11y/click-without-key',
    severity: 'error',
    summary: 'Click handler on a non-interactive element without role and keyboard handler',
    help: 'Use <button> (preferred), or add role and onKeyDown plus tabIndex={0} (WCAG 2.1.1).',
  },
  'a11y/positive-tabindex': {
    id: 'a11y/positive-tabindex',
    severity: 'error',
    summary: 'Positive tabIndex',
    help: 'Use tabIndex={0} or -1. Positive values break the natural focus order (WCAG 2.4.3).',
  },
  'a11y/outline-none-without-ring': {
    id: 'a11y/outline-none-without-ring',
    severity: 'error',
    summary: 'Focus outline removed without a visible replacement',
    help: 'Pair outline-none with focus-visible:ring-2 focus-visible:ring-ring (or a Monarch component). WCAG 2.4.7 needs a visible focus indicator.',
  },
  'overrides/monarch-internals': {
    id: 'overrides/monarch-internals',
    severity: 'error',
    summary: 'Overrides Monarch internals (.mn-* selectors or --mn-* variables)',
    help: 'Monarch classes and variables are not a public styling API. Use component props, or request a variant or an exception through governance.',
  },
  'deps/monarch-version': {
    id: 'deps/monarch-version',
    severity: 'error',
    summary: 'Monarch package version status',
    help: 'Upgrade to a supported version listed in registry/compatibility.json. See the migration guide for each release.',
  },
  'api/deprecated': {
    id: 'api/deprecated',
    severity: 'warn',
    summary: 'Deprecated Monarch API',
    help: 'Move to the replacement named in the message before the removal version.',
  },
  'icons/no-sparkles': {
    id: 'icons/no-sparkles',
    severity: 'error',
    summary: 'Sparkle icon used',
    help: 'Rook does not use sparkle icons (Sparkles, Sparkle, WandSparkles and its old name Wand2). For AI features use the lucide `Bot` icon; elsewhere use a plain icon that names the thing, such as Inbox or Bell.',
  },
  'legacy/bridge-variable': {
    id: 'legacy/bridge-variable',
    severity: 'info',
    summary: 'Legacy shadcn variable used directly (var(--primary) …)',
    help: 'Tracked for migration. Prefer var(--mn-color-…) so the legacy bridge can eventually be removed.',
  },
  'governance/suppression-without-reason': {
    id: 'governance/suppression-without-reason',
    severity: 'warn',
    summary: 'Suppression comment without a reason',
    help: 'Write `monarch-ignore-next-line <rule> -- <reason>`. Suppressions without a reason are ignored.',
  },
  'governance/expired-exception': {
    id: 'governance/expired-exception',
    severity: 'error',
    summary: 'Registry exception has expired',
    help: 'Fix the findings it covered, or renew it in the product registry entry with a new expiry and approver.',
  },
} as const satisfies Record<string, RuleMeta>;

export type RuleId = keyof typeof RULES;
export const RULE_IDS = Object.keys(RULES) as RuleId[];

export interface RawFinding {
  readonly rule: RuleId;
  readonly line: number;
  readonly column: number;
  readonly message: string;
  /** Overrides the rule's default severity for this finding. */
  readonly severity?: Severity;
}

export interface FileContext {
  /** Path relative to scan root. */
  readonly file: string;
  readonly ext: string;
  readonly text: string;
  readonly lines: readonly string[];
  readonly deprecations: readonly Deprecation[];
}

// ---------------------------------------------------------------- shared

const PALETTE =
  'slate|gray|zinc|neutral|stone|red|orange|amber|yellow|lime|green|emerald|teal|cyan|sky|blue|indigo|violet|purple|fuchsia|pink|rose';
const COLOR_PROPS =
  'text|bg|border(?:-[xytrblse])?|ring(?:-offset)?|outline|from|via|to|fill|stroke|divide|placeholder|caret|accent|decoration|shadow';
const VARIANTS = '(?:(?:[\\w-]+|\\[[^\\]\\s]*\\]|[\\w-]+-\\[[^\\]\\s]*\\]):)*';

const PALETTE_RE = new RegExp(
  `(?<![\\w-])${VARIANTS}!?(?:${COLOR_PROPS})-(?:(?:${PALETTE})-(?:50|[1-9]00|950)|white|black)(?:\\/(?:\\d{1,3}|\\[[^\\]]+\\]))?(?![\\w-])`,
  'g',
);
const ARBITRARY_COLOR_RE = new RegExp(
  `(?<![\\w-])${VARIANTS}!?(?:${COLOR_PROPS})-\\[(?:#[0-9a-fA-F]{3,8}|(?:rgba?|hsla?|oklch|oklab)\\(\\s*[\\d.])[^\\]]*\\]`,
  'g',
);
const HEX_RE =
  /(?<=[\s'"`(,:=_])#([0-9a-fA-F]{8}|[0-9a-fA-F]{6}|[0-9a-fA-F]{4}|[0-9a-fA-F]{3})(?![\w-])/g;
const FUNC_COLOR_RE = /\b(?:rgba?|hsla?|oklch|oklab|lab|lch|hwb)\(\s*-?[\d.]/g;

/** Legacy variables from the shadcn-era token copies. */
export const LEGACY_TOKEN_NAMES = new Set([
  'background',
  'foreground',
  'card',
  'card-foreground',
  'popover',
  'popover-foreground',
  'primary',
  'primary-foreground',
  'secondary',
  'secondary-foreground',
  'muted',
  'muted-foreground',
  'accent',
  'accent-foreground',
  'destructive',
  'destructive-foreground',
  'success',
  'success-foreground',
  'warning',
  'warning-foreground',
  'info',
  'info-foreground',
  'border',
  'input',
  'ring',
  'radius',
]);

export function isLegacyToken(name: string): boolean {
  return LEGACY_TOKEN_NAMES.has(name) || name.startsWith('sidebar-') || /^chart-\d+$/.test(name);
}

const isCode = (ext: string) => /^\.(?:[cm]?[jt]sx?)$/.test(ext);
const isJsx = (ext: string) => ext === '.tsx' || ext === '.jsx';
const isCss = (ext: string) => ext === '.css' || ext === '.scss';

/** True for lines that are only a comment (we don't scan commented-out code). */
function isCommentLine(line: string): boolean {
  const t = line.trimStart();
  return t.startsWith('//') || t.startsWith('/*') || t.startsWith('*') || t.startsWith('{/*');
}

function eachMatch(re: RegExp, s: string, fn: (m: RegExpExecArray) => void): void {
  re.lastIndex = 0;
  let m: RegExpExecArray | null;
  while ((m = re.exec(s)) !== null) {
    fn(m);
    if (m[0].length === 0) re.lastIndex++;
  }
}

/** Index → 1-based line/column. */
function locator(text: string): (index: number) => { line: number; column: number } {
  const starts = [0];
  for (let i = 0; i < text.length; i++) if (text.charCodeAt(i) === 10) starts.push(i + 1);
  return (index) => {
    let lo = 0;
    let hi = starts.length - 1;
    while (lo < hi) {
      const mid = (lo + hi + 1) >> 1;
      if ((starts[mid] as number) <= index) lo = mid;
      else hi = mid - 1;
    }
    return { line: lo + 1, column: index - (starts[lo] as number) + 1 };
  };
}

// ---------------------------------------------------------------- colour

function scanColor(ctx: FileContext, out: RawFinding[]): void {
  ctx.lines.forEach((line, i) => {
    if (isCommentLine(line)) return;
    const n = i + 1;
    // Design-token definitions are reported once per block by tokens/local-token-definitions
    // (or overrides/monarch-internals for --mn-*); app-local variables are still checked.
    const decl = isCss(ctx.ext) ? /^\s*--([\w-]+)\s*:/.exec(line) : null;
    const cssDecl = decl
      ? isLegacyToken(decl[1] as string) || (decl[1] as string).startsWith('mn-')
      : false;

    // Classes appear in TS strings and in CSS @apply.
    if (isCode(ctx.ext) || (isCss(ctx.ext) && /@apply\b/.test(line))) {
      eachMatch(PALETTE_RE, line, (m) =>
        out.push({
          rule: 'color/no-raw-palette',
          line: n,
          column: m.index + 1,
          message: `Palette class \`${m[0]}\``,
        }),
      );
      eachMatch(ARBITRARY_COLOR_RE, line, (m) =>
        out.push({
          rule: 'color/no-raw-palette',
          line: n,
          column: m.index + 1,
          message: `Arbitrary colour class \`${m[0]}\``,
        }),
      );
    }

    if (cssDecl) return; // reported by tokens/local-token-definitions
    if (isCss(ctx.ext) && /^\s*@import\b/.test(line)) return;

    eachMatch(HEX_RE, line, (m) => {
      const before = line.slice(0, m.index);
      // Anchors, routes and SVG references, not colours.
      if (/(?:href|to|hash|anchor|id|path|url)\s*[=:]\s*\{?\s*['"`]?$/i.test(before)) return;
      if (/url\(\s*['"]?$/.test(before)) return;
      if (/&$/.test(before)) return;
      const hex = m[1] as string;
      const after = line[m.index + m[0].length] ?? '';
      // "Ticket #1234 created" / 'Order #1042': all-digit, followed by prose → an
      // issue number. A 4-digit all-numeric token is never a colour in practice.
      if (/^\d{4}$/.test(hex)) return;
      if (/^\d+$/.test(hex) && !/^['"`;),\]}]?$/.test(after)) return;
      if (/^\d+$/.test(hex) && /\s$/.test(before) && after === '') return;
      out.push({
        rule: 'color/no-hard-coded-color',
        line: n,
        column: m.index + 1,
        message: `Colour literal \`#${hex}\``,
      });
    });
    eachMatch(FUNC_COLOR_RE, line, (m) => {
      out.push({
        rule: 'color/no-hard-coded-color',
        line: n,
        column: m.index + 1,
        message: `Colour function \`${m[0].replace(/\s+$/, '')}…)\``,
      });
    });
  });
}

// ---------------------------------------------------------------- typography & spacing

const ARB_TYPE_RE = new RegExp(
  `(?<![\\w-])${VARIANTS}!?(text|font|leading|tracking)-\\[([^\\]\\s]+)\\]`,
  'g',
);
const ARB_SPACE_RE = new RegExp(
  `(?<![\\w-])${VARIANTS}!?-?(p[xytrblse]?|m[xytrblse]?|gap(?:-[xy])?|space-[xy]|inset(?:-[xy])?|scroll-[mp][xytrbl]?)-\\[([^\\]\\s]+)\\]`,
  'g',
);

/** px value on the 2px half-grid (the scale has 0.5 = 2px steps), or not a length. */
function onGrid(value: string): boolean {
  if (/var\(|calc\(|env\(|%|vh|vw|dvh|svh|lvh|fr|auto|min\(|max\(|clamp\(/.test(value)) return true;
  const lengths = value.split(/[_\s,]+/).filter(Boolean);
  return lengths.every((len) => {
    // em is relative to the element's font (prose rhythm), so only px/rem are checked.
    const m = /^(-?\d*\.?\d+)(px|rem)?$/.exec(len);
    if (!m) return true;
    const num = Number(m[1]);
    const px = m[2] === 'rem' ? num * 16 : num;
    if (px === 0 || Math.abs(px) === 1) return true; // hairlines
    return Math.abs(px) % 2 === 0;
  });
}

function scanTypeAndSpace(ctx: FileContext, out: RawFinding[]): void {
  ctx.lines.forEach((line, i) => {
    if (isCommentLine(line)) return;
    const n = i + 1;
    if (isCode(ctx.ext) || (isCss(ctx.ext) && /@apply\b/.test(line))) {
      eachMatch(ARB_TYPE_RE, line, (m) => {
        const value = m[2] as string;
        if (/^(?:#|rgba?\(|hsla?\(|oklch|color-mix|var\(|length:var)/.test(value)) return;
        out.push({
          rule: 'typography/no-arbitrary-type',
          line: n,
          column: m.index + 1,
          message: `Arbitrary type value \`${m[0]}\``,
        });
      });
      eachMatch(ARB_SPACE_RE, line, (m) => {
        if (onGrid(m[2] as string)) return;
        out.push({
          rule: 'spacing/off-grid',
          line: n,
          column: m.index + 1,
          message: `Off-grid spacing \`${m[0]}\``,
        });
      });
    }
    if (isCode(ctx.ext)) {
      const ff = /\bfontFamily\s*:\s*['"`]([^'"`]+)['"`]/.exec(line);
      if (ff && !/var\(--/.test(ff[1] as string) && !/^inherit$/.test(ff[1] as string)) {
        out.push({
          rule: 'typography/no-arbitrary-type',
          line: n,
          column: ff.index + 1,
          message: `Inline font family \`${ff[1]}\``,
        });
      }
    }
    if (isCss(ctx.ext)) {
      // Global matching: some hand-written CSS packs many rules on one line.
      eachMatch(/(?:^|[;{\s])font-family\s*:\s*([^;}]+)/g, line, (ff) => {
        if (/^\s*(?:inherit|initial|unset|var\(--)/.test(ff[1] as string)) return;
        out.push({
          rule: 'typography/no-arbitrary-type',
          line: n,
          column: ff.index + 1,
          message: `font-family declared locally: \`${(ff[1] as string).trim().slice(0, 60)}\``,
        });
      });
      eachMatch(
        /(?:^|[;{\s])(padding|margin|gap|row-gap|column-gap)(?:-[a-z]+)?\s*:\s*([^;}]+)/g,
        line,
        (sp) => {
          if (onGrid((sp[2] as string).trim())) return;
          out.push({
            rule: 'spacing/off-grid',
            line: n,
            column: sp.index + 1,
            message: `Off-grid ${sp[1]}: \`${(sp[2] as string).trim()}\``,
          });
        },
      );
    }
  });
}

// ---------------------------------------------------------------- tokens, overrides, legacy

function scanCssStructure(ctx: FileContext, out: RawFinding[]): void {
  if (!isCss(ctx.ext)) return;
  let selector = '';
  let pendingSelector = '';
  let group: { selector: string; line: number; names: string[] } | null = null;
  const flush = () => {
    if (group && group.names.length) {
      out.push({
        rule: 'tokens/local-token-definitions',
        line: group.line,
        column: 1,
        message: `${group.names.length} design-token variable${group.names.length === 1 ? '' : 's'} defined in \`${group.selector || '(top level)'}\` (${group.names
          .slice(0, 4)
          .map((x) => '--' + x)
          .join(', ')}${group.names.length > 4 ? ', …' : ''})`,
      });
    }
    group = null;
  };
  ctx.lines.forEach((line, i) => {
    const n = i + 1;
    if (isCommentLine(line)) return;
    const open = line.indexOf('{');
    if (open !== -1) {
      flush();
      selector = (pendingSelector + ' ' + line.slice(0, open)).trim().replace(/\s+/g, ' ');
      pendingSelector = '';
      if (/(^|[\s,>+~(])\.mn-[a-z]/.test(selector)) {
        out.push({
          rule: 'overrides/monarch-internals',
          line: n,
          column: 1,
          message: `Selector targets Monarch internals: \`${selector.slice(0, 80)}\``,
        });
      }
    } else if (/[,]\s*$/.test(line) && !line.includes(':')) {
      pendingSelector += ' ' + line.trim();
    }
    if (line.includes('}')) flush();
    const def = /^\s*--([\w-]+)\s*:/.exec(line);
    if (def) {
      const name = def[1] as string;
      if (name.startsWith('mn-')) {
        out.push({
          rule: 'overrides/monarch-internals',
          line: n,
          column: (def.index ?? 0) + 1,
          message: `Redefines Monarch variable \`--${name}\``,
        });
      } else if (isLegacyToken(name)) {
        if (!group) group = { selector, line: n, names: [] };
        group.names.push(name);
      }
    }
  });
  flush();
}

const LEGACY_VAR_RE = /var\(\s*--([\w-]+)/g;
const MN_SELECTOR_IN_CODE_RE = /(?:['"`\s&_>+~,(]|^)\.mn-[a-z][\w-]*/g;

function scanLegacyAndOverrides(ctx: FileContext, out: RawFinding[]): void {
  ctx.lines.forEach((line, i) => {
    if (isCommentLine(line)) return;
    const n = i + 1;
    eachMatch(LEGACY_VAR_RE, line, (m) => {
      if (isLegacyToken(m[1] as string)) {
        out.push({
          rule: 'legacy/bridge-variable',
          line: n,
          column: m.index + 1,
          message: `Legacy variable \`--${m[1]}\``,
        });
      }
    });
    if (isCode(ctx.ext)) {
      eachMatch(MN_SELECTOR_IN_CODE_RE, line, (m) => {
        out.push({
          rule: 'overrides/monarch-internals',
          line: n,
          column: m.index + 1,
          message: `Targets Monarch class \`${m[0].trim().replace(/^['"`&_>+~,(]/, '')}\``,
        });
      });
      if (/--mn-[\w-]+['"]?\s*:/.test(line) && !/var\(--mn-/.test(line.split(':')[0] ?? '')) {
        const idx = line.search(/--mn-/);
        out.push({
          rule: 'overrides/monarch-internals',
          line: n,
          column: idx + 1,
          message: 'Sets a Monarch variable inline',
        });
      }
    }
  });
}

// ---------------------------------------------------------------- components

export function kebab(stem: string): string {
  return stem
    .replace(/([a-z0-9])([A-Z])/g, '$1-$2')
    .replace(/_/g, '-')
    .toLowerCase();
}

function scanDuplicate(ctx: FileContext, out: RawFinding[]): void {
  const m =
    /(?:^|\/)components\/(?:ui|primitives|common)\/(?:([A-Za-z][\w-]*)\.(?:tsx|jsx|ts|js)|([A-Za-z][\w-]*)\/index\.(?:tsx|jsx|ts|js))$/.exec(
      ctx.file,
    );
  if (!m) return;
  const stem = kebab((m[1] ?? m[2]) as string);
  const name = MONARCH_COMPONENT_STEMS.get(stem);
  if (!name) return;
  out.push({
    rule: 'components/duplicated-primitive',
    line: 1,
    column: 1,
    message: `Local \`${stem}\` duplicates Monarch \`${name}\``,
  });
}

// ---------------------------------------------------------------- accessibility (JSX)

interface Tag {
  readonly name: string;
  readonly start: number;
  readonly text: string;
}

/** Reads opening JSX tags, balancing braces so arrow functions don't end a tag. */
export function readTags(text: string, names: RegExp): Tag[] {
  const tags: Tag[] = [];
  const re = new RegExp(`<(${names.source})(?=[\\s/>])`, 'g');
  let m: RegExpExecArray | null;
  while ((m = re.exec(text)) !== null) {
    let depth = 0;
    let quote: string | null = null;
    let end = -1;
    for (let i = m.index + m[0].length; i < text.length && i < m.index + 4000; i++) {
      const c = text[i];
      if (quote) {
        if (c === quote && text[i - 1] !== '\\') quote = null;
        continue;
      }
      if (c === '{') depth++;
      else if (c === '}') depth--;
      else if (depth === 0 && (c === '"' || c === "'")) quote = c;
      else if (depth > 0 && (c === '"' || c === "'" || c === '`')) quote = c;
      else if (c === '>' && depth === 0) {
        end = i;
        break;
      }
    }
    if (end !== -1)
      tags.push({ name: m[1] as string, start: m.index, text: text.slice(m.index, end + 1) });
  }
  return tags;
}

const NON_INTERACTIVE =
  /div|span|li|td|tr|p|section|article|header|footer|main|nav|aside|ul|ol|img|h[1-6]|label/;

/** Items of composite widgets whose keyboard handling lives on the container. */
const COMPOSITE_ITEM_ROLE =
  /\srole\s*=\s*["'{]\s*['"]?(?:option|menuitem|tab|treeitem|gridcell)['"]?/;

function scanA11y(ctx: FileContext, out: RawFinding[]): void {
  if (!isJsx(ctx.ext)) return;
  const at = locator(ctx.text);
  // aria-activedescendant: focus stays on the input/container, which handles
  // every key, so its items legitimately take only pointer events.
  const activeDescendant = /aria-activedescendant/.test(ctx.text);
  for (const tag of readTags(ctx.text, /img/)) {
    if (/\{\s*\.\.\./.test(tag.text)) continue;
    if (!/\balt\s*=/.test(tag.text)) {
      const { line, column } = at(tag.start);
      out.push({ rule: 'a11y/img-alt', line, column, message: '<img> has no alt attribute' });
    }
  }
  for (const tag of readTags(ctx.text, NON_INTERACTIVE)) {
    if (!/\sonClick\s*=/.test(tag.text)) continue;
    if (/\{\s*\.\.\./.test(tag.text)) continue;
    if (/\saria-hidden(?!\s*=\s*(?:"false"|\{false\}))(?=[\s=/>])/.test(tag.text)) continue;
    // Wrappers that only stop propagation are not controls.
    const handler = /\sonClick\s*=\s*\{([\s\S]*?)\}\s*(?:\w+=|\/?>|\{)/.exec(tag.text)?.[1] ?? '';
    if (
      /stopPropagation\(\)/.test(handler) &&
      !/[;,]\s*\w+\(/.test(handler.replace(/\w+\.stopPropagation\(\)/, ''))
    )
      continue;
    const hasRole = /\srole\s*=/.test(tag.text);
    const hasKey = /\sonKey(?:Down|Up|Press)\s*=/.test(tag.text);
    if (hasRole && hasKey) continue;
    if (activeDescendant && COMPOSITE_ITEM_ROLE.test(tag.text)) continue;
    const { line, column } = at(tag.start);
    const missing = [hasRole ? null : 'role', hasKey ? null : 'onKeyDown']
      .filter(Boolean)
      .join(' and ');
    out.push({
      rule: 'a11y/click-without-key',
      line,
      column,
      message: `<${tag.name} onClick> without ${missing}`,
    });
  }
  eachMatch(/\btabIndex\s*=\s*(?:\{\s*(\d+)\s*\}|["'](\d+)["'])/g, ctx.text, (m) => {
    const v = Number(m[1] ?? m[2]);
    if (v > 0) {
      const { line, column } = at(m.index);
      out.push({ rule: 'a11y/positive-tabindex', line, column, message: `tabIndex=${v}` });
    }
  });
}

const OUTLINE_NONE_RE = new RegExp(`(?<![\\w-])${VARIANTS}outline-none(?![\\w-])`, 'g');
/** A focus style on a wrapper, e.g. an input group that rings on `focus-within`. */
const FOCUS_WITHIN_RE =
  /focus-within:(?:ring-(?!0(?![\w.]))|ring(?![\w-])|outline-(?!none|0(?![\w.]))|outline(?![\w-])|shadow|border)/;
/** How far up to look for that wrapper. A heuristic: the ancestor is usually in the same JSX block. */
const FOCUS_WITHIN_LOOKBACK = 15;

const FOCUS_REPLACEMENT_RE =
  /(?:focus|focus-visible|focus-within|group-focus(?:-visible)?|peer-focus(?:-visible)?|data-\[focus[^\]]*\]|data-\[highlighted\]|data-highlighted|aria-selected|data-\[selected[^\]]*\]|has-\[:focus-visible\]):(?:ring-(?!0(?![\w.]))|ring(?![\w-])|outline-(?!none|0(?![\w.]))|outline(?![\w-])|shadow|border|bg-|underline)/;

function scanOutline(ctx: FileContext, out: RawFinding[]): void {
  if (!isCode(ctx.ext)) return;
  ctx.lines.forEach((line, i) => {
    if (isCommentLine(line)) return;
    eachMatch(OUTLINE_NONE_RE, line, (m) => {
      // Classes for one element are often split across a few lines (cva, cn()).
      const window = ctx.lines.slice(Math.max(0, i - 3), i + 4).join('\n');
      if (FOCUS_REPLACEMENT_RE.test(window)) return;
      const above = ctx.lines.slice(Math.max(0, i - FOCUS_WITHIN_LOOKBACK), i).join('\n');
      if (FOCUS_WITHIN_RE.test(above)) return;
      out.push({
        rule: 'a11y/outline-none-without-ring',
        line: i + 1,
        column: m.index + 1,
        message: `\`${m[0]}\` with no focus-visible replacement nearby`,
      });
    });
  });
}

// ---------------------------------------------------------------- icons

/** Sparkle-style glyphs from icon libraries Rook apps use. */
const SPARKLE_ICONS = [
  'Sparkles',
  'Sparkle',
  'WandSparkles',
  'Wand2',
  'SparklesIcon',
  'SparkleIcon',
];
const ICON_IMPORT_RE =
  /import\s*\{([^}]*)\}\s*from\s*['"](lucide-react|@radix-ui\/react-icons|react-icons\/[\w-]+|@heroicons\/react[\w/-]*)['"]/g;

function scanSparkles(ctx: FileContext, out: RawFinding[]): void {
  if (!isCode(ctx.ext)) return;
  ICON_IMPORT_RE.lastIndex = 0;
  const imported = new Set<string>();
  for (let m = ICON_IMPORT_RE.exec(ctx.text); m; m = ICON_IMPORT_RE.exec(ctx.text)) {
    for (const part of (m[1] ?? '').split(',')) {
      const [name, alias] = part.trim().split(/\s+as\s+/);
      if (name && SPARKLE_ICONS.includes(name)) imported.add(alias ?? name);
    }
  }
  if (imported.size === 0) return;
  const useRe = new RegExp(`(?<![\\w$.])(${[...imported].join('|')})(?![\\w$])`, 'g');
  ctx.lines.forEach((line, i) => {
    if (isCommentLine(line)) return;
    eachMatch(useRe, line, (m) => {
      out.push({
        rule: 'icons/no-sparkles',
        line: i + 1,
        column: m.index + 1,
        message: `Sparkle icon \`${m[1]}\`; use \`Bot\` for AI features or a plain icon elsewhere`,
      });
    });
  });
}

// ---------------------------------------------------------------- deprecated API

function scanDeprecated(ctx: FileContext, out: RawFinding[]): void {
  if (!isCode(ctx.ext) || ctx.deprecations.length === 0) return;
  for (const d of ctx.deprecations) {
    const importRe = new RegExp(
      `from\\s+['"]${d.package.replace(/[/.]/g, '\\$&')}(?:/[^'"]*)?['"]`,
    );
    if (!importRe.test(ctx.text)) continue;
    const nameRe = new RegExp(`(?<![\\w$.])${d.name.replace(/[.$]/g, '\\$&')}(?![\\w$])`, 'g');
    ctx.lines.forEach((line, i) => {
      if (isCommentLine(line)) return;
      eachMatch(nameRe, line, (m) => {
        out.push({
          rule: 'api/deprecated',
          line: i + 1,
          column: m.index + 1,
          message: `\`${d.name}\` from ${d.package} is deprecated since ${d.since}${d.replacement ? `; use \`${d.replacement}\`` : ''}${d.message ? `. ${d.message}` : ''}`,
        });
      });
    });
  }
}

/** Runs every file-level rule. Package-level rules live in check.ts. */
export function scanFile(ctx: FileContext): RawFinding[] {
  const out: RawFinding[] = [];
  scanColor(ctx, out);
  scanTypeAndSpace(ctx, out);
  scanCssStructure(ctx, out);
  scanLegacyAndOverrides(ctx, out);
  scanDuplicate(ctx, out);
  scanA11y(ctx, out);
  scanOutline(ctx, out);
  scanDeprecated(ctx, out);
  scanSparkles(ctx, out);
  return out;
}

// ---------------------------------------------------------------- suppressions

export interface Suppression {
  /** Line the suppression applies to (1-based). */
  readonly target: number;
  readonly rules: readonly string[];
  readonly reason: string | null;
  readonly line: number;
  readonly column: number;
}

const SUPPRESS_RE =
  /monarch-ignore-next-line\s+([\w/*,\s-]*?)(?:\s+--\s*(.*?))?\s*(?:\*\/\s*\}?)?\s*$/;

// A line holding only a comment: line comment, block comment, JSX comment, or a doc-comment continuation.
const COMMENT_ONLY = /^\s*(?:\/\/.*|\/\*.*\*\/\s*|\{\s*\/\*.*\*\/\s*\}\s*|\*.*)$/;

export function readSuppressions(lines: readonly string[]): Suppression[] {
  const out: Suppression[] = [];
  lines.forEach((line, i) => {
    const idx = line.indexOf('monarch-ignore-next-line');
    if (idx === -1) return;
    const m = SUPPRESS_RE.exec(line.slice(idx));
    if (!m) return;
    const rules = (m[1] ?? '')
      .split(/[\s,]+/)
      .map((r) => r.trim())
      .filter(Boolean);
    const reason = m[2]?.replace(/\*\/\s*\}?\s*$/, '').trim() || null;
    // Target the next line of code, skipping comment-only lines, so the
    // directive can sit alongside eslint-disable-next-line and similar.
    let t = i + 1;
    while (t < lines.length && COMMENT_ONLY.test(lines[t] ?? '')) t++;
    out.push({ target: t + 1, rules, reason, line: i + 1, column: idx + 1 });
  });
  return out;
}

export function ruleMatches(pattern: string, rule: string): boolean {
  if (pattern === '*' || pattern === rule) return true;
  if (pattern.endsWith('/*')) return rule.startsWith(pattern.slice(0, -1));
  return false;
}
