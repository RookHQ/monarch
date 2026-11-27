import nativeFacts from 'virtual:monarch-native-facts';
import rows from 'virtual:monarch-native-catalogue/ios';
import { NativeCatalogue } from '../../features/native/catalogue.js';

/** `/ios/components/`: the iOS inventory and plan, from `platforms/ios`. */
export function IosComponentsPage() {
  return <NativeCatalogue platform="ios" rows={rows} facts={nativeFacts.ios} />;
}
