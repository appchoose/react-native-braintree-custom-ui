//
//  RCTBraintree.m
//  RCTBraintree
//
//  Created by Rickard Ekman on 18/06/16.
//  Copyright © 2016 Rickard Ekman. All rights reserved.
//

#import "RCTBraintree.h"

@implementation RCTBraintree {
    bool runCallback;
}

+ (BOOL)requiresMainQueueSetup
{
    return YES;
}

static NSString *URLScheme;

+ (instancetype)sharedInstance
{
    static RCTBraintree *_sharedInstance = nil;
    static dispatch_once_t onceToken;
    dispatch_once(&onceToken, ^ {
        _sharedInstance = [[RCTBraintree alloc] init];
    });
    return _sharedInstance;
}

RCT_EXPORT_MODULE();

RCT_EXPORT_METHOD(setupWithURLScheme:(NSString *)serverUrl urlscheme:(NSString*)urlscheme callback:(RCTResponseSenderBlock)callback)
{
    URLScheme = urlscheme;
    [BTAppContextSwitcher setReturnURLScheme:urlscheme];

    NSURL *clientTokenURL = [NSURL URLWithString:serverUrl];
    NSMutableURLRequest *clientTokenRequest = [NSMutableURLRequest requestWithURL:clientTokenURL];
    [clientTokenRequest setValue:@"text/plain" forHTTPHeaderField:@"Accept"];

    [[[NSURLSession sharedSession] dataTaskWithRequest:clientTokenRequest completionHandler:^(NSData * _Nullable data, NSURLResponse * _Nullable response, NSError * _Nullable error) {
        NSString *clientToken = [[NSString alloc] initWithData:data encoding:NSUTF8StringEncoding];
        self.braintreeClient = [[BTAPIClient alloc] initWithAuthorization:clientToken];
        self.dataCollector = [[BTDataCollector alloc] initWithAPIClient:self.braintreeClient];

        if (self.braintreeClient == nil) {
            callback(@[@false]);
        } else {
            callback(@[@true]);
        }
    }] resume];
}


RCT_EXPORT_METHOD(showPayPalViewController: (NSString *)amount shippingrequired:(BOOL*)shippingrequired currencyCode:(NSString*)currencyCode callback: (RCTResponseSenderBlock) callback)
{
    dispatch_async(dispatch_get_main_queue(), ^ {

        BTPayPalDriver *payPalDriver = [[BTPayPalDriver alloc] initWithAPIClient:self.braintreeClient];
        BTPayPalCheckoutRequest *request= [[BTPayPalCheckoutRequest alloc] initWithAmount:amount];
        request.currencyCode = currencyCode;
        request.shippingAddressRequired = shippingrequired;
        request.shippingAddressEditable = shippingrequired;
        [payPalDriver requestOneTimePayment:request completion:^(BTPayPalAccountNonce * _Nullable tokenizedPayPalAccount, NSError * _Nullable error)
        {
            NSMutableArray *args = @[[NSNull null]];

            if ( error == nil && tokenizedPayPalAccount != nil ) {
                NSString *email = tokenizedPayPalAccount.email;
                NSString *firstName = tokenizedPayPalAccount.firstName;
                NSString *lastName = tokenizedPayPalAccount.lastName;
                NSString *phone = tokenizedPayPalAccount.phone;

                // See BTPostalAddress.h for details
                //   BTPostalAddress *billingAddress = tokenizedPayPalAccount.billingAddress;
                BTPostalAddress *shippingAddress = tokenizedPayPalAccount.shippingAddress;



                // if (tokenizedPayPalAccount.phone != nil) {
                //     [args addObject:phone];
                // }
                // if (billingAddress != nil) {
                //     [args addObject:billingAddress];
                // }
                if (shippingAddress != nil && shippingrequired) {

                    NSMutableDictionary *contentDictionary = [[NSMutableDictionary alloc]init];
                    [contentDictionary setValue:shippingAddress.streetAddress forKey:@"streetAddress"];
                    [contentDictionary setValue:shippingAddress.recipientName forKey:@"recipientName"];
                    [contentDictionary setValue:shippingAddress.postalCode forKey:@"postalCode"];
                    [contentDictionary setValue:shippingAddress.countryCodeAlpha2 forKey:@"countryCodeAlpha2"];
                    [contentDictionary setValue:shippingAddress.extendedAddress forKey:@"extendedAddress"];
                    [contentDictionary setValue:shippingAddress.region forKey:@"region"];
                    [contentDictionary setValue:shippingAddress.locality forKey:@"locality"];
                    [contentDictionary setValue:phone forKey:@"phone"];
                    args = [@[[NSNull null], tokenizedPayPalAccount.nonce, email, firstName, lastName, contentDictionary] mutableCopy];
                } else {
                    args = [@[[NSNull null], tokenizedPayPalAccount.nonce, email, firstName, lastName] mutableCopy];
                }
            } else if ( error != nil ) {
                args = @[error.description, [NSNull null]];
            }

            callback(args);
        }];
    });
}

