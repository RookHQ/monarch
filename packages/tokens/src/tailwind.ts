/**
 * Tailwind CSS v3 preset.
 *
 *   // tailwind.config.ts
 *   import monarch from '@rookcom/monarch-tokens/tailwind';
 *   export default { presets: [monarch], content: ['./src/**\/*.{ts,tsx}'] };
 *
 * Two colour namespaces:
 * - `mn-*` (Monarch-native): `bg-mn-surface`, `text-mn-fg-muted`, `border-mn-line`.
 * - legacy shadcn names (`bg-primary`, `text-muted-foreground`) backed by the
 *   generated legacy bridge, so existing apps keep working during migration.
 *
 * Written without importing Tailwind, so this package has no Tailwind dependency.
 */
import { breakpoint } from './primitives.js';

const mn = (name: string) =>
  `color-mix(in srgb, var(--mn-color-${name}) calc(<alpha-value> * 100%), transparent)`;
const legacy = (name: string) => `hsl(var(--${name}) / <alpha-value>)`;

function intent(id: string) {
  return {
    DEFAULT: mn(`${id}-solid`),
    solid: mn(`${id}-solid`),
    'solid-hover': mn(`${id}-solid-hover`),
    'solid-active': mn(`${id}-solid-active`),
    'on-solid': mn(`${id}-on-solid`),
    subtle: mn(`${id}-subtle`),
    'subtle-hover': mn(`${id}-subtle-hover`),
    strong: mn(`${id}-strong`),
    'on-strong': mn(`${id}-on-strong`),
    border: mn(`${id}-border`),
    'strong-inverse': mn(`${id}-strong-inverse`),
  };
}

const legacyPair = (name: string) => ({
  DEFAULT: legacy(name),
  foreground: legacy(`${name}-foreground`),
});

type PluginApi = { addVariant: (name: string, selector: string | string[]) => void };

