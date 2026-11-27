/**
 * The words of the /ios/ and /android/ landing pages, per platform. Short on
 * purpose: the long form is on `/<platform>/get-started/` and
 * `/<platform>/guide/`. Every claim is one the platform's README, inventory
 * or guide already makes; versions and counts are read at build time
 * (virtual:monarch-native-facts), never typed in. The prerender step reads
 * the FAQ and the software description from here for the structured data.
 */
import nativeFacts from 'virtual:monarch-native-facts';
import type { Platform } from './labels.js';

export interface LandingScreen {
  /** File under public/native/, without the theme and extension: `ios-iphone`. */
  file: string;
  /** The device the frame draws. */
  device: 'iphone' | 'ipad' | 'android-phone' | 'android-tablet';
  /** "an iPhone", for alternative text. */
  label: string;
}

/**
 * A category tile: a composition drawn by the sample app on both platforms
 * (`public/native/<platform>-tile-<id>.<theme>.webp`), the catalogue groups it
 * counts and where it links.
 */
export interface LandingTile {
  id: string;
  title: string;
  /** Catalogue groups whose entries the tile counts; empty for theming. */
  groups: readonly string[];
  /** What the picture shows, for its alternative text. */
  shows: string;
}

/** The icons of the feature cards (ui/icons.tsx). */
export type LandingIcon =
  'type' | 'shield' | 'contrast' | 'layers' | 'grid' | 'palette' | 'sliders';

export interface LandingContent {
  platform: Platform;
  name: string;
  /** "Monarch for iOS". */
  product: string;
  headline: readonly [string, string];
  lede: string;
  screens: { phone: LandingScreen; tablet: LandingScreen };
  features: readonly { title: string; text: string; icon: LandingIcon }[];
  devicesTitle: string;
  codeTitle: string;
  faq: readonly { question: string; answer: string }[];
  trademarks: string;
  /** For the structured data (SoftwareSourceCode). */
  language: string;
  runtime: string;
}

/** The category tiles, the same on both platforms. */
export const LANDING_TILES: readonly LandingTile[] = [
  {
    id: 'actions',
    title: 'Actions',
    groups: ['actions'],
    shows: 'Primary, secondary, ghost, danger, loading and disabled buttons',
  },
  {
    id: 'forms',
    title: 'Forms',
    groups: ['forms'],
    shows: 'Text fields: filled, with an error and disabled',
  },
  {
    id: 'navigation',
    title: 'Navigation',
    groups: ['navigation'],
    shows: 'A segmented control above plan, seats and region',
  },
  {
    id: 'data-display',
    title: 'Data and charts',
    groups: ['data-display', 'charts', 'data-grid'],
    shows: 'A bar chart of active seats over six months',
  },
  {
    id: 'feedback',
    title: 'Feedback',
    groups: ['feedback'],
    shows: 'Info, warning and success banners',
  },
  {
    id: 'overlays',
    title: 'Overlays',
    groups: ['overlays'],
    shows: 'A confirmation sheet over a dimmed screen',
  },
  {
    id: 'theming',
    title: 'Themes',
    groups: [],
    shows: 'The same row in the light, dark and high contrast themes',
  },
  {
    id: 'intelligence',
    title: 'AI',
    groups: ['intelligence'],
    shows: 'A prompt composer with an attached report',
  },
];

const ios = nativeFacts.ios;
const android = nativeFacts.android;

const releaseAnswer = (p: Platform): string => {
  const f = nativeFacts[p];
  if (!f.unreleased)
    return `Yes. Version ${f.version ?? ''} is released; Get started shows how to add it.`;
  return p === 'ios'
    ? 'Not yet. The first 0.14.N tag is coming; until then, add a local checkout of platforms/ios.'
    : 'Not yet. Until the first release is on Maven Central, publish it to a local Maven repository.';
};

const SHARED_ANSWER =
  'One design language, three implementations. Tokens come from one source; each component page documents web, iOS and Android side by side.';

