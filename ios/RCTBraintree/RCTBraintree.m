//
//  RCTBraintree.m
//  RCTBraintree
//
//  Created by Rickard Ekman on 18/06/16.
//  Copyright © 2016 Rickard Ekman. All rights reserved.
//

#import "RCTBraintree.h"
#import "Skillz+DeepLinking.h"
@import Braintree;

@interface RCTBraintree ()

@property (nonatomic, strong) NSString *URLScheme;

@end


@implementation RCTBraintree

+ (BOOL)requiresMainQueueSetup {
    return YES;
}

+ (instancetype)sharedInstance {
    static RCTBraintree *_sharedInstance = nil;
    static dispatch_once_t onceToken;
    dispatch_once(&onceToken, ^{
        _sharedInstance = [[RCTBraintree alloc] init];
    });
    return _sharedInstance;
}

- (instancetype)init
{
    if (self = [super init]) {
        self.dataCollector = [[BTDataCollector alloc] initWithAPIClient:self.braintreeClient];
    }
    return self;
}

RCT_EXPORT_MODULE();

RCT_EXPORT_METHOD(setupWithClientToken:(NSString *)clientToken
                  callback:(RCTResponseSenderBlock)callback)
{
    self.URLScheme = [[Skillz skillzInstance] getPaymentsDeepLinkURLScheme];
    [BTAppContextSwitcher setReturnURLScheme:self.URLScheme];

    self.braintreeClient = [[BTAPIClient alloc] initWithAuthorization:clientToken];

    if (self.braintreeClient == nil) {
        callback(@[@(NO)]);
    }
    else {
        callback(@[@(YES)]);
    }
}


RCT_EXPORT_METHOD(payPalRequestOneTimePayment:(NSString *)amount
                  currencyCode:(NSString *) currencyCode
                  callback:(RCTResponseSenderBlock) callback)
{
    dispatch_async(dispatch_get_main_queue(), ^{
        BTPayPalDriver *payPalDriver = [[BTPayPalDriver alloc] initWithAPIClient:self.braintreeClient];
        BTPayPalCheckoutRequest *request = [[BTPayPalCheckoutRequest alloc] initWithAmount:amount];
        request.currencyCode = currencyCode;
        
        [payPalDriver tokenizePayPalAccountWithPayPalRequest:request completion:^(BTPayPalAccountNonce * _Nullable tokenizedPayPalAccount, NSError * _Nullable error) {
            [self handlePayPalResult:tokenizedPayPalAccount error:error callback:callback];
        }];
    });
}

RCT_EXPORT_METHOD(payPalRequestBillingAgreement:(NSString *)billingAgreementDescription
                  callback:(RCTResponseSenderBlock) callback)
{
    dispatch_async(dispatch_get_main_queue(), ^{
        BTPayPalDriver *payPalDriver = [[BTPayPalDriver alloc] initWithAPIClient:self.braintreeClient];
        BTPayPalVaultRequest *request = [[BTPayPalVaultRequest alloc] init];
        request.billingAgreementDescription = billingAgreementDescription;

        [payPalDriver tokenizePayPalAccountWithPayPalRequest:request completion:^(BTPayPalAccountNonce * _Nullable tokenizedPayPalAccount, NSError * _Nullable error) {
            [self handlePayPalResult:tokenizedPayPalAccount error:error callback:callback];
        }];
    });
}

