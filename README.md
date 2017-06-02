<<<<<<< HEAD
# FingerPrintScanner
Fingerprint scanner for Android and IOS devices
=======

# react-native-fingerprint-scanner

## Getting started

`$ npm install react-native-fingerprint-scanner --save`

### Mostly automatic installation

`$ react-native link react-native-fingerprint-scanner`

### Manual installation


#### iOS

1. In XCode, in the project navigator, right click `Libraries` ➜ `Add Files to [your project's name]`
2. Go to `node_modules` ➜ `react-native-fingerprint-scanner` and add `ReactNativeFingerprintScanner.xcodeproj`
3. In XCode, in the project navigator, select your project. Add `libReactNativeFingerprintScanner.a` to your project's `Build Phases` ➜ `Link Binary With Libraries`
4. Run your project (`Cmd+R`)<

#### Android

1. Open up `android/app/src/main/java/[...]/MainActivity.java`
  - Add `import com.reactlibrary.ReactNativeFingerprintScannerPackage;` to the imports at the top of the file
  - Add `new ReactNativeFingerprintScannerPackage()` to the list returned by the `getPackages()` method
2. Append the following lines to `android/settings.gradle`:
  	```
  	include ':react-native-fingerprint-scanner'
  	project(':react-native-fingerprint-scanner').projectDir = new File(rootProject.projectDir, 	'../node_modules/react-native-fingerprint-scanner/android')
  	```
3. Insert the following lines inside the dependencies block in `android/app/build.gradle`:
  	```
      compile project(':react-native-fingerprint-scanner')
  	```

#### Windows
[Read it! :D](https://github.com/ReactWindows/react-native)

1. In Visual Studio add the `ReactNativeFingerprintScanner.sln` in `node_modules/react-native-fingerprint-scanner/windows/ReactNativeFingerprintScanner.sln` folder to their solution, reference from their app.
2. Open up your `MainPage.cs` app
  - Add `using Com.Reactlibrary.ReactNativeFingerprintScanner;` to the usings at the top of the file
  - Add `new ReactNativeFingerprintScannerPackage()` to the `List<IReactPackage>` returned by the `Packages` method


## Usage
```javascript
import ReactNativeFingerprintScanner from 'react-native-fingerprint-scanner';

// TODO: What to do with the module?
ReactNativeFingerprintScanner;
```
  
>>>>>>> 1b3b8d4 (initial commit)