export const LANDING: Readonly<Record<Platform, LandingContent>> = {
  ios: {
    platform: 'ios',
    name: 'iOS',
    product: 'Monarch for iOS',
    headline: ['Looks like Monarch.', 'Behaves like iOS.'],
    lede: 'SwiftUI components for iPhone and iPad.',
    screens: {
      phone: { file: 'ios-iphone', device: 'iphone', label: 'an iPhone' },
      tablet: { file: 'ios-ipad', device: 'ipad', label: 'an iPad' },
    },
    features: [
      {
        title: 'Dynamic Type',
        icon: 'type',
        text: 'Text and icons scale to every size, accessibility sizes included.',
      },
      {
        title: 'VoiceOver',
        icon: 'shield',
        text: 'System controls keep their roles, values and gestures.',
      },
      {
        title: 'Light, dark, high contrast',
        icon: 'contrast',
        text: 'Follows Dark Mode, Increase Contrast and Reduce Motion.',
      },
      {
        title: 'Native controls',
        icon: 'layers',
        text: 'Navigation, sheets, menus and pickers stay SwiftUI’s own.',
      },
      {
        title: 'iPhone and iPad',
        icon: 'grid',
        text: 'Size classes, split views and 44-point touch targets.',
      },
      {
        title: 'Your typeface',
        icon: 'palette',
        text: 'San Francisco by default; Poppins as an optional package.',
      },
    ],
    devicesTitle: 'iPhone and iPad.',
    codeTitle: 'Reads like SwiftUI.',
    faq: [
      { question: 'Is it released?', answer: releaseAnswer('ios') },
      {
        question: 'Which iOS versions?',
        answer: `iOS ${ios.requirements.ios ?? '17'} and later, Swift tools ${ios.requirements.swiftTools ?? '6.0'}${ios.requirements.dependencies ? '' : ', no dependencies'}.`,
      },
      {
        question: 'Does it replace SwiftUI’s controls?',
        answer:
          'No. It styles them, such as .buttonStyle(.monarch(.primary)), and adds views only where iOS has none.',
      },
      {
        question: 'Does it work on iPad?',
        answer:
          'Yes. Size classes, NavigationSplitView and resizable windows work as SwiftUI provides them.',
      },
      { question: 'How does it relate to the web and Android?', answer: SHARED_ANSWER },
      {
        question: 'What licence?',
        answer: 'Apache License 2.0. Apple’s SDKs come with Apple’s own terms.',
      },
    ],
    trademarks:
      'Apple, iOS, iPhone, iPad and SwiftUI are trademarks of Apple Inc. Monarch uses the names only to say what it runs on.',
    language: 'Swift',
    runtime: `iOS ${ios.requirements.ios ?? '17'} or later`,
  },
  android: {
    platform: 'android',
    name: 'Android',
    product: 'Monarch for Android',
    headline: ['Looks like Monarch.', 'Behaves like Android.'],
    lede: 'Jetpack Compose components for phones and tablets.',
    screens: {
      phone: { file: 'android-phone', device: 'android-phone', label: 'an Android phone' },
      tablet: { file: 'android-tablet', device: 'android-tablet', label: 'an Android tablet' },
    },
    features: [
      {
        title: 'Font scale',
        icon: 'type',
        text: 'Text in sp grows with the font size; 48dp touch targets.',
      },
      {
        title: 'TalkBack',
        icon: 'shield',
        text: 'Every component exposes its role, state and label.',
      },
      {
        title: 'Light, dark, high contrast',
        icon: 'contrast',
        text: 'Four themes, every colour role checked for contrast.',
      },
      {
        title: 'Material 3 underneath',
        icon: 'layers',
        text: 'Dialogs, sheets, menus and pickers stay Material 3’s own.',
      },
      {
        title: 'Phones to tablets',
        icon: 'grid',
        text: 'Window size classes, foldables and right-to-left layouts.',
      },
      {
        title: 'Haptics',
        icon: 'sliders',
        text: 'Toggles and segmented controls give the platform’s own feedback.',
      },
    ],
    devicesTitle: 'Phones and tablets.',
    codeTitle: 'Reads like Compose.',
    faq: [
      { question: 'Is it released?', answer: releaseAnswer('android') },
      {
        question: 'Which Android versions?',
        answer: `minSdk ${android.requirements.minSdk ?? '24'}, compileSdk and targetSdk ${android.requirements.compileSdk ?? ''}, Kotlin ${android.requirements.kotlin ?? ''}.`,
      },
      {
        question: 'What about dynamic colour?',
        answer:
          'Off by default, because the accent is the brand. MonarchTheme(dynamicColor = true) turns it on.',
      },
      { question: 'How does it relate to the web and iOS?', answer: SHARED_ANSWER },
      {
        question: 'What licence?',
        answer: 'Apache License 2.0, like the AndroidX libraries it uses.',
      },
    ],
    trademarks:
      'Android is a trademark of Google LLC. Monarch uses the name only to say what it runs on.',
    language: 'Kotlin',
    runtime: `Android, minSdk ${android.requirements.minSdk ?? '24'}`,
  },
};

/** The landing page's structured data for the prerender step: the platform library and its FAQ. */
export function landingStructuredData(path: string) {
  const content = path === '/ios' ? LANDING.ios : path === '/android' ? LANDING.android : null;
  if (!content) return null;
  const f = nativeFacts[content.platform];
  return {
    software: {
      name: content.product,
      description: `${content.headline.join(' ')} ${content.lede}`,
      programmingLanguage: content.language,
      runtimePlatform: content.runtime,
      ...(f.version ? { version: f.version } : {}),
    },
    faq: content.faq,
  };
}
