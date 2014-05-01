//
// Created by Ivan Korobkov on 01.05.14.
// Copyright (c) 2014 io.pdef. All rights reserved.
//

#import <objc/runtime.h>
#import "PDJson.h"
#import "PDTypes.h"
#import "PDEnum.h"
#import "PDStruct.h"
#import "PDError.h"


@implementation PDJson
static NSDateFormatter *formatter;

+ (void)initialize {
    formatter = [[NSDateFormatter alloc] init];
    formatter.timeZone = [NSTimeZone timeZoneWithName:@"UTC"];
    formatter.locale = [[NSLocale alloc] initWithLocaleIdentifier:@"en_US_POSIX"];
    formatter.dateFormat = @"yyyy-MM-dd'T'HH:mm:ss'Z'";
}

#pragma mark - Serialize

+ (NSData *)serialize:(id)object type:(id)type options:(NSJSONWritingOptions)options
                error:(NSError **)error {
    id jsonObject = [self _serializeObject:object type:type error:error];
    if (*error) {
        return nil;
    }

    // A json object.
    if ([jsonObject isKindOfClass:NSArray.class] || [jsonObject isKindOfClass:NSDictionary.class]) {
        return [NSJSONSerialization dataWithJSONObject:jsonObject
            options:options error:error];
    }

    // A fragment object.
    // Wrap it into an array, convert into JSON data, and strip the array start/end.
    NSArray *array = @[jsonObject];
    NSData *data = [NSJSONSerialization dataWithJSONObject:array options:0 error:error];
    if (*error) {
        return nil;
    }

    NSString *s = [[NSString alloc] initWithData:data encoding:NSUTF8StringEncoding];
    NSString *result = [s substringWithRange:(NSRange) {1, s.length - 2}];
    return [result dataUsingEncoding:NSUTF8StringEncoding];
}

+ (id)_serializeObject:(id)object type:(id)type error:(NSError **)error {
    NSParameterAssert(type != nil);
    if (!object || [object isEqual:[NSNull null]]) {
        return [NSNull null];
    }

    if ([type isKindOfClass:NSNumber.class]) {
        PDType type0 = (PDType) ((NSNumber *) type).intValue;
        switch (type0) {
            case PDTypeString:return [self _serializeString:object error:error];
            case PDTypeDate:return [self _serializeDate:object error:error];
            default: return [self _serializeNumber:object type:type0 error:error];
        }

    } else if ([type isKindOfClass:PDList.class]) {
        return [self _serializeList:object type:type error:error];

    } else if ([type isKindOfClass:PDSet.class]) {
        return [self _serializeSet:object type:type error:error];

    } else if ([type isKindOfClass:PDMap.class]) {
        return [self _serializeMap:object type:type error:error];

    } else if ([self isClass:type] && [type isSubclassOfClass:PDEnum.class]) {
        return [self _serializeEnum:object type:type error:error];

    } else if ([self isClass:type] && [type isSubclassOfClass:PDStruct.class]) {
        return [self _serializeStruct:object type:type error:error];
    }

    NSString *msg = [NSString stringWithFormat:@"Cannot serialize an object of type '%@'", type];
    *error = [self error:msg];
    return nil;
}

+ (id)_serializeString:(id)object error:(NSError **)error {
    if (![object isKindOfClass:[NSString class]]) {
        NSString *msg = [NSString stringWithFormat:@"Cannot serialize a string from '%@'", object];
        *error = [self error:msg];
        return nil;
    }

    return object;
}

+ (id)_serializeNumber:(id)object type:(PDType)type error:(NSError **)error {
    if (![object isKindOfClass:[NSNumber class]]) {
        NSString *msg = [NSString stringWithFormat:@"Cannot serialize a number from '%@'", object];
        *error = [self error:msg];
        return nil;
    }

    return object;
}

+ (NSString *)_serializeDate:(id)object error:(NSError **)error {
    if (![object isKindOfClass:NSDate.class]) {
        NSString *msg = [NSString stringWithFormat:@"Cannot serialize a date from '%@'", object];
        *error = [self error:msg];
        return nil;
    }

    @synchronized (self) {
        return [formatter stringFromDate:object];
    }
}

