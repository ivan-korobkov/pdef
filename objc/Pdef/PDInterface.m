//
// Created by Ivan Korobkov on 01.05.14.
// Copyright (c) 2014 io.pdef. All rights reserved.
//

#import <objc/runtime.h>
#import "PDTypes.h"
#import "PDInterface.h"


@implementation PDInterface
+ (NSArray *)pdef_methods {
    return nil;
}

+ (PDMethod *)pdef_methodForSelector:(SEL)selector {
    NSArray *methods = [self pdef_methods];
    for (PDMethod *method in methods) {
        if (method.selector == selector) {
            return method;
        }
    }

    return nil;
}

+ (PDMethod *)pdef_methodForName:(NSString *)name {
    NSArray *methods = [self pdef_methods];
    for (PDMethod *method in methods) {
        if ([method.name isEqualToString:name]) {
            return method;
        }
    }

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
        _paramNames = [names copy];
        _paramTypes = [types copy];
    }

    return self;
}

- (BOOL)isLast {
    return PDTypeForType(_result) != PDTypeInterface;
}
@end