export const monarchPreset = {
  // Typed as a tuple so the preset is assignable to Tailwind's `Config`.
  darkMode: ['variant', ['&:is(.dark *)', '&:is([data-mn-theme="dark"] *)']] as [
    'variant',
    string[],
  ],
  theme: {
    screens: { ...breakpoint },
    container: { center: true, padding: '2rem', screens: { '2xl': '1400px' } },
    extend: {
      colors: {
        mn: {
          canvas: mn('bg-canvas'),
          surface: mn('bg-surface'),
          raised: mn('bg-raised'),
          subtle: mn('bg-subtle'),
          muted: mn('bg-muted'),
          emphasis: mn('bg-emphasis'),
          inverse: mn('bg-inverse'),
          fg: {
            DEFAULT: mn('text-default'),
            muted: mn('text-muted'),
            subtle: mn('text-subtle'),
            disabled: mn('text-disabled'),
            inverse: mn('text-inverse'),
            'inverse-muted': mn('text-inverse-muted'),
            link: mn('text-link'),
          },
          line: {
            DEFAULT: mn('border-default'),
            subtle: mn('border-subtle'),
            strong: mn('border-strong'),
            input: mn('border-input'),
            inverse: mn('border-inverse'),
          },
          focus: mn('focus-ring'),
          accent: intent('accent'),
          danger: intent('danger'),
          success: intent('success'),
          warning: intent('warning'),
          info: intent('info'),
          data: Object.fromEntries([1, 2, 3, 4, 5].map((i) => [i, mn(`data-${i}`)])),
        },
        border: legacy('border'),
        input: legacy('input'),
        ring: legacy('ring'),
        background: legacy('background'),
        foreground: legacy('foreground'),
        primary: legacyPair('primary'),
        secondary: legacyPair('secondary'),
        success: legacyPair('success'),
        warning: legacyPair('warning'),
        destructive: legacyPair('destructive'),
        muted: legacyPair('muted'),
        accent: legacyPair('accent'),
        popover: legacyPair('popover'),
        card: legacyPair('card'),
        sidebar: {
          DEFAULT: legacy('sidebar-background'),
          foreground: legacy('sidebar-foreground'),
          primary: legacy('sidebar-primary'),
          'primary-foreground': legacy('sidebar-primary-foreground'),
          accent: legacy('sidebar-accent'),
          'accent-foreground': legacy('sidebar-accent-foreground'),
          border: legacy('sidebar-border'),
          ring: legacy('sidebar-ring'),
        },
      },
      fontFamily: {
        sans: ['var(--mn-font-sans)'],
        mono: ['var(--mn-font-mono)'],
      },
      fontSize: {
        '2xs': ['var(--mn-text-2xs)', { lineHeight: '1rem' }] as [string, { lineHeight: string }],
      },
      borderRadius: {
        xs: 'var(--mn-radius-xs)',
        sm: 'var(--mn-radius-sm)',
        md: 'var(--mn-radius-md)',
        lg: 'var(--mn-radius-lg)',
        xl: 'var(--mn-radius-xl)',
        '2xl': 'var(--mn-radius-2xl)',
      },
      boxShadow: Object.fromEntries(
        ['xs', 'sm', 'md', 'lg', 'xl'].map((k) => [`mn-${k}`, `var(--mn-shadow-${k})`]),
      ),
      height: Object.fromEntries(
        ['xs', 'sm', 'md', 'lg', 'xl'].map((k) => [`control-${k}`, `var(--mn-control-${k})`]),
      ),
      spacing: {
        'section-sm': 'var(--mn-section-sm)',
        'section-md': 'var(--mn-section-md)',
        'section-lg': 'var(--mn-section-lg)',
        rail: 'var(--mn-layout-rail-collapsed)',
        'rail-expanded': 'var(--mn-layout-rail-expanded)',
      },
      maxWidth: {
        content: 'var(--mn-layout-content-max)',
        reading: 'var(--mn-layout-reading-max)',
        'mn-narrow': 'var(--mn-width-narrow)',
        'mn-content': 'var(--mn-width-content)',
        'mn-wide': 'var(--mn-width-wide)',
      },
      transitionDuration: {
        fast: 'var(--mn-duration-fast)',
        base: 'var(--mn-duration-base)',
        slow: 'var(--mn-duration-slow)',
      },
      transitionTimingFunction: {
        standard: 'var(--mn-ease-standard)',
        enter: 'var(--mn-ease-enter)',
        exit: 'var(--mn-ease-exit)',
        emphasized: 'var(--mn-ease-emphasized)',
      },
      zIndex: {
        sticky: 'var(--mn-z-sticky)',
        rail: 'var(--mn-z-rail)',
        dropdown: 'var(--mn-z-dropdown)',
        overlay: 'var(--mn-z-overlay)',
        modal: 'var(--mn-z-modal)',
        popover: 'var(--mn-z-popover)',
        toast: 'var(--mn-z-toast)',
        tooltip: 'var(--mn-z-tooltip)',
      },
      backgroundImage: {
        'mn-spectrum': 'var(--mn-gradient-spectrum)',
        'mn-wash': 'var(--mn-gradient-wash)',
        'mn-accent': 'var(--mn-gradient-accent)',
        'mn-inverse': 'var(--mn-gradient-inverse)',
      },
      keyframes: {
        fadeIn: { '0%': { opacity: '0' }, '100%': { opacity: '1' } },
        slideUp: {
          '0%': { opacity: '0', transform: 'translateY(8px)' },
          '100%': { opacity: '1', transform: 'translateY(0)' },
        },
        scaleIn: {
          '0%': { opacity: '0', transform: 'scale(0.97)' },
          '100%': { opacity: '1', transform: 'scale(1)' },
        },
        'accordion-down': {
          from: { height: '0' },
          to: { height: 'var(--radix-accordion-content-height)' },
        },
        'accordion-up': {
          from: { height: 'var(--radix-accordion-content-height)' },
          to: { height: '0' },
        },
      },
      animation: {
        'fade-in': 'fadeIn var(--mn-duration-slow) var(--mn-ease-enter)',
        'slide-up': 'slideUp var(--mn-duration-slow) var(--mn-ease-enter)',
        'scale-in': 'scaleIn var(--mn-duration-base) var(--mn-ease-enter)',
        'accordion-down': 'accordion-down var(--mn-duration-base) var(--mn-ease-standard)',
        'accordion-up': 'accordion-up var(--mn-duration-base) var(--mn-ease-standard)',
      },
    },
  },
  plugins: [
    ({ addVariant }: PluginApi) => {
      // Matches the existing RookRail contract (<html data-rook-rail="expanded">).
      addVariant('rail-expanded', 'html[data-rook-rail="expanded"] &');
    },
  ],
};

export default monarchPreset;
