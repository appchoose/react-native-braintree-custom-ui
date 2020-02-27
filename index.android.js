'use strict';

import {
  NativeModules
} from 'react-native';
const Braintree = NativeModules.Braintree;

module.exports = {
  setup(token) {
    return new Promise(function (resolve, reject) {
      Braintree.setup(token, test => resolve(test), err => reject(err));
    });
  },

  getCardNonce(parameters = {}) {
    return new Promise(function (resolve, reject) {
      Braintree.getCardNonce(
        parameters,
        nonce => resolve(nonce),
        err => reject(err)
      );
    });
  },
  check3DSecure(parameters = {}) {
    return new Promise(function (resolve, reject) {
      console.log(parameters)
      Braintree.check3DSecure(parameters, nonce => resolve(nonce),
        err => reject(err));
    });
  },
  showPayPalViewController(amount, shippingrequired) {
    return new Promise(function (resolve, reject) {
      Braintree.paypalRequest(amount, (nonce
        // ,
        // email,
        // firstName,
        // lastName,
        // billingAddress,
        // shippingAddress
      ) => resolve({
        nonce,
        // email,
        // firstName,
        // lastName,
        // billingAddress,
        // shippingAddress
      }), error => reject(error));
    });
  },
};