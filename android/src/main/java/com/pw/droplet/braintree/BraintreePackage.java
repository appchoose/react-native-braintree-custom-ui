package com.pw.droplet.braintree;

import com.braintreepayments.api.VenmoClient;
import com.facebook.react.ReactPackage;
import com.facebook.react.bridge.JavaScriptModule;
import com.facebook.react.bridge.NativeModule;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.uimanager.ViewManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class BraintreePackage implements ReactPackage {
    private final VenmoClient venmoClient;
    private Braintree mModuleInstance;

  public BraintreePackage(VenmoClient venmoClient) {

      this.venmoClient = venmoClient;
  }

    public Braintree getModuleInstance() {
        return mModuleInstance;
    }

  @Override
  public List<NativeModule> createNativeModules(ReactApplicationContext reactContext) {
    List<NativeModule> modules = new ArrayList<>();
    mModuleInstance = new Braintree(reactContext, venmoClient);

    modules.add(mModuleInstance);
    return modules;
  }

 // Deprecated RN 0.47
  public List<Class<? extends JavaScriptModule>> createJSModules() {
    return Collections.emptyList();
  }

  @Override
  public List<ViewManager> createViewManagers(ReactApplicationContext reactContext) {
    return Collections.emptyList();
  }
}
