import nativeFacts from 'virtual:monarch-native-facts';
import changelog from 'virtual:monarch-native-changelog/android';
import { NativeReleases } from '../../features/native/releases.js';

/** `/android/releases/`: `platforms/android/CHANGELOG.md`. */
export function AndroidReleasesPage() {
  return <NativeReleases platform="android" changelog={changelog} facts={nativeFacts.android} />;
}
