//
// Created by Ivan Korobkov on 30.04.14.
// Copyright (c) 2014 io.pdef. All rights reserved.
//

#import <objc/runtime.h>
#import "PDTypes.h"
#import "PDStruct.h"
#import "PDEnum.h"
#import "PDInterface.h"


PDType PDTypeForType(id type) {
    if ([type isKindOfClass:NSNumber.class]) {
        return PDTypePrimitive;

    } else if ([type isKindOfClass:PDList.class]) {
        return PDTypeList;

    } else if ([type isKindOfClass:PDMap.class]) {
        return PDTypeMap;

    } else if ([type isKindOfClass:PDSet.class]) {
        return PDTypeSet;

    }

    if (!class_isMetaClass(object_getClass(type))) {
        return PDTypeUndefined;
    }

    if ([type isSubclassOfClass:PDEnum.class]) {
        return PDTypeEnum;

    } else if ([type isSubclassOfClass:PDStruct.class]) {
        return PDTypeStruct;

    } else if ([type isSubclassOfClass:PDInterface.class]) {
        return PDTypeInterface;
    }

    return PDTypeUndefined;
}


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