+ (id)_serializeList:(id)object type:(PDList *)type error:(NSError **)error {
    if (![object isKindOfClass:NSArray.class]) {
        NSString *msg = [NSString stringWithFormat:@"Cannot serialize a list from '%@'", object];
        *error = [self error:msg];
        return nil;
    }

    NSArray *list = object;
    NSMutableArray *result = [[NSMutableArray alloc] init];

    id itemType = type.item;
    for (id item in list) {
        id serialized = [self _serializeObject:item type:itemType error:error];
        if (*error) {
            return nil;
        }

        serialized = serialized != nil ? serialized : [NSNull null];
        [result addObject:serialized];
    }

    return result;
}

+ (id)_serializeSet:(id)object type:(PDSet *)type error:(NSError **)error {
    if (![object isKindOfClass:NSSet.class]) {
        NSString *msg = [NSString stringWithFormat:@"Cannot serialize a set from '%@'", object];
        *error = [self error:msg];
        return nil;
    }

    NSSet *set = object;
    NSMutableArray *result = [[NSMutableArray alloc] init];

    id itemType = type.item;
    for (id element in set) {
        id serialized = [self _serializeObject:element type:itemType error:error];
        if (*error) {
            return nil;
        }

        serialized = serialized != nil ? serialized : [NSNull null];
        [result addObject:serialized];
    }

    return result;
}

+ (id)_serializeMap:(id)object type:(PDMap *)type error:(NSError **)error {
    if (![object isKindOfClass:NSDictionary.class]) {
        NSString *msg = [NSString stringWithFormat:@"Cannot serialize a map from '%@'", object];
        *error = [self error:msg];
        return nil;
    }

    id keyType = type.key;
    id valueType = type.value;

    NSDictionary *dict = object;
    NSMutableDictionary *result = [[NSMutableDictionary alloc] init];

    for (id key in dict) {
        id value = [dict objectForKey:key];
        id serializedKey = [self _serializeKey:key type:keyType error:error];
        if (*error) {
            return nil;
        }

        id serializeValue = [self _serializeObject:value type:valueType error:error];
        if (*error) {
            return nil;
        }

        serializeValue = serializeValue != nil ? serializeValue : [NSNull null];
        [result setObject:serializeValue forKey:serializedKey];
    }

    return result;
}

+ (NSString *)_serializeKey:(id)key type:(id)type error:(NSError **)error {
    if ([type isKindOfClass:NSNumber.class]) {
        PDType type0 = (PDType) ((NSNumber *) type).intValue;
        switch (type0) {
            case PDTypeString:return key;
            case PDTypeDate:return [self _serializeDate:key error:error];
            case PDTypeBool: {
                if (![key isKindOfClass:NSNumber.class]) {
                    NSString *msg = [NSString
                        stringWithFormat:@"Cannot serialize a bool from '%@'", key];
                    *error = [self error:msg];
                    return nil;
                }

                BOOL bool0 = ((NSNumber *) key).boolValue;
                return bool0 ? @"true" : @"false";
            }
            default: {
                if (![key isKindOfClass:NSNumber.class]) {
                    NSString *msg = [NSString
                        stringWithFormat:@"Cannot serialize a number from '%@'", key];
                    *error = [self error:msg];
                    return nil;
                }

                NSNumber *number = key;
                return [number stringValue];
            }
        }

    } else if ([type isKindOfClass:PDEnum.class]) {
        return [self _serializeEnum:key type:type error:error];
    }

    NSString *msg = [NSString stringWithFormat:@"Cannot parse a map key from '%@'", key];
    *error = [self error:msg];
    return nil;
}

+ (NSString *)_serializeEnum:(id)object type:(Class)type error:(NSError **)error {
    NSParameterAssert(type != nil);
    NSParameterAssert([type isSubclassOfClass:PDEnum.class]);

    if (![object isKindOfClass:NSNumber.class]) {
        NSString *msg = [NSString stringWithFormat:@"Cannot serialize an enum from '%@'", object];
        *error = [self error:msg];
        return nil;
    }

    NSInteger value = ((NSNumber *) object).integerValue;
    return [type nameForEnumValue:value];
}

