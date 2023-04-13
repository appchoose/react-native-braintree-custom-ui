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
  getCardNonce(parameters = {}) {
    return new Promise(function(resolve, reject) {
      Braintree.getCardNonce(
          parameters,
          nonce => resolve(nonce),
          err => reject(err)
      );
    });
  },
  check3DSecure(parameters = {}) {
    return new Promise(function(resolve, reject) {
      console.log(parameters)
      Braintree.check3DSecure(
          parameters,
          nonce => resolve(nonce),
          err => reject(err)
      );
    });
  },
};
