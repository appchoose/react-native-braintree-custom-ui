import {
  NativeModules
} from 'react-native';

const NativeBraintree = NativeModules.Braintree;

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

export type DeviceData = string;

class Braintree {
  setup(clientToken: string): Promise<boolean> {
    return new Promise((resolve: (result: boolean) => void, reject: (reason: string) => void) => {
      NativeBraintree.setup(clientToken,
        () => resolve(true),
        (error: string) => reject(error)
      );
    });
  }

  getPayPalOneTimePaymentNonce(amount: number, currencyCode: string): Promise<PayPalSuccess> {
    return new Promise((resolve: (result: PayPalSuccess) => void, reject: (reason: string) => void) => {
      NativeBraintree.payPalRequestOneTimePayment(amount, currencyCode,
        (payPalSuccess: PayPalSuccess) => resolve(payPalSuccess),
        (error: string) => reject(error)
      );
    });
  }

  getPayPalBillingAgreementNonce(billingAgreementDescription: string): Promise<PayPalSuccess> {
    return new Promise((resolve: (result: PayPalSuccess) => void, reject: (reason: string | null) => void) => {
      NativeBraintree.payPalRequestBillingAgreement(billingAgreementDescription,
        (payPalSuccess: PayPalSuccess) => resolve(payPalSuccess),
        (error: string) => reject(error)
      );
    });
  }

  getCardNonce(parameters = {}): Promise<CardNonce> {
    return new Promise((resolve: (result: CardNonce) => void, reject: (reason: string | null) => void) => {
      NativeBraintree.getCardNonce(
        parameters,
        (nonce: CardNonce) => resolve(nonce),
        (error: string) => reject(error)
      );
    });
  }

  getDeviceData(options = {}): Promise<DeviceData> {
    return new Promise((resolve: (result: DeviceData) => void, reject: (reason: string) => void) => {
      NativeBraintree.getDeviceData(
        options,
        (deviceData: string) => resolve(deviceData),
        (error: string) => reject(error)
      );
    });
  }
};

export default new Braintree();