package com.pw.droplet.braintree;

import java.util.Map;
import java.util.HashMap;
import android.util.Log;
import java.util.concurrent.TimeUnit;
import com.braintreepayments.api.interfaces.BraintreeCancelListener;
import java.io.IOException;
import androidx.appcompat.app.AppCompatActivity;
import com.squareup.okhttp.MediaType;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.RequestBody;
import com.squareup.okhttp.Response;

import com.google.gson.Gson;
import android.os.Bundle;
import android.content.Intent;
import android.content.Context;
import com.braintreepayments.api.ThreeDSecure;
import com.braintreepayments.api.models.ThreeDSecureRequest;
import com.braintreepayments.api.models.ThreeDSecureAdditionalInformation;
import com.braintreepayments.api.models.ThreeDSecurePostalAddress;

import com.braintreepayments.api.models.PaymentMethodNonce;
import com.braintreepayments.api.BraintreeFragment;
import android.app.Activity;
import com.braintreepayments.api.exceptions.InvalidArgumentException;
import com.braintreepayments.api.exceptions.BraintreeError;
import com.braintreepayments.api.exceptions.ErrorWithResponse;
import com.braintreepayments.api.models.CardBuilder;
import com.braintreepayments.api.Card;
import com.braintreepayments.api.PayPal;
import com.braintreepayments.api.models.PayPalRequest;
import com.braintreepayments.api.interfaces.PaymentMethodNonceCreatedListener;
import com.braintreepayments.api.interfaces.BraintreeErrorListener;
import com.braintreepayments.api.models.CardNonce;
import com.facebook.react.bridge.Callback;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.ReadableMap;

public class Braintree extends ReactContextBaseJavaModule   {
  private static final int PAYMENT_REQUEST = 1706816330;
  private String token;

  private Callback successCallback;
  private Callback errorCallback;

  private Context mActivityContext;
  private BraintreeFragment mBraintreeFragment;

  private ReadableMap threeDSecureOptions;

  public Braintree(ReactApplicationContext reactContext) {
    super(reactContext);
  }

  @Override
  public String getName() {
    return "Braintree";
  }

  public String getToken() {
    return this.token;
  }

  public void setToken(String token) {
    this.token = token;
  }


  @ReactMethod
  public void setup(final String url, final Callback successCallback, final Callback errorCallback) {
  try {
     Log.d("PAYMENT_REQUEST",url);
  OkHttpClient client = new OkHttpClient();
client.setConnectTimeout(15, TimeUnit.SECONDS); // connect timeout
client.setReadTimeout(15, TimeUnit.SECONDS);    // socket timeout
Request request = new Request.Builder()
                     .url(url)
                     .build();
                     try {
Response response = client.newCall(request).execute();
String res = response.body().string();
        this.mBraintreeFragment = BraintreeFragment.newInstance((AppCompatActivity) getCurrentActivity(),  res);
           Log.d("PAYMENT_REQUEST", res);
         }catch(IOException e){
              Log.e("PAYMENT_REQUEST", "I got an error", e);
         }
           
                this.mBraintreeFragment.addListener(new BraintreeCancelListener() {
            @Override
            public void onCancel(int requestCode) {
              nonceErrorCallback("USER_CANCELLATION");
            }
          });
      this.mBraintreeFragment.addListener(new PaymentMethodNonceCreatedListener() {
        @Override
        public void onPaymentMethodNonceCreated(PaymentMethodNonce paymentMethodNonce) {
            CardNonce cardNonce = (CardNonce) paymentMethodNonce;
            if (!cardNonce.getThreeDSecureInfo().isLiabilityShiftPossible()) {
              nonceErrorCallback("3DSECURE_NOT_ABLE_TO_SHIFT_LIABILITY");
            } else if (!cardNonce.getThreeDSecureInfo().isLiabilityShifted()) {
              nonceErrorCallback("3DSECURE_LIABILITY_NOT_SHIFTED");
            } else {
              nonceCallback(paymentMethodNonce.getNonce());
            }
        }
      });
      
      this.mBraintreeFragment.addListener(new BraintreeErrorListener() {
        @Override
        public void onError(Exception error) {
              Log.e("PAYMENT_REQUEST", "I got an error", error);
          if (error instanceof ErrorWithResponse) {
            ErrorWithResponse errorWithResponse = (ErrorWithResponse) error;
            BraintreeError cardErrors = errorWithResponse.errorFor("creditCard");
            if (cardErrors != null) {
              Gson gson = new Gson();
              final Map<String, String> errors = new HashMap<>();
              BraintreeError numberError = cardErrors.errorFor("number");
              BraintreeError cvvError = cardErrors.errorFor("cvv");
              BraintreeError expirationDateError = cardErrors.errorFor("expirationDate");
              BraintreeError postalCode = cardErrors.errorFor("postalCode");

              if (numberError != null) {
                errors.put("card_number", numberError.getMessage());
              }

              if (cvvError != null) {
                errors.put("cvv", cvvError.getMessage());
              }

              if (expirationDateError != null) {
                errors.put("expiration_date", expirationDateError.getMessage());
              }

              // TODO add more fields
              if (postalCode != null) {
                errors.put("postal_code", postalCode.getMessage());
              }

              nonceErrorCallback(gson.toJson(errors));
            } else {
              nonceErrorCallback(errorWithResponse.getErrorResponse());
            }
          }
        }
      });
      this.setToken(token);
      successCallback.invoke(this.getToken()); 
      } catch (InvalidArgumentException e) {
              Log.e("PAYMENT_REQUEST", "I got an error", e);
      errorCallback.invoke(e.getMessage());
    }
  }

