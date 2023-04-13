package com.appchoose.braintree;

import java.util.Map;
import java.util.HashMap;
import android.util.Log;
import java.util.concurrent.TimeUnit;
import java.io.IOException;
import androidx.appcompat.app.AppCompatActivity;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.OkHttpClient.Builder;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import com.google.gson.Gson;
import android.os.Bundle;
import android.content.Intent;
import android.content.Context;

import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;

import com.braintreepayments.api.BraintreeClient;
import com.braintreepayments.api.BraintreeRequestCodes;
import com.braintreepayments.api.BrowserSwitchResult;
import com.braintreepayments.api.Card;
import com.braintreepayments.api.CardClient;
import com.braintreepayments.api.PayPalAccountNonce;
import com.braintreepayments.api.PayPalCheckoutRequest;
import com.braintreepayments.api.PayPalClient;
import com.braintreepayments.api.PayPalPaymentIntent;
import com.braintreepayments.api.PostalAddress;
import com.braintreepayments.api.ThreeDSecureAdditionalInformation;
import com.braintreepayments.api.ThreeDSecureClient;
import com.braintreepayments.api.ThreeDSecurePostalAddress;
import com.braintreepayments.api.ThreeDSecureRequest;
import com.braintreepayments.api.ThreeDSecureResult;
import com.braintreepayments.api.UserCanceledException;

import android.app.Activity;
import com.facebook.react.bridge.ActivityEventListener;
import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.LifecycleEventListener;
import com.facebook.react.bridge.Callback;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.bridge.WritableNativeMap;

public class Braintree extends ReactContextBaseJavaModule implements ActivityEventListener, LifecycleEventListener {
    private final Context mContext;
    private FragmentActivity mCurrentActivity;
    private BraintreeClient mBraintreeClient;
    private PayPalClient mPayPalClient;
    private ThreeDSecureClient mThreeDSecureClient;

    private boolean mShippingRequired;

    private Callback successCallback;
    private Callback errorCallback;

    public Braintree(ReactApplicationContext reactContext) {
        super(reactContext);
        mContext = reactContext;
        reactContext.addLifecycleEventListener(this);
        reactContext.addActivityEventListener(this);
    }

    @Override
    public String getName() {
        return "Braintree";
    }

    @Override
    public void onActivityResult(Activity activity, int requestCode, int resultCode, Intent intent) {
        switch (requestCode) {
            case BraintreeRequestCodes.THREE_D_SECURE:
                if (mThreeDSecureClient != null) {
                    mThreeDSecureClient.onActivityResult(
                            resultCode,
                            intent,
                            this::handleThreeDSecureResult
                    );
                }
                break;
        }
    }

    @Override
    public void onNewIntent(Intent intent) {
        if (mCurrentActivity != null) {
            mCurrentActivity.setIntent(intent);
        }
    }

    @Override
    public void onHostResume() {
        if (mBraintreeClient != null && mCurrentActivity != null) {
            BrowserSwitchResult browserSwitchResult =
                    mBraintreeClient.deliverBrowserSwitchResult(mCurrentActivity);
            if (browserSwitchResult != null) {
                switch (browserSwitchResult.getRequestCode()) {
                    case BraintreeRequestCodes.PAYPAL:
                        if (mPayPalClient != null) {
                            mPayPalClient.onBrowserSwitchResult(
                                    browserSwitchResult,
                                    this::handlePayPalResult
                            );
                        }
                        break;
                    case BraintreeRequestCodes.THREE_D_SECURE:
                        if (mThreeDSecureClient != null) {
                            mThreeDSecureClient.onBrowserSwitchResult(
                                    browserSwitchResult,
                                    this::handleThreeDSecureResult
                            );
                        }
                        break;
                }
            }
        }
    }

    @ReactMethod
    public void setup(final String url, final Callback successCallback, final Callback errorCallback) {
        String token = "";
        mCurrentActivity = (FragmentActivity) getCurrentActivity();
        try {
            OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build();
            Request request = new Request.Builder()
                .url(url)
                .build();

            Response response = client.newCall(request).execute();

            token = response.body().string();
        } catch (IOException e) {
            errorCallback.invoke(e.getMessage());
        }
        mBraintreeClient = new BraintreeClient(mContext, token);
        successCallback.invoke(token);
    }

