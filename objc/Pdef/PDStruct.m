//
// Created by Ivan Korobkov on 01.05.14.
// Copyright (c) 2014 io.pdef. All rights reserved.
//

#import <objc/runtime.h>
#import "PDStruct.h"
#import "PDEnum.h"
#import "PDTypes.h"
#import "PDJson.h"


@implementation PDStruct

// Implement in a subclass.
+ (NSDictionary *)properties {
    return nil;
}

- (NSDictionary *)pdef_properties {
    return [self.class properties];
}

- (instancetype)initWithJson:(NSData *)data error:(NSError **)error {
    if (self = [super init]) {
        [PDJson parseJson:data intoStruct:self error:error];
        if (*error) {
            return nil;
        }
    }

    return self;
}

- (NSData *)toJson:(NSError **)error {
    return [PDJson serializeStruct:self options:NULL error:error];
}

- (NSData *)toJsonWithOptions:(NSJSONWritingOptions)options error:(NSError **)error {
    return [PDJson serializeStruct:self options:options error:error];
}

- (id)initWithCoder:(NSCoder *)coder {
    if (self = [super init]) {
        id cls = [self class];
        NSDictionary *properties = [cls properties];

        for (NSString *key in [properties allKeys]) {
            id type = properties[key];
            id value;

            if ([type isKindOfClass:NSNumber.class]) {
                PDPrimitive primitive = (PDPrimitive) [type intValue];
                switch (primitive) {
                    case PDPrimitiveBool: value = @([coder decodeBoolForKey:key]); break;
                    case PDPrimitiveInt16: value = @([coder decodeInt32ForKey:key]); break;
                    case PDPrimitiveInt32: value = @([coder decodeInt32ForKey:key]); break;
                    case PDPrimitiveInt64: value = @([coder decodeInt64ForKey:key]); break;
                    case PDPrimitiveFloat: value = @([coder decodeFloatForKey:key]); break;
                    case PDPrimitiveDouble: value = @([coder decodeDoubleForKey:key]); break;
                    case PDPrimitiveString:
                    case PDPrimitiveDate: value = [coder decodeObjectForKey:key]; break;
                    case PDPrimitiveVoid:
                    default: continue;
                }

            } else if ([type isKindOfClass:PDEnum.class]) {
                value = @([coder decodeIntegerForKey:key]);

            } else {
                value = [coder decodeObjectForKey:key];
            }

            if (value) {
                [self setValue:value forKey:key];
            }
        }
    }

    return self;
}

- (void)encodeWithCoder:(NSCoder *)coder {
    id cls = [self class];
    NSDictionary *properties = [cls properties];

    for (NSString *key in [properties allKeys]) {
        id type = properties[key];
        id value = [self valueForKey:key];
        if (!value) {
            continue;
        }

        if ([type isKindOfClass:NSNumber.class]) {
            PDPrimitive primitive = (PDPrimitive) [type intValue];
            NSNumber *number = value;

            switch (primitive) {
                case PDPrimitiveBool: [coder encodeBool:number.boolValue forKey:key]; break;
                case PDPrimitiveInt16: [coder encodeInt32:number.shortValue forKey:key]; break;
                case PDPrimitiveInt32: [coder encodeInt:number.intValue forKey:key]; break;
                case PDPrimitiveInt64: [coder encodeInt64:number.longLongValue forKey:key]; break;
                case PDPrimitiveFloat: [coder encodeFloat:number.floatValue forKey:key]; break;
                case PDPrimitiveDouble: [coder encodeDouble:number.doubleValue forKey:key]; break;
                case PDPrimitiveString:
                case PDPrimitiveDate: [coder encodeObject:value forKey:key]; break;
                case PDPrimitiveVoid:
                default: continue;
            }

        } else if ([type isKindOfClass:PDEnum.class]) {
            NSNumber *number = value;
            [coder encodeInt32:number.intValue forKey:key];

        } else {
            [coder encodeObject:value forKey:key];
        }
    }
}

- (BOOL)isEqual:(id)other {
    return [self isEqualToStruct:other];
}

- (BOOL)isEqualToStruct:(PDStruct *)other {
    if (other == self)
        return YES;
    if (!other || ![[other class] isEqual:[self class]])
        return NO;

    id cls = [self class];
    NSDictionary *properties = [cls properties];
    for (NSString *key in [properties allKeys]) {
        id value0 = [self valueForKey:key];
        id value1 = [other valueForKey:key];

        if (value0 == nil && value1 == nil) {
            continue;
        }

        if (![value0 isEqual:value1]) {
            return NO;
        }
    }

    return YES;
}

- (NSUInteger)hash {
    NSUInteger hash = 31u;

    id cls = [self class];
    NSDictionary *properties = [cls properties];
    for (NSString *key in [properties allKeys]) {
        id value = [self valueForKey:key];
        hash = hash * 31u + [value hash];
    }

    return hash;
}

- (id)copyWithZone:(NSZone *)zone {
    id cls = [self class];
    id copy = [cls allocWithZone:zone];
    NSDictionary *properties = [cls properties];

    for (NSString *key in [properties allKeys]) {
        id value0 = [self valueForKey:key];
        if (!value0) {
            continue;
        }

        id type = properties[key];
        id value1;

        if ([value0 isKindOfClass:NSArray.class]) {
            NSArray *array = value0;
            value1 = [[[array class] allocWithZone:zone] initWithArray:array copyItems:YES];

        } else if ([value0 isKindOfClass:NSSet.class]) {
            NSSet *set = value0;
            value1 = [[[set class] allocWithZone:zone] initWithSet:set copyItems:YES];

        } else if ([value1 isKindOfClass:NSDictionary.class]) {
            NSDictionary *dict = value0;
            value1 = [[[dict class] allocWithZone:zone] initWithDictionary:dict copyItems:YES];

        } else if ([type conformsToProtocol:@protocol(NSCopying)]) {
            value1 = [value0 copyWithZone:zone];

        } else {
            value1 = value0;
        }

        [copy setValue:value1 forKey:key];
    }

    return copy;
}
@end