- (void)handlePayPalResult:(BTPayPalAccountNonce * _Nullable)tokenizedPayPalAccount
                     error:(NSError * _Nullable)error
                  callback:(RCTResponseSenderBlock)callback
{
    NSMutableArray *args = @[[NSNull null]];

    if (error == nil && tokenizedPayPalAccount != nil) {
        NSString *email = tokenizedPayPalAccount.email;
        NSString *firstName = tokenizedPayPalAccount.firstName;
        NSString *lastName = tokenizedPayPalAccount.lastName;
        NSString *phone = tokenizedPayPalAccount.phone;

        // See BTPostalAddress.h for details
        BTPostalAddress *billingAddress = tokenizedPayPalAccount.billingAddress;
        BTPostalAddress *shippingAddress = tokenizedPayPalAccount.shippingAddress;

        args = [@[[NSNull null], tokenizedPayPalAccount.nonce, email, firstName, lastName] mutableCopy];

        if (tokenizedPayPalAccount.phone != nil) {
            [args addObject:phone];
        }
        if (billingAddress != nil) {
            [args addObject:billingAddress];
        }
        if (shippingAddress != nil) {
            [args addObject:shippingAddress];
        }
    } else if ( error != nil ) {
        args = @[error.description, [NSNull null]];
    } else { // per braintree docs, if both error and token are nil, user cancelled
        args = @[@"USER_CANCELLATION", [NSNull null]];
    }

    callback(args);
}

RCT_EXPORT_METHOD(venmoRequestMultiUseAgreement:(NSString *)agreement
                  profileId:(NSString *) profileId
                  shouldVault:(BOOL) shouldVault
                  callback:(RCTResponseSenderBlock) callback)
{
    dispatch_async(dispatch_get_main_queue(), ^{
        BTVenmoDriver *venmoDriver = [[BTVenmoDriver alloc] initWithAPIClient:self.apiClient];
        BTVenmoRequest *venmoRequest = [[BTVenmoRequest alloc] init];
        venmoRequest.vault = shouldVault;
        venmoRequest.profileID = profileId;
        venmoRequest.paymentMethodUsage = BTVenmoPaymentMethodUsageMultiUse;
        [venmoDriver tokenizeVenmoAccountWithVenmoRequest:venmoRequest completion:^(BTVenmoAccountNonce * _Nullable venmoAccount, NSError * _Nullable error) {
            NSMutableArray *args = [[NSMutableArray alloc] init];
            if (venmoAccount) {
                args = @[venmoAccount.nonce, [NSNull null]];
            } else if (error) {
                args = @[error.description, [NSNull null]];
            }
            callback(args);
        }];
    })
}

RCT_EXPORT_METHOD(getCardNonce:(NSDictionary *)params
                  callback:(RCTResponseSenderBlock)callback)
{
    NSMutableDictionary *parameters = [params mutableCopy];
    BTCardClient *cardClient = [[BTCardClient alloc] initWithAPIClient:self.braintreeClient];

    BTCard *card = [self createCardWithParameters:parameters];

    [cardClient tokenizeCard:card
                  completion:^(BTCardNonce *tokenizedCard, NSError *error) {
        if (error == nil) {
            callback(@[[NSNull null], tokenizedCard.nonce]);
            return;
        }

        NSArray *args = @[];
        NSMutableDictionary *userInfo = [error.userInfo mutableCopy];

        [userInfo removeObjectForKey:@"com.braintreepayments.BTHTTPJSONResponseBodyKey"];
        [userInfo removeObjectForKey:@"com.braintreepayments.BTHTTPURLResponseKey"];
        NSError *serialisationErr;
        NSData *jsonData = [NSJSONSerialization dataWithJSONObject:userInfo
                                                           options:NSJSONWritingPrettyPrinted
                                                             error:&serialisationErr];

        if (!jsonData) {
            args = @[serialisationErr.description, [NSNull null]];
        } else {
            NSString *jsonString = [[NSString alloc] initWithData:jsonData encoding:NSUTF8StringEncoding];
            args = @[jsonString, [NSNull null]];
        }
        callback(args);
    }];
}

