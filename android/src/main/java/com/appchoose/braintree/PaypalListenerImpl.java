package com.appchoose.braintree;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.braintreepayments.api.PayPalAccountNonce;
import com.braintreepayments.api.PayPalListener;
import com.braintreepayments.api.PostalAddress;
import com.braintreepayments.api.UserCanceledException;

import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.bridge.WritableNativeMap;
import com.facebook.react.modules.core.DeviceEventManagerModule;

public class PaypalListenerImpl implements PayPalListener {
    private final ReactApplicationContext context;
    private boolean shippingRequired;

    public PaypalListenerImpl(@NonNull ReactApplicationContext context, @NonNull boolean shippingRequired) {
        this.context = context;
        this.shippingRequired = shippingRequired;
    }

    private void sendEvent(String eventName,
        @Nullable WritableMap params) {
        context
            .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
            .emit(eventName, params);
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

    @Override
    public void onPayPalSuccess(@NonNull PayPalAccountNonce payPalAccountNonce) {
        WritableNativeMap map = new WritableNativeMap();
        map.putString("nonce", payPalAccountNonce.getString());
        map.putString("email", payPalAccountNonce.getEmail());
        map.putString("firstName", payPalAccountNonce.getFirstName());
        map.putString("lastName", payPalAccountNonce.getLastName());
        map.putString("phone", payPalAccountNonce.getPhone());
        final PostalAddress shippingAddress = payPalAccountNonce.getShippingAddress();
        if (shippingRequired && shippingAddress != null) {
            map.putMap("shippingAddress", getPayPalAddressMap(shippingAddress));
        }
        sendEvent("PaypalStatus", map);
    }

    @Override
    public void onPayPalFailure(@NonNull Exception error) {
        WritableNativeMap map = new WritableNativeMap();
        if (error instanceof UserCanceledException) {
            map.putString("error", "USER_CANCELLATION");
        } else {
            map.putString("error", error.getMessage());
        }
        sendEvent("PaypalStatus", map);
    }


}
