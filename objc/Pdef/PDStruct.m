//
// Created by Ivan Korobkov on 01.05.14.
// Copyright (c) 2014 io.pdef. All rights reserved.
//

#import <objc/runtime.h>
#import "PDStruct.h"
#import "PDTypes.h"
#import "PDJson.h"


@implementation PDStruct

// Implement in a subclass.
+ (NSDictionary *)pdef_properties {
    return nil;
}

- (NSDictionary *)pdef_properties {
    return [self.class pdef_properties];
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
    return [PDJson serializeStruct:self options:0 error:error];
}

- (NSData *)toJsonWithOptions:(NSJSONWritingOptions)options error:(NSError **)error {
    return [PDJson serializeStruct:self options:options error:error];
}

- (id)initWithCoder:(NSCoder *)coder {
    self = [super init];
    if (!self) return nil;

    NSDictionary *properties = self.pdef_properties;
    for (NSString *key in properties) {
        id type = properties[key];
        PDType type0 = PDTypeForType(type);

        id value;
        switch (type0) {
            case PDTypeBool:
                value = @([coder decodeBoolForKey:key]);
                break;
            case PDTypeInt16:
                value = @([coder decodeInt32ForKey:key]);
                break;
            case PDTypeInt32:
                value = @([coder decodeInt32ForKey:key]);
                break;
            case PDTypeInt64:
                value = @([coder decodeInt64ForKey:key]);
                break;
            case PDTypeFloat:
                value = @([coder decodeFloatForKey:key]);
                break;
            case PDTypeDouble:
                value = @([coder decodeDoubleForKey:key]);
                break;
            case PDTypeEnum: {
                value = @([coder decodeIntegerForKey:key]);
                break;
            }
            default: {
                value = [coder decodeObjectForKey:key];
                break;
            }
        }

        if (!value) continue;
        [self setValue:value forKey:key];
    }

    return self;
}

- (void)encodeWithCoder:(NSCoder *)coder {
    NSDictionary *properties = self.pdef_properties;
    for (NSString *key in properties) {
        id value = [self valueForKey:key];
        if (!value) continue;

        id type = properties[key];
        PDType type0 = PDTypeForType(type);

        switch (type0) {
            case PDTypeBool:
                [coder encodeBool:[value boolValue] forKey:key];
                break;
            case PDTypeInt16:
                [coder encodeInt32:[value shortValue] forKey:key];
                break;
            case PDTypeInt32:
                [coder encodeInt:[value intValue] forKey:key];
                break;
            case PDTypeInt64:
                [coder encodeInt64:[value longLongValue] forKey:key];
                break;
            case PDTypeFloat:
                [coder encodeFloat:[value floatValue] forKey:key];
                break;
            case PDTypeDouble:
                [coder encodeDouble:[value doubleValue] forKey:key];
                break;
            case PDTypeEnum: {
                [coder encodeInteger:[value integerValue] forKey:key];
                break;
            }
            default: {
                [coder encodeObject:value forKey:key];
                break;
            }
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

    NSDictionary *properties = self.pdef_properties;
    for (NSString *key in properties) {
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

    NSDictionary *properties = self.pdef_properties;
    for (NSString *key in properties) {
        id value = [self valueForKey:key];
        hash = hash * 31u + [value hash];
    }

    return hash;
}

- (id)copyWithZone:(NSZone *)zone {
    id cls = [self class];
    id copy = [cls allocWithZone:zone];
    NSDictionary *properties = self.pdef_properties;

    for (NSString *key in properties) {
        id value0 = [self valueForKey:key];
        if (!value0) continue;

        id type = properties[key];
        PDType type0 = PDTypeForType(type);

        id value1;
        switch (type0) {
            case PDTypeList: {
                NSArray *array = value0;
                value1 = [[[array class] allocWithZone:zone] initWithArray:array copyItems:YES];
                break;
            }
            case PDTypeSet: {
                NSSet *set = value0;
                value1 = [[[set class] allocWithZone:zone] initWithSet:set copyItems:YES];
                break;
            }
            case PDTypeMap: {
                NSDictionary *dict = value0;
                value1 = [[[dict class] allocWithZone:zone] initWithDictionary:dict copyItems:YES];
                break;
            }
            case PDTypeString:
            case PDTypeDate:
            case PDTypeStruct: {
                value1 = [value0 copyWithZone:zone];
                break;
            }
            default: {
                value1 = value0;
            }
        }

        [copy setValue:value1 forKey:key];
    }

    return copy;
}
@end