- (void)onLookupComplete:(__unused BTThreeDSecureRequest *)request result:(__unused BTThreeDSecureLookup *)lookup next:(void (^)(void))next
{
    // Optionally inspect the lookup result and prepare UI if a challenge is required
    next();
}

- (void)run3DSecureCheck:(NSDictionary *)parameters callback: (RCTResponseSenderBlock)callback
{
    BTThreeDSecureRequest *threeDSecureRequest = [[BTThreeDSecureRequest alloc] init];
    threeDSecureRequest.amount = [NSDecimalNumber decimalNumberWithString: parameters[@"amount"]];
    threeDSecureRequest.nonce =  parameters[@"nonce"];
    // Make sure that self conforms to the BTThreeDSecureRequestDelegate protocol
    threeDSecureRequest.threeDSecureRequestDelegate = self;
    threeDSecureRequest.email = parameters[@"email"];
    threeDSecureRequest.versionRequested = BTThreeDSecureVersion2;

    BTThreeDSecurePostalAddress *address = [BTThreeDSecurePostalAddress new];
    address.givenName =  parameters[@"firstname"]; // ASCII-printable characters required, else will throw a validation error
    address.surname = parameters[@"lastname"]; // ASCII-printable characters required, else will throw a validation error
    address.phoneNumber = parameters[@"phone"];
    address.streetAddress = parameters[@"streetAddress"];
    address.locality = parameters[@"locality"];
    address.postalCode = parameters[@"postalCode"];
    threeDSecureRequest.billingAddress = address;
    // Optional additional information.
    // For best results, provide as many of these elements as possible.
    BTThreeDSecureAdditionalInformation *additionalInformation = [BTThreeDSecureAdditionalInformation new];
    additionalInformation.shippingAddress = address;
    threeDSecureRequest.additionalInformation = additionalInformation;

    //
    self.paymentFlowDriver = [[BTPaymentFlowDriver alloc] initWithAPIClient:self.braintreeClient];
    self.paymentFlowDriver.viewControllerPresentingDelegate = self;

    [self.paymentFlowDriver startPaymentFlow:threeDSecureRequest completion:^(BTPaymentFlowResult * _Nonnull result, NSError * _Nonnull error) {
        NSArray *args = @[];
        if (error) {
            args = @[error.localizedDescription, [NSNull null]];
            // Handle error
        } else if (result) {
            BTThreeDSecureResult *threeDSecureResult = (BTThreeDSecureResult *)result;
            if (threeDSecureResult.tokenizedCard.threeDSecureInfo.liabilityShiftPossible) {
                if (threeDSecureResult.tokenizedCard.threeDSecureInfo.liabilityShifted) {
                    args = @[[NSNull null], threeDSecureResult.tokenizedCard.nonce];
                } else {
                    // 3D Secure authentication failed
                    args = @[@"failed", [NSNull null]];
                }
            } else {
                args = @[[NSNull null], threeDSecureResult.tokenizedCard.nonce];
            }
        } else {
            // 3D Secure authentication was not possible
            args = @[[NSNull null], parameters[@"nonce"]];
        }
        callback(args);
    }];

}

