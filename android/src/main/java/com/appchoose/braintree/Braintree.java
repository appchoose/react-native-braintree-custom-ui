package com.appchoose.braintree;

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
import android.content.Intent;

import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;

import com.braintreepayments.api.BraintreeClient;
import com.braintreepayments.api.BraintreeRequestCodes;
import com.braintreepayments.api.PayPalAccountNonce;
import com.braintreepayments.api.PayPalCheckoutRequest;
import com.braintreepayments.api.PayPalClient;
import com.braintreepayments.api.PayPalPaymentIntent;

import android.app.Activity;
import com.facebook.react.bridge.ActivityEventListener;
import com.facebook.react.bridge.LifecycleEventListener;
import com.facebook.react.bridge.Callback;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;

public class Braintree extends ReactContextBaseJavaModule implements ActivityEventListener, LifecycleEventListener {
    private final ReactApplicationContext mContext;
    private FragmentActivity mCurrentActivity;
    private BraintreeClient mBraintreeClient;
    private PayPalClient mPayPalClient;
    private PaypalListenerImpl mPaypalListener;

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
    public void onNewIntent(Intent intent) {
        if (mCurrentActivity != null) {
            mCurrentActivity.setIntent(intent);
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
        mPaypalListener = new PaypalListenerImpl(mContext);
        successCallback.invoke(token);
    }

    @ReactMethod
    public void paypalRequest(final String amount, final boolean shippingRequired, final String currencyCode) {
        mCurrentActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mPaypalListener.setShippingRequired(shippingRequired);
                mPayPalClient = new PayPalClient(mCurrentActivity, mBraintreeClient);
                mPayPalClient.setListener(mPaypalListener);
                PayPalCheckoutRequest request = new PayPalCheckoutRequest(amount);
                request.setCurrencyCode(currencyCode);
                request.setIntent(PayPalPaymentIntent.AUTHORIZE);
                request.setShippingAddressRequired(shippingRequired);
                request.setShippingAddressEditable(shippingRequired);
                mPayPalClient.tokenizePayPalAccount(
                    mCurrentActivity,
                    request
                );
            }
        });
    }

    @Override
    public void onActivityResult(Activity activity, int requestCode, int resultCode, Intent intent) {
        //NOTE: empty implementation
    }

    @Override
    public void onHostResume() {
        //NOTE: empty implementation
    }

    @Override
    public void onHostPause() {
        if (mPaypalListener != null) {
            mPaypalListener.incrementOnHostPauseCounter();
        }
    }

    @Override
    public void onHostDestroy() {
        //NOTE: empty implementation
    }

    @ReactMethod
    public void addListener(String eventName) {
        //NOTE: empty implementation
    }

    @ReactMethod
    public void removeListeners(Integer count) {
        //NOTE: empty implementation
    }
}
