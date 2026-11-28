/**
 * Descriptions and search keywords of the iOS and Android pages, by path. Kept out
 * of navigation.ts (the entry chunk): the search index and the prerender step
 * (meta descriptions, llms.txt) read them from here.
 */
export const NATIVE_NAV_DETAILS: Readonly<
  Record<string, { description: string; keywords: readonly string[] }>
> = {
  '/android': {
    description:
      'Monarch for Android: Kotlin and Jetpack Compose components on Material 3, with font scale, TalkBack, light, dark and high contrast themes, phones and tablets.',
    keywords: ['kotlin', 'jetpack compose', 'material 3', 'native', 'gradle', 'android'],
  },
  '/android/get-started': {
    description:
      'Install Monarch for Android, apply MonarchTheme and use the tokens: requirements, local Maven repository and Gradle setup.',
    keywords: ['install', 'setup', 'gradle', 'maven', 'theme', 'tokens'],
  },
  '/android/guide': {
    description:
      'How Monarch for Android is built: Android first, Material 3 for platform layers, previews, examples, accessibility, adaptive layouts and API policy.',
    keywords: [
      'material 3',
      'accessibility',
      'talkback',
      'adaptive',
      'reference',
      'platform terms',
    ],
  },
  '/android/components': {
    description:
      'The Monarch for Android inventory and plan: coverage, status and native type of every component.',
    keywords: ['catalogue', 'catalog', 'compose', 'parity'],
  },
  '/android/releases': {
    description: 'Release notes of Monarch for Android, from its changelog.',
    keywords: ['changelog', 'release notes', 'migration'],
  },
  '/ios': {
    description:
      'Monarch for iOS: SwiftUI styles and components with Dynamic Type, VoiceOver, light, dark and high contrast themes, for iPhone and iPad.',
    keywords: ['swift', 'swiftui', 'iphone', 'ipad', 'native', 'swift package', 'ios'],
  },
  '/ios/get-started': {
    description:
      'Install Monarch for iOS with Swift Package Manager, apply monarchTheme() and use the tokens: requirements, installation and setup.',
    keywords: ['install', 'setup', 'swift package manager', 'spm', 'theme', 'tokens'],
  },
  '/ios/guide': {
    description:
      'How Monarch for iOS is built: Apple first, styles over system controls, previews, examples, accessibility, iPad layouts and API policy.',
    keywords: ['swiftui', 'accessibility', 'voiceover', 'ipad', 'reference', 'platform terms'],
  },
  '/ios/components': {
    description:
      'The Monarch for iOS inventory and plan: coverage, status and native type of every component.',
    keywords: ['catalogue', 'catalog', 'swiftui', 'parity'],
  },
  '/ios/releases': {
    description: 'Release notes of Monarch for iOS, from its changelog.',
    keywords: ['changelog', 'release notes', 'migration'],
  },
  '/native/adaptive-layout': {
    description:
      'Insets and safe areas, the keyboard, window sizes, navigation, sheets and text scaling on Android and iOS.',
    keywords: ['edge-to-edge', 'safe area', 'ipad', 'foldable', 'tablet', 'dynamic type'],
  },
  '/native/accessibility': {
    description: 'Accessibility rules and testing for Monarch on Android and iOS.',
    keywords: ['talkback', 'voiceover', 'a11y'],
  },
  '/native/compatibility': {
    description: 'The tested toolchain and OS range of Monarch for Android and iOS.',
    keywords: ['versions', 'matrix', 'xcode', 'gradle'],
  },
  '/native/contributing': {
    description: 'How to add or change a component in Monarch for Android and iOS.',
    keywords: ['contribute', 'android', 'ios'],
  },
};
