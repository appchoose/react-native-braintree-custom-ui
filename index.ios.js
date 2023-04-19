'use strict';

import {
  NativeModules
} from 'react-native';
const RCTBraintree = NativeModules.Braintree;

module.exports = {
  setup(serverUrl, urlscheme) {
    return new Promise(function(resolve, reject) {
      RCTBraintree.setupWithURLScheme(serverUrl, urlscheme, function(success) {
        success == true ? resolve(true) : reject('Invalid Token');
      });
    });
  },
  showPayPalViewController(amount, shippingRequired, currencyCode) {
    return new Promise(function(resolve, reject) {
      RCTBraintree.showPayPalViewController(
          amount,
          shippingRequired,
          currencyCode,
          function(err, nonce, email, firstName, lastName, shipping) {
            nonce != null ? resolve({
              nonce,
              email,
              firstName,
              lastName,
              shipping
            }) : reject(err);
          });
    });
  },
};
