'use strict';

import {
  NativeModules,
  NativeEventEmitter,
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
      const paypalEmitter = new NativeEventEmitter();
      paypalEmitter.addListener("PaypalStatus", (params) => callback(params));
      const callback = (params) => {
        paypalEmitter.removeAllListeners("PaypalStatus");
        if (params.error) {
          reject(params.error);
        } else {
          const {
            nonce,
            email,
            firstName,
            lastName,
            phone,
            shippingAddress
          } = params;
          resolve({
            nonce,
            email,
            firstName,
            lastName,
            phone,
            shipping: shippingAddress
          });
        }
      }
      Braintree.paypalRequest(
        amount,
        shippingRequired,
        currencyCode
      );

    });
  },
};