+ (id)_serializeStruct:(id)object type:(Class)type error:(NSError **)error {
    if (![object isKindOfClass:type]) {
        NSString *msg = [NSString
            stringWithFormat:@"Cannot serialize a struct as '%@' from '%@'", type, object];
        *error = [self error:msg];
        return nil;
    }

    NSDictionary *properties = [type properties];
    NSMutableDictionary *result = [[NSMutableDictionary alloc] init];

    for (NSString *key in [properties allKeys]) {
        id value = [object valueForKey:key];
        if (!value) {
            continue;
        }

        id propType = properties[key];
        id serialized = [self _serializeObject:value type:propType error:error];
        if (*error) {
            return nil;
        }

        // Skip null keys.
        if (!serialized) {
            continue;
        }

        result[key] = serialized;
    }

    return result;
}

#pragma mark - Parse

+ (id)parse:(NSData *)data type:(id)type error:(NSError **)error {
    id object = [NSJSONSerialization JSONObjectWithData:data
        options:NSJSONReadingAllowFragments error:error];
    if (*error) {
        return nil;
    }

    return [self _parseObject:object type:type error:error];
}

+ (id)_parseObject:(id)object type:(id)type error:(NSError **)error {
    NSParameterAssert(type != nil);
    if (!object || object == [NSNull null]) {
        return nil;
    }

    if ([type isKindOfClass:NSNumber.class]) {
        PDType type0 = (PDType) ((NSNumber *) type).intValue;
        switch (type0) {
            case PDTypeString:return [self _parseString:object error:error];
            case PDTypeDate:return [self _parseDate:object error:error];
            default: return [self _parseNumber:object type:type0 error:error];
        }

    } else if ([type isKindOfClass:PDList.class]) {
        return [self _parseList:object type:type error:error];

    } else if ([type isKindOfClass:PDSet.class]) {
        return [self _parseSet:object type:type error:error];

    } else if ([type isKindOfClass:PDMap.class]) {
        return [self _parseMap:object type:type error:error];

    } else if ([self isClass:type] && [type isSubclassOfClass:PDEnum.class]) {
        return [self _parseEnum:object type:type error:error];

    } else if ([self isClass:type] && [type isSubclassOfClass:PDStruct.class]) {
        return [self _parseStruct:object type:type error:error];
    }

    NSString *msg = [NSString stringWithFormat:@"Cannot parse a type '%@' from '%@'", type, object];
    *error = [self error:msg];
    return nil;
}

+ (id)_parseString:(id)object error:(NSError **)error {
    if (![object isKindOfClass:[NSString class]]) {
        NSString *msg = [NSString stringWithFormat:@"Cannot parse a string from '%@'", object];
        *error = [self error:msg];
        return nil;
    }

    return object;
}

+ (id)_parseNumber:(id)object type:(PDType)type error:(NSError **)error {
    if (![object isKindOfClass:[NSNumber class]]) {
        NSString *msg = [NSString stringWithFormat:@"Cannot parse a number from '%@'", object];
        *error = [self error:msg];
        return nil;
    }

    NSNumber *number = object;
    switch (type) {
        case PDTypeBool:return @([number boolValue]);
        case PDTypeInt16:return @([number shortValue]);
        case PDTypeInt32:return @([number intValue]);
        case PDTypeInt64:return @([number longLongValue]);
        case PDTypeFloat:return @([number floatValue]);
        case PDTypeDouble:return @([number doubleValue]);
        default: return nil;
    }
}

+ (id)_parseDate:(id)object error:(NSError **)error {
    if (![object isKindOfClass:NSString.class]) {
        NSString *msg = [NSString stringWithFormat:@"Cannot parse a date from '%@'", object];
        *error = [self error:msg];
        return nil;
    }

    NSDate *date;
    @synchronized (self) {
        date = [formatter dateFromString:object];
    }

    if (!date) {
        NSString *msg = [NSString stringWithFormat:@"Failed to parse a date from '%@'", object];
        *error = [self error:msg];
        return nil;
    }

    return date;
}

+ (id)_parseList:(id)object type:(PDList *)type error:(NSError **)error {
    if (![object isKindOfClass:NSArray.class]) {
        NSString *msg = [NSString stringWithFormat:@"Cannot parse a list from '%@'", object];
        *error = [self error:msg];
        return nil;
    }

    NSArray *list = object;
    NSMutableArray *result = [[NSMutableArray alloc] init];

    id itemType = type.item;
    for (id item in list) {
        id parsed = [self _parseObject:item type:itemType error:error];
        if (*error) {
            return nil;
        }

        parsed = parsed != nil ? parsed : [NSNull null];
        [result addObject:parsed];
    }

    return result;
}

