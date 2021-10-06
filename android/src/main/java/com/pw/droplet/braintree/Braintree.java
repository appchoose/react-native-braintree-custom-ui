package com.pw.droplet.braintree;

import android.util.Log;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentActivity;

import com.braintreepayments.api.BraintreeClient;
import com.braintreepayments.api.BrowserSwitchResult;
import com.braintreepayments.api.Card;
import com.braintreepayments.api.CardClient;
import com.braintreepayments.api.CardNonce;
import com.braintreepayments.api.CardTokenizeCallback;
import com.braintreepayments.api.Configuration;
import com.braintreepayments.api.ConfigurationCallback;
import com.braintreepayments.api.DataCollector;
import com.braintreepayments.api.DataCollectorCallback;
import com.braintreepayments.api.PayPalAccountNonce;
import com.braintreepayments.api.PayPalBrowserSwitchResultCallback;
import com.braintreepayments.api.PayPalCheckoutRequest;
import com.braintreepayments.api.PayPalClient;
import com.braintreepayments.api.PayPalFlowStartedCallback;
import com.braintreepayments.api.PayPalPaymentIntent;
import com.braintreepayments.api.PayPalRequest;
import com.braintreepayments.api.PayPalVaultRequest;
import com.braintreepayments.api.PostalAddress;
import com.braintreepayments.api.UserCanceledException;
import com.facebook.react.bridge.Callback;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.bridge.WritableNativeMap;

import java.util.Objects;

import javax.annotation.Nonnull;

public class Braintree extends ReactContextBaseJavaModule {
    private static final String TAG = "BraintreeRNModule";
    private String token;

    private Callback payPalSuccessCallback;
    private Callback payPalErrorCallback;

    private BraintreeClient braintreeClient;
    private DataCollector dataCollector;
    private PayPalClient payPalClient;

    public Braintree(ReactApplicationContext reactContext) {
        super(reactContext);
    }

    @Override @Nonnull
    public String getName() {
        return "Braintree";
    }

    /**
     * Invoke this during onResume of your payment activity. It will call either success or failure callbacks
     * that were originally passed when calling either {@link #payPalRequestOneTimePayment} or {@link #payPalRequestBillingAgreement}
     *
     * @param activity your payment activity
     */
    public void onPaymentActivityResume(FragmentActivity activity) {
        BrowserSwitchResult browserSwitchResult = this.braintreeClient.deliverBrowserSwitchResult(activity);
        if (browserSwitchResult != null) {
            this.payPalClient.onBrowserSwitchResult(browserSwitchResult, new PayPalBrowserSwitchResultCallback() {
                @Override
                public void onResult(@Nullable PayPalAccountNonce payPalAccountNonce, @Nullable Exception error) {
                    if (error != null) {
                        invokePayPalErrorCallback(error);
                    } else if (payPalAccountNonce != null) {
                        invokePayPalSuccessCallback(payPalAccountNonce);
                    }
                }
            });
        }
    }

    @ReactMethod
    public void setup(final String token, final Callback successCallback, final Callback errorCallback) {
        try {
            this.token = token;
            this.braintreeClient = new BraintreeClient(Objects.requireNonNull(getCurrentActivity()), this.token);
            this.dataCollector = new DataCollector(this.braintreeClient);
            this.braintreeClient.getConfiguration(new ConfigurationCallback() {
                @Override
                public void onResult(@androidx.annotation.Nullable Configuration configuration, @androidx.annotation.Nullable Exception error) {
                    if (error != null) {
                        errorCallback.invoke(error.toString());
                    } else {
                        successCallback.invoke();
                    }
                }
            });
        } catch (Exception e) {
            errorCallback.invoke(e.getMessage());
        }
    }

    @ReactMethod
    public void getCardNonce(final ReadableMap parameters, final Callback successCallback, final Callback errorCallback) {
        Card card = new Card();
        card.setShouldValidate(false);

        if (parameters.hasKey("number")) {
            card.setNumber(parameters.getString("number"));
        }

        if (parameters.hasKey("cvv")) {
            card.setCvv(parameters.getString("cvv"));
        }

        if (parameters.hasKey("expirationDate")) {
            card.setExpirationDate(parameters.getString("expirationDate"));
        } else {
            if (parameters.hasKey("expirationMonth")) {
                card.setExpirationMonth((parameters.getString("expirationMonth")));
            }

            if (parameters.hasKey("expirationYear")) {
                card.setExpirationYear(parameters.getString("expirationYear"));
            }
        }

        if (parameters.hasKey("cardholderName")) {
            card.setCardholderName(parameters.getString("cardholderName"));
        }

        if (parameters.hasKey("firstName")) {
            card.setFirstName(parameters.getString("firstName"));
        }

        if (parameters.hasKey("lastName")) {
            card.setLastName(parameters.getString("lastName"));
        }

        if (parameters.hasKey("countryCode")) {
            card.setCountryCode(parameters.getString("countryCode"));
        }

        if (parameters.hasKey("locality")) {
            card.setLocality(parameters.getString("locality"));
        }

        if (parameters.hasKey("postalCode")) {
            card.setPostalCode(parameters.getString("postalCode"));
        }

        if (parameters.hasKey("region")) {
            card.setRegion(parameters.getString("region"));
        }

        if (parameters.hasKey("streetAddress")) {
            card.setStreetAddress(parameters.getString("streetAddress"));
        }

        if (parameters.hasKey("extendedAddress")) {
            card.setExtendedAddress(parameters.getString("extendedAddress"));
        }

        CardClient cardClient = new CardClient(this.braintreeClient);
        cardClient.tokenize(card, new CardTokenizeCallback() {
            @Override
            public void onResult(@androidx.annotation.Nullable CardNonce cardNonce, @androidx.annotation.Nullable Exception error) {
                if (error != null) {
                    errorCallback.invoke(error.toString());
                }
                if (cardNonce != null) {
                    successCallback.invoke(cardNonce.getString());
                }
            }
        });
    }

