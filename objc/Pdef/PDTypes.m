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
    return [[self alloc] initWithKey:key value:value];
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
