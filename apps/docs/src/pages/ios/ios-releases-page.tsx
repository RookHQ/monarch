import nativeFacts from 'virtual:monarch-native-facts';
import changelog from 'virtual:monarch-native-changelog/ios';
import { NativeReleases } from '../../features/native/releases.js';

/** `/ios/releases/`: `platforms/ios/CHANGELOG.md`. */
export function IosReleasesPage() {
  return <NativeReleases platform="ios" changelog={changelog} facts={nativeFacts.ios} />;
}
