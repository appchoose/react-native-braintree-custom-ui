import {
  NativeModules,
} from 'react-native';

const RCTBraintree = NativeModules.Braintree;

export type CardNonce = string;
export type PayPalSuccess = {
  nonce: string,
  email: string | null,
  firstName: string | null,
  lastName: string | null,
  billingAddress: Address | null,
  shippingAddress: Address | null,
};

export type Address = {
  recipientName: string | null,
  streetAddress: string | null,
  extendedAddress: string | null, // address line 2
  locality: string | null, // city
  countryCodeAlpha2: string | null,
  postalCode: string | null,
  region: string | null, // state
};

export type VenmoNonce = string;

export type DeviceData = string;

class Braintree {
  setup(clientToken: string): Promise<boolean> {
    return new Promise((resolve: (result: boolean) => void, reject: (reason: string) => void) => {
      RCTBraintree.setupWithClientToken(clientToken, (success: boolean) => {
        success ? resolve(true) : reject('Invalid Token');
      });
    });
  }

  getPayPalOneTimePaymentNonce(amount: number, currencyCode: string): Promise<PayPalSuccess> {
    return new Promise((resolve: (result: PayPalSuccess) => void, reject: (reason: string | null) => void) => {
      RCTBraintree.payPalRequestOneTimePayment(amount, currencyCode, (err: string | null, nonce: string | null, email: string | null, firstName: string | null, lastName: string | null, billingAddress: Address | null, shippingAddress: Address | null) => {
        if (nonce) {
          resolve({
            nonce,
            email,
            firstName,
            lastName,
            billingAddress,
            shippingAddress,
          });
        } else {
          reject(err);
        }
      });
    });
  }

  getPayPalBillingAgreementNonce(billingAgreementDescription: string): Promise<PayPalSuccess> {
    return new Promise((resolve: (result: PayPalSuccess) => void, reject: (reason: string | null) => void) => {
      RCTBraintree.payPalRequestBillingAgreement(billingAgreementDescription, (err: string | null, nonce: string | null, email: string | null, firstName: string | null, lastName: string | null, billingAddress: Address | null, shippingAddress: Address | null) => {
        if (nonce) {
          resolve({
            nonce,
            email,
            firstName,
            lastName,
            billingAddress,
            shippingAddress,
          });
        } else {
          reject(err);
        }
      });
    });
  }

  getVenmoMultiUseAgreementNonce(profileId: string, shouldVault: boolean): Promise<VenmoNonce> {
    return new Promise((resolve: (result: VenmoNonce) => void, reject: (reason: string | null) => void) => {
      RCTBraintree.venmoRequestMultiUseAgreement(profileId, shouldVault, (err: string | null, nonce: string | null) => {
        if (nonce) {
          resolve(
            nonce
          );
        } else {
          reject(err);
        }
      });
    });
  }

  getCardNonce(parameters = {}): Promise<CardNonce> {
    return new Promise((resolve: (result: CardNonce) => void, reject: (reason: string | null) => void) => {
      RCTBraintree.getCardNonce(parameters, (err: string | null, nonce: string | null) => {
        if (nonce) {
          resolve(nonce);
        } else {
          reject(err);
        }
      });
    });
  }

  getDeviceData(options = {}): Promise<DeviceData> {
    return new Promise((resolve: (result: DeviceData) => void, reject: (reason: string | null) => void) => {
      RCTBraintree.getDeviceData(options, (err: string | null, deviceData: DeviceData | null) => {
        deviceData ? resolve(deviceData) : reject(err);
      });
    });
  }
}

export default new Braintree();