    @ReactMethod
    public void paypalRequest(final String amount, final boolean shippingRequired, final String currencyCode, final Callback successCallback, final Callback errorCallback) {
        this.successCallback = successCallback;
        this.errorCallback = errorCallback;

        mShippingRequired = shippingRequired;
        mPayPalClient = new PayPalClient(mBraintreeClient);
        PayPalCheckoutRequest request = new PayPalCheckoutRequest(amount);
        request.setCurrencyCode(currencyCode);
        request.setIntent(PayPalPaymentIntent.AUTHORIZE);
        request.setShippingAddressRequired(shippingRequired);
        request.setShippingAddressEditable(shippingRequired);
        mPayPalClient.tokenizePayPalAccount(
            mCurrentActivity,
            request,
            e -> handlePayPalResult(null, e)
        );
    }

    @ReactMethod
    public void getCardNonce(final ReadableMap parameters, final Callback successCallback, final Callback errorCallback) {
        this.successCallback = successCallback;
        this.errorCallback = errorCallback;

        CardClient cardClient = new CardClient(mBraintreeClient);
        Card card = new Card();

        if (parameters.hasKey("number"))
            card.setNumber(parameters.getString("number"));

        if (parameters.hasKey("cvv"))
            card.setCvv(parameters.getString("cvv"));

        // In order to keep compatibility with iOS implementation, do not accept expirationMonth and exporationYear,
        // accept rather expirationDate (which is combination of expirationMonth/expirationYear)
        if (parameters.hasKey("expirationDate"))
            card.setExpirationDate(parameters.getString("expirationDate"));

        if (parameters.hasKey("cardholderName"))
            card.setCardholderName(parameters.getString("cardholderName"));

        if (parameters.hasKey("firstname"))
            card.setFirstName(parameters.getString("firstname"));

        if (parameters.hasKey("lastname"))
            card.setLastName(parameters.getString("lastname"));

        if (parameters.hasKey("countryCode"))
            card.setCountryCode(parameters.getString("countryCode"));

        if (parameters.hasKey("countryCodeAlpha2"))
            card.setCountryCode(parameters.getString("countryCodeAlpha2"));

        if (parameters.hasKey("locality"))
            card.setLocality(parameters.getString("locality"));

        if (parameters.hasKey("postalCode"))
            card.setPostalCode(parameters.getString("postalCode"));

        if (parameters.hasKey("region"))
            card.setRegion(parameters.getString("region"));

        if (parameters.hasKey("streetAddress"))
            card.setStreetAddress(parameters.getString("streetAddress"));

        if (parameters.hasKey("extendedAddress"))
            card.setExtendedAddress(parameters.getString("extendedAddress"));

        cardClient.tokenize(card, (cardNonce, error) -> {
            if (error != null) {
                nonceErrorCallback(error);
                return;
            }
            if (cardNonce != null) {
                nonceCallback(cardNonce.getString());
            }
        });
    }