    @ReactMethod
    public void payPalRequestOneTimePayment(final String amount, final String currencyCode, final Callback successCallback, final Callback errorCallback) {
        this.payPalSuccessCallback = successCallback;
        this.payPalErrorCallback = errorCallback;

        PayPalCheckoutRequest request = new PayPalCheckoutRequest(amount);
        request.setCurrencyCode(currencyCode);
        request.setIntent(PayPalPaymentIntent.AUTHORIZE);

        this.tokenizePayPalAccount(request);
    }

    @ReactMethod
    public void payPalRequestBillingAgreement(final String billingAgreementDescription, final Callback successCallback, final Callback errorCallback) {
        this.payPalSuccessCallback = successCallback;
        this.payPalErrorCallback = errorCallback;

        PayPalVaultRequest request = new PayPalVaultRequest();
        request.setBillingAgreementDescription(billingAgreementDescription);

        this.tokenizePayPalAccount(request);
    }

    @ReactMethod
    public void getDeviceData(final ReadableMap options, final Callback successCallback, final Callback errorCallback) {
        try {
            this.dataCollector.collectDeviceData(Objects.requireNonNull(getCurrentActivity()), new DataCollectorCallback() {
                @Override
                public void onResult(@androidx.annotation.Nullable String deviceData, @androidx.annotation.Nullable Exception error) {
                    if (error != null) {
                        errorCallback.invoke(error.toString());
                    } else {
                        successCallback.invoke(deviceData);
                    }
                }
            });
        } catch (Exception error) {
            errorCallback.invoke(error.toString());
        }
    }

    private void tokenizePayPalAccount(PayPalRequest request) {
        try {
            payPalClient = new PayPalClient(this.braintreeClient);
            payPalClient.tokenizePayPalAccount((AppCompatActivity) Objects.requireNonNull(getCurrentActivity()), request, this.payPalFlowStartedCallback);
        } catch (Exception error) {
            invokePayPalErrorCallback(error);
        }
    }

    private WritableMap getPayPalAddressMap(PostalAddress address) {
        WritableNativeMap map = new WritableNativeMap();
        map.putString("recipientName", address.getRecipientName());
        map.putString("streetAddress", address.getStreetAddress());
        map.putString("extendedAddress", address.getExtendedAddress());
        map.putString("locality", address.getLocality());
        map.putString("countryCodeAlpha2", address.getCountryCodeAlpha2());
        map.putString("postalCode", address.getPostalCode());
        map.putString("region", address.getRegion());
        return map;
    }

    private void invokePayPalSuccessCallback(PayPalAccountNonce nonce) {
        if (this.payPalSuccessCallback != null) {
            WritableNativeMap map = new WritableNativeMap();
            map.putString("nonce", nonce.getString());
            map.putString("firstName", nonce.getFirstName());
            map.putString("lastName", nonce.getLastName());

            if (nonce.getBillingAddress() != null && nonce.getBillingAddress().getPostalCode() != null) {
                map.putMap("billingAddress", getPayPalAddressMap(nonce.getBillingAddress()));
            }

            if (nonce.getShippingAddress() != null && nonce.getShippingAddress().getPostalCode() != null) {
                map.putMap("shippingAddress", getPayPalAddressMap(nonce.getShippingAddress()));
            }

            this.payPalSuccessCallback.invoke(map);
        } else {
            Log.e(TAG, "PayPal Success Callback is null");
        }
        this.payPalErrorCallback = null;
        this.payPalSuccessCallback = null;
    }

    private void invokePayPalErrorCallback(Exception error) {
        if (this.payPalErrorCallback != null) {
            if (error instanceof UserCanceledException) {
                this.payPalErrorCallback.invoke("USER_CANCELLATION"); // parity with iOS
            } else {
                this.payPalErrorCallback.invoke(error.toString());
            }
        } else {
            Log.e(TAG, "PayPal Error Callback is null");
        }
        this.payPalErrorCallback = null;
        this.payPalSuccessCallback = null;
    }

    private PayPalFlowStartedCallback payPalFlowStartedCallback = new PayPalFlowStartedCallback() {
        @Override
        public void onResult(@Nullable Exception error) {
            if (error != null) {
                invokePayPalErrorCallback(error);
            }
        }
    };
}
