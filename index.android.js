'use strict';

import {
  NativeModules
} from 'react-native';

const Braintree = NativeModules.Braintree;

module.exports = {
  setup(token) {
    return new Promise(function(resolve, reject) {
      Braintree.setup(token, test => resolve(test), err => reject(err));
    });
  },
  showPayPalViewController(amount, shippingRequired, currencyCode) {
    return new Promise(function(resolve, reject) {
      Braintree.paypalRequest(
          amount,
          shippingRequired,
          currencyCode,
          ({
             nonce,
             email,
             firstName,
             lastName,
             phone,
             shippingAddress
           }) => resolve({
            nonce,
            email,
            firstName,
            lastName,
            phone,
            shipping: shippingAddress
          }),
          error => reject(error)
      );
    });
  },
};