    @ReactMethod
    public void check3DSecure(final ReadableMap parameters, final Callback successCallback, final Callback errorCallback) {
        this.successCallback = successCallback;
        this.errorCallback = errorCallback;

        ThreeDSecurePostalAddress address = new ThreeDSecurePostalAddress();

        if (parameters.hasKey("firstname"))
            address.setGivenName(parameters.getString("firstname"));

        if (parameters.hasKey("lastname"))
            address.setSurname(parameters.getString("lastname"));

        if (parameters.hasKey("phone"))
            address.setPhoneNumber(parameters.getString("phone"));

        if (parameters.hasKey("countryCode"))
            address.setCountryCodeAlpha2(parameters.getString("countryCode"));

        if (parameters.hasKey("locality"))
            address.setLocality(parameters.getString("locality"));

        if (parameters.hasKey("postalCode"))
            address.setPostalCode(parameters.getString("postalCode"));

        if (parameters.hasKey("region"))
            address.setRegion(parameters.getString("region"));

        if (parameters.hasKey("streetAddress"))
            address.setStreetAddress(parameters.getString("streetAddress"));

        if (parameters.hasKey("extendedAddress"))
            address.setExtendedAddress(parameters.getString("extendedAddress"));

        mThreeDSecureClient = new ThreeDSecureClient(mBraintreeClient);

        ThreeDSecureAdditionalInformation additionalInformation = new ThreeDSecureAdditionalInformation();
        additionalInformation.setShippingAddress(address);

        final ThreeDSecureRequest threeDSecureRequest = new ThreeDSecureRequest();
        threeDSecureRequest.setNonce(parameters.getString("nonce"));
        threeDSecureRequest.setEmail(parameters.getString("email"));
        threeDSecureRequest.setBillingAddress(address);
        threeDSecureRequest.setVersionRequested(ThreeDSecureRequest.VERSION_2);
        threeDSecureRequest.setAdditionalInformation(additionalInformation);
        threeDSecureRequest.setAmount(parameters.getString("amount"));

        mThreeDSecureClient.performVerification(
                mCurrentActivity,
                threeDSecureRequest,
                (threeDSecureResult, error) -> {
                    if (error != null) {
                        nonceErrorCallback(error);
                        return;
                    }
                    if (threeDSecureResult != null) {
                        mThreeDSecureClient.continuePerformVerification(
                                mCurrentActivity,
                                threeDSecureRequest,
                                threeDSecureResult,
                                this::handleThreeDSecureResult);
                    }
                });
    }

    private WritableMap getPayPalAddressMap(PostalAddress address) {
        WritableNativeMap map = new WritableNativeMap();
        map.putString("streetAddress", address.getStreetAddress());
        map.putString("recipientName", address.getRecipientName());
        map.putString("postalCode", address.getPostalCode());
        map.putString("countryCodeAlpha2", address.getCountryCodeAlpha2());
        map.putString("extendedAddress", address.getExtendedAddress());
        map.putString("region", address.getRegion());
        map.putString("locality", address.getLocality());
        return map;
    }

    private void handlePayPalResult(
            @Nullable PayPalAccountNonce payPalAccountNonce,
            @Nullable Exception error
    ) {
        if (error != null) {
            nonceErrorCallback(error);
            return;
        }
        if (payPalAccountNonce != null) {
            WritableNativeMap map = new WritableNativeMap();
            map.putString("nonce", payPalAccountNonce.getString());
            map.putString("email", payPalAccountNonce.getEmail());
            map.putString("firstName", payPalAccountNonce.getFirstName());
            map.putString("lastName", payPalAccountNonce.getLastName());
            map.putString("phone", payPalAccountNonce.getPhone());
//             if (payPalAccountNonce.getBillingAddress() != null) {
//                 map.putMap("billingAddress", getPayPalAddressMap(payPalAccountNonce.getBillingAddress()));
//             }
            final PostalAddress shippingAddress = payPalAccountNonce.getShippingAddress();
            if (mShippingRequired && shippingAddress != null) {
                map.putMap("shippingAddress", getPayPalAddressMap(shippingAddress));
            }
            this.successCallback.invoke(map);
        }
    }

    private void handleThreeDSecureResult(ThreeDSecureResult threeDSecureResult, Exception error) {
        if (error != null) {
            nonceErrorCallback(error);
            return;
        }
        if (threeDSecureResult != null && threeDSecureResult.getTokenizedCard() != null) {
            nonceCallback(threeDSecureResult.getTokenizedCard().getString());
        }
    }

    private void nonceErrorCallback(Exception error) {
        if (error instanceof UserCanceledException) {
            this.errorCallback.invoke("USER_CANCELLATION");
        } else {
            this.errorCallback.invoke(error.getMessage());
        }
    }

    private void nonceCallback(String nonce) {
        this.successCallback.invoke(nonce);
    }

    @Override
    public void onHostPause() {
        //NOTE: empty implementation
    }

    @Override
    public void onHostDestroy() {
        //NOTE: empty implementation
    }
}
