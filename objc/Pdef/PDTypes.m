//
// Created by Ivan Korobkov on 30.04.14.
// Copyright (c) 2014 io.pdef. All rights reserved.
//

#import "PDTypes.h"



@implementation PDList
+ (id)listWithItem:(id)item {
    return [[self alloc] initWithItem:item];
}

- (instancetype)initWithItem:(id)item {
    NSParameterAssert(item != nil);
    if (self = [super init]) {
        _item = item;
    }

    return self;
}
@end


@implementation PDSet
+ (id)setWithItem:(id)item {
    return [[self alloc] initWithItem:item];
}

- (instancetype)initWithItem:(id)item {
    NSParameterAssert(item != nil);
    if (self = [super init]) {
        _item = item;
    }

    return self;
}
@end


@implementation PDMap
+ (id)mapWithKey:(id)key value:(id)value {
    return nil;
}

- (instancetype)initWithKey:(id)key value:(id)value {
    NSParameterAssert(key != nil);
    NSParameterAssert(value != nil);
    if (self = [super init]) {
        _key = key;
        _value = value;
    }

    return self;
}
@end


@implementation PDEnum
+ (NSDictionary *)valuesToNames {
    return nil;
}
@end


@implementation PDStruct
+ (NSDictionary *)properties {
    return nil;
}
@end


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
    NSParameterAssert((options & PDMethodGet) && (options & PDMethodPost) == NO);

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
