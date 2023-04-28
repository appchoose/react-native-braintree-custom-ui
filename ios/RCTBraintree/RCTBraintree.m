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
            } else if (error != nil && (error.code == 4 || error.code == 6)) {
                args = @[@"USER_CANCELLATION", [NSNull null]];
            } else {
                args = @[error.description, [NSNull null]];
            }

            callback(args);
        }];
    });
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

@end
