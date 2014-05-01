//
// Created by Ivan Korobkov on 01.05.14.
// Copyright (c) 2014 io.pdef. All rights reserved.
//

#import "PDTypes.h"
#import "PDInterface.h"


@implementation PDInterface
+ (NSArray *)methods {
    return nil;
}
@end


@implementation PDMethod
+ (PDMethod *)methodWithSelector:(SEL)selector name:(NSString *)name
                         options:(PDMethodOptions)options
                          result:(id)result paramNames:(NSArray *)names
                      paramTypes:(NSArray *)types {
    return [[PDMethod alloc] initWithSelector:selector name:name options:options
        result:result paramNames:names paramTypes:types];
}

- (instancetype)initWithSelector:(SEL)selector name:(NSString *)name
                         options:(PDMethodOptions)options result:(id)result
                      paramNames:(NSArray *)names paramTypes:(NSArray *)types {
    NSParameterAssert(selector);
    NSParameterAssert((options & PDMethodGet) || (options & PDMethodPost));
    NSParameterAssert(!((options & PDMethodGet) && (options & PDMethodPost)));

    if (self = [super init]) {
        _selector = selector;
        _name = name;

        _get = options & PDMethodGet;
        _post = options & PDMethodPost;
        _request = options & PDMethodRequest;

        _result = result ? result : @(PDTypeVoid);
        _paramTypes = [names copy];
        _paramNames = [types copy];
    }

    return self;
}
@end