  @ReactMethod
  public void getCardNonce(final ReadableMap parameters, final Callback successCallback, final Callback errorCallback)  {
    this.successCallback = successCallback;
    this.errorCallback = errorCallback;

    CardBuilder cardBuilder = new CardBuilder()
      .validate(false);

    if (parameters.hasKey("number"))
      cardBuilder.cardNumber(parameters.getString("number"));

    if (parameters.hasKey("cvv"))
      cardBuilder.cvv(parameters.getString("cvv"));

    // In order to keep compatibility with iOS implementation, do not accept expirationMonth and exporationYear,
    // accept rather expirationDate (which is combination of expirationMonth/expirationYear)
    if (parameters.hasKey("expirationDate"))
      cardBuilder.expirationDate(parameters.getString("expirationDate"));

    if (parameters.hasKey("cardholderName"))
      cardBuilder.cardholderName(parameters.getString("cardholderName"));

    if (parameters.hasKey("firstname"))
      cardBuilder.firstName(parameters.getString("firstname"));

    if (parameters.hasKey("lastname"))
      cardBuilder.lastName(parameters.getString("lastname"));

    if (parameters.hasKey("countryCode"))
      cardBuilder.countryCode(parameters.getString("countryCode"));

    if (parameters.hasKey("locality"))
      cardBuilder.locality(parameters.getString("locality"));

    if (parameters.hasKey("postalCode"))
      cardBuilder.postalCode(parameters.getString("postalCode"));

    if (parameters.hasKey("region"))
      cardBuilder.region(parameters.getString("region"));

    if (parameters.hasKey("streetAddress"))
      cardBuilder.streetAddress(parameters.getString("streetAddress"));

    if (parameters.hasKey("extendedAddress"))
      cardBuilder.extendedAddress(parameters.getString("extendedAddress"));

ThreeDSecure.performVerification(this.mBraintreeFragment, cardBuilder, parameters.getString("amount"));
    // Card.tokenize(this.mBraintreeFragment, cardBuilder);
  }
  @ReactMethod
  public void check3DSecure(final ReadableMap parameters, final Callback successCallback, final Callback errorCallback) {
    Log.d("PAYMENT_REQUEST",parameters.getString("nonce"));
    this.successCallback = successCallback;
    this.errorCallback = errorCallback;


ThreeDSecurePostalAddress address = new ThreeDSecurePostalAddress();

 if (parameters.hasKey("firstname"))
      address.givenName(parameters.getString("firstname"));

    if (parameters.hasKey("lastname"))
      address.surname(parameters.getString("lastname"));

    if (parameters.hasKey("phone"))
      address.phoneNumber(parameters.getString("phone"));

    if (parameters.hasKey("locality"))
      address.locality(parameters.getString("locality"));

    if (parameters.hasKey("postalCode"))
      address.postalCode(parameters.getString("postalCode"));

    if (parameters.hasKey("region"))
      address.region(parameters.getString("region"));

    if (parameters.hasKey("streetAddress"))
      address.streetAddress(parameters.getString("streetAddress"));

    if (parameters.hasKey("extendedAddress"))
      address.extendedAddress(parameters.getString("extendedAddress"));

// For best results, provide as many additional elements as possible.
ThreeDSecureAdditionalInformation additionalInformation = new ThreeDSecureAdditionalInformation()
    .shippingAddress(address);

ThreeDSecureRequest threeDSecureRequest = new ThreeDSecureRequest()
        .nonce(parameters.getString("nonce"))
         .email(parameters.getString("email"))
         .billingAddress(address)
           .versionRequested(ThreeDSecureRequest.VERSION_2)
           .additionalInformation(additionalInformation)
        .amount(parameters.getString("amount"));

ThreeDSecure.performVerification(this.mBraintreeFragment, threeDSecureRequest);
  }

  public void nonceCallback(String nonce) {
    this.successCallback.invoke(nonce);
  }

  public void nonceErrorCallback(String error) {
    this.errorCallback.invoke(error);
  }

  @ReactMethod
  public void paypalRequest(final String amount , final Callback successCallback, final Callback errorCallback) {
    this.successCallback = successCallback;
    this.errorCallback = errorCallback;
      PayPalRequest request = new PayPalRequest(amount)
    .currencyCode("EUR")
    .intent(PayPalRequest.INTENT_AUTHORIZE);

  PayPal.requestOneTimePayment(this.mBraintreeFragment, request);
  }
  
  public void onNewIntent(Intent intent){}
}