RCT_EXPORT_METHOD(check3DSecure: (NSDictionary *)parameters callback: (RCTResponseSenderBlock)callback)
{
    [self run3DSecureCheck:parameters callback:callback];
}


RCT_EXPORT_METHOD(getCardNonce: (NSDictionary *)params callback: (RCTResponseSenderBlock)callback)
{
    NSMutableDictionary *parameters = [params mutableCopy];
    BTCardClient *cardClient = [[BTCardClient alloc] initWithAPIClient: self.braintreeClient];
    BTCard *card = [[BTCard alloc] init];
    card.number = parameters[@"number"];
    card.expirationMonth = parameters[@"expirationMonth"];
    card.expirationYear = parameters[@"expirationYear"];
    card.cvv = parameters[@"cvv"];
    card.shouldValidate = NO;
    [cardClient tokenizeCard:card
    completion:^(BTCardNonce *tokenizedCard, NSError *error) {
        if ( error == nil ) {
            if(parameters[@"amount"] == nil) {
                NSArray *args = @[];
                args = @[[NSNull null], tokenizedCard.nonce];
                callback(args);
            }
            else {
                parameters[@"nonce"] = tokenizedCard.nonce;
                [self run3DSecureCheck:parameters callback:callback];
            }
        }
        else {

            NSArray *args = @[];
            NSMutableDictionary *userInfo = [error.userInfo mutableCopy];

            [userInfo removeObjectForKey:@"com.braintreepayments.BTHTTPJSONResponseBodyKey"];
            [userInfo removeObjectForKey:@"com.braintreepayments.BTHTTPURLResponseKey"];
            NSError *serialisationErr;
            NSData *jsonData = [NSJSONSerialization dataWithJSONObject:userInfo
                                options:NSJSONWritingPrettyPrinted
                                error:&serialisationErr];

            if (! jsonData) {
                args = @[serialisationErr.description, [NSNull null]];
            } else {
                NSString *jsonString = [[NSString alloc] initWithData:jsonData encoding:NSUTF8StringEncoding];
                args = @[jsonString, [NSNull null]];
            }
            callback(args);
        }


    }];
}

- (BOOL)application:(UIApplication *)application
    openURL:(NSURL *)url
    options:(NSDictionary<UIApplicationOpenURLOptionsKey,id> *)options
{

    if ([url.scheme localizedCaseInsensitiveCompare:URLScheme] == NSOrderedSame) {
        return [BTAppContextSwitcher handleOpenURL:url];
    }
    return NO;
}

#pragma mark - BTViewControllerPresentingDelegate

- (void)paymentDriver:(id)paymentDriver requestsPresentationOfViewController:(UIViewController *)viewController
{
    [self.reactRoot presentViewController:viewController animated:YES completion:nil];
}

- (void)paymentDriver:(id)paymentDriver requestsDismissalOfViewController:(UIViewController *)viewController
{
    if (!self.reactRoot.isBeingDismissed) {
        [self.reactRoot.presentingViewController dismissViewControllerAnimated:YES completion:nil];
    }
}

// #pragma mark - BTDropInViewControllerDelegate

- (void)userDidCancelPayment
{
    [self.reactRoot dismissViewControllerAnimated:YES completion:nil];
    runCallback = FALSE;
    self.callback(@[@"USER_CANCELLATION", [NSNull null]]);
}

- (UIViewController*)reactRoot
{
    UIViewController *root = [UIApplication sharedApplication].keyWindow.rootViewController;
    while (root.presentedViewController) {
        root = root.presentedViewController;
    }

    return root;
}

@end
