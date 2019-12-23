'use strict';

import {
  NativeModules,
} from 'react-native';


const RCTBraintree = NativeModules.Braintree;

var Braintree = {
  setup(serverUrl, urlscheme) {
    return new Promise(function (resolve, reject) {
      RCTBraintree.setupWithURLScheme(serverUrl, urlscheme, function (success) {
        success == true ? resolve(true) : reject('Invalid Token');
      });
    });
  },
  showPayPalViewController(amount) {
    return new Promise(function (resolve, reject) {
      RCTBraintree.showPayPalViewController(amount, function (err, nonce, email, firstName, lastName, billingAddress, shippingAddress) {
        nonce != null ? resolve({
          nonce,
          email,
          firstName,
          lastName,
          billingAddress,
          shippingAddress
        }) : reject(err);
      });
    });
  },
  check3DSecure(parameters = {}) {
    return new Promise(function (resolve, reject) {
      RCTBraintree.check3DSecure(parameters, function (
        err,
        nonce
      ) {
        nonce !== null ?
          resolve(nonce) :
          reject(err);
      });
    });
  },
  getCardNonce(parameters = {}) {
    return new Promise(function (resolve, reject) {
      RCTBraintree.getCardNonce(parameters, function (
        err,
        nonce
      ) {
        nonce !== null ?
          resolve(nonce) :
          reject(err);
      });
    });
  },

  getDeviceData(options = {}) {
    return new Promise(function (resolve, reject) {
      RCTBraintree.getDeviceData(options, function (err, deviceData) {
        deviceData != null ? resolve(deviceData) : reject(err);
      });
    });
  },
};

module.exports = Braintree;