- (BTCard*)createCardWithParameters:(NSMutableDictionary*)parameters
{
    BTCard *card = [[BTCard alloc] init];
    card.number = parameters[@"number"];
    card.expirationMonth = parameters[@"expirationMonth"];
    card.expirationYear = parameters[@"expirationYear"];
    card.cvv = parameters[@"cvv"];
    card.postalCode = parameters[@"postalCode"];

    if (parameters[@"cardholderName"] != nil) {
        card.cardholderName = parameters[@"cardholderName"];
    }

    if (parameters[@"firstName"] != nil) {
        card.firstName = parameters[@"firstName"];
    }

    if (parameters[@"lastName"] != nil) {
        card.lastName = parameters[@"lastName"];
    }

    if (parameters[@"streetAddress"] != nil) {
        card.streetAddress = parameters[@"streetAddress"];
    }

    if (parameters[@"extendedAddress"] != nil) {
        card.extendedAddress = parameters[@"extendedAddress"];
    }

    if (parameters[@"locality"] != nil) {
        card.locality = parameters[@"locality"];
    }

    if (parameters[@"region"] != nil) {
        card.region = parameters[@"region"];
    }

    if (parameters[@"countryCode"] != nil) {
        // We always send down ISO 3-letter codes since android uses those too
        card.countryCodeAlpha3 = parameters[@"countryCode"];
    }

    card.shouldValidate = NO;
    return card;
}

RCT_EXPORT_METHOD(getDeviceData:(NSDictionary *)options
                  callback:(RCTResponseSenderBlock)callback)
{
    dispatch_async(dispatch_get_main_queue(), ^{
        NSError *error = nil;
        NSString *deviceData = nil;
        NSString *dataSelector = options[@"dataCollector"];

        //Initialize the data collector in V5
        self.dataCollector = [[BTDataCollector alloc] initWithAPIClient: self.braintreeClient];
        
        //Data collection methods
        if ([dataSelector isEqualToString:@"card"] || [dataSelector isEqualToString:@"both"]) {
            [self.dataCollector collectDeviceData:^(NSString * _Nonnull deviceData) {
                deviceData = deviceData;
            }];
        } else if ([dataSelector isEqualToString:@"paypal"] || [dataSelector isEqualToString:@"venmo"]) {
            deviceData = [PPDataCollector collectPayPalDeviceData];
        } else {
            NSMutableDictionary* details = [NSMutableDictionary dictionary];
            [details setValue:@"Invalid data collector" forKey:NSLocalizedDescriptionKey];
            error = [NSError errorWithDomain:@"RCTBraintree" code:255 userInfo:details];

            SKZLog(@"Invalid data collector: %@. Use one of: `card`, `paypal`, or `both`", dataSelector);
        }

        NSArray *args = @[];
        if (error == nil) {
            args = @[[NSNull null], deviceData];
        } else {
            args = @[error.description, [NSNull null]];
        }

        callback(args);
    });
}

- (BOOL)application:(UIApplication *)application
            openURL:(NSURL *)url
            options:(NSDictionary<UIApplicationOpenURLOptionsKey,id> *)options
{
    if ([url.scheme localizedCaseInsensitiveCompare:self.URLScheme] == NSOrderedSame) {
        return [BTAppContextSwitcher handleOpenURL:url];
    }
    return NO;
}

#pragma mark - BTViewControllerPresentingDelegate

- (void)paymentDriver:(id)paymentDriver
requestsPresentationOfViewController:(UIViewController *)viewController
{
    [self.reactRoot presentViewController:viewController animated:YES completion:nil];
}

- (void)paymentDriver:(id)paymentDriver
requestsDismissalOfViewController:(UIViewController *)viewController
{
    if (!self.reactRoot.isBeingDismissed) {
        [self.reactRoot.presentingViewController dismissViewControllerAnimated:YES completion:nil];
    }
}

// #pragma mark - BTDropInViewControllerDelegate

- (void)userDidCancelPayment
{
    [self.reactRoot dismissViewControllerAnimated:YES completion:nil];
    self.callback(@[@"USER_CANCELLATION", [NSNull null]]);
}

- (UIViewController *)reactRoot
{
    UIViewController *root = [UIApplication sharedApplication].keyWindow.rootViewController;
    while (root.presentedViewController) {
        root = root.presentedViewController;
    }

    return root;
}

@end