+ (id)_parseSet:(id)object type:(PDSet *)type error:(NSError **)error {
    if (![object isKindOfClass:NSArray.class]) {
        NSString *msg = [NSString stringWithFormat:@"Cannot parse a set from '%@'", object];
        *error = [self error:msg];
        return nil;
    }

    NSArray *list = object;
    NSMutableSet *result = [[NSMutableSet alloc] init];

    id itemType = type.item;
    for (id element in list) {
        id parsed = [self _parseObject:element type:itemType error:error];
        if (*error) {
            return nil;
        }

        parsed = parsed != nil ? parsed : [NSNull null];
        [result addObject:parsed];
    }

    return result;
}

+ (id)_parseMap:(id)object type:(PDMap *)type error:(NSError **)error {
    if (![object isKindOfClass:NSDictionary.class]) {
        NSString *msg = [NSString stringWithFormat:@"Cannot parse a map from '%@'", object];
        *error = [self error:msg];
        return nil;
    }

    id keyType = type.key;
    id valueType = type.value;

    NSDictionary *dict = object;
    NSMutableDictionary *result = [[NSMutableDictionary alloc] init];

    for (id key in dict) {
        id value = [dict objectForKey:key];
        id parsedKey = [self _parseKey:key type:keyType error:error];
        if (*error) {
            return nil;
        }

        id parsedValue = [self _parseObject:value type:valueType error:error];
        if (*error) {
            return nil;
        }

        parsedValue = parsedValue != nil ? parsedValue : [NSNull null];
        [result setObject:parsedValue forKey:parsedKey];
    }

    return result;
}

+ (id)_parseKey:(NSString *)key type:(id)type error:(NSError **)error {
    if ([type isKindOfClass:NSNumber.class]) {
        PDType type0 = (PDType) ((NSNumber *) type).intValue;
        switch (type0) {
            case PDTypeBool: {
                if ([@"true" isEqualToString:key]) return @YES;
                if ([@"false" isEqualToString:key]) return @NO;
                return @([key boolValue]);
            }
            case PDTypeInt16:return @([key intValue]);
            case PDTypeInt32:return @([key intValue]);
            case PDTypeInt64:return @([key longLongValue]);
            case PDTypeFloat:return @([key floatValue]);
            case PDTypeDouble:return @([key doubleValue]);
            case PDTypeString:return key;
            case PDTypeDate:return [self _parseDate:key error:error];
            default: break;
        }

    } else if ([type isKindOfClass:PDEnum.class]) {
        return [self _parseEnum:key type:type error:error];
    }

    NSString *msg = [NSString stringWithFormat:@"Cannot parse a map key from '%@'", key];
    *error = [self error:msg];
    return nil;
}

+ (id)_parseEnum:(id)object type:(Class)type error:(NSError **)error {
    if (![object isKindOfClass:NSString.class]) {
        NSString *msg = [NSString stringWithFormat:@"Cannot parse an enum from '%@'", object];
        *error = [self error:msg];
        return nil;
    }

    NSString *name = object;
    NSInteger value = [type enumValueForName:name];
    return @(value);
}

+ (id)_parseStruct:(id)object type:(Class)type error:(NSError **)error {
    NSParameterAssert([type isSubclassOfClass:PDStruct.class]);

    if (![object isKindOfClass:NSDictionary.class]) {
        NSString *msg = [NSString stringWithFormat:@"Cannot parse a struct from '%@'", object];
        *error = [self error:msg];
        return nil;
    }

    PDStruct *result = [[type alloc] init];
    NSDictionary *dict = object;
    NSDictionary *properties = [type properties];

    for (NSString *key in [properties allKeys]) {
        id value = dict[key];
        if (!value) {
            continue;
        }

        id propType = properties[key];
        id parsed = [self _parseObject:value type:propType error:error];
        if (*error) {
            return nil;
        }

        [result setValue:parsed forKey:key];
    }

    return result;
}

+ (BOOL)isClass:(id)type {
    return class_isMetaClass(object_getClass(type));
}

+ (NSError *)error:(NSString *)msg {
    msg = msg ? msg : @"JSON serialization/deserialization error";
    return [NSError errorWithDomain:PDErrorDomain code:PDErrorJsonError userInfo:@{
        NSLocalizedDescriptionKey : msg
    }];
}
@end
