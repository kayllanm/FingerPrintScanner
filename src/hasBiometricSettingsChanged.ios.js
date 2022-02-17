import { NativeModules } from 'react-native';
import createError from './createError';

const { ReactNativeFingerprintScanner } = NativeModules;

export default () => {
  return new Promise((resolve, reject) => {
    ReactNativeFingerprintScanner.hasBiometricSettingsChanged((error, hasSettingsChanged) => {
      if (error) return reject(createError(error.code, error.message));
      resolve(hasSettingsChanged);
    });
  });
}
