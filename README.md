# react-native-braintree-custom-ui
[![npm version](https://badge.fury.io/js/react-native-braintree-custom-ui.svg)](https://badge.fury.io/js/react-native-braintree-custom-ui)

An effort to update https://github.com/kraffslol/react-native-braintree-xplat.

- **v1.1.0** - Braintree Android SDK V4 & iOS V5.
- **v1.0.18** - Braintree Android SDK V3 & iOS V4.

Required RN 0.60+ for auto linking on iOS.

## Installation

```json
"react-native-braintree-custom-ui": "appchoose/react-native-braintree-custom-ui#1.1.0",
```

## Setup

This plugin uses only Tokenization Key for initialisation (https://developers.braintreepayments.com/guides/authorization/tokenization-key/android/v2)
Put your serverUrl where the plugin will be able to make a GET request and get the token from your server

```js
var BTClient = require('react-native-braintree-custom-ui');
BTClient.setup(<serverUrl>,'your.bundle.id.payments');
```

### Android
In Your `AndroidManifest.xml`, `android:allowBackup="false"` can be replaced `android:allowBackup="true"`, it is responsible for app backup.

Also, add this intent-filter to your main activity in `AndroidManifest.xml`

> Note: `android:exported` is required if your app compile SDK version is API 31 (Android 12) or later.

```xml
<activity>
    ...
    <intent-filter>
        <action android:name="android.intent.action.VIEW" />
        <category android:name="android.intent.category.DEFAULT" />
        <category android:name="android.intent.category.BROWSABLE" />
        <data android:scheme="${applicationId}.braintree" />
    </intent-filter>
</activity>
```

### iOS
Add a bundle url scheme {BUNDLE_IDENTIFIER}.payments in your app Info via XCode or manually in the Info.plist. In your Info.plist, you should have something like:

```xml 
<key>CFBundleURLTypes</key>
<array>
    <dict>
        <key>CFBundleTypeRole</key>
        <string>Editor</string>
        <key>CFBundleURLName</key>
        <string>com.myapp</string>
        <key>CFBundleURLSchemes</key>
        <array>
            <string>com.myapp.payments</string>
        </array>
    </dict>
</array>
```

Update code in `AppDelegate.m`:

```objective-c
#import "BraintreeCore.h"

...
- (BOOL)application:(UIApplication *)application didFinishLaunchingWithOptions:(NSDictionary *)launchOptions
{
    ...
    [BTAppContextSwitcher setReturnURLScheme:self.paymentsURLScheme];
}

- (BOOL)application:(UIApplication *)application
            openURL:(NSURL *)url
            options:(NSDictionary<UIApplicationOpenURLOptionsKey,id> *)options {

    if ([url.scheme localizedCaseInsensitiveCompare:self.paymentsURLScheme] == NSOrderedSame) {
        return [BTAppContextSwitcher handleOpenURL:url];
    }
    
    return [RCTLinkingManager application:application openURL:url options:options];
}

- (NSString *)paymentsURLScheme {
    NSString *bundleIdentifier = [[NSBundle mainBundle] bundleIdentifier];
    return [NSString stringWithFormat:@"%@.%@", bundleIdentifier, @"payments"];
}
```

## Usage

### PayPal Checkout 

This plugin implements Paypal Checkout https://developers.braintreepayments.com/guides/paypal/checkout-with-paypal/android/v2

You will need to provide an amount to make it works

```js
BTClient.showPayPalViewController(<amount>, <shippingRequired>, <currency>).then(function({nonce,
        email,
        firstName,
        lastName,
        billingAddress,
        shippingAddress}) {
  //payment succeeded, pass nonce to server
})
.catch(function(err) {
  //error handling
});
```
