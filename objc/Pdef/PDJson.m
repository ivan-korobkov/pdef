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


#define AssertTrue(expr, error, msg, ...) \
    if(!expr) { \
        NSString *msg0 = msg ? msg : @"JSON serialization/deserialization error"; \
        NSString *message = [NSString stringWithFormat:msg0, ## __VA_ARGS__]; \
        *error = [NSError errorWithDomain:PDErrorDomain code:PDErrorJsonError \
            userInfo:@{NSLocalizedDescriptionKey : message}]; \
        return nil; \
    }


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

+ (NSData *)serializeStruct:(PDStruct *)aStruct options:(NSJSONWritingOptions)options
                      error:(NSError **)error {
    return [self serialize:aStruct type:aStruct.class options:options error:error];
}

+ (id)_serializeObject:(id)object type:(id)type error:(NSError **)error {
    NSParameterAssert(type != nil);
    if (!object || [object isEqual:[NSNull null]]) {
        return [NSNull null];
    }

    PDType type0 = PDTypeForType(type);
    switch (type0) {
        case PDTypeBool:
        case PDTypeInt16:
        case PDTypeInt32:
        case PDTypeInt64:
        case PDTypeFloat:
        case PDTypeDouble:
            return [self _serializeNumber:object error:error];

        case PDTypeString:return [self _serializeString:object error:error];
        case PDTypeDate:return [self _serializeDate:object error:error];
        case PDTypeList: return [self _serializeList:object type:type error:error];
        case PDTypeSet:return [self _serializeSet:object type:type error:error];
        case PDTypeMap: return [self _serializeMap:object type:type error:error];
        case PDTypeEnum: return [self _serializeEnum:object type:type error:error];
        case PDTypeStruct: return [self _serializeStruct:object type:type error:error];

        default: {
            NSString *msg = [NSString stringWithFormat:@"Cannot serialize a type '%@'", type];
            *error = [self error:msg];
            return nil;
        }
    }
}

+ (id)_serializeString:(id)object error:(NSError **)error {
    AssertTrue([object isKindOfClass:[NSString class]], error,
    @"Cannot serialize a string from '%@'", [object class])

    return object;
}

+ (id)_serializeNumber:(id)object error:(NSError **)error {
    AssertTrue([object isKindOfClass:[NSNumber class]], error,
    @"Cannot serialize a number from '%@'", [object class])

    return object;
}

+ (NSString *)_serializeDate:(id)object error:(NSError **)error {
    AssertTrue([object isKindOfClass:NSDate.class], error,
    @"Cannot serialize a date from '%@'", [object class])

    @synchronized (self) {
        return [formatter stringFromDate:object];
    }
}

+ (id)_serializeList:(id)object type:(PDList *)type error:(NSError **)error {
    AssertTrue([object isKindOfClass:NSArray.class], error,
    @"Cannot serialize a list from '%@'", [object class])

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
    AssertTrue([object isKindOfClass:NSSet.class], error,
    @"Cannot serialize a set from '%@'", [object class])

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
    AssertTrue([object isKindOfClass:NSDictionary.class], error,
    @"Cannot serialize a map from '%@'", [object class])

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
    PDType type0 = PDTypeForType(type);
    switch (type0) {
        case PDTypeBool: {
            AssertTrue([key isKindOfClass:NSNumber.class], error,
            @"Cannot serialize a bool from '%@'", [key class])

            BOOL bool0 = ((NSNumber *) key).boolValue;
            return bool0 ? @"true" : @"false";
        }

        case PDTypeInt16:
        case PDTypeInt32:
        case PDTypeInt64:
        case PDTypeFloat:
        case PDTypeDouble: {
            AssertTrue([key isKindOfClass:NSNumber.class], error,
            @"Cannot serialize a number from '%@'", [key class])

            NSNumber *number = key;
            return [number stringValue];
        }

        case PDTypeString:return key;
        case PDTypeDate:return [self _serializeDate:key error:error];
        case PDTypeEnum: return [self _serializeEnum:key type:type error:error];

        default: {
            NSString *msg = [NSString
                stringWithFormat:@"Cannot parse a map key from '%@'", [key class]];
            *error = [self error:msg];
            return nil;
        }
    }
}

+ (NSString *)_serializeEnum:(id)object type:(Class)type error:(NSError **)error {
    AssertTrue([object isKindOfClass:NSNumber.class], error,
    @"Cannot serialize an enum from '%@'", [object class])

    NSInteger value = ((NSNumber *) object).integerValue;
    return [type nameForEnumValue:value];
}

+ (id)_serializeStruct:(id)object type:(Class)type error:(NSError **)error {
    AssertTrue([object isKindOfClass:type], error,
    @"Cannot serialize a struct as '%@' from '%@'", type, [object class])

    NSDictionary *properties = [type pdef_properties];
    NSMutableDictionary *result = [[NSMutableDictionary alloc] init];

    for (NSString *key in properties) {
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

+ (id)parseJson:(NSData *)data type:(id)type error:(NSError **)error {
    id object = [NSJSONSerialization JSONObjectWithData:data
        options:NSJSONReadingAllowFragments error:error];
    if (*error) {
        return nil;
    }

    return [self _parseObject:object type:type error:error];
}

+ (void)parseJson:(NSData *)data intoStruct:(PDStruct *)aStruct error:(NSError **)error {
    id object = [NSJSONSerialization JSONObjectWithData:data
        options:NSJSONReadingAllowFragments error:error];
    if (*error) {
        return;
    }

    [self _parseStructInto:aStruct object:object error:error];
}

+ (id)parseJsonObject:(id)object type:(id)type error:(NSError **)error {
    return [self _parseObject:object type:type error:error];
}

+ (id)_parseObject:(id)object type:(id)type error:(NSError **)error {
    NSParameterAssert(type != nil);
    if (!object || object == [NSNull null]) {
        return nil;
    }

    PDType type0 = PDTypeForType(type);
    switch (type0) {
        case PDTypeBool:
        case PDTypeInt16:
        case PDTypeInt32:
        case PDTypeInt64:
        case PDTypeFloat:
        case PDTypeDouble: return [self _parseNumber:object type:type0 error:error];

        case PDTypeString: return [self _parseString:object error:error];
        case PDTypeDate: return [self _parseDate:object error:error];
        case PDTypeList: return [self _parseList:object type:type error:error];
        case PDTypeSet: return [self _parseSet:object type:type error:error];
        case PDTypeMap: return [self _parseMap:object type:type error:error];
        case PDTypeEnum: return [self _parseEnum:object type:type error:error];
        case PDTypeStruct: return [self _parseStruct:object type:type error:error];

        default: {
            NSString *msg = [NSString
                stringWithFormat:@"Cannot parse a type '%@' from '%@'", type, [object class]];
            *error = [self error:msg];
            return nil;
        }
    }
}

+ (id)_parseString:(id)object error:(NSError **)error {
    AssertTrue([object isKindOfClass:[NSString class]], error,
    @"Cannot parse a string from '%@'", [object class])

    return object;
}

+ (id)_parseNumber:(id)object type:(PDType)type error:(NSError **)error {
    AssertTrue([object isKindOfClass:[NSNumber class]], error,
    @"Cannot parse a number from '%@'", [object class])

    switch (type) {
        case PDTypeBool:return @([object boolValue]);
        case PDTypeInt16:return @([object shortValue]);
        case PDTypeInt32:return @([object intValue]);
        case PDTypeInt64:return @([object longLongValue]);
        case PDTypeFloat:return @([object floatValue]);
        case PDTypeDouble:return @([object doubleValue]);
        default: return nil;
    }
}

+ (id)_parseDate:(id)object error:(NSError **)error {
    AssertTrue([object isKindOfClass:NSString.class], error,
    @"Cannot parse a date from '%@'", [object class])

    NSDate *date;
    @synchronized (self) {
        date = [formatter dateFromString:object];
    }

    AssertTrue(date, error, @"Failed to parse a date from '%@'", [object class])
    return date;
}

+ (id)_parseList:(id)object type:(PDList *)type error:(NSError **)error {
    AssertTrue([object isKindOfClass:NSArray.class], error,
    @"Cannot parse a list from '%@'", [object class])

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
    AssertTrue([object isKindOfClass:NSArray.class], error,
    @"Cannot parse a set from '%@'", [object class])

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
    AssertTrue([object isKindOfClass:NSDictionary.class], error,
    @"Cannot parse a map from '%@'", [object class])

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
    PDType type0 = PDTypeForType(type);
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
        case PDTypeEnum: return [self _parseEnum:key type:type error:error];

        default: {
            NSString *msg = [NSString
                stringWithFormat:@"Cannot parse a map key from '%@'", [key class]];
            *error = [self error:msg];
            return nil;
        }
    }
}

+ (id)_parseEnum:(id)object type:(Class)type error:(NSError **)error {
    AssertTrue([object isKindOfClass:NSString.class], error,
    @"Cannot parse an enum from '%@'", [object class])

    NSString *name = object;
    NSInteger value = [type enumValueForName:name];
    return @(value);
}

+ (id)_parseStruct:(id)object type:(Class)type error:(NSError **)error {
    NSParameterAssert([type isSubclassOfClass:PDStruct.class]);

    PDStruct *result = [[type alloc] init];
    [self _parseStructInto:result object:object error:error];
    return *error ? nil : result;

}

// Modifies and returns the same struct.
+ (id)_parseStructInto:(PDStruct *)aStruct object:(id)object error:(NSError **)error {
    AssertTrue([object isKindOfClass:NSDictionary.class], error,
    @"Cannot parse a struct from '%@'", [object class])

    Class cls = aStruct.class;
    NSDictionary *dict = object;
    NSDictionary *properties = [cls pdef_properties];

    for (NSString *key in properties) {
        id value = dict[key];
        if (!value) {
            continue;
        }

        id propType = properties[key];
        id parsed = [self _parseObject:value type:propType error:error];
        if (*error) {
            return nil;
        }

        [aStruct setValue:parsed forKey:key];
    }

    return aStruct;
}

+ (NSError *)error:(NSString *)msg {
    msg = msg ? msg : @"JSON serialization/deserialization error";
    return [NSError errorWithDomain:PDErrorDomain code:PDErrorJsonError userInfo:@{
        NSLocalizedDescriptionKey : msg
    }];
}
@end
