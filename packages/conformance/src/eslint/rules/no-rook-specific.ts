import {
  defineRule,
  REACT_PACKAGE_RE,
  importedName,
  on,
  ruleDocsUrl,
  stringValue,
} from '../ast.js';
import type { ImportDeclaration, MonarchRule } from '../types.js';

/** The same test as the manifest's `scope: "rook"`. */
const ROOK_NAME_RE = /Rook|^rook|^ROOK_/;
const ROOK_ENTRY = '@rookcom/monarch-react/rook';
const PRODUCT_CSS_RE =
  /^@rookcom\/monarch-tokens\/(?:dist\/)?css\/products\/([a-z0-9-]+)(?:\.css)?$/;

/** What a team outside Rook uses instead (docs/guides/rook-specific-exports.md). */
const ALTERNATIVES: readonly (readonly [RegExp, string])[] = [
  [/^RookRail|^rookRailScript$|^ROOK_RAIL_/, "SidebarNav in AppShell's `rail` slot"],
  [/^RookBar|^useRookBarPanel$/, 'SiteHeader (/marketing) or TopNav, with AccountMenu (/patterns)'],
  [
    /^ROOK_THEME_/,
    'ThemeToggle with your own `cookieName`, and the same name in appearanceScript({ themeCookie })',
  ],
  [/^SignInWithRookButton/, 'SignInForm (/patterns) with your own providers, or a Button'],
  [
    /^rookIntelligenceTransport$|^mapRookIntelligenceEvent$|^RookIntelligence/,
    'createSseTransport with your own endpoint and event mapper, or your own AiTransport',
  ],
];

export const alternativeFor = (name: string): string =>
  ALTERNATIVES.find(([re]) => re.test(name))?.[1] ?? 'the general component for the job';

export const noRookSpecific: MonarchRule = defineRule(
  {
    type: 'suggestion',
    docs: {
      description:
        'Disallow exports that encode Rook platform contracts (scope "rook" in the manifest) and Rook product accents, for teams outside Rook.',
      url: ruleDocsUrl('no-rook-specific'),
      recommended: false,
    },
    messages: {
      rookExport:
        '`{{name}}` encodes a Rook platform contract (scope "rook" in the manifest). Outside Rook, use {{alternative}}.',
      rookEntry:
        '`@rookcom/monarch-react/rook` holds the exports built for the Rook platform. Outside Rook, use the general components instead.',
      productAccent:
        'The `{{product}}` accent belongs to a Rook product. Outside Rook, load `@rookcom/monarch-tokens/css/products/neutral`, or derive your own with deriveTheme(hex) and brandThemeCss.',
    },
    schema: [],
  },
  (context) => {
    return {
      ImportDeclaration: on<ImportDeclaration>((node) => {
        const source = stringValue(node.source);
        if (source === undefined) return;
        const product = PRODUCT_CSS_RE.exec(source)?.[1];
        if (product && product !== 'neutral') {
          context.report({ node: node.source, messageId: 'productAccent', data: { product } });
          return;
        }
        if (!REACT_PACKAGE_RE.test(source)) return;
        let reported = false;
        for (const spec of node.specifiers) {
          const name = importedName(spec);
          if (!name || !ROOK_NAME_RE.test(name)) continue;
          reported = true;
          context.report({
            node: spec,
            messageId: 'rookExport',
            data: { name, alternative: alternativeFor(name) },
          });
        }
        if (!reported && source === ROOK_ENTRY)
          context.report({ node: node.source, messageId: 'rookEntry' });
      }),
    };
  },
);
