import nativeFacts from 'virtual:monarch-native-facts';
import ios from 'virtual:monarch-native-platform/ios';
import { PlatformLanding } from '../../features/native/platform-landing.js';

/** `/ios/`: the landing page of Monarch for iOS. The long form is `/ios/get-started/` and `/ios/guide/`. */
export function IosPage() {
  return <PlatformLanding platform="ios" facts={nativeFacts.ios} data={ios.landing} />;
}
